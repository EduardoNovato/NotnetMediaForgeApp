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
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.col.notnetmediaforge.ui.components.MediaPreviewCard
import com.col.notnetmediaforge.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAnalyzed: () -> Unit
) {
    val state = viewModel.uiState.collectAsState().value
    val engineState by viewModel.engineState.collectAsState()
    val clipboard = LocalClipboardManager.current

    var navigated by rememberSaveable { mutableStateOf(false) }

    // Al analizar con éxito, ir automáticamente a la pantalla de detalle.
    LaunchedEffect(state.mediaItem?.id) {
        val media = state.mediaItem
        if (media != null && !navigated) {
            navigated = true
            onAnalyzed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Header()

        // Estado del motor de descarga
        when (engineState) {
            is EngineState.Initializing -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        "Preparando el motor de descarga… (solo la primera vez puede tardar)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is EngineState.Error -> {
                Text(
                    "Error al preparar el motor: ${(engineState as EngineState.Error).message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
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
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                },
                singleLine = true,
                enabled = !state.isAnalyzing,
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
                    enabled = !state.isAnalyzing && engineReady
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pegar")
                }
                if (state.isAnalyzing) {
                    OutlinedButton(
                        onClick = { viewModel.cancelAnalysis() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cancelar")
                    }
                } else {
                    Button(
                        onClick = { viewModel.analyze() },
                        modifier = Modifier.weight(1f),
                        enabled = engineReady
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Analizar")
                    }
                }
            }
        }

        if (state.error != null) {
            ErrorMessage(message = state.error)
        }

        val media = state.mediaItem
        if (media != null && navigated) {
            MediaPreviewCard(media = media, onClick = onAnalyzed)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "NotnetMediaForge",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Descarga videos y audio desde cualquier plataforma",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}
