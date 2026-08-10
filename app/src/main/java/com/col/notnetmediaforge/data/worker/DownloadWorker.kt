package com.col.notnetmediaforge.data.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.col.notnetmediaforge.NotnetMediaForgeApp
import com.col.notnetmediaforge.data.model.DownloadRequest
import com.col.notnetmediaforge.data.model.DownloadStatus
import com.col.notnetmediaforge.data.model.DownloadType
import com.col.notnetmediaforge.data.notification.NotificationHelper
import com.col.notnetmediaforge.data.repository.MediaStoreManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * Descarga un medio en segundo plano mediante yt-dlp + FFmpeg,
 * mostrando el progreso en una notificación en primer plano y guardando
 * el resultado en MediaStore.
 */
class DownloadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val container = (appContext.applicationContext as NotnetMediaForgeApp).container
    private val repository = container.youtubeDLRepository
    private val history = container.downloadHistoryRepository

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()
        val type = DownloadType.valueOf(inputData.getString(KEY_TYPE) ?: DownloadType.VIDEO.name)
        val qualityHeight = inputData.getInt(KEY_QUALITY_HEIGHT, 0)
        val audioQuality = inputData.getString(KEY_AUDIO_QUALITY)
        val title = inputData.getString(KEY_TITLE) ?: url
        val uploader = inputData.getString(KEY_UPLOADER)
        val thumbnail = inputData.getString(KEY_THUMBNAIL)
        val duration = inputData.getLong(KEY_DURATION, 0L)

        val notificationId = itemId.hashCode()
        val processId = itemId

        setForeground(
            foregroundInfo(notificationId, title, 0f, processId)
        )

        history.update(itemId) { it.copy(status = DownloadStatus.RUNNING, progress = 0f) }

        val outputDir = File(
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: applicationContext.filesDir,
            itemId
        )
        outputDir.mkdirs()

        return coroutineScope {
            val progressChannel = Channel<Float>(Channel.CONFLATED)
            val collectJob = launch {
                var last = -1
                for (progress in progressChannel) {
                    val intProgress = progress.toInt()
                    if (intProgress != last) {
                        last = intProgress
                        setProgress(workDataOf(PROGRESS to intProgress))
                        setForeground(
                            foregroundInfo(notificationId, title, progress, processId)
                        )
                        history.update(itemId) { it.copy(status = DownloadStatus.RUNNING, progress = progress) }
                    }
                }
            }

            try {
                val downloadedFile = repository.download(
                    url = url,
                    type = type,
                    qualityHeight = qualityHeight,
                    audioQuality = audioQuality,
                    outputDir = outputDir,
                    processId = processId,
                    onProgress = { progress, _, _ -> progressChannel.trySend(progress) }
                )

                val isAudio = type == DownloadType.AUDIO_MP3
                val displayName = downloadedFile.name
                val mimeType = guessMimeType(downloadedFile.extension, isAudio)
                val uri = MediaStoreManager.save(
                    applicationContext, downloadedFile, displayName, mimeType, isAudio
                )

                MediaStoreManager.deleteTempDirectory(outputDir)
                setProgress(workDataOf(PROGRESS to 100))
                NotificationHelper.cancel(applicationContext, notificationId)
                NotificationHelper.show(
                    applicationContext,
                    notificationId,
                    NotificationHelper.buildDoneNotification(applicationContext, notificationId, title, true)
                )

                history.update(itemId) {
                    it.copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 100f,
                        savedUri = uri.toString(),
                        fileDisplayName = displayName,
                        mimeType = mimeType,
                        error = null
                    )
                }
                Result.success()
            } catch (e: CancellationException) {
                MediaStoreManager.deleteTempDirectory(outputDir)
                NotificationHelper.cancel(applicationContext, notificationId)
                history.update(itemId) { it.copy(status = DownloadStatus.CANCELLED, progress = 0f) }
                Result.failure()
            } catch (e: com.yausername.youtubedl_android.YoutubeDL.CanceledException) {
                MediaStoreManager.deleteTempDirectory(outputDir)
                NotificationHelper.cancel(applicationContext, notificationId)
                history.update(itemId) { it.copy(status = DownloadStatus.CANCELLED, progress = 0f) }
                Result.failure()
            } catch (e: Exception) {
                MediaStoreManager.deleteTempDirectory(outputDir)
                NotificationHelper.cancel(applicationContext, notificationId)
                NotificationHelper.show(
                    applicationContext,
                    notificationId,
                    NotificationHelper.buildDoneNotification(applicationContext, notificationId, title, false)
                )
                history.update(itemId) { it.copy(status = DownloadStatus.FAILED, progress = 0f, error = e.message) }
                Result.failure()
            } finally {
                collectJob.cancel()
            }
        }
    }

    private fun foregroundInfo(notificationId: Int, title: String, progress: Float, processId: String): ForegroundInfo =
        ForegroundInfo(
            notificationId,
            NotificationHelper.buildProgressNotification(
                applicationContext, notificationId, title, progress, processId
            ),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

    private fun guessMimeType(extension: String, isAudio: Boolean): String {
        return when (extension.lowercase()) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            else -> if (isAudio) "audio/mpeg" else "video/mp4"
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_TYPE = "type"
        const val KEY_QUALITY_HEIGHT = "qualityHeight"
        const val KEY_AUDIO_QUALITY = "audioQuality"
        const val KEY_TITLE = "title"
        const val KEY_THUMBNAIL = "thumbnail"
        const val KEY_UPLOADER = "uploader"
        const val KEY_DURATION = "duration"
        const val KEY_ITEM_ID = "itemId"
        const val PROGRESS = "progress"

        fun uniqueName(itemId: String) = "download_$itemId"

        fun buildInputData(request: DownloadRequest, itemId: String): androidx.work.Data =
            workDataOf(
                KEY_URL to request.url,
                KEY_TYPE to request.type.name,
                KEY_QUALITY_HEIGHT to request.qualityHeight,
                KEY_AUDIO_QUALITY to (request.audioQuality ?: ""),
                KEY_TITLE to request.title,
                KEY_THUMBNAIL to (request.thumbnail ?: ""),
                KEY_UPLOADER to (request.uploader ?: ""),
                KEY_DURATION to request.durationSeconds,
                KEY_ITEM_ID to itemId
            )
    }
}
