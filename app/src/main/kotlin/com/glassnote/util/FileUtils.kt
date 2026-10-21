package com.glassnote.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.Locale

data class CopiedFile(
    val absolutePath: String,
    val fileName: String,
    val fileSize: String,
)

object FileUtils {

    /**
     * Copies a content Uri into the app's private attachments directory and
     * returns the absolute file path. Returns null when the Uri cannot be read.
     */
    suspend fun copyUriToInternalStorage(context: Context, uri: Uri): String? =
        copyFileToInternalStorage(context, uri)?.absolutePath

    /**
     * Copies an arbitrary Uri and keeps the display name and human-readable
     * size so a FileBlock can render its own metadata without reopening the
     * ContentResolver.
     */
    suspend fun copyFileToInternalStorage(context: Context, uri: Uri): CopiedFile? =
        withContext(Dispatchers.IO) {
            runCatching {
                val metadata = queryMetadata(context, uri)
                val originalName = metadata.first
                    ?.substringAfterLast('/')
                    ?.substringAfterLast('\\')
                    ?.takeIf { it.isNotBlank() }
                    ?: "attachment"
                val extension = originalName.substringAfterLast('.', "")
                    .takeIf { it.isNotBlank() && it.length <= 12 }
                val storedName = buildString {
                    append(UUID.randomUUID().toString())
                    if (extension != null) {
                        append('.')
                        append(extension)
                    }
                }

                val targetDir = File(context.filesDir, ATTACHMENTS_DIR).apply {
                    if (!exists()) mkdirs()
                }
                val target = File(targetDir, storedName)

                val input = context.contentResolver.openInputStream(uri)
                    ?: return@runCatching null
                input.use { source ->
                    target.outputStream().use { sink ->
                        source.copyTo(sink, DEFAULT_BUFFER_SIZE)
                    }
                }

                val size = metadata.second ?: target.length()
                CopiedFile(
                    absolutePath = target.absolutePath,
                    fileName = originalName,
                    fileSize = formatFileSize(size),
                )
            }.getOrNull()
        }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 0L) return "0 KB"
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) {
            return String.format(Locale.US, "%.1f KB", kb)
        }
        val mb = kb / 1024.0
        if (mb < 1024.0) {
            return String.format(Locale.US, "%.1f MB", mb)
        }
        return String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }

    private fun queryMetadata(context: Context, uri: Uri): Pair<String?, Long?> {
        var name: String? = null
        var size: Long? = null
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                        name = cursor.getString(nameIndex)
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        }
        return name to size
    }

    private const val ATTACHMENTS_DIR = "attachments"
}

/** Convenience for Coil: model the local absolute path as a File. */
fun String.asLocalFile(): File = File(this)
