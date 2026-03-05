package com.junkfood.seal.database.objects

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a download that is queued to execute at a future time.
 *
 * [preferencesJson] stores a JSON-serialised [com.junkfood.seal.util.DownloadUtil.DownloadPreferences]
 * so that the WorkManager worker can reconstruct the exact download configuration.
 */
@Entity(tableName = "ScheduledTask")
data class ScheduledTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** The URL to download */
    val url: String,
    /** Human-readable title (best-effort, may be empty) */
    val title: String = "",
    /** Thumbnail URL for display in the Scheduled Downloads page */
    val thumbnailUrl: String = "",
    /** Epoch-milliseconds when the download should begin */
    val scheduledTimeMillis: Long,
    /** See [com.junkfood.seal.util.ScheduleNetworkPreference] */
    val networkPreference: Int = 1, // BOTH by default
    /** True when the URL points to a playlist rather than a single video */
    val isPlaylist: Boolean = false,
    /** JSON-encoded DownloadPreferences */
    val preferencesJson: String = "",
    /** WorkManager unique work-request name (used for cancellation) */
    val workRequestId: String = "",
    /** Epoch-milliseconds when this record was created */
    val createdAtMillis: Long = System.currentTimeMillis(),
)
