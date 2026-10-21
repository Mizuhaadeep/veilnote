package com.glassnote.data.local.converter

import androidx.room.TypeConverter
import com.glassnote.domain.model.NoteBlock
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Gson stores blocks as a tagged JSON array. The explicit `type` discriminator
 * keeps old data readable when new block kinds are added later.
 */
class NoteBlockListConverter {

    @TypeConverter
    fun fromBlocks(blocks: List<NoteBlock>?): String =
        gson.toJson(blocks.orEmpty(), blockListType)

    @TypeConverter
    fun toBlocks(value: String?): List<NoteBlock> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<NoteBlock>>(value, blockListType)
                .orEmpty()
                .filterNotNull()
        }.getOrDefault(emptyList())
    }

    companion object {
        private val gson: Gson = GsonBuilder()
            .registerTypeAdapter(NoteBlock::class.java, NoteBlockAdapter)
            .create()

        private val blockListType: Type = object : TypeToken<List<NoteBlock>>() {}.type

        fun encode(blocks: List<NoteBlock>): String =
            gson.toJson(blocks, blockListType)

        fun decode(value: String?): List<NoteBlock> {
            if (value.isNullOrBlank()) return emptyList()
            return runCatching {
                gson.fromJson<List<NoteBlock>>(value, blockListType)
                    .orEmpty()
                    .filterNotNull()
            }.getOrDefault(emptyList())
        }

        fun legacyContentBlocks(content: String): List<NoteBlock> =
            listOf(NoteBlock.TextBlock(content = content))
    }
}

private object NoteBlockAdapter : JsonSerializer<NoteBlock>, JsonDeserializer<NoteBlock> {

    override fun serialize(
        src: NoteBlock,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement = JsonObject().apply {
        addProperty("id", src.id)
        when (src) {
            is NoteBlock.TextBlock -> {
                addProperty("type", "text")
                addProperty("content", src.content)
            }

            is NoteBlock.ImageBlock -> {
                addProperty("type", "image")
                addProperty("localPath", src.localPath)
            }

            is NoteBlock.FileBlock -> {
                addProperty("type", "file")
                addProperty("localPath", src.localPath)
                addProperty("fileName", src.fileName)
                addProperty("fileSize", src.fileSize)
            }

            is NoteBlock.LocationBlock -> {
                addProperty("type", "location")
                addProperty("latitude", src.latitude)
                addProperty("longitude", src.longitude)
                addProperty("address", src.address)
            }
        }
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): NoteBlock? {
        if (!json.isJsonObject) return null
        val obj = json.asJsonObject
        val id = obj.stringOrNull("id") ?: return null
        return when (obj.stringOrNull("type")) {
            "text" -> NoteBlock.TextBlock(
                id = id,
                content = obj.stringOrNull("content").orEmpty(),
            )

            "image" -> NoteBlock.ImageBlock(
                id = id,
                localPath = obj.stringOrNull("localPath").orEmpty(),
            )

            "file" -> NoteBlock.FileBlock(
                id = id,
                localPath = obj.stringOrNull("localPath").orEmpty(),
                fileName = obj.stringOrNull("fileName").orEmpty(),
                fileSize = obj.stringOrNull("fileSize").orEmpty(),
            )

            "location" -> NoteBlock.LocationBlock(
                id = id,
                latitude = obj.get("latitude")?.asDouble ?: 0.0,
                longitude = obj.get("longitude")?.asDouble ?: 0.0,
                address = obj.stringOrNull("address").orEmpty(),
            )

            else -> null
        }
    }

    private fun JsonObject.stringOrNull(name: String): String? =
        get(name)?.takeUnless { it.isJsonNull }?.asString
}
