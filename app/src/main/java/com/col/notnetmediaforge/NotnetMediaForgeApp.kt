package com.col.notnetmediaforge

import android.app.Application
import com.col.notnetmediaforge.data.notification.NotificationHelper
import com.col.notnetmediaforge.di.AppContainer
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clase Application: inicializa el contenedor de dependencias, el canal de
 * notificaciones y el motor yt-dlp + FFmpeg.
 */
class NotnetMediaForgeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)

        // Inicialización del motor de descarga en segundo plano.
        // IMPORTANTE: no bloquear el hilo principal.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                YoutubeDL.getInstance().init(this@NotnetMediaForgeApp)
                FFmpeg.getInstance().init(this@NotnetMediaForgeApp)
            }
        }
    }
}
