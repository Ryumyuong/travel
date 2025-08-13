package com.hehe.travel

data class ItineraryDay(
    val day: Int = 0,
    val title: String = "",
    val activities: List<Activity> = emptyList()
)