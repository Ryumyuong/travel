package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.hehe.travel.databinding.ActivityDateRangeBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import android.view.View

class DateRangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDateRangeBinding
    private lateinit var adapter: DayAdapter
    private val range = RangeState()
    private var curYM: YearMonth = YearMonth.now()
    private lateinit var countryName: String
    private lateinit var userName: String

    // ⭐ 타임아웃 60초로 설정 (기본값 10초 → 60초)
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Gemini API Key
    private val GEMINI_API_KEY = BuildConfig.API_KEY

    // Profile 데이터
    private var gender: String = ""
    private var age: Int = 0
    private var profileTags: List<String> = emptyList()
    private var hasSemiPass: Boolean = false
    private var budgetLabel: String = ""

    // 여행 취향 데이터
    private var travelStyle: String = ""
    private var travelTraits: List<String> = emptyList()

    // 로딩 상태
    private var isLoading = false

    // 진입 경로 (semipass: 새미패스 경유 → type 2, taste: 취향맞춤 → type 3, guest: 비회원)
    private var flowType: String = "taste"

    // 비회원 여부
    private var isGuest: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDateRangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCalendar(binding.rvCalendar, binding.tvMonth)
        setupButtons()
        setupBottomNav()

        countryName = intent.getStringExtra("country") ?: ""
        userName = intent.getStringExtra("login") ?: ""
        flowType = intent.getStringExtra("flowType") ?: "taste"
        isGuest = intent.getBooleanExtra("isGuest", false) || FirebaseAuth.getInstance().currentUser == null

        // Profile + 여행취향 데이터 로드
        loadUserData()


    }

    // Profile과 여행취향 데이터 함께 로드
    private fun loadUserData() {
        // 비회원인 경우 GuestProfileData에서 로드
        if (isGuest) {
            gender = GuestProfileData.gender
            age = GuestProfileData.ageDecade
            profileTags = GuestProfileData.purposes
            userName = GuestProfileData.nickname.ifEmpty { "여행자" }
            hasSemiPass = false
            budgetLabel = ""
            Log.d("DateRange", "Guest Profile 로드: nickname=$userName")
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 1. Profile 로드
        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    gender = doc.getString("gender") ?: ""
                    age = doc.getLong("ageDecade")?.toInt() ?: 0
                    profileTags = doc.get("purposes") as? List<String> ?: emptyList()
                    userName = doc.getString("nickname") ?: userName
                    hasSemiPass = doc.getBoolean("hasSemiPass") ?: false
                    budgetLabel = doc.getString("budgetLabel") ?: ""
                    Log.d("DateRange", "Profile 로드: hasSemiPass=$hasSemiPass, budgetLabel=$budgetLabel")
                }
            }

        // 2. 여행 취향 로드
        Firebase.firestore.collection("mbti").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    travelStyle = doc.getString("travelStyle") ?: ""
                    travelTraits = doc.get("travelTraits") as? List<String> ?: emptyList()
                    Log.d("DateRange", "여행 취향 로드: $travelStyle")
                }
            }
            .addOnFailureListener { e ->
                Log.e("DateRange", "여행취향 로드 실패: ${e.message}")
            }
    }

    private fun setupCalendar(rv: RecyclerView, tvMonth: TextView) {
        rv.setupCalendarGrid()
        adapter = DayAdapter(range) { onDayClick(it) }
        rv.adapter = adapter
        val barColor = getColor(R.color.range_bar_12)

        binding.rvCalendar.addItemDecoration(RangeCapsuleDecoration(range, barColor))
        renderMonth(tvMonth)

        binding.btnPrevMonth.setOnClickListener {
            curYM = curYM.minusMonths(1)
            renderMonth(tvMonth)
        }
        binding.btnNextMonth.setOnClickListener {
            curYM = curYM.plusMonths(1)
            renderMonth(tvMonth)
        }
    }

    private fun renderMonth(tvMonth: TextView) {
        tvMonth.text = MONTH_FMT.format(curYM.atDay(1))
        adapter.submitList(buildMonth(curYM))
        updatePickedText()
    }

    private fun onDayClick(date: LocalDate) {
        if (range.start == null) {
            range.start = date
            range.end = null
        } else if (range.end == null) {
            range.end = date
        } else {
            range.start = date
            range.end = null
        }
        adapter.notifyDataSetChanged()
        updatePickedText()
    }

    private fun updatePickedText() {
        val s = range.start
        val e = range.end ?: range.start
        binding.tvPicked.text = if (s == null) {
            "기간을 선택해주세요."
        } else {
            val a = if (e == null) s else minOf(s, e)
            val b = if (e == null) s else maxOf(s, e)
            "선택한 기간: $a ~ $b"
        }
    }

    // 선택한 키워드 수집
    private fun collectCheckedChips(): List<String> {
        val chipGroup = binding.chipGroup
        val checkedList = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                checkedList.add(chip.text.toString())
            }
        }
        return checkedList
    }

    private fun setupButtons() {
        binding.btnPrev.setOnClickListener { finish() }

        binding.btnConfirm.setOnClickListener {
            if (isLoading) return@setOnClickListener

            val s = range.start
            val e = range.end ?: range.start
            if (s == null || e == null) {
                Toast.makeText(this, "날짜 범위를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val a = minOf(s, e)
            val b = maxOf(s, e)
            val startStr = a.toString()
            val endStr = b.toString()
            val nights = ChronoUnit.DAYS.between(a, b).toInt()

            // 선택한 키워드 수집
            val selectedKeywords = collectCheckedChips()

            // Gemini AI 호출
            generateItineraryWithGemini(
                country = countryName,
                startDate = startStr,
                endDate = endStr,
                nights = nights,
                keywords = selectedKeywords
            )
        }
    }

    // Gemini AI로 여행 일정 생성
    private fun generateItineraryWithGemini(
        country: String,
        startDate: String,
        endDate: String,
        nights: Int,
        keywords: List<String>
    ) {
        isLoading = true
        val days = nights + 1

        // 로딩 오버레이 표시
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.tvLoadingTitle.text = "AI가 ${days}일 일정을 생성하고 있어요"
        binding.tvLoadingSubtitle.text = "잠시만 기다려주세요..."

        // 5일 이상이면 병렬 요청으로 분할
        if (days > 5) {
            generateItineraryParallel(country, startDate, endDate, days, keywords)
        } else {
            generateItinerarySingle(country, startDate, endDate, days, keywords)
        }
    }

    // 단일 요청 (5일 이하)
    private fun generateItinerarySingle(
        country: String,
        startDate: String,
        endDate: String,
        days: Int,
        keywords: List<String>
    ) {
        val prompt = buildPrompt(country, 1, days, keywords)

        callGeminiApi(prompt) { responseBody ->
            if (responseBody != null) {
                binding.tvLoadingSubtitle.text = "일정 준비 중..."
                parseAndNavigate(responseBody, country, startDate, endDate, days - 1, keywords)
            } else {
                binding.loadingOverlay.visibility = View.GONE
                Toast.makeText(this, "AI 응답 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 병렬 요청 (6일 이상)
    private fun generateItineraryParallel(
        country: String,
        startDate: String,
        endDate: String,
        totalDays: Int,
        keywords: List<String>
    ) {
        // 5일씩 분할
        val chunkSize = 5
        val chunks = mutableListOf<Pair<Int, Int>>() // (startDay, endDay)

        var currentDay = 1
        while (currentDay <= totalDays) {
            val endDay = minOf(currentDay + chunkSize - 1, totalDays)
            chunks.add(Pair(currentDay, endDay))
            currentDay = endDay + 1
        }

        val results = Array<String?>(chunks.size) { null }
        val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
        val hasError = java.util.concurrent.atomic.AtomicBoolean(false)

        binding.tvLoadingSubtitle.text = "0 / ${chunks.size} 파트 완료"

        chunks.forEachIndexed { index, (startDay, endDay) ->
            val daysInChunk = endDay - startDay + 1
            val prompt = buildPrompt(country, startDay, endDay, keywords)

            callGeminiApi(prompt) { responseBody ->
                if (hasError.get()) return@callGeminiApi

                if (responseBody != null) {
                    results[index] = responseBody
                    val completed = completedCount.incrementAndGet()

                    runOnUiThread {
                        binding.tvLoadingSubtitle.text = "$completed / ${chunks.size} 파트 완료"
                    }

                    // 모든 파트 완료
                    if (completed == chunks.size) {
                        runOnUiThread {
                            binding.tvLoadingSubtitle.text = "일정 합치는 중..."
                            mergeAndNavigate(results, country, startDate, endDate, totalDays - 1, keywords)
                        }
                    }
                } else {
                    if (hasError.compareAndSet(false, true)) {
                        runOnUiThread {
                            binding.loadingOverlay.visibility = View.GONE
                            Toast.makeText(this, "AI 응답 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // Gemini API 호출 공통 함수
    private fun callGeminiApi(prompt: String, callback: (String?) -> Unit) {
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 8192)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Gemini", "API 호출 실패: ${e.message}")
                runOnUiThread { callback(null) }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("Gemini", "응답 코드: ${response.code}")

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        callback(responseBody)
                    } else {
                        callback(null)
                    }
                }
            }
        })
    }

    // 여러 응답을 합쳐서 처리
    private fun mergeAndNavigate(
        responses: Array<String?>,
        country: String,
        startDate: String,
        endDate: String,
        nights: Int,
        keywords: List<String>
    ) {
        try {
            val allDays = mutableListOf<JSONObject>()

            responses.forEach { responseBody ->
                if (responseBody == null) return@forEach

                val json = JSONObject(responseBody)
                val candidates = json.getJSONArray("candidates")
                val content = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                var cleanJson = content
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                cleanJson = tryFixIncompleteJson(cleanJson)

                val daysArray = JSONArray(cleanJson)
                for (i in 0 until daysArray.length()) {
                    allDays.add(daysArray.getJSONObject(i))
                }
            }

            // 합쳐진 JSON 생성
            val mergedArray = JSONArray()
            allDays.forEachIndexed { index, dayObj ->
                dayObj.put("day", index + 1) // day 번호 재정렬
                mergedArray.put(dayObj)
            }

            val mergedJson = mergedArray.toString()
            Log.d("Gemini", "합쳐진 일정: ${allDays.size}일")

            // 기존 parseAndNavigate 로직 재사용
            parseAndNavigateDirect(mergedJson, country, startDate, endDate, nights, keywords)

        } catch (e: Exception) {
            Log.e("Gemini", "병합 실패: ${e.message}")
            e.printStackTrace()
            binding.loadingOverlay.visibility = View.GONE
            Toast.makeText(this, "일정 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // JSON 직접 파싱 (병합된 결과용)
    private fun parseAndNavigateDirect(
        cleanJson: String,
        country: String,
        startDate: String,
        endDate: String,
        nights: Int,
        keywords: List<String>
    ) {
        try {
            val daysArray = JSONArray(cleanJson)
            val daysList = mutableListOf<DayPlanData>()

            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                val day = dayObj.getInt("day")
                val title = dayObj.getString("title")
                val placesArray = dayObj.getJSONArray("places")

                val places = mutableListOf<PlaceData>()
                for (j in 0 until placesArray.length()) {
                    val placeObj = placesArray.getJSONObject(j)
                    places.add(PlaceData(
                        name = placeObj.getString("name"),
                        description = placeObj.optString("description", ""),
                        time = placeObj.optString("time", ""),
                        duration = placeObj.optString("duration", ""),
                        tip = placeObj.optString("tip", "")
                    ))
                }

                daysList.add(DayPlanData(day, title, places))
            }

            // Firebase에 저장 (비회원이 아닌 경우에만)
            if (!isGuest) {
                saveItineraryToHistory(country, startDate, endDate, nights, daysList, keywords)
            }

            // 화면 이동
            if (flowType == "taste") {
                val intent = Intent(this, PlanActivity::class.java).apply {
                    putExtra("country", country)
                    putExtra("login", userName)
                    putExtra("startDate", startDate)
                    putExtra("endDate", endDate)
                    putExtra("nights", nights)
                    putExtra("travelStyle", travelStyle)
                    putStringArrayListExtra("keywords", ArrayList(keywords))
                    putExtra("itineraryJson", cleanJson)
                    putExtra("isGuest", isGuest)
                }
                startActivity(intent)
            } else {
                val intent = Intent(this, SearchCountryResult::class.java).apply {
                    putExtra("country", country)
                    putExtra("login", userName)
                    putExtra("startDate", startDate)
                    putExtra("endDate", endDate)
                    putExtra("nights", nights)
                    putExtra("travelStyle", travelStyle)
                    putStringArrayListExtra("keywords", ArrayList(keywords))
                    putExtra("itineraryJson", cleanJson)
                    putExtra("isGuest", isGuest)
                }
                startActivity(intent)
            }

        } catch (e: Exception) {
            Log.e("Gemini", "파싱 실패: ${e.message}")
            binding.loadingOverlay.visibility = View.GONE
            Toast.makeText(this, "일정 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // AI 프롬프트 생성 (일차 범위 지정)
    private fun buildPrompt(
        country: String,
        startDay: Int,
        endDay: Int,
        keywords: List<String>
    ): String {
        val keywordText = if (keywords.isNotEmpty()) keywords.joinToString(", ") else "일반 관광"
        val daysCount = endDay - startDay + 1

        return """
${country} 여행 ${startDay}~${endDay}일차. 키워드:$keywordText
JSON만:[{"day":${startDay},"title":"테마","places":[{"name":"장소","description":"10자","time":"09:00","duration":"2시간","tip":"팁"}]}]
${daysCount}일분, 일차당 3개 장소.
        """.trimIndent()
    }

    // 불완전한 JSON 복구 시도
    private fun tryFixIncompleteJson(json: String): String {
        var fixed = json.trim()

        // [ 로 시작하는지 확인
        if (!fixed.startsWith("[")) {
            val startIndex = fixed.indexOf("[")
            if (startIndex != -1) {
                fixed = fixed.substring(startIndex)
            } else {
                return fixed
            }
        }

        // 이미 완전한 JSON인지 확인
        try {
            JSONArray(fixed)
            return fixed
        } catch (e: Exception) {
            // 불완전한 JSON - 복구 시도
        }

        // 열린 괄호 카운트
        var braceCount = 0  // {}
        var bracketCount = 0  // []
        var inString = false
        var lastValidIndex = 0

        for (i in fixed.indices) {
            val c = fixed[i]

            if (c == '"' && (i == 0 || fixed[i - 1] != '\\')) {
                inString = !inString
            }

            if (!inString) {
                when (c) {
                    '{' -> braceCount++
                    '}' -> {
                        braceCount--
                        if (braceCount == 0 && bracketCount == 1) {
                            // 하나의 day 객체가 완료됨
                            lastValidIndex = i
                        }
                    }
                    '[' -> bracketCount++
                    ']' -> {
                        bracketCount--
                        if (bracketCount == 0) {
                            lastValidIndex = i
                        }
                    }
                }
            }
        }

        // 마지막 유효한 day 객체까지 자르고 닫기
        if (lastValidIndex > 0 && lastValidIndex < fixed.length - 1) {
            fixed = fixed.substring(0, lastValidIndex + 1)

            // 배열 닫기
            if (!fixed.endsWith("]")) {
                fixed += "]"
            }
        } else {
            // 강제로 닫기 시도
            while (braceCount > 0) {
                fixed += "}"
                braceCount--
            }
            while (bracketCount > 0) {
                fixed += "]"
                bracketCount--
            }
        }

        return fixed
    }

    // 여행 취향 설명 생성
    private fun buildTravelStyleDesc(): String {
        if (travelTraits.isEmpty()) return "여행 취향 정보 없음 - 균형 잡힌 일정으로 추천해주세요."

        val descriptions = mutableListOf<String>()

        travelTraits.forEach { trait ->
            val desc = when {
                trait.contains("계획적") -> "- 계획적인 여행자: 체계적인 일정 선호, 예약 필수 장소 포함"
                trait.contains("즉흥적") -> "- 즉흥적인 여행자: 유연한 일정, 자유 시간 포함"
                trait.contains("활동적") -> "- 활동적인 여행자: 다양한 액티비티, 빠듯한 일정 OK"
                trait.contains("여유로운") -> "- 여유로운 여행자: 느긋한 일정, 카페/휴식 시간 포함"
                trait.contains("모험") -> "- 모험을 즐기는 여행자: 현지인 맛집, 숨은 명소 추천"
                trait.contains("안정") -> "- 안정을 추구하는 여행자: 검증된 관광지, 안전한 코스"
                trait.contains("어울리는") -> "- 사람들과 어울리는 여행자: 현지 투어, 그룹 액티비티 추천"
                trait.contains("조용히") -> "- 조용히 즐기는 여행자: 한적한 장소, 프라이빗한 경험"
                else -> "- $trait"
            }
            descriptions.add(desc)
        }

        return descriptions.joinToString("\n")
    }

    // 응답 파싱 및 화면 이동
    private fun parseAndNavigate(
        responseBody: String,
        country: String,
        startDate: String,
        endDate: String,
        nights: Int,
        keywords: List<String>
    ) {
        try {
            val json = JSONObject(responseBody)
            val candidates = json.getJSONArray("candidates")
            val content = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            // JSON 파싱 (```json ... ``` 제거)
            var cleanJson = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.d("Gemini", "파싱된 JSON: $cleanJson")

            // 불완전한 JSON 복구 시도
            cleanJson = tryFixIncompleteJson(cleanJson)
            Log.d("Gemini", "복구된 JSON: $cleanJson")

            val daysArray = JSONArray(cleanJson)
            val daysList = mutableListOf<DayPlanData>()

            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                val day = dayObj.getInt("day")
                val title = dayObj.getString("title")
                val placesArray = dayObj.getJSONArray("places")

                val places = mutableListOf<PlaceData>()
                for (j in 0 until placesArray.length()) {
                    val placeObj = placesArray.getJSONObject(j)
                    places.add(PlaceData(
                        name = placeObj.getString("name"),
                        description = placeObj.optString("description", ""),
                        time = placeObj.optString("time", ""),
                        duration = placeObj.optString("duration", ""),
                        tip = placeObj.optString("tip", "")
                    ))
                }

                daysList.add(DayPlanData(day, title, places))
            }

            // Firebase에 저장 (비회원이 아닌 경우에만)
            if (!isGuest) {
                saveItineraryToHistory(country, startDate, endDate, nights, daysList, keywords)
            } else {
                // 비회원인 경우 GuestProfileData에 저장
                GuestProfileData.startDate = startDate
                GuestProfileData.endDate = endDate
                GuestProfileData.keywords = keywords
            }

            // flowType에 따라 다른 화면으로 이동
            if (flowType == "taste") {
                // 취향찾기 흐름 → PlanActivity로 이동
                val intent = Intent(this, PlanActivity::class.java).apply {
                    putExtra("country", country)
                    putExtra("login", userName)
                    putExtra("startDate", startDate)
                    putExtra("endDate", endDate)
                    putExtra("nights", nights)
                    putExtra("travelStyle", travelStyle)
                    putStringArrayListExtra("keywords", ArrayList(keywords))
                    putExtra("itineraryJson", cleanJson)
                    putExtra("isGuest", isGuest)
                }
                startActivity(intent)
            } else {
                // guest/semipass 흐름 → SearchCountryResult로 이동
                val intent = Intent(this, SearchCountryResult::class.java).apply {
                    putExtra("country", country)
                    putExtra("login", userName)
                    putExtra("startDate", startDate)
                    putExtra("endDate", endDate)
                    putExtra("nights", nights)
                    putExtra("travelStyle", travelStyle)
                    putStringArrayListExtra("keywords", ArrayList(keywords))
                    putExtra("itineraryJson", cleanJson)
                    putExtra("isGuest", isGuest)
                }
                startActivity(intent)
            }

        } catch (e: Exception) {
            Log.e("Gemini", "파싱 실패: ${e.message}")
            e.printStackTrace()
            binding.loadingOverlay.visibility = View.GONE
            Toast.makeText(this, "일정 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Firebase history에 저장 (새미패스 경유인 경우만)
    private fun saveItineraryToHistory(
        country: String,
        startDate: String,
        endDate: String,
        nights: Int,
        days: List<DayPlanData>,
        keywords: List<String>
    ) {
        // 취향맞춤 흐름이면 여기서 저장하지 않음 (PlanActivity에서 "내 일정에 추가" 시 저장)
        if (flowType == "taste") {
            Log.d("Gemini", "취향맞춤 흐름 - history 저장 스킵 (PlanActivity에서 저장)")
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val daysMap = days.map { d ->
            mapOf(
                "day" to d.day,
                "title" to d.title,
                "places" to d.places.map { p ->
                    mapOf(
                        "name" to p.name,
                        "description" to p.description,
                        "time" to p.time,
                        "duration" to p.duration,
                        "tip" to p.tip
                    )
                }
            )
        }

        val doc = hashMapOf(
            "uid" to uid,
            "country" to country,
            "startDate" to startDate,
            "endDate" to endDate,
            "nights" to nights,
            "daysCount" to days.size,
            "days" to daysMap,
            "travelStyle" to travelStyle,
            "keywords" to keywords,
            "hasSemiPass" to hasSemiPass,
            "budgetLabel" to budgetLabel,
            "createdAt" to FieldValue.serverTimestamp()
        )

        Firebase.firestore
            .collection("history")
            .document(uid)
            .collection("trips")
            .add(doc)
            .addOnSuccessListener { ref ->
                Log.d("Gemini", "새미패스 히스토리 저장 완료: ${ref.id}")
            }
            .addOnFailureListener { e ->
                Log.e("Gemini", "히스토리 저장 실패: ${e.message}")
            }
    }

    private fun setupBottomNav() {
        val bottom = binding.bottomNav
        bottom.selectedItemId = R.id.tab_country
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_country -> true
                R.id.tab_search -> {
                    startActivity(Intent(this, MainContainerActivity::class.java)
                        .putExtra("initialTab", R.id.tab_search)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                R.id.tab_profile -> {
                    // 비회원인 경우 로그인 유도
                    if (isGuest) {
                        android.widget.Toast.makeText(this, "로그인이 필요한 기능입니다.", android.widget.Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("fromGuestFlow", true)
                        startActivity(intent)
                        return@setOnItemSelectedListener false
                    }
                    startActivity(Intent(this, MainContainerActivity::class.java)
                        .putExtra("initialTab", R.id.tab_profile)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                else -> false
            }
        }
    }
}