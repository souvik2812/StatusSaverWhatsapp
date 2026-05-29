package com.example.statussaver.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for:
 *  1. Persisting the SAF granted URI permission across app restarts.
 *  2. Traversing the WhatsApp .Statuses document tree to produce [StatusItem] objects.
 *  3. Copying a status file to the correct public folder via MediaStore.
 */
class StorageRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "status_saver_prefs"
        private const val KEY_GRANTED_URI = "granted_tree_uri"

        private const val DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"

        // The document ID of the WhatsApp .Statuses folder on primary storage
        private const val WA_STATUS_DOC_ID =
            "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses"

        /**
         * Document URI used as EXTRA_INITIAL_URI hint so the SAF picker
         * opens *directly inside* the .Statuses folder — the user only needs
         * to tap "Use this folder" once. No manual navigation required.
         */
        val WHATSAPP_STATUS_INITIAL_URI: Uri = DocumentsContract.buildDocumentUri(
            DOCUMENTS_AUTHORITY,
            WA_STATUS_DOC_ID
        )

        /**
         * Tree URI used after the user grants access — passed to
         * DocumentFile.fromTreeUri() and takePersistableUriPermission().
         */
        val WHATSAPP_STATUS_TREE_URI: Uri = DocumentsContract.buildTreeDocumentUri(
            DOCUMENTS_AUTHORITY,
            WA_STATUS_DOC_ID
        )

        // ── Supported MIME types ──────────────────────────────────────────────
        private val ALLOWED_MIME_TYPES = setOf(
            // Images
            "image/jpeg",
            // Videos
            "video/mp4",
            // Audio — WhatsApp voice notes & audio statuses
            "audio/opus",   // .opus (WhatsApp voice notes)
            "audio/ogg",    // .ogg (some devices report opus as ogg)
            "audio/mp4",    // .m4a
            "audio/mpeg",   // .mp3
            "audio/aac",    // .aac
            "audio/3gpp"    // .3gp audio
        )
    }

    // ─── Persisted Permission ────────────────────────────────────────────────

    /** Returns the persisted tree URI, or null if the user has not yet granted access. */
    fun getPersistedUri(): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_GRANTED_URI, null) ?: return null
        return Uri.parse(raw)
    }

    /**
     * Called once the user grants access via [ActivityResultContracts.OpenDocumentTree].
     * Acquires a persistable read permission and stores the URI so the app can access
     * it on every subsequent launch without prompting again.
     */
    fun persistUri(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GRANTED_URI, uri.toString())
            .apply()
    }

    /** Clears the persisted URI (used when the user wants to reset access). */
    fun clearPersistedUri() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_GRANTED_URI, null)
        if (raw != null) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(raw),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        prefs.edit().remove(KEY_GRANTED_URI).apply()
    }

    // ─── Directory Traversal ─────────────────────────────────────────────────

    /**
     * Traverses the granted document tree and returns a list of [StatusItem] objects
     * for every `.jpg`, `.mp4`, `.opus`, `.m4a`, `.mp3`, `.aac` file found.
     *
     * This function is safe to call on [Dispatchers.IO] — all I/O happens there.
     */
    suspend fun fetchStatuses(treeUri: Uri): List<StatusItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext emptyList()

        val results = mutableListOf<StatusItem>()
        traverseDocument(root, results)
        // Sort newest first (by name which contains a timestamp)
        results.sortedByDescending { it.name }
    }

    private fun traverseDocument(dir: DocumentFile, out: MutableList<StatusItem>) {
        val children = dir.listFiles()
        for (doc in children) {
            when {
                doc.isDirectory -> traverseDocument(doc, out)
                doc.isFile -> {
                    val mime = doc.type ?: guessMimeType(doc.name ?: "") ?: continue
                    if (mime !in ALLOWED_MIME_TYPES) continue
                    out.add(
                        StatusItem(
                            uri = doc.uri,
                            name = doc.name ?: "status_${System.currentTimeMillis()}",
                            mimeType = mime,
                            isVideo = mime == "video/mp4",
                            sizeBytes = doc.length()
                        )
                    )
                }
            }
        }
    }

    private fun guessMimeType(name: String): String? = when {
        name.endsWith(".jpg", ignoreCase = true) ||
        name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
        name.endsWith(".mp4", ignoreCase = true)  -> "video/mp4"
        name.endsWith(".opus", ignoreCase = true) -> "audio/opus"
        name.endsWith(".ogg", ignoreCase = true)  -> "audio/ogg"
        name.endsWith(".m4a", ignoreCase = true)  -> "audio/mp4"
        name.endsWith(".mp3", ignoreCase = true)  -> "audio/mpeg"
        name.endsWith(".aac", ignoreCase = true)  -> "audio/aac"
        name.endsWith(".3gp", ignoreCase = true)  -> "audio/3gpp"
        else -> null
    }

    // ─── MediaStore Writer ───────────────────────────────────────────────────

    /**
     * Copies [item] from the SAF document tree into the appropriate public folder:
     *  - Images  → Pictures/StatusSaver
     *  - Videos  → Movies/StatusSaver  (Bug fix: Downloads is NOT allowed for Video.Media)
     *  - Audio   → Music/StatusSaver
     *
     * @return The [Uri] of the newly created MediaStore entry, or null on failure.
     */
    suspend fun downloadStatus(item: StatusItem): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // ── Choose the correct MediaStore collection & relative directory ─────
        val (collection, relativeDir) = when {
            item.isAudio -> {
                // Audio files → Music/StatusSaver
                val col = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                col to "${Environment.DIRECTORY_MUSIC}/StatusSaver"
            }
            item.isVideo -> {
                // Videos → Movies/StatusSaver
                // ⚠ IMPORTANT: Android 10+ does NOT allow Video.Media to write to "Downloads".
                // Allowed directories for Video.Media are: DCIM, Movies, Pictures.
                val col = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                col to "${Environment.DIRECTORY_MOVIES}/StatusSaver"
            }
            else -> {
                // Images → Pictures/StatusSaver
                val col = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                col to "${Environment.DIRECTORY_PICTURES}/StatusSaver"
            }
        }

        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val destUri = resolver.insert(collection, cv) ?: return@withContext null

        runCatching {
            resolver.openInputStream(item.uri)?.use { input ->
                resolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cv.clear()
                cv.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(destUri, cv, null, null)
            }
        }.onFailure {
            // Clean up the orphaned MediaStore record on failure
            runCatching { resolver.delete(destUri, null, null) }
            return@withContext null
        }

        destUri
    }
}
