package com.col.notnetmediaforge.data.repository

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import com.col.notnetmediaforge.data.model.FormatItem
import com.col.notnetmediaforge.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CancellationException

/**
 * Envoltorio de youtubedl-android / yt-dlp + FFmpeg.
 * Expone análisis de URLs y descarga con progreso y cancelación.
 */
class YoutubeDLRepository(private val context: Context) {

    /**
     * Inicializa el motor (yt-dlp + python + ffmpeg). Es idempotente.
     */
    @Throws(Exception::class)
    fun initialize() {
        YoutubeDL.getInstance().init(context)
    }

    /**
     * Obtiene la información de un medio y sus formatos disponibles.
     * Equivale a `yt-dlp --dump-json`.
     */
    suspend fun fetchMedia(url: String): MediaItem = withContext(Dispatchers.IO) {
        val info = try {
            YoutubeDL.getInstance().getInfo(url.trim())
        } catch (e: InterruptedException) {
            throw CancellationException("Análisis interrumpido")
        }
        val title = info.title ?: info.fulltitle ?: "Sin título"
        val formats = buildFormats(info)
        MediaItem(
            id = info.id ?: url,
            title = title,
            thumbnail = info.thumbnail,
            durationSeconds = info.duration.toLong(),
            uploader = info.uploader,
            webpageUrl = info.webpageUrl ?: url.trim(),
            extractor = info.extractor,
            description = info.description,
            formats = formats
        )
    }

    private fun buildFormats(info: VideoInfo): List<FormatItem> {
        return (info.formats ?: emptyList())
            .map { it.toFormatItem() }
            .filter { it.hasVideo && it.height > 0 }
            .groupBy { it.height }
            .mapNotNull { (_, group) -> group.maxByOrNull { it.bitrate } }
            .sortedByDescending { it.height }
    }

    private fun VideoFormat.toFormatItem(): FormatItem {
        val hasVideo = !vcodec.isNullOrEmpty() && vcodec != "none"
        val hasAudio = !acodec.isNullOrEmpty() && acodec != "none"
        return FormatItem(
            formatId = formatId ?: "",
            ext = ext,
            height = height,
            width = width,
            fps = fps,
            videoCodec = vcodec,
            audioCodec = acodec,
            note = formatNote,
            fileSizeBytes = fileSize.takeIf { it > 0 } ?: fileSizeApproximate,
            bitrate = tbr,
            hasVideo = hasVideo,
            hasAudio = hasAudio
        )
    }

    /**
     * Ejecuta una descarga en segundo plano dentro del directorio [outputDir]
     * (un directorio privado por descarga). Devuelve el archivo resultante.
     *
     * @param processId identificador usado para cancelar la descarga.
     * @param onProgress callback (progreso 0..100, ETA en segundos, línea de estado).
     */
    suspend fun download(
        url: String,
        type: com.col.notnetmediaforge.data.model.DownloadType,
        qualityHeight: Int,
        audioQuality: String?,
        outputDir: File,
        processId: String,
        onProgress: (Float, Long, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url.trim())
        request.addOption("--no-mtime")
        request.addOption("-o", "${outputDir.absolutePath}/%(title)s.%(ext)s")

        when (type) {
            com.col.notnetmediaforge.data.model.DownloadType.VIDEO -> {
                val selector = if (qualityHeight > 0) {
                    "bv*[height<=$qualityHeight]+ba/b[height<=$qualityHeight]/b"
                } else {
                    "bv*+ba/b"
                }
                request.addOption("-f", selector)
                request.addOption("--merge-output-format", "mp4")
            }
            com.col.notnetmediaforge.data.model.DownloadType.AUDIO_MP3 -> {
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", audioQuality ?: "0")
                request.addOption("--embed-thumbnail")
                request.addOption("-f", "ba/b")
            }
        }

        try {
            YoutubeDL.getInstance().execute(request, processId) { progress, eta, line ->
                onProgress(progress, eta, line)
            }
        } catch (e: InterruptedException) {
            throw CancellationException("Descarga interrumpida")
        }

        val file = outputDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".part") && !it.name.startsWith(".") }
            ?.maxByOrNull { it.lastModified() }
            ?: throw IllegalStateException("No se encontró el archivo descargado")

        file
    }

    /**
     * Cancela una descarga activa identificada por [processId].
     */
    fun cancel(processId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
    }
}
