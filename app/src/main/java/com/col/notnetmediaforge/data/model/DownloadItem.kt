package com.col.notnetmediaforge.data.model

/**
 * Entrada del historial de descargas, persistida localmente.
 */
enum class DownloadStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class DownloadItem(
    val id: String,
    val url: String,
    val type: DownloadType,
    val title: String,
    val thumbnail: String?,
    val uploader: String?,
    val durationSeconds: Long,
    val createdAtMillis: Long,
    val status: DownloadStatus,
    val progress: Float,
    val savedUri: String?,
    val fileDisplayName: String?,
    val mimeType: String?,
    val error: String?
)
