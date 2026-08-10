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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
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
import androidx.compose.material3.Button
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
import com.col.notnetmediaforge.R
import com.col.notnetmediaforge.data.model.DownloadType
import com.col.notnetmediaforge.data.model.MediaItem
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
                Button(onClick = onBack) { Text("Volver") }
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

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(media.title, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    media.uploader?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatDuration(media.durationSeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Selector Video / MP3
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = selectedType == DownloadType.VIDEO,
                    onClick = { selectedType = DownloadType.VIDEO },
                    label = { Text("Video") },
                    colors = FilterChipDefaults.filterChipColors()
                )
                FilterChip(
                    selected = selectedType == DownloadType.AUDIO_MP3,
                    onClick = { selectedType = DownloadType.AUDIO_MP3 },
                    label = { Text("Audio MP3") },
                    colors = FilterChipDefaults.filterChipColors()
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
            Button(
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
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isMp3) "Descargar MP3" else "Descargar video", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

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
                    label = { Text(label) }
                )
            }
        }
    }
}
