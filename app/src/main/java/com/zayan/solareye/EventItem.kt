package com.zayan.solareye

data class EventItem(
    val detectionType: String? = null,
    val time: String? = null,
    val thumbnail: Int? = R.drawable.sample_event_history,
    val headerTitle: String? = null
) {
    val isHeader: Boolean
        get() = headerTitle != null
}