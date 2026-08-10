package com.col.notnetmediaforge.data.repository

import android.content.Context
import com.col.notnetmediaforge.data.model.DownloadType
import com.col.notnetmediaforge.data.model.FormatItem
import com.col.notnetmediaforge.data.model.MediaItem
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
     * Obtiene la información de un medio y sus formatos disponibles
     * (equivalente a `yt-dlp --dump-json`).
     *
     * Usa caché local: analizar la misma URL de nuevo es instantáneo
     * (hasta [CACHE_TTL_MILLIS]). Se ejecuta con un [processId] propio para
     * poder cancelarla o interrumpirla si tarda demasiado. Reintenta hasta
     * [MAX_ANALYZE_ATTEMPTS] veces si el fallo es de red/DNS, que suelen
     * ser transitorios.
     */
    suspend fun fetchMedia(url: String, processId: String): MediaItem = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        val cacheKey = cacheKey(trimmed)
        loadCached(cacheKey)?.let { return@withContext it }

        var lastError: Exception? = null
        repeat(MAX_ANALYZE_ATTEMPTS) { attempt ->
            try {
                val request = YoutubeDLRequest(trimmed)
                request.addOption("--dump-json")
                request.addOption("--no-playlist")
                request.addOption("--no-warnings")
                request.addOption("--force-ipv4")
                request.addOption("--socket-timeout", "15")
                request.addOption("--retries", "2")
                request.addOption("--extractor-retries", "1")
                request.addOption("--no-check-formats")
                val response = execute(request, processId)
                val media = parseVideoInfo(response.out, trimmed)
                cache(cacheKey, media)
                return@withContext media
            } catch (e: InterruptedException) {
                throw CancellationException("Análisis interrumpido")
            } catch (e: com.yausername.youtubedl_android.YoutubeDL.CanceledException) {
                throw CancellationException("Análisis cancelado")
            } catch (e: Exception) {
                lastError = e
                if (!isNetworkError(e) || attempt == MAX_ANALYZE_ATTEMPTS - 1) {
                    throw e
                }
            }
        }
        throw lastError ?: IllegalStateException("Análisis fallido")
    }

    private fun execute(request: YoutubeDLRequest, processId: String) =
        YoutubeDL.getInstance().execute(request, processId)

    /** Detecta errores transitorios de red/DNS que merecen un reintento. */
    private fun isNetworkError(e: Exception): Boolean {
        val message = e.message?.lowercase() ?: return false
        return message.contains("no address associated with hostname") ||
            message.contains("temporary failure in name resolution") ||
            message.contains("network is unreachable") ||
            message.contains("timed out") ||
            message.contains("connection refused") ||
            message.contains("unable to download api page") ||
            message.contains("errno 7") ||
            message.contains("errno 8") ||
            message.contains("errno 110")
    }

    // ----- Caché de análisis -----

    private fun cacheDir(): File = File(context.cacheDir, "analysis").apply { mkdirs() }

    private fun cacheKey(url: String): String {
        val md5 = java.security.MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "$md5.json"
    }

    private fun loadCached(key: String): MediaItem? {
        val file = File(cacheDir(), key)
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > CACHE_TTL_MILLIS) {
            file.delete()
            return null
        }
        return try {
            parseVideoInfo(file.readText(), key)
        } catch (_: Exception) {
            null
        }
    }

    private fun cache(key: String, media: MediaItem) {
        runCatching {
            val json = JSONObject().apply {
                put("id", media.id)
                put("title", media.title)
                put("thumbnail", media.thumbnail ?: JSONObject.NULL)
                put("durationSeconds", media.durationSeconds)
                put("uploader", media.uploader ?: JSONObject.NULL)
                put("webpageUrl", media.webpageUrl)
                put("extractor", media.extractor ?: JSONObject.NULL)
                put("description", media.description ?: JSONObject.NULL)
                put("formats", org.json.JSONArray().apply {
                    media.formats.forEach { f ->
                        put(JSONObject().apply {
                            put("format_id", f.formatId)
                            put("ext", f.ext ?: JSONObject.NULL)
                            put("height", f.height)
                            put("width", f.width)
                            put("fps", f.fps)
                            put("vcodec", f.videoCodec ?: JSONObject.NULL)
                            put("acodec", f.audioCodec ?: JSONObject.NULL)
                            put("format_note", f.note ?: JSONObject.NULL)
                            put("filesize", f.fileSizeBytes)
                            put("tbr", f.bitrate)
                        })
                    }
                })
            }
            File(cacheDir(), key).writeText(json.toString())
        }
    }

    private fun parseVideoInfo(json: String, url: String): MediaItem {
        val root = JSONObject(json)
        val formats = root.optJSONArray("formats")
        val formatItems = if (formats != null) {
            buildList {
                for (i in 0 until formats.length()) {
                    val f = formats.getJSONObject(i)
                    add(f.toFormatItem())
                }
            }.filter { it.hasVideo && it.height > 0 }
                .groupBy { it.height }
                .mapNotNull { (_, group) -> group.maxByOrNull { it.bitrate } }
                .sortedByDescending { it.height }
        } else {
            emptyList()
        }
        return MediaItem(
            id = root.optString("id").ifBlank { url },
            title = root.optString("title").ifBlank { root.optString("fulltitle").ifBlank { "Sin título" } },
            thumbnail = root.optNullable("thumbnail"),
            durationSeconds = root.optLong("duration"),
            uploader = root.optNullable("uploader"),
            webpageUrl = root.optString("webpage_url").ifBlank { url },
            extractor = root.optNullable("extractor"),
            description = root.optNullable("description"),
            formats = formatItems
        )
    }

    private fun JSONObject.toFormatItem(): FormatItem {
        val vcodec = optNullable("vcodec")
        val acodec = optNullable("acodec")
        return FormatItem(
            formatId = optString("format_id"),
            ext = optNullable("ext"),
            height = optInt("height"),
            width = optInt("width"),
            fps = optInt("fps"),
            videoCodec = vcodec,
            audioCodec = acodec,
            note = optNullable("format_note"),
            fileSizeBytes = optLongOrZero("filesize").takeIf { it > 0 } ?: optLongOrZero("filesize_approx"),
            bitrate = optInt("tbr"),
            hasVideo = !vcodec.isNullOrEmpty() && vcodec != "none",
            hasAudio = !acodec.isNullOrEmpty() && acodec != "none"
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
        type: DownloadType,
        qualityHeight: Int,
        audioQuality: String?,
        outputDir: File,
        processId: String,
        onProgress: (Float, Long, String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url.trim())
        request.addOption("--no-mtime")
        request.addOption("--no-playlist")
        request.addOption("--force-ipv4")
        request.addOption("-o", "${outputDir.absolutePath}/%(title)s.%(ext)s")

        when (type) {
            DownloadType.VIDEO -> {
                val selector = if (qualityHeight > 0) {
                    "bv*[height<=$qualityHeight]+ba/b[height<=$qualityHeight]/b"
                } else {
                    "bv*+ba/b"
                }
                request.addOption("-f", selector)
                request.addOption("--merge-output-format", "mp4")
            }
            DownloadType.AUDIO_MP3 -> {
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
     * Cancela un proceso activo (análisis o descarga) identificado por [processId].
     */
    fun cancel(processId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
    }

    /**
     * Actualiza yt-dlp a la última versión estable.
     */
    @Throws(Exception::class)
    fun updateYtdlp() {
        YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
    }

    private fun JSONObject.optNullable(key: String): String? =
        if (isNull(key)) null else optString(key)

    private fun JSONObject.optLongOrZero(key: String): Long =
        if (isNull(key)) 0L else optLong(key)

    companion object {
        private const val MAX_ANALYZE_ATTEMPTS = 2
        private const val CACHE_TTL_MILLIS = 10 * 60 * 1000L
    }
}
