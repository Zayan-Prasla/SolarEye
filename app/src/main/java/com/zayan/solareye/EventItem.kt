package com.zayan.solareye

data class EventItem(
    val detectionType: String? = null,
    val time: String? = null,
    val thumbnail: String? = null,
    val headerTitle: String? = null
) {
    val isHeader: Boolean
        get() = headerTitle != null
}