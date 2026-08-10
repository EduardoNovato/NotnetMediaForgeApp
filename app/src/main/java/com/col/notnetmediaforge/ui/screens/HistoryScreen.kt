package com.col.notnetmediaforge.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.col.notnetmediaforge.data.model.DownloadItem
import com.col.notnetmediaforge.data.model.DownloadStatus
import com.col.notnetmediaforge.data.model.DownloadType
import com.col.notnetmediaforge.ui.components.formatDuration
import com.col.notnetmediaforge.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val items by viewModel.historyItems.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Historial") },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearHistory) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Borrar historial")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aún no hay descargas", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Las descargas realizadas aparecerán aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onOpen = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(item.savedUri), item.mimeType ?: "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            }
                        },
                        onCancel = { viewModel.cancelDownload(item.id) },
                        onDelete = { viewModel.removeHistoryItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: DownloadItem,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val running = item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.QUEUED
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildSubtitle(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = statusLabel(item.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(item.status)
                )
            }

            if (running) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (item.savedUri != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Guardado en Descargas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.status == DownloadStatus.COMPLETED && item.savedUri != null) {
                    TextButton(onClick = onOpen) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Abrir")
                    }
                }
                if (running) {
                    TextButton(onClick = onCancel) { Text("Cancelar") }
                }
                TextButton(onClick = onDelete) { Text("Eliminar") }
            }
        }
    }
}

private fun buildSubtitle(item: DownloadItem): String {
    val type = if (item.type == DownloadType.AUDIO_MP3) "MP3" else "Video"
    val duration = formatDuration(item.durationSeconds)
    return "$type • $duration"
}

private fun statusLabel(status: DownloadStatus): String = when (status) {
    DownloadStatus.QUEUED -> "En cola"
    DownloadStatus.RUNNING -> "Descargando…"
    DownloadStatus.COMPLETED -> "Completado"
    DownloadStatus.CANCELLED -> "Cancelado"
    DownloadStatus.FAILED -> "Fallido"
}

@Composable
private fun statusColor(status: DownloadStatus) = when (status) {
    DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
    DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
