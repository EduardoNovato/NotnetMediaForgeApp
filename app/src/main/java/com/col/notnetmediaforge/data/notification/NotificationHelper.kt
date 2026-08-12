package com.col.notnetmediaforge.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.col.notnetmediaforge.MainActivity
import com.col.notnetmediaforge.R

/**
 * Crea y actualiza las notificaciones de progreso de descarga,
 * incluida la acción de cancelar.
 */
object NotificationHelper {

    const val CHANNEL_ID = "downloads"
    const val EXTRA_PROCESS_ID = "extra_process_id"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_downloads_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_downloads_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Notificación de progreso usada para el servicio en primer plano.
     */
    fun buildProgressNotification(
        context: Context,
        notificationId: Int,
        title: String,
        progress: Float,
        processId: String
    ): android.app.Notification {
        val cancelIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, DownloadCancelReceiver::class.java)
                .putExtra(EXTRA_PROCESS_ID, processId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notif_downloading))
            .setContentIntent(openAppIntent(context, notificationId))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress.toInt(), progress <= 0f)
            .addAction(0, context.getString(R.string.notif_cancel), cancelIntent)
            .build()
    }

    fun buildDoneNotification(context: Context, notificationId: Int, title: String, success: Boolean): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setContentTitle(title)
            .setContentText(
                context.getString(if (success) R.string.notif_done else R.string.notif_failed)
            )
            .setContentIntent(openAppIntent(context, notificationId))
            .setAutoCancel(true)
            .build()
    }

    private fun openAppIntent(context: Context, notificationId: Int): PendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun show(context: Context, notificationId: Int, notification: android.app.Notification) {
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
