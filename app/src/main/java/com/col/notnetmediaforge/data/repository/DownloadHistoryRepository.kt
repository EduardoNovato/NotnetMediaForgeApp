package com.col.notnetmediaforge.data.repository

import android.content.Context
import com.col.notnetmediaforge.data.model.DownloadItem
import com.col.notnetmediaforge.data.model.DownloadRequest
import com.col.notnetmediaforge.data.model.DownloadStatus
import com.col.notnetmediaforge.data.model.DownloadType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Persiste el historial de descargas en un archivo JSON dentro del
 * almacenamiento interno de la app. Expuesto como StateFlow para la UI.
 */
class DownloadHistoryRepository(context: Context) {

    private val file = File(context.filesDir, "download_history.json")
    private val _items = MutableStateFlow<List<DownloadItem>>(load())

    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    init {
        _items.value = load()
    }

    fun newId(): String = UUID.randomUUID().toString()

    /**
     * Crea y persiste una nueva entrada a partir de una petición de descarga.
     */
    fun createEntry(request: DownloadRequest, id: String, createdAtMillis: Long): DownloadItem {
        val item = DownloadItem(
            id = id,
            url = request.url,
            type = request.type,
            title = request.title,
            thumbnail = request.thumbnail,
            uploader = request.uploader,
            durationSeconds = request.durationSeconds,
            createdAtMillis = createdAtMillis,
            status = DownloadStatus.QUEUED,
            progress = 0f,
            savedUri = null,
            fileDisplayName = null,
            mimeType = null,
            error = null
        )
        _items.value = listOf(item) + _items.value
        save()
        return item
    }

    fun update(id: String, transform: (DownloadItem) -> DownloadItem) {
        _items.value = _items.value.map { if (it.id == id) transform(it) else it }
        save()
    }

    fun remove(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
        save()
    }

    fun clear() {
        _items.value = emptyList()
        save()
    }

    private fun save() {
        val array = JSONArray()
        _items.value.forEach { array.put(it.toJson()) }
        try {
            file.writeText(array.toString())
        } catch (_: Exception) {
        }
    }

    private fun load(): List<DownloadItem> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            buildList {
                for (i in 0 until array.length()) {
                    add(array.getJSONObject(i).toDownloadItem())
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun DownloadItem.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("url", url)
        put("type", type.name)
        put("title", title)
        put("thumbnail", thumbnail ?: JSONObject.NULL)
        put("uploader", uploader ?: JSONObject.NULL)
        put("durationSeconds", durationSeconds)
        put("createdAtMillis", createdAtMillis)
        put("status", status.name)
        put("progress", progress)
        put("savedUri", savedUri ?: JSONObject.NULL)
        put("fileDisplayName", fileDisplayName ?: JSONObject.NULL)
        put("mimeType", mimeType ?: JSONObject.NULL)
        put("error", error ?: JSONObject.NULL)
    }

    private fun JSONObject.toDownloadItem(): DownloadItem = DownloadItem(
        id = optString("id"),
        url = optString("url"),
        type = DownloadType.valueOf(optString("type", DownloadType.VIDEO.name)),
        title = optString("title"),
        thumbnail = optNullable("thumbnail"),
        uploader = optNullable("uploader"),
        durationSeconds = optLong("durationSeconds"),
        createdAtMillis = optLong("createdAtMillis"),
        status = DownloadStatus.valueOf(optString("status", DownloadStatus.FAILED.name)),
        progress = optDouble("progress", 0.0).toFloat(),
        savedUri = optNullable("savedUri"),
        fileDisplayName = optNullable("fileDisplayName"),
        mimeType = optNullable("mimeType"),
        error = optNullable("error")
    )

    private fun JSONObject.optNullable(key: String): String? {
        return if (isNull(key)) null else optString(key)
    }
}
