package com.hehe.travel

import java.util.Date

/**
 * 여행 기록 아이템
 * @param type 카드 타입: 1=일반(새미패스X), 2=새미패스, 3=취향맞춤(planhistory)
 */
data class TravelHistoryItem(
    val documentId: String = "",
    val country: String = "",
    val nights: Int = 0,
    val daysCount: Int = 0,
    val startDate: String = "",
    val endDate: String = "",
    val travelStyle: String = "",
    val hasSemiPass: Boolean = false,
    val type: Int = 1,  // 1: 일반, 2: 새미패스, 3: 취향맞춤
    val savedAt: Date? = null,
    val budgetLabel: String = "",
    var isPinned: Boolean = false  // 고정 여부
) {
    // 카드 타입에 따른 제목 생성
    fun getDisplayTitle(): String {
        // 날짜가 없는 경우 (SearchActivity에서 저장)
        val hasDate = nights > 0 || daysCount > 0
        val tripDuration = if (hasDate) "${nights}박${daysCount}일 " else ""

        return when (type) {
            1 -> {
                if (hasDate) {
                    "${tripDuration}여행"
                } else {
                    "추천하는 여행"
                }
            }
            2 -> {
                val prefix = when (budgetLabel) {
                    "부족", "매우 부족" -> "갓성비"
                    "여유로움", "매우 여유로움" -> "럭셔리"
                    else -> "균형잡힌"
                }
                "$prefix ${tripDuration}여행"
            }
            3 -> "내 취향에 딱 맞춘 ${tripDuration}여행"
            else -> "${tripDuration}여행"
        }
    }

    // 국기 + 여행지 표시
    fun getCountryWithFlag(): String {
        val flag = getCountryFlag(country)
        return "$flag $country"
    }

    // 국가명에 따른 국기 이모지 반환
    private fun getCountryFlag(countryName: String): String {
        return when {
            countryName.contains("일본") -> "\uD83C\uDDEF\uD83C\uDDF5"
            countryName.contains("한국") -> "\uD83C\uDDF0\uD83C\uDDF7"
            countryName.contains("중국") -> "\uD83C\uDDE8\uD83C\uDDF3"
            countryName.contains("대만") -> "\uD83C\uDDF9\uD83C\uDDFC"
            countryName.contains("홍콩") -> "\uD83C\uDDED\uD83C\uDDF0"
            countryName.contains("마카오") -> "\uD83C\uDDF2\uD83C\uDDF4"
            countryName.contains("태국") -> "\uD83C\uDDF9\uD83C\uDDED"
            countryName.contains("베트남") -> "\uD83C\uDDFB\uD83C\uDDF3"
            countryName.contains("싱가포르") -> "\uD83C\uDDF8\uD83C\uDDEC"
            countryName.contains("말레이시아") -> "\uD83C\uDDF2\uD83C\uDDFE"
            countryName.contains("인도네시아") || countryName.contains("발리") -> "\uD83C\uDDEE\uD83C\uDDE9"
            countryName.contains("필리핀") -> "\uD83C\uDDF5\uD83C\uDDED"
            countryName.contains("캄보디아") -> "\uD83C\uDDF0\uD83C\uDDED"
            countryName.contains("라오스") -> "\uD83C\uDDF1\uD83C\uDDE6"
            countryName.contains("미얀마") -> "\uD83C\uDDF2\uD83C\uDDF2"
            countryName.contains("인도") -> "\uD83C\uDDEE\uD83C\uDDF3"
            countryName.contains("네팔") -> "\uD83C\uDDF3\uD83C\uDDF5"
            countryName.contains("스리랑카") -> "\uD83C\uDDF1\uD83C\uDDF0"
            countryName.contains("몰디브") -> "\uD83C\uDDF2\uD83C\uDDFB"
            countryName.contains("미국") || countryName.contains("하와이") || countryName.contains("괌") || countryName.contains("사이판") -> "\uD83C\uDDFA\uD83C\uDDF8"
            countryName.contains("캐나다") -> "\uD83C\uDDE8\uD83C\uDDE6"
            countryName.contains("멕시코") -> "\uD83C\uDDF2\uD83C\uDDFD"
            countryName.contains("브라질") -> "\uD83C\uDDE7\uD83C\uDDF7"
            countryName.contains("아르헨티나") -> "\uD83C\uDDE6\uD83C\uDDF7"
            countryName.contains("칠레") -> "\uD83C\uDDE8\uD83C\uDDF1"
            countryName.contains("페루") -> "\uD83C\uDDF5\uD83C\uDDEA"
            countryName.contains("영국") || countryName.contains("런던") -> "\uD83C\uDDEC\uD83C\uDDE7"
            countryName.contains("프랑스") || countryName.contains("파리") -> "\uD83C\uDDEB\uD83C\uDDF7"
            countryName.contains("독일") -> "\uD83C\uDDE9\uD83C\uDDEA"
            countryName.contains("이탈리아") || countryName.contains("로마") -> "\uD83C\uDDEE\uD83C\uDDF9"
            countryName.contains("스페인") || countryName.contains("바르셀로나") -> "\uD83C\uDDEA\uD83C\uDDF8"
            countryName.contains("포르투갈") -> "\uD83C\uDDF5\uD83C\uDDF9"
            countryName.contains("네덜란드") -> "\uD83C\uDDF3\uD83C\uDDF1"
            countryName.contains("벨기에") -> "\uD83C\uDDE7\uD83C\uDDEA"
            countryName.contains("스위스") -> "\uD83C\uDDE8\uD83C\uDDED"
            countryName.contains("오스트리아") -> "\uD83C\uDDE6\uD83C\uDDF9"
            countryName.contains("체코") || countryName.contains("프라하") -> "\uD83C\uDDE8\uD83C\uDDFF"
            countryName.contains("폴란드") -> "\uD83C\uDDF5\uD83C\uDDF1"
            countryName.contains("헝가리") -> "\uD83C\uDDED\uD83C\uDDFA"
            countryName.contains("크로아티아") -> "\uD83C\uDDED\uD83C\uDDF7"
            countryName.contains("그리스") -> "\uD83C\uDDEC\uD83C\uDDF7"
            countryName.contains("터키") || countryName.contains("튀르키예") -> "\uD83C\uDDF9\uD83C\uDDF7"
            countryName.contains("러시아") -> "\uD83C\uDDF7\uD83C\uDDFA"
            countryName.contains("호주") || countryName.contains("시드니") -> "\uD83C\uDDE6\uD83C\uDDFA"
            countryName.contains("뉴질랜드") -> "\uD83C\uDDF3\uD83C\uDDFF"
            countryName.contains("피지") -> "\uD83C\uDDEB\uD83C\uDDEF"
            countryName.contains("두바이") || countryName.contains("아랍에미리트") -> "\uD83C\uDDE6\uD83C\uDDEA"
            countryName.contains("이집트") -> "\uD83C\uDDEA\uD83C\uDDEC"
            countryName.contains("모로코") -> "\uD83C\uDDF2\uD83C\uDDE6"
            countryName.contains("남아프리카") || countryName.contains("케이프타운") -> "\uD83C\uDDFF\uD83C\uDDE6"
            countryName.contains("케냐") -> "\uD83C\uDDF0\uD83C\uDDEA"
            else -> "\uD83C\uDF0D"  // 기본: 지구 이모지
        }
    }

    // 날짜 포맷
    fun getFormattedDate(): String {
        // 날짜가 없는 경우 저장 날짜 표시
        if (startDate.isEmpty() || endDate.isEmpty()) {
            return savedAt?.let {
                val sdf = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA)
                "${sdf.format(it)}"
            } ?: "날짜 미정"
        }
        val start = startDate.replace("-", ".")
        val end = endDate.replace("-", ".")
        return "$start - $end"
    }
}
