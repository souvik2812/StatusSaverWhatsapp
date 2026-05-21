package com.example.statussaver.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.statussaver.data.StatusItem
import com.example.statussaver.ui.DownloadState

@Composable
fun MediaViewerScreen(
    items: List<StatusItem>,
    initialIndex: Int,
    downloadStates: Map<String, DownloadState>,
    onDownload: (StatusItem) -> Unit,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.size }
    var showOverlays by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { items[it].uri.toString() }
        ) { page ->
            val item = items[page]
            // Pass visibility state so click can toggle overlays
            MediaViewerItem(
                item = item,
                onToggleOverlays = { showOverlays = !showOverlays }
            )
        }

        // Overlays
        AnimatedVisibility(
            visible = showOverlays,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(8.dp)
        ) {
            val currentItem = items[pagerState.currentPage]
            val dlState = downloadStates[currentItem.uri.toString()] ?: DownloadState.Idle

            Row(
                modifier = Modifier.background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                )
            ) {
                // Back Button
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                // Download Button
                IconButton(
                    onClick = { onDownload(currentItem) },
                    enabled = dlState !is DownloadState.InProgress && dlState !is DownloadState.Done
                ) {
                    when (dlState) {
                        is DownloadState.InProgress -> {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        is DownloadState.Done -> {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaViewerItem(
    item: StatusItem,
    onToggleOverlays: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggleOverlays
            ),
        contentAlignment = Alignment.Center
    ) {
        if (item.isVideo) {
            VideoPlayer(uri = item.uri)
        } else {
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun VideoPlayer(uri: Uri) {
    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true // Show native video controls (play/pause/seek)
                    // The underlying AndroidView shouldn't block our clicks
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}
