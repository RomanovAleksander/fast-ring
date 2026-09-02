package com.oleksandr.fastflow.domain.model

/**
 * Which colour set the app paints with (SPEC 5.1).
 *
 * Lives in `domain` because it is persisted in DataStore and read by the
 * widget, not only by Compose.
 */
enum class ThemePalette {
    MINT,
    SYSTEM,
    ;

    companion object {
        val DEFAULT = MINT

        fun fromName(name: String?): ThemePalette =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
