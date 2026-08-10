package com.col.notnetmediaforge.ui.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.col.notnetmediaforge.EngineState
import com.col.notnetmediaforge.NotnetMediaForgeApp
import com.col.notnetmediaforge.R
import com.col.notnetmediaforge.data.model.DownloadRequest
import com.col.notnetmediaforge.data.model.DownloadType
import com.col.notnetmediaforge.data.model.MediaItem
import com.col.notnetmediaforge.data.worker.DownloadWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI

/**
 * ViewModel compartido por toda la navegación de la app. Gestiona el análisis
 * de enlaces, la selección de formatos y la puesta en cola de descargas.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app as NotnetMediaForgeApp
    private val container = appContext.container
    private val repository = container.youtubeDLRepository
    private val history = container.downloadHistoryRepository
    private val workManager = WorkManager.getInstance(app)

    val engineState: StateFlow<EngineState> = appContext.engineState

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val historyItems = history.items

    fun updateUrl(text: String) {
        _uiState.update { it.copy(urlText = text) }
    }

    fun clear() {
        _uiState.value = HomeUiState()
    }

    fun analyze() {
        val url = _uiState.value.urlText.trim()
        if (url.isEmpty()) {
            _uiState.update { it.copy(error = getApp().getString(R.string.error_no_url)) }
            return
        }
        if (!isValidUrl(url)) {
            _uiState.update { it.copy(error = getApp().getString(R.string.error_invalid_url)) }
            return
        }
        when (val engine = engineState.value) {
            EngineState.Initializing -> {
                _uiState.update { it.copy(error = "El motor de descarga aún se está preparando. Espera unos segundos y reintenta.") }
                return
            }
            is EngineState.Error -> {
                _uiState.update { it.copy(error = "Motor de descarga no disponible: ${engine.message}") }
                return
            }
            EngineState.Ready -> Unit
        }
        val processId = "analyze_${System.currentTimeMillis()}"
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null, analyzeProcessId = processId) }

            var failureMessage: String? = null
            val media = withTimeoutOrNull(ANALYZE_TIMEOUT_MILLIS) {
                try {
                    repository.fetchMedia(url, processId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failureMessage = e.message
                    null
                }
            }

            if (media == null) {
                repository.cancel(processId)
                val message = failureMessage?.take(300)
                    ?: "El análisis tardó demasiado. Comprueba tu conexión e inténtalo de nuevo."
                _uiState.update { it.copy(isAnalyzing = false, analyzeProcessId = null, error = message) }
            } else {
                _uiState.update { it.copy(isAnalyzing = false, analyzeProcessId = null, mediaItem = media) }
            }
        }
    }

    fun cancelAnalysis() {
        _uiState.value.analyzeProcessId?.let { repository.cancel(it) }
        _uiState.update { it.copy(isAnalyzing = false, analyzeProcessId = null, error = null) }
    }

    /**
     * Recibe una URL procedente del menú "Compartir" de otra app.
     */
    fun handleSharedUrl(url: String) {
        if (url.isBlank()) return
        _uiState.update { it.copy(urlText = url) }
        analyze()
    }

    fun startDownload(type: DownloadType, qualityHeight: Int, audioQuality: String?) {
        val media = _uiState.value.mediaItem ?: return
        val request = DownloadRequest(
            url = media.webpageUrl,
            type = type,
            qualityHeight = qualityHeight,
            audioQuality = audioQuality,
            title = media.title,
            thumbnail = media.thumbnail,
            uploader = media.uploader,
            durationSeconds = media.durationSeconds,
            extractor = media.extractor
        )
        enqueue(request)
    }

    private fun enqueue(request: DownloadRequest) {
        val itemId = history.newId()
        history.createEntry(request, itemId, System.currentTimeMillis())
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(DownloadWorker.buildInputData(request, itemId))
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            DownloadWorker.uniqueName(itemId),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelDownload(itemId: String) {
        repository.cancel(itemId)
        workManager.cancelUniqueWork(DownloadWorker.uniqueName(itemId))
    }

    fun clearHistory() {
        history.clear()
    }

    fun removeHistoryItem(itemId: String) {
        history.remove(itemId)
    }

    private fun isValidUrl(url: String): Boolean {
        return runCatching {
            val uri = URI(url)
            (uri.scheme == "http" || uri.scheme == "https") && Patterns.WEB_URL.matcher(url).matches()
        }.getOrDefault(false)
    }

    private fun getApp() = appContext

    companion object {
        private const val ANALYZE_TIMEOUT_MILLIS = 120_000L
    }
}

data class HomeUiState(
    val urlText: String = "",
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val mediaItem: MediaItem? = null,
    val analyzeProcessId: String? = null
)
