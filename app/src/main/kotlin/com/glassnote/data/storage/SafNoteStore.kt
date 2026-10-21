package com.glassnote.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.glassnote.data.local.converter.NoteBlockListConverter
import com.glassnote.domain.model.AmbienceColor
import com.glassnote.domain.model.Note
import com.glassnote.domain.model.NoteBlock
import com.glassnote.domain.model.Tag
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SafFolder(
    val treeUri: Uri,
    val folderName: String,
)

/**
 * Public-folder mirror. Room remains the cache/source of truth; this class
 * writes one JSON note per `.veil` file and reads them again on startup.
 */
class SafNoteStore(
    private val context: Context,
) {

    private val resolver get() = context.contentResolver

    suspend fun prepare(treeUri: Uri): SafFolder = withContext(Dispatchers.IO) {
        resolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        val folder = resolveFolder(treeUri)
        SafFolder(
            treeUri = treeUri,
            folderName = folder.name?.takeIf(String::isNotBlank) ?: FOLDER_NAME,
        )
    }

    suspend fun readAll(treeUri: Uri): List<Note> = withContext(Dispatchers.IO) {
        val folder = resolveFolder(treeUri)
        folder.listFiles()
            .asSequence()
            .filter { it.isFile && it.name?.endsWith(EXTENSION, ignoreCase = true) == true }
            .mapNotNull { file ->
                runCatching { parseNote(readText(file.uri)) }.getOrNull()
            }
            .toList()
    }

    suspend fun export(treeUri: Uri, notes: List<Note>) = withContext(Dispatchers.IO) {
        val folder = resolveFolder(treeUri)
        val existing = folder.listFiles()
            .filter { it.isFile && it.name?.endsWith(EXTENSION, ignoreCase = true) == true }

        val expectedNames = mutableSetOf<String>()
        notes.forEach { note ->
            val name = fileNameFor(note.id)
            expectedNames += name
            val file = existing.firstOrNull { it.name == name }
                ?: folder.createFile(MIME_TYPE, name)
                ?: throw IOException("Cannot create $name")

            resolver.openOutputStream(file.uri, "wt")?.bufferedWriter()?.use { writer ->
                writer.write(note.toJson().toString(2))
            } ?: throw IOException("Cannot write ${file.uri}")
        }

        existing
            .filter { file ->
                val name = file.name.orEmpty()
                name.startsWith(FILE_PREFIX) && name !in expectedNames
            }
            .forEach { stale -> stale.delete() }
    }

    private fun resolveFolder(treeUri: Uri): DocumentFile {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("Cannot open tree $treeUri")
        if (!root.isDirectory || !root.canRead()) {
            throw SecurityException("Tree is not readable: $treeUri")
        }
        if (root.name.equals(FOLDER_NAME, ignoreCase = true)) return root

        val existing = root.listFiles().firstOrNull {
            it.isDirectory && it.name.equals(FOLDER_NAME, ignoreCase = true)
        }
        return existing
            ?: root.createDirectory(FOLDER_NAME)
            ?: throw IOException("Cannot create $FOLDER_NAME under ${root.name}")
    }

    private fun readText(uri: Uri): String {
        return resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IOException("Cannot read $uri")
    }

    private fun parseNote(raw: String): Note {
        val json = JSONObject(raw)
        val tagArray = json.optJSONArray("tags") ?: JSONArray()
        val tags = buildList {
            for (index in 0 until tagArray.length()) {
                val tag = tagArray.optJSONObject(index) ?: continue
                add(
                    Tag(
                        id = tag.optLong("id", 0L),
                        name = tag.optString("name"),
                        createdAt = tag.optLong("createdAt", 0L),
                    ),
                )
            }
        }
        val storedPasswordHash = json.optString("passwordHash")
            .takeIf { it.isNotBlank() && it != "null" }
        val blocks = json.optJSONArray("blocks")?.let { array ->
            NoteBlockListConverter.decode(array.toString())
        } ?: json.optString("content").let { legacy ->
            if (legacy.isEmpty() && !json.has("content")) {
                emptyList()
            } else {
                listOf(NoteBlock.TextBlock(content = legacy))
            }
        }
        return Note(
            id = json.getLong("id"),
            title = json.optString("title"),
            blocks = blocks,
            ambienceColor = AmbienceColor.fromStorageKey(json.optString("ambience")),
            pinned = json.optBoolean("pinned", false),
            isPinnedBottom = json.optBoolean("pinnedBottom", false),
            isEncrypted = json.optBoolean("isEncrypted", false) &&
                storedPasswordHash != null,
            passwordHash = storedPasswordHash,
            createdAt = json.optLong("createdAt", 0L),
            updatedAt = json.optLong("updatedAt", 0L),
            tags = tags,
        )
    }

    private fun Note.toJson(): JSONObject = JSONObject().apply {
        put("version", 2)
        put("id", id)
        put("title", title)
        put("blocks", JSONArray(NoteBlockListConverter.encode(blocks)))
        put("ambience", ambienceColor.storageKey)
        put("pinned", pinned)
        put("pinnedBottom", isPinnedBottom)
        put("isEncrypted", isEncrypted)
        put("passwordHash", passwordHash ?: JSONObject.NULL)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put(
            "tags",
            JSONArray().apply {
                tags.forEach { tag ->
                    put(
                        JSONObject().apply {
                            put("id", tag.id)
                            put("name", tag.name)
                            put("createdAt", tag.createdAt)
                        },
                    )
                }
            },
        )
    }

    companion object {
        const val FOLDER_NAME = "Veilnote"
        const val EXTENSION = ".veil"
        private const val FILE_PREFIX = "note_"
        private const val MIME_TYPE = "application/octet-stream"

        fun fileNameFor(noteId: Long): String = "$FILE_PREFIX$noteId$EXTENSION"
    }
}
