package com.glassnote.domain.model

/**
 * Palette used by a note's glass card.
 *
 * The [storageKey] is the value persisted in Room. Keep these stable when
 * introducing new colours so existing notes continue to resolve correctly.
 */
enum class AmbienceColor(val storageKey: String) {
    MIST("mist"),
    SAGE("sage"),
    OCEAN("ocean"),
    AMBER("amber"),
    ROSE("rose"),
    VIOLET("violet"),
    ;

    companion object {
        val DEFAULT: AmbienceColor = MIST

        fun fromStorageKey(key: String?): AmbienceColor =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}
