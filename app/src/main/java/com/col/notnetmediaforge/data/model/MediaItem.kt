package com.col.notnetmediaforge.data.model

/**
 * Información básica de un medio obtenida mediante yt-dlp (--dump-json).
 * Es el modelo de dominio independiente de la librería subyacente.
 */
data class MediaItem(
    val title: String,
    val thumbnail: String?,
    val durationSeconds: Long,
    val uploader: String?,
    val webpageUrl: String,
    val extractor: String?,
    val formats: List<FormatItem>
)

/**
 * Un formato/calidad concreto disponible para el medio.
 */
data class FormatItem(
    val height: Int,
    val bitrate: Int,
    val hasVideo: Boolean
)
