package com.col.notnetmediaforge

import android.app.Application
import android.os.Environment
import android.util.Log
import com.col.notnetmediaforge.data.logging.CrashLogger
import com.col.notnetmediaforge.data.notification.NotificationHelper
import com.col.notnetmediaforge.di.AppContainer
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Estado del motor de descarga (yt-dlp + python + ffmpeg). */
sealed interface EngineState {
    data object Initializing : EngineState
    data object Ready : EngineState
    data class Error(val message: String) : EngineState
}

/**
 * Clase Application: inicializa el contenedor de dependencias, el canal de
 * notificaciones y el motor yt-dlp + FFmpeg, exponiendo su estado para que
 * la UI no lance procesos antes de que el motor esté listo.
 */
class NotnetMediaForgeApp : Application() {

    lateinit var container: AppContainer
        private set

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Initializing)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        cleanupInterruptedDownloads()

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                restoreBundledYtdlp()
                YoutubeDL.getInstance().init(this@NotnetMediaForgeApp)
                FFmpeg.getInstance().init(this@NotnetMediaForgeApp)
            }.onSuccess {
                probeYtdlp()
                _engineState.value = EngineState.Ready
            }.onFailure { e ->
                _engineState.value = EngineState.Error(e.message ?: "init failed")
            }
        }
    }

    /**
     * El update STABLE de esta librería descarga el asset `yt-dlp` del repo
     * yt-dlp/yt-dlp, que es un binario para Linux x86_64 y NO funciona bajo
     * libpython.so en Android/arm64 (rompe las descargas con errores tipo
     * "'...so' is not a valid URL"). Por eso el auto-update está desactivado:
     * en cada arranque se restaura el binario yt-dlp embebido en el APK, que
     * es el único compatible con el runtime Android.
     */
    private fun restoreBundledYtdlp() {
        runCatching {
            val dir = File(File(noBackupFilesDir, YoutubeDL.baseName), YoutubeDL.ytdlpDirName)
            if (dir.exists()) dir.deleteRecursively()
            YoutubeDL.getInstance().init_ytdlp(this, dir)
            Log.i("Engine", "yt-dlp restaurado desde el bundle del APK")
        }
    }

    /**
     * Verifica el binario yt-dlp realmente presente en el dispositivo y lo
     * ejecuta con --version por el MISMO camino que usan las descargas.
     * El resultado queda en `files/engine_diag.log` para diagnóstico.
     */
    private fun probeYtdlp() {
        val diag = runCatching {
            val dir = File(File(noBackupFilesDir, YoutubeDL.baseName), YoutubeDL.ytdlpDirName)
            val bin = File(dir, YoutubeDL.ytdlpBin)
            val header = runCatching {
                if (bin.exists()) {
                    val b = ByteArray(8)
                    java.io.FileInputStream(bin).use { it.read(b) }
                    val isElf = b.size >= 4 && b[0] == 0x7F.toByte() &&
                        b[1] == 'E'.code.toByte() && b[2] == 'L'.code.toByte() && b[3] == 'F'.code.toByte()
                    val isZipapp = b.size >= 2 && b[0] == '#'.code.toByte() && b[1] == '!'.code.toByte()
                    "exists size=${bin.length()} elf=$isElf zipapp=$isZipapp"
                } else "missing"
            }.getOrElse { "unreadable: ${it.message}" }

            val versionRun = runCatching {
                val req = YoutubeDLRequest("https://example.invalid")
                req.addOption("--version")
                val resp = YoutubeDL.getInstance().execute(req)
                "OK '${resp.out.trim()}'"
            }.getOrElse { "FAIL ${it::class.simpleName}: ${it.message?.take(300)}" }

            "bin=$header\nversion=$versionRun"
        }.getOrElse { "diag error: ${it.message}" }

        Log.i("Engine", "Diagnóstico yt-dlp:\n$diag")
        runCatching { File(filesDir, "engine_diag.log").writeText("$diag\n") }
    }

    /**
     * Borra restos de descargas interrumpidas (proceso muerto a media descarga):
     * el worker solo limpia su directorio temporal si termina el try completo,
     * así que lo que sobrevive a un force-stop se limpia aquí en el próximo arranque.
     */
    private fun cleanupInterruptedDownloads() {
        runCatching {
            val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@runCatching
            val now = System.currentTimeMillis()
            val staleMillis = 60 * 60 * 1000L
            dir.listFiles()?.forEach { child ->
                if (now - child.lastModified() > staleMillis) {
                    if (child.isDirectory) child.deleteRecursively() else child.delete()
                }
            }
        }
    }
}
