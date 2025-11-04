package com.hehe.travel

data class DayPlan(
    val day: Int,
    val title: String,
    val places: List<PlaceItem> = emptyList(),
    val description: String? = null
)
