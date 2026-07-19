package com.junkfood.seal.database.objects

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.junkfood.seal.util.Comment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A snapshot of a video's comments fetched by the "Comment Download" tool. Comments are
 * serialized to JSON and stored in a single column (same pattern as [SavedVideoInfo]'s tags)
 * rather than a separate child table — comment sets are read/written as a whole unit (never
 * queried per-comment), so a normalized table would only add join overhead for no benefit.
 */
@Entity
@Serializable
data class SavedCommentSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoTitle: String,
    @ColumnInfo(defaultValue = "") val uploader: String = "",
    @ColumnInfo(defaultValue = "") val videoUrl: String = "",
    @ColumnInfo(defaultValue = "") val thumbnailUrl: String = "",
    @ColumnInfo(defaultValue = "[]") val commentsJson: String = "[]",
    @ColumnInfo(defaultValue = "0") val commentCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val savedAtMillis: Long = 0L,
) {
    @Ignore
    constructor() : this(id = 0, videoTitle = "")

    @get:Ignore
    val comments: List<Comment>
        get() = runCatching { Json.decodeFromString<List<Comment>>(commentsJson) }
            .getOrDefault(emptyList())

    companion object {
        fun encodeComments(comments: List<Comment>): String =
            runCatching { Json.encodeToString(comments) }.getOrDefault("[]")
    }
}
