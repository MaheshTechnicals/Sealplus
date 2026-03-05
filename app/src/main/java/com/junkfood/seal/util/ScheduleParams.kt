package com.junkfood.seal.util

/** Network constraint options for scheduled downloads */
enum class ScheduleNetworkPreference(val id: Int) {
    WIFI_ONLY(0),
    BOTH(1),
    MOBILE_DATA(2);

    companion object {
        fun fromId(id: Int): ScheduleNetworkPreference =
            entries.firstOrNull { it.id == id } ?: BOTH
    }
}

/**
 * Carries schedule information from the Configure dialog through
 * to the download engine (either Preset path or Custom/Format-Selection path).
 */
data class ScheduleParams(
    val scheduledTimeMillis: Long,
    val networkPreference: ScheduleNetworkPreference = ScheduleNetworkPreference.WIFI_ONLY,
)
