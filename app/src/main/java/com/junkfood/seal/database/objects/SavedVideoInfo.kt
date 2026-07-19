package com.junkfood.seal.database.objects

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A snapshot of video metadata fetched by the "Video Info Download" tool. Saved as a card
 * on the tool page so the user can revisit title/description/tags later without re-fetching.
 */
@Entity
@Serializable
data class SavedVideoInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    @ColumnInfo(defaultValue = "") val description: String = "",
    @ColumnInfo(defaultValue = "[]") val tagsJson: String = "[]",
    @ColumnInfo(defaultValue = "") val videoUrl: String = "",
    @ColumnInfo(defaultValue = "") val thumbnailUrl: String = "",
    @ColumnInfo(defaultValue = "") val uploader: String = "",
    @ColumnInfo(defaultValue = "") val uploadDate: String = "",
    @ColumnInfo(defaultValue = "") val durationString: String = "",
    @ColumnInfo(defaultValue = "-1") val viewCount: Long = -1L,
    @ColumnInfo(defaultValue = "-1") val likeCount: Int = -1,
    @ColumnInfo(defaultValue = "") val extractor: String = "",
    @ColumnInfo(defaultValue = "0") val savedAtMillis: Long = 0L,
) {
    @Ignore
    constructor() : this(id = 0, title = "")

    @get:Ignore
    val tags: List<String>
        get() = runCatching { Json.decodeFromString<List<String>>(tagsJson) }.getOrDefault(emptyList())

    companion object {
        fun encodeTags(tags: List<String>): String =
            runCatching { Json.encodeToString(tags) }.getOrDefault("[]")
    }
}
