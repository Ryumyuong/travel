package com.hehe.travel

data class PlanItem(
    val title: String,
    val description: String,
    val time: String = "",
    val duration: String = "",
    val tip: String = ""
)