package com.example.statussaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.statussaver.data.StorageRepository
import com.example.statussaver.theme.StatusSaverTheme
import com.example.statussaver.ui.GalleryUiState
import com.example.statussaver.ui.StatusViewModel
import com.example.statussaver.ui.components.GalleryScreen
import com.example.statussaver.ui.components.MediaViewerScreen

import androidx.activity.SystemBarStyle
import androidx.compose.runtime.DisposableEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: StatusViewModel = viewModel()
            val isDark by vm.isDarkMode.collectAsState()
            val color by vm.themeColor.collectAsState()

            val systemDark = isSystemInDarkTheme()
            val currentIsDark = isDark ?: systemDark
            val currentColor = color ?: "Violet"

            DisposableEffect(currentIsDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { currentIsDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { currentIsDark }
                )
                onDispose {}
            }

            StatusSaverTheme(
                isDarkMode = currentIsDark,
                themeColor = currentColor
            ) {
                StatusSaverApp(
                    vm = vm,
                    isDark = currentIsDark,
                    themeColor = currentColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSaverApp(
    vm: StatusViewModel,
    isDark: Boolean,
    themeColor: String
) {
    val uiState       by vm.uiState.collectAsState()
    val downloadStates by vm.downloadStates.collectAsState()
    val selectedTab   by vm.selectedTab.collectAsState()
    val snackbarMsg   by vm.snackbarMessage.collectAsState()
    val viewerState   by vm.viewerState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showThemeMenu by remember { mutableStateOf(false) }

    // ── SAF folder picker launcher ───────────────────────────────────────────
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.onFolderGranted(uri)
        }
    }

    val launchPicker: () -> Unit = {
        folderPickerLauncher.launch(StorageRepository.WHATSAPP_STATUS_INITIAL_URI)
    }

    // ── Snackbar side-effect ─────────────────────────────────────────────────
    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
        vm.consumeSnackbar()
    }

    // If viewer is open, show it fullscreen (no Scaffold, no TopAppBar)
    if (viewerState != null) {
        MediaViewerScreen(
            items = viewerState!!.items,
            initialIndex = viewerState!!.initialIndex,
            downloadStates = downloadStates,
            onDownload = vm::downloadItem,
            onClose = vm::closeViewer
        )
        return
    }

    // ── Scaffold (Gallery View) ──────────────────────────────────────────────
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Status Saver",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.setDarkMode(!isDark) }) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Theme Color",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            val colors = listOf("Violet", "Red", "Orange", "Blue", "Yellow", "Green", "Pink", "Dark Green")
                            colors.forEach { colorName ->
                                DropdownMenuItem(
                                    text = { Text(colorName) },
                                    onClick = {
                                        vm.setThemeColor(colorName)
                                        showThemeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (uiState is GalleryUiState.Success || uiState is GalleryUiState.Error) {
                        IconButton(onClick = { vm.loadStatuses() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(12.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GalleryScreen(
                uiState        = uiState,
                downloadStates = downloadStates,
                selectedTab    = selectedTab,
                onTabSelected  = vm::selectTab,
                onGrantAccess  = launchPicker,
                onRefresh      = vm::loadStatuses,
                onDownload     = vm::downloadItem,
                onItemClick    = { item, index, items ->
                    vm.openViewer(index, items)
                },
                modifier       = Modifier.fillMaxSize()
            )
        }
    }
}
