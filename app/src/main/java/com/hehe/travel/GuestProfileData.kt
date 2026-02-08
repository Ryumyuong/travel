package com.hehe.travel

/**
 * 비회원 프로필 데이터 임시 저장용 싱글톤
 * 로그인 전까지 메모리에 유지
 */
object GuestProfileData {
    // 기본 프로필 (SammyFirstQuestion)
    var nickname: String = ""
    var gender: String = ""
    var ageDecade: Int = 30
    var purposes: List<String> = emptyList()

    // SemiPass 데이터 (SammySecondQuestion)
    var companion: String = ""
    var flightTimeValue: Double = 10.0
    var flightTimeLabel: String = ""
    var budgetValue: Int = 50
    var budgetLabel: String = ""
    var energyValue: Int = 50
    var energyLabel: String = ""
    var shoppingValue: Int = 50
    var shoppingLabel: String = ""
    var personalityValue: Int = 50
    var personalityLabel: String = ""
    var sleepValue: Int = 50
    var sleepLabel: String = ""
    var accommodationValue: Double = 5.0
    var accommodationLabel: String = ""

    // 나라 및 날짜 정보
    var country: String = ""
    var startDate: String = ""
    var endDate: String = ""
    var keywords: List<String> = emptyList()

    // AI 결과 데이터
    var flightDesc: String = ""
    var accommodationDesc: String = ""
    var restaurantDesc: String = ""

    fun clear() {
        // 기본 프로필
        nickname = ""
        gender = ""
        ageDecade = 30
        purposes = emptyList()

        // SemiPass 데이터
        companion = ""
        flightTimeValue = 10.0
        flightTimeLabel = ""
        budgetValue = 50
        budgetLabel = ""
        energyValue = 50
        energyLabel = ""
        shoppingValue = 50
        shoppingLabel = ""
        personalityValue = 50
        personalityLabel = ""
        sleepValue = 50
        sleepLabel = ""
        accommodationValue = 5.0
        accommodationLabel = ""

        // 나라/날짜
        country = ""
        startDate = ""
        endDate = ""
        keywords = emptyList()

        // AI 결과
        flightDesc = ""
        accommodationDesc = ""
        restaurantDesc = ""
    }

    fun hasProfile(): Boolean {
        return nickname.isNotEmpty()
    }

    fun hasSemiPass(): Boolean {
        return companion.isNotEmpty()
    }
}
