package com.glassnote.data.settings

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies a user-picked background into app-private storage.
 *
 * A content:// URI can be revoked by the provider or by the system after a
 * reboot. Owning the bytes makes the chosen background survive restarts and
 * avoids keeping a persistable URI permission for an unrelated gallery app.
 */
class BackgroundImageStore(context: Context) {

    private val appContext = context.applicationContext
    private val backgroundsDirectory = File(appContext.filesDir, DIRECTORY_NAME)

    suspend fun import(source: Uri): Uri = withContext(Dispatchers.IO) {
        backgroundsDirectory.mkdirs()
        check(backgroundsDirectory.isDirectory) { "Background directory is unavailable" }

        val mimeType = appContext.contentResolver.getType(source)
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.lowercase()
            ?.takeIf { it.matches(SAFE_EXTENSION) }
            ?: "img"

        val finalFile = File(
            backgroundsDirectory,
            "ambient_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension",
        )
        val temporaryFile = File(backgroundsDirectory, "${finalFile.name}.tmp")

        try {
            appContext.contentResolver.openInputStream(source)?.use { input ->
                temporaryFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Unable to open selected image")

            if (temporaryFile.length() <= 0L) {
                throw IOException("Selected image is empty")
            }

            if (!temporaryFile.renameTo(finalFile)) {
                temporaryFile.copyTo(finalFile, overwrite = true)
                temporaryFile.delete()
            }
            Uri.fromFile(finalFile)
        } catch (failure: Exception) {
            temporaryFile.delete()
            finalFile.delete()
            throw failure
        }
    }

    suspend fun deleteManagedImage(uriString: String?) = withContext(Dispatchers.IO) {
        managedFileFor(uriString)?.delete()
        Unit
    }

    suspend fun clearManagedImages() = withContext(Dispatchers.IO) {
        backgroundsDirectory.listFiles()?.forEach { file -> file.delete() }
        Unit
    }

    private fun managedFileFor(uriString: String?): File? {
        if (uriString.isNullOrBlank()) return null
        val uri = uriString.toUri()
        if (uri.scheme != "file") return null

        val path = uri.path ?: return null
        val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return null
        val directory = runCatching { backgroundsDirectory.canonicalFile }.getOrNull() ?: return null
        val directoryPrefix = directory.path + File.separator

        return file.takeIf { it.path.startsWith(directoryPrefix) }
    }

    private companion object {
        const val DIRECTORY_NAME = "backgrounds"
        val SAFE_EXTENSION = Regex("[a-z0-9]{1,5}")
    }
}
