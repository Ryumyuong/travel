package com.hehe.travel

import java.util.Date

data class TripHistoryItem(
    val docId: String,
    val country: String,
    val nickname: String,
    val nights: Int,
    val daysCount: Int,
    val startDate: String,
    val endDate: String,
    val travelStyle: String,
    val savedAt: Date?,
    val keywords: List<String>,
    val daysJson: String
)
