package com.hehe.travel

data class RecommendRequest(
    val country: String,
    val startDate: String,
    val endDate: String,
    val gender: String?,
    val age: Int?,
    val preference: List<String>?
)
