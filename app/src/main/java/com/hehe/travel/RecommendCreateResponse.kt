package com.hehe.travel

data class RecommendCreateResponse(
    val success: Boolean,
    val planId: String?,
    val message: String? = null
)
