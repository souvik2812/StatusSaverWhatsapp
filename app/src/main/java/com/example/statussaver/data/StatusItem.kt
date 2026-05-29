package com.example.statussaver.data

import android.net.Uri

/**
 * Represents a single WhatsApp status item (image, video, or audio).
 *
 * @param uri        The SAF document URI pointing to the original file in the .Statuses directory.
 * @param name       The document display name (e.g. "img-20240101-WA0001.jpg").
 * @param mimeType   MIME type string: "image/jpeg", "video/mp4", "audio/opus", etc.
 * @param isVideo    True if this is a video file.
 * @param sizeBytes  File size in bytes; -1 if unknown.
 */
data class StatusItem(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val isVideo: Boolean,
    val sizeBytes: Long = -1L
) {
    /** True if this is an audio file (.opus, .m4a, .mp3, .aac). */
    val isAudio: Boolean
        get() = mimeType.startsWith("audio/")

    /** Human-readable formatted file size. */
    val formattedSize: String
        get() = when {
            sizeBytes <= 0L -> "Unknown"
            sizeBytes < 1024L -> "${sizeBytes} B"
            sizeBytes < 1024L * 1024L -> "${"%.1f".format(sizeBytes / 1024.0)} KB"
            else -> "${"%.1f".format(sizeBytes / (1024.0 * 1024.0))} MB"
        }
}
