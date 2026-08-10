package com.col.notnetmediaforge.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.col.notnetmediaforge.data.model.DownloadItem
import com.col.notnetmediaforge.data.model.DownloadStatus
import com.col.notnetmediaforge.data.model.DownloadType
import com.col.notnetmediaforge.ui.components.BrandIconBadge
import com.col.notnetmediaforge.ui.components.StatusBadge
import com.col.notnetmediaforge.ui.components.formatDuration
import com.col.notnetmediaforge.ui.theme.BrandGradientEnd
import com.col.notnetmediaforge.ui.theme.BrandGradientStart
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
            EmptyHistory(modifier = Modifier.padding(padding))
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
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandIconBadge(icon = Icons.Outlined.FileDownload, size = 72)
        Spacer(Modifier.height(20.dp))
        Text("Aún no hay descargas", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Analiza un enlace y pulsa Descargar;\nsu progreso aparecerá aquí en tiempo real.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    val statusIcon: ImageVector
    val statusColor: Color
    when (item.status) {
        DownloadStatus.QUEUED -> {
            statusIcon = Icons.Outlined.Schedule
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
        DownloadStatus.RUNNING -> {
            statusIcon = if (item.progress >= 99.5f) Icons.Outlined.Sync else Icons.Outlined.Downloading
            statusColor = MaterialTheme.colorScheme.primary
        }
        DownloadStatus.COMPLETED -> {
            statusIcon = Icons.Outlined.CheckCircle
            statusColor = MaterialTheme.colorScheme.secondary
        }
        DownloadStatus.CANCELLED -> {
            statusIcon = Icons.Outlined.Cancel
            statusColor = MaterialTheme.colorScheme.outline
        }
        DownloadStatus.FAILED -> {
            statusIcon = Icons.Outlined.ErrorOutline
            statusColor = MaterialTheme.colorScheme.error
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HistoryThumbnail(item)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildSubtitle(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusBadge(icon = statusIcon, label = statusLabel(item.status, item.progress), color = statusColor)
                }
            }

            if (item.status == DownloadStatus.FAILED) {
                val errorText = item.error?.trim()?.lines()?.lastOrNull()?.takeIf { it.isNotBlank() }
                if (errorText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (running) {
                Spacer(Modifier.height(10.dp))
                val processing = item.status == DownloadStatus.RUNNING && item.progress >= 99.5f
                if (processing || item.status == DownloadStatus.QUEUED) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { item.progress / 100f },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${item.progress.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
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

@Composable
private fun HistoryThumbnail(item: DownloadItem) {
    Box(
        modifier = Modifier
            .size(width = 84.dp, height = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        BrandGradientStart.copy(alpha = 0.28f),
                        BrandGradientEnd.copy(alpha = 0.28f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!item.thumbnail.isNullOrBlank()) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = if (item.type == DownloadType.AUDIO_MP3) {
                    Icons.Outlined.MusicNote
                } else {
                    Icons.Outlined.FileDownload
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun buildSubtitle(item: DownloadItem): String {
    val type = if (item.type == DownloadType.AUDIO_MP3) "MP3" else "Video"
    val duration = formatDuration(item.durationSeconds)
    return "$type • $duration"
}

private fun statusLabel(status: DownloadStatus, progress: Float = 0f): String = when (status) {
    DownloadStatus.QUEUED -> "En cola"
    DownloadStatus.RUNNING -> if (progress >= 99.5f) "Procesando…" else "Descargando…"
    DownloadStatus.COMPLETED -> "Completado"
    DownloadStatus.CANCELLED -> "Cancelado"
    DownloadStatus.FAILED -> "Fallido"
}
