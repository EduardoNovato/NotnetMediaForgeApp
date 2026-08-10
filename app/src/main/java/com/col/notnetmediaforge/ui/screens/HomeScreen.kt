package com.col.notnetmediaforge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.col.notnetmediaforge.EngineState
import com.col.notnetmediaforge.ui.components.BrandIconBadge
import com.col.notnetmediaforge.ui.components.GradientButton
import com.col.notnetmediaforge.ui.components.MediaPreviewCard
import com.col.notnetmediaforge.ui.components.StatusCard
import com.col.notnetmediaforge.ui.theme.BrandGradient
import com.col.notnetmediaforge.ui.theme.DarkErrorContainer
import com.col.notnetmediaforge.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAnalyzed: () -> Unit
) {
    val state = viewModel.uiState.collectAsState().value
    val engineState by viewModel.engineState.collectAsState()
    val clipboard = LocalClipboardManager.current

    // Cuenta de análisis por la que ya navegamos a Detalle. Evita volver
    // a saltar al Detalle al regresar a esta pantalla, y también funciona
    // si se re-analiza el mismo enlace.
    var lastNavigatedCount by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(state.analysisCount) {
        if (state.analysisCount > lastNavigatedCount && state.mediaItem != null) {
            lastNavigatedCount = state.analysisCount
            onAnalyzed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Header()

        // Estado del motor de descarga
        when (engineState) {
            is EngineState.Initializing -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    StatusCard(
                        icon = Icons.Outlined.Sync,
                        message = "Preparando el motor de descarga… (solo la primera vez puede tardar)",
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            is EngineState.Error -> {
                StatusCard(
                    icon = Icons.Outlined.Warning,
                    message = "Error al preparar el motor: ${(engineState as EngineState.Error).message}",
                    containerColor = DarkErrorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is EngineState.Ready -> Unit
        }

        val engineReady = engineState is EngineState.Ready

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.urlText,
                onValueChange = { viewModel.updateUrl(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enlace de video o audio") },
                placeholder = { Text("https://…") },
                leadingIcon = {
                    Icon(Icons.Outlined.Link, contentDescription = null)
                },
                singleLine = true,
                enabled = !state.isAnalyzing,
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.analyze() })
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.getText()?.text?.let(viewModel::updateUrl)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isAnalyzing && engineReady,
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pegar")
                }
                if (state.isAnalyzing) {
                    OutlinedButton(
                        onClick = { viewModel.cancelAnalysis() },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cancelar")
                    }
                } else {
                    GradientButton(
                        text = "Analizar",
                        icon = Icons.Outlined.Search,
                        onClick = { viewModel.analyze() },
                        enabled = engineReady,
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                }
            }
        }

        if (state.error != null) {
            StatusCard(
                icon = Icons.Outlined.Warning,
                message = state.error,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val media = state.mediaItem
        if (media != null) {
            MediaPreviewCard(media = media, onClick = onAnalyzed)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandIconBadge(
            icon = Icons.Outlined.CloudDownload,
            size = 56,
            gradient = BrandGradient
        )
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "NotnetMediaForge",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Descarga videos y audio de cualquier plataforma",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
