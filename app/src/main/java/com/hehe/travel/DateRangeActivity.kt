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
    private val GEMINI_API_KEY = "AIzaSyBnuHvwMS-h7v_i8oRgfE_4iLfEBPAtjXI"

    // Profile 데이터
    private var gender: String = ""
    private var age: Int = 0
    private var profileTags: List<String> = emptyList()

    // 여행 취향 데이터
    private var travelStyle: String = ""
    private var travelTraits: List<String> = emptyList()

    // 로딩 상태
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDateRangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCalendar(binding.rvCalendar, binding.tvMonth)
        setupButtons()
        setupBottomNav()

        countryName = intent.getStringExtra("country") ?: ""
        userName = intent.getStringExtra("login") ?: ""

        // Profile + 여행취향 데이터 로드
        loadUserData()
    }

    // Profile과 여행취향 데이터 함께 로드
    private fun loadUserData() {
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
                    Log.d("DateRange", "Profile 로드: gender=$gender, age=$age")
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
        binding.btnConfirm.text = "AI가 일정 생성 중..."
        binding.btnConfirm.isEnabled = false

        val prompt = buildPrompt(country, startDate, endDate, nights, keywords)

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
                put("maxOutputTokens", 4096)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$GEMINI_API_KEY")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Gemini", "API 호출 실패: ${e.message}")
                runOnUiThread {
                    isLoading = false
                    binding.btnConfirm.text = "선택완료"
                    binding.btnConfirm.isEnabled = true
                    Toast.makeText(this@DateRangeActivity, "AI 호출 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("Gemini", "응답 코드: ${response.code}")
                Log.d("Gemini", "응답: $responseBody")

                runOnUiThread {
                    isLoading = false
                    binding.btnConfirm.text = "선택완료"
                    binding.btnConfirm.isEnabled = true

                    if (response.isSuccessful && responseBody != null) {
                        parseAndNavigate(responseBody, country, startDate, endDate, nights, keywords)
                    } else {
                        Toast.makeText(this@DateRangeActivity, "AI 응답 오류: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // AI 프롬프트 생성
    private fun buildPrompt(
        country: String,
        startDate: String,
        endDate: String,
        nights: Int,
        keywords: List<String>
    ): String {
        val keywordText = if (keywords.isNotEmpty()) keywords.joinToString(", ") else "일반 관광"
        val days = nights + 1

        // 여행 취향 설명
        val travelStyleDesc = buildTravelStyleDesc()

        return """
당신은 전문 여행 플래너입니다. 다음 사용자 정보를 바탕으로 ${country} ${nights}박 ${days}일 여행 일정을 생성해주세요.

[사용자 정보]
- 이름: $userName
- 성별: $gender
- 연령대: ${age}대
- 여행지: $country
- 기간: $startDate ~ $endDate (${nights}박 ${days}일)
- 선호 키워드: $keywordText

[여행 취향]
$travelStyleDesc

[요청 형식]
다음 JSON 형식으로만 응답해주세요. 다른 텍스트 없이 JSON만 출력하세요:

```json
[
    {
        "day": 1,
        "title": "1일차 테마 제목",
        "places": [
            {
                "name": "장소명",
                "description": "장소 설명 (20자 내외)",
                "time": "09:00",
                "duration": "2시간",
                "tip": "꿀팁"
            }
        ]
    },
    {
        "day": 2,
        "title": "2일차 테마 제목",
        "places": [...]
    }
]
```

각 일차별로 3~5개 장소를 추천해주세요.
사용자의 여행 취향과 선호 키워드에 맞는 장소를 추천해주세요.
실제 존재하는 장소만 추천해주세요.
        """.trimIndent()
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
            val cleanJson = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.d("Gemini", "파싱된 JSON: $cleanJson")

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

            // Firebase에 저장
            saveItineraryToHistory(country, startDate, endDate, nights, daysList, keywords)

            // PlanActivity로 이동
            val intent = Intent(this, PlanActivity::class.java).apply {
                putExtra("country", country)
                putExtra("login", userName)
                putExtra("startDate", startDate)
                putExtra("endDate", endDate)
                putExtra("nights", nights)
                putExtra("travelStyle", travelStyle)
                putStringArrayListExtra("keywords", ArrayList(keywords))
                putExtra("itineraryJson", cleanJson)
            }
            startActivity(intent)

        } catch (e: Exception) {
            Log.e("Gemini", "파싱 실패: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "일정 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Firebase history에 저장
    private fun saveItineraryToHistory(
        country: String,
        startDate: String,
        endDate: String,
        nights: Int,
        days: List<DayPlanData>,
        keywords: List<String>
    ) {
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
            "createdAt" to FieldValue.serverTimestamp()
        )

        Firebase.firestore
            .collection("history")
            .document(uid)
            .collection("trips")
            .add(doc)
            .addOnSuccessListener { ref ->
                Log.d("Gemini", "히스토리 저장 완료: ${ref.id}")
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
                    startActivity(Intent(this, QuestionnaireActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                R.id.tab_profile -> {
                    startActivity(Intent(this, MyInfoActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                else -> false
            }
        }
    }
}