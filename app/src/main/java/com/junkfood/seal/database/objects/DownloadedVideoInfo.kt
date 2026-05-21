package com.junkfood.seal.database.objects

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class DownloadedVideoInfo(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val videoTitle: String,
    val videoAuthor: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val videoPath: String,
    @ColumnInfo(defaultValue = "") val videoId: String = "",
    @ColumnInfo(defaultValue = "Unknown") val extractor: String = "Unknown",
    @ColumnInfo(defaultValue = "-1") val downloadTimeMillis: Long = -1L,
    @ColumnInfo(defaultValue = "-1") val averageSpeedBytesPerSec: Long = -1L,
    @ColumnInfo(defaultValue = "0") val isHidden: Boolean = false,
) {
    @Ignore
    constructor() :
        this(
            id = 0,
            videoTitle = "Video",
            videoAuthor = "Author",
            videoUrl = "Url",
            thumbnailUrl = "Thumbnail",
            videoPath = "Path",
            videoId = "",
            extractor = "Unknown",
            downloadTimeMillis = -1L,
            averageSpeedBytesPerSec = -1L,
            isHidden = false,
        )
}
