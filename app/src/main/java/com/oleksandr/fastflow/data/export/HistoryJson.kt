package com.oleksandr.fastflow.data.export

import com.oleksandr.fastflow.domain.model.Fast
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON export and import of the history (SPEC 3.5).
 *
 * Lives in `data` rather than `domain` so the serialization annotations stay
 * out of the pure layer.
 */
object HistoryJson {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    private data class FastDto(
        val id: Long = 0,
        val planId: String,
        val targetMinutes: Int,
        val eatingWindowMinutes: Int? = null,
        val startMillis: Long,
        val endMillis: Long? = null,
        val timeZoneId: String,
        val note: String? = null,
        val createdAt: Long = 0,
        val updatedAt: Long = 0,
    )

    @Serializable
    private data class Backup(
        val version: Int = FORMAT_VERSION,
        val exportedAt: Long,
        val fasts: List<FastDto>,
    )

    const val FORMAT_VERSION = 1

    fun export(fasts: List<Fast>, nowMillis: Long): String = json.encodeToString(
        // Explicit serializer: the reified overload cannot see a private type.
        Backup.serializer(),
        Backup(
            exportedAt = nowMillis,
            fasts = fasts.sortedBy { it.startMillis }.map { it.toDto() },
        ),
    )

    /**
     * Reads a backup back into domain models.
     *
     * Ids are dropped so imported records are appended rather than overwriting
     * whatever currently occupies those rows.
     */
    fun import(text: String): List<Fast> =
        json.decodeFromString(Backup.serializer(), text).fasts.map { it.toDomain() }

    private fun Fast.toDto() = FastDto(
        id = id,
        planId = planId,
        targetMinutes = targetMinutes,
        eatingWindowMinutes = eatingWindowMinutes,
        startMillis = startMillis,
        endMillis = endMillis,
        timeZoneId = timeZoneId,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun FastDto.toDomain() = Fast(
        id = 0,
        planId = planId,
        targetMinutes = targetMinutes,
        eatingWindowMinutes = eatingWindowMinutes,
        startMillis = startMillis,
        endMillis = endMillis,
        timeZoneId = timeZoneId,
        note = note,
        createdAt = if (createdAt == 0L) startMillis else createdAt,
        updatedAt = if (updatedAt == 0L) startMillis else updatedAt,
    )
}
