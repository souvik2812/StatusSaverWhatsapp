package com.example.statussaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.statussaver.data.StorageRepository
import com.example.statussaver.theme.NeonViolet
import com.example.statussaver.theme.StatusSaverTheme
import com.example.statussaver.ui.GalleryUiState
import com.example.statussaver.ui.StatusViewModel
import com.example.statussaver.ui.components.GalleryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StatusSaverTheme {
                StatusSaverApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSaverApp(vm: StatusViewModel = viewModel()) {

    val uiState       by vm.uiState.collectAsState()
    val downloadStates by vm.downloadStates.collectAsState()
    val selectedTab   by vm.selectedTab.collectAsState()
    val snackbarMsg   by vm.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // ── SAF folder picker launcher ───────────────────────────────────────────
    // Pre-populate the picker with the WhatsApp .Statuses URI hint.
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.onFolderGranted(uri)
        }
        // If uri == null the user dismissed the picker — we stay on permission screen.
    }

    // Helper to launch the picker pointed at the WA Statuses directory
    val launchPicker: () -> Unit = {
        folderPickerLauncher.launch(StorageRepository.WHATSAPP_STATUS_INITIAL_URI)
    }

    // ── Snackbar side-effect ─────────────────────────────────────────────────
    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
        vm.consumeSnackbar()
    }

    // ── Scaffold ─────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Status Saver",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                },
                actions = {
                    // Only show refresh when we have a successful load
                    if (uiState is GalleryUiState.Success || uiState is GalleryUiState.Error) {
                        IconButton(onClick = { vm.loadStatuses() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh statuses",
                                tint = NeonViolet
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = Color.White,
                    actionColor = NeonViolet,
                    dismissActionContentColor = Color.White.copy(alpha = 0.7f),
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
                modifier       = Modifier.fillMaxSize()
            )
        }
    }
}
