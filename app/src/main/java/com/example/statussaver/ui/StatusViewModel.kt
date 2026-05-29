package com.example.statussaver.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.statussaver.data.PreferencesManager
import com.example.statussaver.data.StatusItem
import com.example.statussaver.data.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── UI State ────────────────────────────────────────────────────────────────

/** All possible states the gallery screen can be in. */
sealed interface GalleryUiState {
    /** The user has not yet granted folder access. */
    data object NeedsPermission : GalleryUiState

    /** Status files are being loaded. */
    data object Loading : GalleryUiState

    /** Statuses loaded successfully; list may be empty. */
    data class Success(val items: List<StatusItem>) : GalleryUiState

    /** An error occurred (e.g. URI permission expired or directory empty). */
    data class Error(val message: String) : GalleryUiState
}

/** Download state for an individual item (keyed by URI string). */
sealed interface DownloadState {
    data object Idle : DownloadState
    data object InProgress : DownloadState
    data object Done : DownloadState
    data class Failed(val reason: String) : DownloadState
}

/** State for the full-screen media viewer. */
data class ViewerState(
    val initialIndex: Int,
    val items: List<StatusItem>
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

class StatusViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = StorageRepository(application)
    private val prefs = PreferencesManager(application)

    // ─── Theme State ───
    val isDarkMode: StateFlow<Boolean?> = prefs.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeColor: StateFlow<String?> = prefs.themeColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Main gallery UI state. */
    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    /** Per-item download states, keyed by URI string. */
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    /** Snackbar message event (null = no message). Consumed by UI after display. */
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    /** Active tab index: 0=All, 1=Photos, 2=Videos */
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    /** Full screen viewer state (null means closed) */
    private val _viewerState = MutableStateFlow<ViewerState?>(null)
    val viewerState: StateFlow<ViewerState?> = _viewerState.asStateFlow()

    init {
        checkPersistedPermission()
    }

    // ─── Theme Handlers ───
    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch { prefs.saveIsDarkMode(isDark) }
    }

    fun setThemeColor(color: String) {
        viewModelScope.launch { prefs.saveThemeColor(color) }
    }

    // ─── Viewer Handlers ───
    fun openViewer(initialIndex: Int, items: List<StatusItem>) {
        _viewerState.value = ViewerState(initialIndex, items)
    }

    fun closeViewer() {
        _viewerState.value = null
    }

    // ─── Initialisation ──────────────────────────────────────────────────────

    /** On launch, check if we already have a persisted URI from a previous session. */
    private fun checkPersistedPermission() {
        val uri = repo.getPersistedUri()
        if (uri != null) {
            loadStatuses(uri)
        } else {
            _uiState.value = GalleryUiState.NeedsPermission
        }
    }

    // ─── Permission Handling ─────────────────────────────────────────────────

    /**
     * Called by [MainActivity] when the SAF folder picker returns a granted URI.
     * Persists the permission and immediately loads statuses.
     */
    fun onFolderGranted(uri: Uri) {
        repo.persistUri(uri)
        loadStatuses(uri)
    }

    /** Resets access — next launch will show the permission screen again. */
    fun revokeAccess() {
        repo.clearPersistedUri()
        _uiState.value = GalleryUiState.NeedsPermission
        _downloadStates.value = emptyMap()
    }

    // ─── Data Loading ────────────────────────────────────────────────────────

    /** (Re-)loads statuses from the document tree. */
    fun loadStatuses(uri: Uri? = null) {
        val treeUri = uri ?: repo.getPersistedUri() ?: run {
            _uiState.value = GalleryUiState.NeedsPermission
            return
        }
        _uiState.value = GalleryUiState.Loading
        viewModelScope.launch {
            runCatching { repo.fetchStatuses(treeUri) }
                .onSuccess { items ->
                    _uiState.value = GalleryUiState.Success(items)
                    // Reset download states for any items no longer present
                    val uriKeys = items.map { it.uri.toString() }.toSet()
                    _downloadStates.update { prev ->
                        prev.filterKeys { it in uriKeys }
                    }
                    // Update viewer if open
                    _viewerState.value?.let { currentViewer ->
                        _viewerState.value = currentViewer.copy(items = items)
                    }
                }
                .onFailure { err ->
                    _uiState.value = GalleryUiState.Error(
                        err.message ?: "Failed to read statuses. Please re-grant folder access."
                    )
                }
        }
    }

    // ─── Tab Selection ───────────────────────────────────────────────────────

    fun selectTab(index: Int) {
        _selectedTab.value = index.coerceIn(0, 3)
    }

    // ─── Download ────────────────────────────────────────────────────────────

    fun downloadItem(item: StatusItem) {
        val key = item.uri.toString()
        // Guard against duplicate taps
        val current = _downloadStates.value[key]
        if (current is DownloadState.InProgress) return

        _downloadStates.update { it + (key to DownloadState.InProgress) }

        viewModelScope.launch {
            runCatching { repo.downloadStatus(item) }
                .onSuccess { savedUri ->
                    if (savedUri != null) {
                        _downloadStates.update { it + (key to DownloadState.Done) }
                        val dir = when {
                            item.isAudio -> "Music"
                            item.isVideo -> "Movies"
                            else -> "Pictures"
                        }
                        _snackbarMessage.value = "✅ Saved to $dir/StatusSaver"
                    } else {
                        _downloadStates.update { it + (key to DownloadState.Failed("Save failed")) }
                        _snackbarMessage.value = "❌ Save failed. Please try again."
                    }
                }
                .onFailure { err ->
                    _downloadStates.update {
                        it + (key to DownloadState.Failed(err.message ?: "Unknown error"))
                    }
                    _snackbarMessage.value = "❌ ${err.message ?: "Save failed"}"
                }
        }
    }

    /** Called by the UI after the snackbar has been shown to clear the event. */
    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }
}
