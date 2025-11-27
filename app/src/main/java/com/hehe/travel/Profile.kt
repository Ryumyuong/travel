package com.hehe.travel

data class Profile(
    val nickname: String? = null,
    val gender: String? = null,
    val ageDecade: Int? = null,      // Firestore에서 숫자면 Int/Long 모두 OK
    val budgetLabel: String? = null,
    val energyLabel: String? = null,
    val companion: String? = null,
    val tags: List<String>? = null
)
