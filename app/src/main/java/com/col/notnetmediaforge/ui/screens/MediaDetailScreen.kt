package com.col.notnetmediaforge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.col.notnetmediaforge.data.model.DownloadType
import com.col.notnetmediaforge.data.model.MediaItem
import com.col.notnetmediaforge.ui.components.GradientButton
import com.col.notnetmediaforge.ui.components.Thumbnail
import com.col.notnetmediaforge.ui.components.formatDuration
import com.col.notnetmediaforge.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private data class AudioQualityOption(val code: String, val label: String)

private val audioQualities = listOf(
    AudioQualityOption("0", "Mejor"),
    AudioQualityOption("320", "320 kbps"),
    AudioQualityOption("256", "256 kbps"),
    AudioQualityOption("192", "192 kbps"),
    AudioQualityOption("128", "128 kbps")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val media = viewModel.uiState.value.mediaItem
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedType by rememberSaveable { mutableStateOf(DownloadType.VIDEO) }
    var selectedQualityHeight by rememberSaveable { mutableStateOf(0) }
    var selectedAudioQuality by rememberSaveable { mutableStateOf("0") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Descargar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (media == null) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No hay información del enlace.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.Button(onClick = onBack) { Text("Volver") }
            }
            return@Scaffold
        }

        val videoHeights = media.formats.map { it.height }.distinct().sortedDescending()
        val videoQualityOptions = listOf(0 to "Mejor") + videoHeights.map { it to "${it}p" }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Thumbnail(thumbnailUrl = media.thumbnail, modifier = Modifier.fillMaxWidth())

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(media.title, style = MaterialTheme.typography.titleLarge)
                MediaInfoRow(media)
            }

            // Selector Video / MP3
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = selectedType == DownloadType.VIDEO,
                    onClick = { selectedType = DownloadType.VIDEO },
                    label = { Text("Video") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = selectedChipColors(selectedType == DownloadType.VIDEO)
                )
                FilterChip(
                    selected = selectedType == DownloadType.AUDIO_MP3,
                    onClick = { selectedType = DownloadType.AUDIO_MP3 },
                    label = { Text("Audio MP3") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = selectedChipColors(selectedType == DownloadType.AUDIO_MP3)
                )
            }

            if (selectedType == DownloadType.VIDEO) {
                QualitySection(
                    title = "Calidad de video",
                    options = videoQualityOptions,
                    selected = selectedQualityHeight
                ) { selectedQualityHeight = it }
            } else {
                QualitySection(
                    title = "Calidad de audio",
                    options = audioQualities.map { it.code.toInt() to it.label },
                    selected = selectedAudioQuality.toInt()
                ) { selectedAudioQuality = it.toString() }
            }

            val isMp3 = selectedType == DownloadType.AUDIO_MP3
            GradientButton(
                text = if (isMp3) "Descargar MP3" else "Descargar video",
                icon = if (isMp3) Icons.Outlined.MusicNote else Icons.Outlined.Download,
                onClick = {
                    viewModel.startDownload(
                        type = selectedType,
                        qualityHeight = if (isMp3) 0 else selectedQualityHeight,
                        audioQuality = if (isMp3) selectedAudioQuality else null
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (isMp3) "Descarga MP3 iniciada en segundo plano"
                            else "Descarga iniciada en segundo plano"
                        )
                    }
                    onOpenHistory()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }
    }
}

@Composable
private fun MediaInfoRow(media: MediaItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        media.uploader?.let {
            InfoChip(icon = Icons.Outlined.AccountCircle, text = it)
        }
        if (media.durationSeconds > 0) {
            InfoChip(icon = Icons.Outlined.Schedule, text = formatDuration(media.durationSeconds))
        }
        media.extractor?.let {
            InfoChip(icon = Icons.Outlined.Public, text = it)
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun selectedChipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    },
    labelColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    },
    iconColor = if (selected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
)

@Composable
private fun QualitySection(
    title: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                    colors = selectedChipColors(selected == value)
                )
            }
        }
    }
}
