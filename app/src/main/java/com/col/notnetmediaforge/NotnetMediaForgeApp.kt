package com.col.notnetmediaforge

import android.app.Application
import com.col.notnetmediaforge.data.logging.CrashLogger
import com.col.notnetmediaforge.data.notification.NotificationHelper
import com.col.notnetmediaforge.di.AppContainer
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                YoutubeDL.getInstance().init(this@NotnetMediaForgeApp)
                FFmpeg.getInstance().init(this@NotnetMediaForgeApp)
            }.onSuccess {
                _engineState.value = EngineState.Ready
                maybeUpdateYtdlp()
            }.onFailure { e ->
                _engineState.value = EngineState.Error(e.message ?: "init failed")
            }
        }
    }

    /**
     * Actualiza el binario yt-dlp a la última versión estable, una vez cada
     * 24 h, sin bloquear nada. Un yt-dlp desactualizado es la causa más común
     * de análisis lentos o fallidos.
     */
    private fun maybeUpdateYtdlp() {
        val prefs = getSharedPreferences("engine", MODE_PRIVATE)
        val lastAttempt = prefs.getLong("ytdlp_update_ts", 0L)
        if (System.currentTimeMillis() - lastAttempt < 24L * 60 * 60 * 1000) return
        prefs.edit().putLong("ytdlp_update_ts", System.currentTimeMillis()).apply()
        runCatching {
            YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel.STABLE)
        }
    }
}
