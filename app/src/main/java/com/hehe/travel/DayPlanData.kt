package com.hehe.travel

data class DayPlanData(
    val day: Int,
    val title: String,
    val places: MutableList<PlaceData>
)
