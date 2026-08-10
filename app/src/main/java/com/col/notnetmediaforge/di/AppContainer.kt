package com.col.notnetmediaforge.di

import android.content.Context
import com.col.notnetmediaforge.data.repository.DownloadHistoryRepository
import com.col.notnetmediaforge.data.repository.YoutubeDLRepository

/**
 * Contenedor de dependencias sencillo (service locator) que expone los
 * repositorios compartidos por ViewModels y workers.
 */
class AppContainer(context: Context) {

    val youtubeDLRepository: YoutubeDLRepository = YoutubeDLRepository(context.applicationContext)
    val downloadHistoryRepository: DownloadHistoryRepository = DownloadHistoryRepository(context.applicationContext)
}
