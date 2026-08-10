package com.col.notnetmediaforge.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yausername.youtubedl_android.YoutubeDL

/**
 * Cancela una descarga activa cuando el usuario pulsa "Cancelar" en la
 * notificación. El [DownloadWorker] detecta la cancelación (CanceledException)
 * y actualiza el historial en consecuencia.
 */
class DownloadCancelReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val processId = intent.getStringExtra(NotificationHelper.EXTRA_PROCESS_ID) ?: return
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
    }
}
