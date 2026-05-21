package com.example.statussaver.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.statussaver.data.StatusItem
import com.example.statussaver.theme.DeepTeal
import com.example.statussaver.theme.DownloadingAmber
import com.example.statussaver.theme.ElectricBlue
import com.example.statussaver.theme.NeonViolet
import com.example.statussaver.theme.SuccessGreen
import com.example.statussaver.ui.DownloadState
import com.example.statussaver.ui.GalleryUiState

// ─── Tab Definitions ─────────────────────────────────────────────────────────

private val TABS = listOf("All ✦", "Photos 🖼", "Videos 🎬")

// ─── Gallery Root ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    uiState: GalleryUiState,
    downloadStates: Map<String, DownloadState>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onGrantAccess: () -> Unit,
    onRefresh: () -> Unit,
    onDownload: (StatusItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {

        // ── Tab Row ──────────────────────────────────────────────────────────
        if (uiState is GalleryUiState.Success) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NeonViolet,
                edgePadding = 0.dp,
            ) {
                TABS.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }
        }

        // ── Body ─────────────────────────────────────────────────────────────
        when (uiState) {
            is GalleryUiState.NeedsPermission -> PermissionScreen(onGrantAccess = onGrantAccess)
            is GalleryUiState.Loading         -> LoadingScreen()
            is GalleryUiState.Error           -> ErrorScreen(message = uiState.message, onRetry = onRefresh)
            is GalleryUiState.Success         -> {
                val filtered = when (selectedTab) {
                    1 -> uiState.items.filter { !it.isVideo }
                    2 -> uiState.items.filter { it.isVideo }
                    else -> uiState.items
                }
                if (filtered.isEmpty()) {
                    EmptyScreen(selectedTab = selectedTab, onRefresh = onRefresh)
                } else {
                    StatusGrid(
                        items = filtered,
                        downloadStates = downloadStates,
                        onDownload = onDownload
                    )
                }
            }
        }
    }
}

// ─── Permission Screen ────────────────────────────────────────────────────────

@Composable
private fun PermissionScreen(onGrantAccess: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D1A), Color(0xFF13132B), Color(0xFF1A1A3E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Glowing icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(listOf(NeonViolet.copy(alpha = 0.3f), Color.Transparent)),
                        shape = CircleShape
                    )
                    .border(1.dp, NeonViolet.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = NeonViolet,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Status Saver",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Grant access to your WhatsApp Statuses folder to view and save media.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // CTA button with gradient
            Button(
                onClick = onGrantAccess,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(
                        Brush.horizontalGradient(listOf(NeonViolet, ElectricBlue))
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Grant Folder Access",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Navigate to:\nAndroid/media/com.whatsapp/WhatsApp/Media/.Statuses",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// ─── Loading Screen ───────────────────────────────────────────────────────────

@Composable
private fun LoadingScreen() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NeonViolet, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text("Loading statuses…", color = Color.White.copy(alpha = 0.6f))
        }
    }
}

// ─── Error Screen ─────────────────────────────────────────────────────────────

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Error, contentDescription = null, tint = com.example.statussaver.theme.ErrorRed, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}

// ─── Empty Screen ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyScreen(selectedTab: Int, onRefresh: () -> Unit) {
    val label = when (selectedTab) {
        1 -> "No photos found"
        2 -> "No videos found"
        else -> "No statuses found"
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("🌫", fontSize = 52.sp)
            Spacer(Modifier.height(12.dp))
            Text(label, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Pull up on WhatsApp and view some statuses first.", color = Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonViolet, modifier = Modifier.size(32.dp))
            }
        }
    }
}

// ─── Status Grid ──────────────────────────────────────────────────────────────

@Composable
private fun StatusGrid(
    items: List<StatusItem>,
    downloadStates: Map<String, DownloadState>,
    onDownload: (StatusItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = items, key = { it.uri.toString() }) { item ->
            StatusCard(
                item = item,
                downloadState = downloadStates[item.uri.toString()] ?: DownloadState.Idle,
                onDownload = { onDownload(item) }
            )
        }
    }
}

// ─── Status Card ─────────────────────────────────────────────────────────────

@Composable
private fun StatusCard(
    item: StatusItem,
    downloadState: DownloadState,
    onDownload: () -> Unit
) {
    val context = LocalContext.current

    // Scale animation on download press
    val scale by animateFloatAsState(
        targetValue = if (downloadState is DownloadState.InProgress) 0.96f else 1f,
        animationSpec = tween(150),
        label = "card_scale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13132B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .scale(scale)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(NeonViolet.copy(alpha = 0.4f), ElectricBlue.copy(alpha = 0.2f))),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Thumbnail ─────────────────────────────────────────────────
            val imageRequest = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .apply {
                    if (item.isVideo) {
                        decoderFactory(VideoFrameDecoder.Factory())
                        videoFrameMillis(1000L)
                    }
                }
                .build()

            AsyncImage(
                model = imageRequest,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // ── Gradient Overlay (bottom) ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.55f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // ── Video Play Icon Overlay ────────────────────────────────────
            if (item.isVideo) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                )
            }

            // ── Bottom Row: file name + download button ────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.formattedSize,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // ── Download Action Button ─────────────────────────────────
                DownloadButton(downloadState = downloadState, onDownload = onDownload)
            }
        }
    }
}

// ─── Download Button ─────────────────────────────────────────────────────────

@Composable
private fun DownloadButton(downloadState: DownloadState, onDownload: () -> Unit) {
    val bgColor = when (downloadState) {
        is DownloadState.Idle       -> NeonViolet
        is DownloadState.InProgress -> DownloadingAmber
        is DownloadState.Done       -> SuccessGreen
        is DownloadState.Failed     -> com.example.statussaver.theme.ErrorRed
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .background(bgColor, shape = CircleShape)
            .clickable(enabled = downloadState !is DownloadState.InProgress && downloadState !is DownloadState.Done) {
                onDownload()
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = downloadState is DownloadState.InProgress,
            enter = fadeIn() + scaleIn(),
            exit  = fadeOut() + scaleOut()
        ) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(
            visible = downloadState is DownloadState.Done,
            enter = fadeIn() + scaleIn(),
            exit  = fadeOut() + scaleOut()
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Saved", tint = Color.White, modifier = Modifier.size(18.dp))
        }
        AnimatedVisibility(
            visible = downloadState is DownloadState.Idle || downloadState is DownloadState.Failed,
            enter = fadeIn() + scaleIn(),
            exit  = fadeOut() + scaleOut()
        ) {
            Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
