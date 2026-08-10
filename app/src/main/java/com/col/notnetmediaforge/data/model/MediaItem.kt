package com.col.notnetmediaforge.data.model

/**
 * Información básica de un medio obtenida mediante yt-dlp (--dump-json).
 * Es el modelo de dominio independiente de la librería subyacente.
 */
data class MediaItem(
    val id: String,
    val title: String,
    val thumbnail: String?,
    val durationSeconds: Long,
    val uploader: String?,
    val webpageUrl: String,
    val extractor: String?,
    val description: String?,
    val formats: List<FormatItem>
)

/**
 * Un formato/calidad concreto disponible para el medio.
 */
data class FormatItem(
    val formatId: String,
    val ext: String?,
    val height: Int,
    val width: Int,
    val fps: Int,
    val videoCodec: String?,
    val audioCodec: String?,
    val note: String?,
    val fileSizeBytes: Long,
    val bitrate: Int,
    val hasVideo: Boolean,
    val hasAudio: Boolean
) {
    val isVideoOnly: Boolean get() = hasVideo && !hasAudio
    val resolutionLabel: String
        get() = if (height > 0) "${height}p" else (note ?: "auto")
}
