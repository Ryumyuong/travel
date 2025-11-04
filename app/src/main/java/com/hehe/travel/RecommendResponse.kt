package com.hehe.travel

data class RecommendResponse(
    val success: Boolean? = null,   // nullable!
    val message: String? = null,
    val days: List<DayPlan>? = null,
    val result: String? = null
)
