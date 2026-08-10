package com.col.notnetmediaforge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.col.notnetmediaforge.ui.navigation.AppNavHost
import com.col.notnetmediaforge.ui.navigation.Routes
import com.col.notnetmediaforge.ui.theme.NotnetMediaForgeTheme
import com.col.notnetmediaforge.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private var sharedUrl by mutableStateOf<String?>(null)

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* resultado ignorado; la descarga fallará con error visible si se deniega */ }

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* si se deniega, el progreso se ve igualmente en la pestaña Historial */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedUrl = extractUrl(intent)
        requestLegacyStoragePermissionIfNeeded()
        requestNotificationsPermissionIfNeeded()

        setContent {
            NotnetMediaForgeTheme {
                val viewModel: MainViewModel = viewModel()
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                LaunchedEffect(sharedUrl) {
                    sharedUrl?.let { url ->
                        sharedUrl = null
                        viewModel.handleSharedUrl(url)
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (currentRoute == Routes.HOME || currentRoute == Routes.HISTORY) {
                            AppBottomBar(currentRoute, navController)
                        }
                    }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractUrl(intent)?.let { sharedUrl = it }
    }

    private fun extractUrl(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> extractUrlFromText(intent.getStringExtra(Intent.EXTRA_TEXT))
            else -> null
        }
    }

    private fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return text.split(Regex("\\s+")).firstOrNull {
            it.startsWith("http://") || it.startsWith("https://")
        }
    }

    private fun requestLegacyStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                storagePermissionLauncher.launch(permission)
            }
        }
    }

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationsPermissionLauncher.launch(permission)
            }
        }
    }
}

@Composable
private fun AppBottomBar(currentRoute: String?, navController: NavHostController) {
    val items = listOf(
        Pair(Routes.HOME, "Descargar") to Icons.Outlined.FileDownload,
        Pair(Routes.HISTORY, "Historial") to Icons.Outlined.CheckCircle
    )
    NavigationBar(
        tonalElevation = 10.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        items.forEach { (entry, icon) ->
            val (route, label) = entry
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) {
                        Log.d("Nav", "tap en pestaña ya activa: $route")
                        return@NavigationBarItem
                    }
                    Log.d(
                        "Nav",
                        "tap $route desde $currentRoute, stack=${navController.currentBackStack.value.map { it.destination.route }}"
                    )
                    runCatching {
                        // Si la pestaña ya está en la pila, vuelve a ella
                        // descartando lo de encima; si no, la añade.
                        val popped = navController.popBackStack(route, false)
                        if (!popped) {
                            navController.navigate(route) { launchSingleTop = true }
                        }
                        Log.d(
                            "Nav",
                            "tras tap $route (popBackStack=$popped): stack=${navController.currentBackStack.value.map { it.destination.route }}"
                        )
                    }.onFailure {
                        Log.e("Nav", "No se pudo navegar a $route", it)
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) {
                            if (route == Routes.HOME) Icons.Filled.FileDownload else Icons.Filled.CheckCircle
                        } else {
                            icon
                        },
                        contentDescription = null
                    )
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
