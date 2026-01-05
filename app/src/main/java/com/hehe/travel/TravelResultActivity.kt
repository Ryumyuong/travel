package com.hehe.travel

import android.util.Log
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TravelResultActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Gemini API Key
    private val GEMINI_API_KEY = BuildConfig.API_KEY

    // 재시도 관련 변수
    private var retryCount = 0
    private val maxRetries = 3
    private val handler = Handler(Looper.getMainLooper())
    private var currentRetryRunnable: Runnable? = null
    private lateinit var tvLoadingStatus: TextView

    // Views
    private lateinit var tvHeader: TextView
    private lateinit var loadingContainer: View
    private lateinit var resultContainer: View

    private lateinit var tvFlightTitle: TextView
    private lateinit var tvFlightDesc: TextView
    private lateinit var tvAccommodationTitle: TextView
    private lateinit var tvAccommodationDesc: TextView
    private lateinit var tvRestaurantTitle: TextView
    private lateinit var tvRestaurantDesc: TextView
    private lateinit var tvItineraryTitle: TextView
    private lateinit var tvItineraryDesc: TextView

    private lateinit var btnSaveTrip: AppCompatButton
    private lateinit var btnRestart: AppCompatButton

    private var country = ""
    private var nickname = ""
    private var hasSemiPass = false  // ⭐ 새미패스 작성 여부
    private var budgetLevel = ""     // ⭐ 예산 수준 저장

    // 저장할 AI 응답 데이터
    private var savedFlightDesc = ""
    private var savedAccommodationDesc = ""
    private var savedRestaurantDesc = ""
    private var savedItineraryDesc = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_travel_result)

        auth = FirebaseAuth.getInstance()
        country = intent.getStringExtra("country") ?: ""

        initViews()
        loadProfileAndGenerateResult()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        tvHeader = findViewById(R.id.tvHeader)
        loadingContainer = findViewById(R.id.loadingContainer)
        resultContainer = findViewById(R.id.resultContainer)
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus)

        tvFlightTitle = findViewById(R.id.tvFlightTitle)
        tvFlightDesc = findViewById(R.id.tvFlightDesc)
        tvAccommodationTitle = findViewById(R.id.tvAccommodationTitle)
        tvAccommodationDesc = findViewById(R.id.tvAccommodationDesc)
        tvRestaurantTitle = findViewById(R.id.tvRestaurantTitle)
        tvRestaurantDesc = findViewById(R.id.tvRestaurantDesc)
        tvItineraryTitle = findViewById(R.id.tvItineraryTitle)
        tvItineraryDesc = findViewById(R.id.tvItineraryDesc)

        btnSaveTrip = findViewById(R.id.btnSaveTrip)
        btnRestart = findViewById(R.id.btnRestart)

        btnSaveTrip.setOnClickListener {
            saveToHistory()
        }

        btnRestart.setOnClickListener {
            if (hasSemiPass) {
                // 새미패스 O → 여행 다시 시작 (QuestionnaireActivity로)
                val intent = android.content.Intent(this, QuestionnaireActivity::class.java)
                intent.putExtra("flowType", "semipass")  // 새미패스 경유 표시
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            } else {
                // 새미패스 X → 새미 패스 작성하기
                val intent = android.content.Intent(this, SammySecondQuestionActivity::class.java)
                startActivity(intent)
            }
            finish()
        }
    }

    // 새미패스 여부 및 예산에 따라 UI 업데이트
    private fun updateUIBySemiPassStatus() {
        if (!hasSemiPass) {
            // 새미패스 X: 인기순 안내 (고정)
            tvHeader.text = "${nickname}님의 $country 여행,\n가장 많이 찾은 순으로 안내드려요!"
            tvFlightTitle.text = "best 비행기 추천"
            tvAccommodationTitle.text = "best 숙소 추천"
            tvRestaurantTitle.text = "best 맛집 추천"
            btnRestart.text = "새미 패스 작성하기 ✔️"
        } else {
            // 새미패스 O: 예산에 따라 다른 안내
            when (budgetLevel) {
                "매우 부족", "부족" -> {
                    // 갓성비 추천
                    tvHeader.text = "${nickname}님의 $country 여행,\n갓성비 있게 안내드려요"
                    tvFlightTitle.text = "최저가 best 비행기"
                    tvAccommodationTitle.text = "${country}의 갓성비 best 숙소"
                    tvRestaurantTitle.text = "가격도 저렴하지만 맛까지 챙긴 실속 맛집"
                }
                "여유로움", "매우 여유로움" -> {
                    // 고급/럭셔리 추천
                    tvHeader.text = "${nickname}님의 $country 여행,\n고급스럽게 안내드려요"
                    tvFlightTitle.text = "프리미엄 비행기 추천"
                    tvAccommodationTitle.text = "${country}의 럭셔리 best 숙소"
                    tvRestaurantTitle.text = "분위기와 맛 모두 잡은 프리미엄 맛집"
                }
                else -> {
                    // 적당함: 균형 잡힌 추천
                    tvHeader.text = "${nickname}님의 $country 여행,\n균형 잡힌 일정으로 안내드려요"
                    tvFlightTitle.text = "가성비 좋은 비행기 추천"
                    tvAccommodationTitle.text = "${country}의 인기 숙소 추천"
                    tvRestaurantTitle.text = "현지인이 추천하는 맛집"
                }
            }
            btnRestart.text = "여행 다시 시작하기 ✈️"
        }
    }

    private fun loadProfileAndGenerateResult() {
        val uid = auth.currentUser?.uid ?: return

        // 재시도 카운트 초기화
        retryCount = 0

        // 로딩 표시
        loadingContainer.visibility = View.VISIBLE
        resultContainer.visibility = View.GONE
        tvLoadingStatus.text = "맞춤 여행을 준비하고 있어요..."

        // profiles와 mbti 데이터를 동시에 가져오기
        val db = Firebase.firestore
        val profileRef = db.collection("profiles").document(uid)
        val mbtiRef = db.collection("mbti").document(uid)

        profileRef.get()
            .addOnSuccessListener { profileDoc ->
                if (profileDoc.exists()) {
                    nickname = profileDoc.getString("nickname") ?: "여행자"
                    val gender = profileDoc.getString("gender") ?: ""
                    val ageDecade = profileDoc.getLong("ageDecade")?.toInt() ?: 30
                    val companion = profileDoc.getString("companion") ?: ""
                    val budgetLabel = profileDoc.getString("budgetLabel") ?: ""
                    val energyLabel = profileDoc.getString("energyLabel") ?: ""
                    val purposes = profileDoc.get("purposes") as? List<String> ?: emptyList()

                    // 추가 프로필 필드들
                    val flightTimeLabel = profileDoc.getString("flightTimeLabel") ?: ""
                    val personalityLabel = profileDoc.getString("personalityLabel") ?: ""
                    val shoppingLabel = profileDoc.getString("shoppingLabel") ?: ""
                    val sleepLabel = profileDoc.getString("sleepLabel") ?: ""

                    // ⭐ 새미패스 여부 확인
                    hasSemiPass = profileDoc.getBoolean("hasSemiPass") ?: false
                    budgetLevel = budgetLabel  // 예산 수준 저장

                    // 새미패스 여부에 따라 UI 변경
                    updateUIBySemiPassStatus()

                    // MBTI 데이터 가져오기
                    mbtiRef.get()
                        .addOnSuccessListener { mbtiDoc ->
                            val mbtiAnswers = if (mbtiDoc.exists()) {
                                parseMbtiAnswers(mbtiDoc)
                            } else {
                                emptyList()
                            }

                            // AI 호출 (프로필 + MBTI 포함)
                            generateTravelRecommendation(
                                country = country,
                                nickname = nickname,
                                gender = gender,
                                ageDecade = ageDecade,
                                companion = companion,
                                budgetLabel = budgetLabel,
                                energyLabel = energyLabel,
                                purposes = purposes,
                                flightTimeLabel = flightTimeLabel,
                                personalityLabel = personalityLabel,
                                shoppingLabel = shoppingLabel,
                                sleepLabel = sleepLabel,
                                mbtiAnswers = mbtiAnswers
                            )
                        }
                        .addOnFailureListener {
                            // MBTI 실패해도 프로필만으로 진행
                            generateTravelRecommendation(
                                country = country,
                                nickname = nickname,
                                gender = gender,
                                ageDecade = ageDecade,
                                companion = companion,
                                budgetLabel = budgetLabel,
                                energyLabel = energyLabel,
                                purposes = purposes,
                                flightTimeLabel = flightTimeLabel,
                                personalityLabel = personalityLabel,
                                shoppingLabel = shoppingLabel,
                                sleepLabel = sleepLabel,
                                mbtiAnswers = emptyList()
                            )
                        }
                } else {
                    hasSemiPass = false
                    updateUIBySemiPassStatus()
                    generateTravelRecommendation(country = country)
                }
            }
            .addOnFailureListener {
                loadingContainer.visibility = View.GONE
                hasSemiPass = false
                updateUIBySemiPassStatus()
                Toast.makeText(this, "프로필 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
    }

    // MBTI selectedAnswers 파싱
    private fun parseMbtiAnswers(mbtiDoc: com.google.firebase.firestore.DocumentSnapshot): List<String> {
        val answers = mutableListOf<String>()
        try {
            val selectedAnswers = mbtiDoc.get("selectedAnswers") as? List<Map<String, Any>> ?: return emptyList()
            for (answer in selectedAnswers) {
                val questionTag = answer["questionTag"] as? String ?: ""
                val selectedText = answer["selectedText"] as? String ?: ""
                if (questionTag.isNotEmpty() && selectedText.isNotEmpty()) {
                    answers.add("$questionTag: $selectedText")
                }
            }
        } catch (e: Exception) {
            Log.e("TravelResult", "MBTI 파싱 오류: ${e.message}")
        }
        return answers
    }

    private fun generateTravelRecommendation(
        country: String,
        nickname: String = "여행자",
        gender: String = "",
        ageDecade: Int = 30,
        companion: String = "",
        budgetLabel: String = "",
        energyLabel: String = "",
        purposes: List<String> = emptyList(),
        flightTimeLabel: String = "",
        personalityLabel: String = "",
        shoppingLabel: String = "",
        sleepLabel: String = "",
        mbtiAnswers: List<String> = emptyList()
    ) {
        val prompt = buildPrompt(
            country, nickname, gender, ageDecade, companion, budgetLabel, energyLabel, purposes,
            flightTimeLabel, personalityLabel, shoppingLabel, sleepLabel, mbtiAnswers
        )

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
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TravelResult", "Network failure: ${e.message}", e)
                runOnUiThread {
                    // 네트워크 오류 시 재시도
                    if (retryCount < maxRetries) {
                        retryWithDelay(country, nickname, gender, ageDecade, companion, budgetLabel, energyLabel, purposes, flightTimeLabel, personalityLabel, shoppingLabel, sleepLabel, mbtiAnswers)
                    } else {
                        showFinalError("네트워크 오류", "네트워크 연결을 확인해주세요.\n\n에러: ${e.message}")
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("TravelResult", "Response code: ${response.code}")
                Log.d("TravelResult", "Response body: $responseBody")

                runOnUiThread {
                    when {
                        response.isSuccessful && responseBody != null -> {
                            // 성공 - 재시도 카운트 초기화
                            retryCount = 0
                            loadingContainer.visibility = View.GONE
                            resultContainer.visibility = View.VISIBLE
                            parseAndDisplayResult(responseBody)
                        }
                        response.code == 429 -> {
                            // Rate Limit 초과 - 재시도
                            Log.w("TravelResult", "Rate limit exceeded (429), retry: $retryCount")
                            if (retryCount < maxRetries) {
                                retryWithDelay(country, nickname, gender, ageDecade, companion, budgetLabel, energyLabel, purposes, flightTimeLabel, personalityLabel, shoppingLabel, sleepLabel, mbtiAnswers)
                            } else {
                                showFinalError(
                                    "요청 한도 초과",
                                    "잠시 후 다시 시도해주세요.\n\nAI 서버가 많은 요청을 처리 중입니다.\n약 1분 후에 다시 검색해주세요."
                                )
                            }
                        }
                        else -> {
                            Log.e("TravelResult", "API Error: ${response.code}")
                            showFinalError("오류 발생", "응답 오류: ${response.code}\n\n잠시 후 다시 시도해주세요.")
                        }
                    }
                }
            }
        })
    }

    private fun buildPrompt(
        country: String,
        nickname: String,
        gender: String,
        ageDecade: Int,
        companion: String,
        budgetLabel: String,
        energyLabel: String,
        purposes: List<String>,
        flightTimeLabel: String = "",
        personalityLabel: String = "",
        shoppingLabel: String = "",
        sleepLabel: String = "",
        mbtiAnswers: List<String> = emptyList()
    ): String {
        val purposeText = if (purposes.isNotEmpty()) purposes.joinToString(", ") else "일반 여행"

        // 새미패스 여부 및 예산에 따라 추천 기준 변경
        val (recommendationCriteria, flightTitle, accommodationTitle, restaurantTitle) = if (!hasSemiPass) {
            // 새미패스 X: 인기순
            arrayOf(
                "가장 인기 있고 많이 찾는 순으로 추천해주세요. 리뷰가 많고 평점이 높은 옵션을 우선 추천합니다.",
                "best 비행기 추천",
                "best 숙소 추천",
                "best 맛집 추천"
            )
        } else {
            when (budgetLevel) {
                "매우 부족", "부족" -> {
                    // 갓성비 추천
                    arrayOf(
                        "가격 대비 성능이 좋은 갓성비 위주로 추천해주세요. 저렴하면서도 품질 좋은 옵션을 우선 추천합니다.",
                        "최저가 best 비행기",
                        "${country}의 갓성비 best 숙소",
                        "가격도 저렴하지만 맛까지 챙긴 실속 맛집"
                    )
                }
                "여유로움", "매우 여유로움" -> {
                    // 고급/럭셔리 추천
                    arrayOf(
                        "고급스럽고 럭셔리한 옵션 위주로 추천해주세요. 프리미엄 서비스와 최고급 시설을 우선 추천합니다.",
                        "프리미엄 비행기 추천",
                        "${country}의 럭셔리 best 숙소",
                        "분위기와 맛 모두 잡은 프리미엄 맛집"
                    )
                }
                else -> {
                    // 균형 잡힌 추천
                    arrayOf(
                        "가격과 품질의 균형이 좋은 옵션 위주로 추천해주세요. 합리적인 가격에 만족스러운 품질을 제공하는 옵션을 추천합니다.",
                        "가성비 좋은 비행기 추천",
                        "${country}의 인기 숙소 추천",
                        "현지인이 추천하는 맛집"
                    )
                }
            }
        }

        // 프로필 정보 구성
        val profileInfo = buildString {
            append("여행자 프로필:\n")
            if (gender.isNotEmpty()) append("- 성별: $gender\n")
            if (ageDecade > 0) append("- 나이대: ${ageDecade}대\n")
            if (companion.isNotEmpty()) append("- 동행: $companion\n")
            if (budgetLabel.isNotEmpty()) append("- 예산: $budgetLabel\n")
            if (energyLabel.isNotEmpty()) append("- 활동량: $energyLabel\n")
            if (purposeText.isNotEmpty()) append("- 여행 목적: $purposeText\n")
            if (flightTimeLabel.isNotEmpty()) append("- 선호 비행시간: $flightTimeLabel\n")
            if (personalityLabel.isNotEmpty()) append("- 성격: $personalityLabel\n")
            if (shoppingLabel.isNotEmpty()) append("- 쇼핑 선호도: $shoppingLabel\n")
            if (sleepLabel.isNotEmpty()) append("- 수면 스타일: $sleepLabel\n")
        }

        // MBTI 성향 정보 구성
        val mbtiInfo = if (mbtiAnswers.isNotEmpty()) {
            buildString {
                append("\n여행 성향 분석 (MBTI 기반):\n")
                mbtiAnswers.forEach { answer ->
                    append("- $answer\n")
                }
            }
        } else ""

        return """
${country} 여행 추천. $recommendationCriteria

$profileInfo$mbtiInfo
위 여행자의 프로필과 성향을 고려하여 맞춤 추천해주세요.

아래 JSON 형식으로만 응답 (마크다운 없이, 모든 값은 문자열):
{"flight":"항공사명 ex) 대한항공","accommodation":"숙소1, 숙소2","restaurant":"맛집1, 맛집2"}
        """.trimIndent()
    }

    private fun parseAndDisplayResult(responseBody: String) {
        Log.d("TravelResult", "Parsing response...")
        try {
            val json = JSONObject(responseBody)
            val candidates = json.getJSONArray("candidates")
            val content = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            Log.d("TravelResult", "AI Response text: $content")

            // JSON 추출 (여러 형식 지원)
            val cleanJson = extractJson(content)

            Log.d("TravelResult", "Clean JSON: $cleanJson")

            val result = JSONObject(cleanJson)

            // 비행기
            savedFlightDesc = getStringOrArray(result, "flight")
            tvFlightDesc.text = savedFlightDesc

            // 숙소
            savedAccommodationDesc = getStringOrArray(result, "accommodation")
            tvAccommodationDesc.text = savedAccommodationDesc

            // 맛집
            savedRestaurantDesc = getStringOrArray(result, "restaurant")
            tvRestaurantDesc.text = savedRestaurantDesc

            // 일정 섹션 숨기기
            tvItineraryTitle.visibility = View.GONE
            tvItineraryDesc.visibility = View.GONE

            Log.d("TravelResult", "Successfully parsed and displayed all sections")

        } catch (e: Exception) {
            Log.e("TravelResult", "Parse error: ${e.message}", e)
            e.printStackTrace()
            // 파싱 실패시 원본 텍스트 표시
            try {
                val json = JSONObject(responseBody)
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                tvFlightTitle.text = "AI 추천 결과"
                tvFlightDesc.text = text
                tvAccommodationTitle.visibility = View.GONE
                tvAccommodationDesc.visibility = View.GONE
                tvRestaurantTitle.visibility = View.GONE
                tvRestaurantDesc.visibility = View.GONE
                tvItineraryTitle.visibility = View.GONE
                tvItineraryDesc.visibility = View.GONE
            } catch (e2: Exception) {
                Log.e("TravelResult", "Final parse error: ${e2.message}", e2)
                tvFlightTitle.text = "파싱 실패"
                tvFlightDesc.text = "결과 파싱 실패: ${e2.message}\n\n원본: $responseBody"
            }
        }
    }

    // JSON 값이 문자열 또는 배열일 때 처리
    private fun getStringOrArray(json: JSONObject, key: String): String {
        return try {
            // 먼저 문자열로 시도
            json.getString(key)
        } catch (e: Exception) {
            try {
                // 배열이면 쉼표로 연결
                val arr = json.getJSONArray(key)
                (0 until arr.length()).joinToString(", ") { arr.getString(it) }
            } catch (e2: Exception) {
                ""
            }
        }
    }

    // JSON 추출 함수 (여러 형식 지원)
    private fun extractJson(text: String): String {
        Log.d("TravelResult", "extractJson input: $text")

        // { 와 } 사이의 JSON만 추출
        val jsonStart = text.indexOf("{")
        val jsonEnd = text.lastIndexOf("}")

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            val result = text.substring(jsonStart, jsonEnd + 1).trim()
            Log.d("TravelResult", "extractJson output: $result")
            return result
        }

        Log.e("TravelResult", "extractJson failed - no JSON found")
        return text.trim()
    }

    // 비행기: 문장마다 줄바꿈
    private fun formatFlight(text: String): String {
        return text
            .replace(". ", ".\n\n")
            .replace("! ", "!\n\n")
            .replace("다. ", "다.\n\n")
            .replace("요. ", "요.\n\n")
            .trim()
    }

    // 숙소/맛집: 번호마다 줄바꿈
    private fun formatNumberedList(text: String): String {
        return text
            .replace(Regex("(\\d+)\\.\\s*"), "\n\n$1. ")  // 숫자. 앞에 줄바꿈
            .replace(Regex("^\\n+"), "")  // 맨 앞 줄바꿈 제거
            .trim()
    }

    // 일정: * 제거, 일차별 줄바꿈
    private fun formatItinerary(text: String): String {
        return text
            .replace("**", "")  // ** 제거
            .replace("*", "")   // * 제거
            .replace(Regex("(\\d+일차)"), "\n\n$1")  // 일차 앞에 줄바꿈
            .replace(Regex("^\\n+"), "")  // 맨 앞 줄바꿈 제거
            .trim()
    }

    // 재시도 로직 (지수 백오프)
    private fun retryWithDelay(
        country: String,
        nickname: String = "여행자",
        gender: String = "",
        ageDecade: Int = 30,
        companion: String = "",
        budgetLabel: String = "",
        energyLabel: String = "",
        purposes: List<String> = emptyList(),
        flightTimeLabel: String = "",
        personalityLabel: String = "",
        shoppingLabel: String = "",
        sleepLabel: String = "",
        mbtiAnswers: List<String> = emptyList()
    ) {
        retryCount++
        // 지수 백오프: 10초, 20초, 40초
        val delaySeconds = 10 * (1 shl (retryCount - 1))

        Log.d("TravelResult", "Retrying in ${delaySeconds}s (attempt $retryCount/$maxRetries)")

        // 카운트다운 시작
        startCountdown(delaySeconds) {
            generateTravelRecommendation(country, nickname, gender, ageDecade, companion, budgetLabel, energyLabel, purposes, flightTimeLabel, personalityLabel, shoppingLabel, sleepLabel, mbtiAnswers)
        }
    }

    // 카운트다운 타이머
    private fun startCountdown(seconds: Int, onComplete: () -> Unit) {
        var remaining = seconds

        currentRetryRunnable?.let { handler.removeCallbacks(it) }

        fun tick() {
            if (remaining > 0) {
                updateLoadingStatus("AI 서버 대기 중... ${remaining}초 후 재시도 ($retryCount/$maxRetries)")
                remaining--
                currentRetryRunnable = Runnable { tick() }
                handler.postDelayed(currentRetryRunnable!!, 1000)
            } else {
                updateLoadingStatus("AI에게 여행 추천 요청 중...")
                onComplete()
            }
        }
        tick()
    }

    // 로딩 상태 메시지 업데이트
    private fun updateLoadingStatus(message: String) {
        if (::tvLoadingStatus.isInitialized) {
            tvLoadingStatus.text = message
        }
    }

    // 최종 에러 표시 (재시도 모두 실패)
    private fun showFinalError(title: String, message: String) {
        loadingContainer.visibility = View.GONE
        resultContainer.visibility = View.VISIBLE

        tvFlightTitle.text = title
        tvFlightDesc.text = message

        // 다른 섹션 숨기기
        tvAccommodationTitle.visibility = View.GONE
        tvAccommodationDesc.visibility = View.GONE
        tvRestaurantTitle.visibility = View.GONE
        tvRestaurantDesc.visibility = View.GONE
        tvItineraryTitle.visibility = View.GONE
        tvItineraryDesc.visibility = View.GONE

        // 저장 버튼 비활성화
        btnSaveTrip.visibility = View.GONE

        Toast.makeText(this, "잠시 후 다시 시도해주세요", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 핸들러 콜백 정리
        currentRetryRunnable?.let { handler.removeCallbacks(it) }
    }

    // Firebase history에 저장
    private fun saveToHistory() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val historyData = hashMapOf(
            "uid" to uid,
            "country" to country,
            "nickname" to nickname,
            "flight" to savedFlightDesc,
            "accommodation" to savedAccommodationDesc,
            "restaurant" to savedRestaurantDesc,
            "itinerary" to savedItineraryDesc,
            "hasSemiPass" to hasSemiPass,
            "budgetLabel" to budgetLevel,
            "nights" to 0,  // SearchActivity에서는 날짜 선택 없음
            "daysCount" to 0,
            "startDate" to "",
            "endDate" to "",
            "travelStyle" to "",
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        Firebase.firestore
            .collection("history")
            .document(uid)               // ★ userId로 문서 고정
            .collection("trips")         // ★ 하위 컬렉션에 저장
            .add(historyData)
            .addOnSuccessListener { doc ->
                Log.d("TravelResult", "Saved history: ${doc.id}")
                Toast.makeText(this, "여행이 저장되었습니다! ❤️", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("TravelResult", "Error: ${e.message}", e)
                Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show()
            }
    }
}