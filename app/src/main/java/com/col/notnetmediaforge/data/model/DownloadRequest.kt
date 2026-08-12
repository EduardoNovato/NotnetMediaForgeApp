package com.col.notnetmediaforge.data.model

/**
 * Tipo de conversión/descarga solicitada por el usuario.
 */
enum class DownloadType {
    VIDEO,
    AUDIO_MP3
}

/**
 * Petición concreta de descarga. Se pasa al WorkManager para su ejecución
 * en segundo plano.
 */
data class DownloadRequest(
    val url: String,
    val type: DownloadType,
    val qualityHeight: Int,          // 0 = mejor disponible (solo Video)
    val audioQuality: String?,       // "0", "192", "128", etc. (solo MP3)
    val title: String,
    val thumbnail: String?,
    val uploader: String?,
    val durationSeconds: Long
)
