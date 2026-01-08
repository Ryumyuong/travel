package com.hehe.travel

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SearchCountryResult : AppCompatActivity() {

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
    private lateinit var tvHeaderSub: TextView
    private lateinit var sammyPassLabel: TextView
    private lateinit var loadingContainer: View
    private lateinit var resultContainer: View

    private lateinit var tvFlightTitle: TextView
    private lateinit var tvFlightDesc: TextView
    private lateinit var ivAirlineLogo: android.widget.ImageView
    private lateinit var ivBookingSiteLogo: android.widget.ImageView
    private lateinit var tvAccommodationTitle: TextView
    private lateinit var tvAccommodationDesc: TextView
    private lateinit var tvRestaurantTitle: TextView
    private lateinit var tvRestaurantDesc: TextView

    private lateinit var btnSaveTrip: AppCompatButton
    private lateinit var btnRestart: AppCompatButton
    private lateinit var bottomNav: com.google.android.material.bottomnavigation.BottomNavigationView

    // 다시추천 버튼들
    private lateinit var btnRefreshFlight: TextView
    private lateinit var btnRefreshAccommodation: TextView
    private lateinit var btnRefreshRestaurant: TextView

    // 프로필 정보 저장 (다시추천에 사용)
    private var savedGender = ""
    private var savedAgeDecade = 30
    private var savedCompanion = ""
    private var savedBudgetLabel = ""
    private var savedEnergyLabel = ""
    private var savedPurposes: List<String> = emptyList()
    private var savedFlightTimeLabel = ""
    private var savedPersonalityLabel = ""
    private var savedShoppingLabel = ""
    private var savedSleepLabel = ""
    private var savedMbtiAnswers: List<String> = emptyList()

    private var country = ""
    private var nickname = ""
    private var hasSemiPass = false  // ⭐ 새미패스 작성 여부
    private var budgetLevel = ""     // ⭐ 예산 수준 저장

    // 저장할 AI 응답 데이터
    private var savedFlightDesc = ""
    private var savedAccommodationDesc = ""
    private var savedBookingSite = ""
    private var savedRestaurantDesc = ""
    private var savedItineraryDesc = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search_country_result)

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
        tvHeaderSub = findViewById(R.id.tvHeaderSub)
        sammyPassLabel = findViewById(R.id.sammyPassLabel)
        loadingContainer = findViewById(R.id.loadingContainer)
        resultContainer = findViewById(R.id.resultContainer)
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus)

        tvFlightTitle = findViewById(R.id.tvFlightTitle)
        tvFlightDesc = findViewById(R.id.tvFlightDesc)
        ivAirlineLogo = findViewById(R.id.ivAirlineLogo)
        ivBookingSiteLogo = findViewById(R.id.ivBookingSiteLogo)
        tvAccommodationTitle = findViewById(R.id.tvAccommodationTitle)
        tvAccommodationDesc = findViewById(R.id.tvAccommodationDesc)
        tvRestaurantTitle = findViewById(R.id.tvRestaurantTitle)
        tvRestaurantDesc = findViewById(R.id.tvRestaurantDesc)

        btnSaveTrip = findViewById(R.id.btnSaveTrip)
        btnRestart = findViewById(R.id.btnRestart)
        bottomNav = findViewById(R.id.bottomNav)

        // 다시추천 버튼들
        btnRefreshFlight = findViewById(R.id.btnRefreshFlight)
        btnRefreshAccommodation = findViewById(R.id.btnRefreshAccommodation)
        btnRefreshRestaurant = findViewById(R.id.btnRefreshRestaurant)

        setupBottomNav()
        setupRefreshButtons()

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
            sammyPassLabel.visibility = View.GONE
            tvHeader.text = "${nickname}님의 $country 여행,"
            tvHeaderSub.text = "가장 많이 찾은 순으로 안내드려요!"
            tvFlightTitle.text = "best 비행기 추천"
            tvAccommodationTitle.text = "best 숙소 추천"
            tvRestaurantTitle.text = "best 맛집 추천"
            btnRestart.text = "새미 패스 작성하기"
        } else {
            // 새미패스 O: 뱃지 표시
            sammyPassLabel.visibility = View.VISIBLE
            // 예산에 따라 다른 안내
            when (budgetLevel) {
                "매우 부족", "부족" -> {
                    // 갓성비 추천
                    tvHeader.text = "${nickname}님의 $country 여행,"
                    tvHeaderSub.text = "갓성비 있게 안내드려요!"
                    tvFlightTitle.text = "최저가 best 비행기"
                    tvAccommodationTitle.text = "${country}의 갓성비 best 숙소"
                    tvRestaurantTitle.text = "가격도 저렴하지만 맛까지 챙긴 실속 맛집"
                }
                "여유로움", "매우 여유로움" -> {
                    // 고급/럭셔리 추천
                    tvHeader.text = "${nickname}님의 $country 여행,"
                    tvHeaderSub.text = "고급스럽게 안내드려요!"
                    tvFlightTitle.text = "프리미엄 비행기 추천"
                    tvAccommodationTitle.text = "${country}의 럭셔리 best 숙소"
                    tvRestaurantTitle.text = "분위기와 맛 모두 잡은 프리미엄 맛집"
                }
                else -> {
                    // 적당함: 균형 잡힌 추천
                    tvHeader.text = "${nickname}님의 $country 여행,"
                    tvHeaderSub.text = "균형 잡힌 일정으로 안내드려요!"
                    tvFlightTitle.text = "가성비 좋은 비행기 추천"
                    tvAccommodationTitle.text = "${country}의 인기 숙소 추천"
                    tvRestaurantTitle.text = "현지인이 추천하는 맛집"
                }
            }
            btnRestart.text = "여행 다시 시작하기"
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

                    // 프로필 정보 저장 (다시추천에 사용)
                    savedGender = gender
                    savedAgeDecade = ageDecade
                    savedCompanion = companion
                    savedBudgetLabel = budgetLabel
                    savedEnergyLabel = energyLabel
                    savedPurposes = purposes
                    savedFlightTimeLabel = flightTimeLabel
                    savedPersonalityLabel = personalityLabel
                    savedShoppingLabel = shoppingLabel
                    savedSleepLabel = sleepLabel

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

                            // MBTI 정보 저장
                            savedMbtiAnswers = mbtiAnswers

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
{"flight":"항공사명 ex) 대한항공","accommodation":"숙소명","booking_site":"예약사이트(Hotels.com, Booking.com, Agoda, Airbnb, 야놀자, 여기어때, 트립닷컴, 익스피디아 중 하나)","restaurant":"맛집1, 맛집2"}
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
            updateAirlineLogo(savedFlightDesc)

            // 숙소
            savedAccommodationDesc = getStringOrArray(result, "accommodation")
            tvAccommodationDesc.text = savedAccommodationDesc

            // 예약 사이트
            savedBookingSite = getStringOrArray(result, "booking_site")
            if (savedBookingSite.isNotEmpty()) {
                updateBookingSiteLogo(savedBookingSite)
            }

            // 맛집
            savedRestaurantDesc = getStringOrArray(result, "restaurant")
            tvRestaurantDesc.text = savedRestaurantDesc

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

        // 저장 버튼 비활성화
        btnSaveTrip.visibility = View.GONE

        Toast.makeText(this, "잠시 후 다시 시도해주세요", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 핸들러 콜백 정리
        currentRetryRunnable?.let { handler.removeCallbacks(it) }
    }

    // 항공사 로고 매칭
    private fun getAirlineLogo(airlineName: String): Int {
        return when {
            airlineName.contains("대한항공") || airlineName.contains("Korean Air") || airlineName.contains("KE") -> R.drawable.ic_korean_air
            airlineName.contains("아시아나") || airlineName.contains("Asiana") || airlineName.contains("OZ") -> R.drawable.ic_asiana
            airlineName.contains("제주항공") || airlineName.contains("Jeju Air") || airlineName.contains("7C") -> R.drawable.ic_jejuair
            airlineName.contains("진에어") || airlineName.contains("Jin Air") || airlineName.contains("LJ") -> R.drawable.ic_jinair
            airlineName.contains("티웨이") || airlineName.contains("T'way") || airlineName.contains("Tway") || airlineName.contains("TW") -> R.drawable.ic_twayair
            airlineName.contains("에어부산") || airlineName.contains("Air Busan") || airlineName.contains("BX") -> R.drawable.ic_airbusan
            airlineName.contains("에어서울") || airlineName.contains("Air Seoul") || airlineName.contains("RS") -> R.drawable.ic_airseoul
            airlineName.contains("에어프레미아") || airlineName.contains("Air Premia") || airlineName.contains("YP") -> R.drawable.ic_air_premia
            airlineName.contains("이스타") || airlineName.contains("Eastar") || airlineName.contains("ZE") -> R.drawable.ic_estarair
            else -> 0  // 매칭되는 로고 없음
        }
    }

    // 항공사 로고 업데이트
    private fun updateAirlineLogo(airlineName: String) {
        val logoRes = getAirlineLogo(airlineName)
        if (logoRes != 0) {
            ivAirlineLogo.setImageResource(logoRes)
            ivAirlineLogo.visibility = View.VISIBLE
        } else {
            ivAirlineLogo.visibility = View.GONE
        }
    }

    // 예약 사이트 로고 매칭
    private fun getBookingSiteLogo(siteName: String): Int {
        return when {
            siteName.contains("Hotels.com") || siteName.contains("호텔스닷컴") -> R.drawable.ic_hotels_com
            siteName.contains("Booking.com") || siteName.contains("부킹닷컴") || siteName.contains("부킹") -> R.drawable.ic_booking_com
            siteName.contains("Agoda") || siteName.contains("아고다") -> R.drawable.ic_agoda
            siteName.contains("Airbnb") || siteName.contains("에어비앤비") -> R.drawable.ic_airbnb
            siteName.contains("야놀자") || siteName.contains("Yanolja") -> R.drawable.ic_yanolja
            siteName.contains("여기어때") || siteName.contains("Goodchoice") -> R.drawable.ic_goodchoice
            siteName.contains("트립닷컴") || siteName.contains("Trip.com") -> R.drawable.ic_trip_com
            siteName.contains("익스피디아") || siteName.contains("Expedia") -> R.drawable.ic_expedia
            else -> R.drawable.ic_hotels_com  // 기본값
        }
    }

    // 예약 사이트 로고 업데이트
    private fun updateBookingSiteLogo(siteName: String) {
        val logoRes = getBookingSiteLogo(siteName)
        ivBookingSiteLogo.setImageResource(logoRes)
        ivBookingSiteLogo.visibility = View.VISIBLE
    }

    // 다시추천 버튼 설정
    private fun setupRefreshButtons() {
        btnRefreshFlight.setOnClickListener {
            refreshSingleItem("flight")
        }
        btnRefreshAccommodation.setOnClickListener {
            refreshSingleItem("accommodation")
        }
        btnRefreshRestaurant.setOnClickListener {
            refreshSingleItem("restaurant")
        }
    }

    // 개별 항목 다시추천
    private fun refreshSingleItem(itemType: String) {
        val targetView = when (itemType) {
            "flight" -> tvFlightDesc
            "accommodation" -> tvAccommodationDesc
            "restaurant" -> tvRestaurantDesc
            else -> return
        }

        // 로딩 표시
        val originalText = targetView.text.toString()
        targetView.text = "추천 중..."

        val itemName = when (itemType) {
            "flight" -> "비행기"
            "accommodation" -> "숙소"
            "restaurant" -> "맛집"
            else -> ""
        }

        val prompt = buildSingleItemPrompt(itemType, itemName)

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
                put("temperature", 0.9)  // 다양한 결과를 위해 높은 temperature
                put("maxOutputTokens", 1024)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TravelResult", "Refresh failed: ${e.message}", e)
                runOnUiThread {
                    targetView.text = originalText
                    Toast.makeText(this@SearchCountryResult, "다시추천 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val json = JSONObject(responseBody)
                            val content = json.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")

                            // JSON에서 해당 항목 추출
                            val cleanJson = extractJson(content)
                            val result = JSONObject(cleanJson)
                            val newValue = getStringOrArray(result, itemType)

                            if (newValue.isNotEmpty()) {
                                targetView.text = newValue
                                // 저장된 값도 업데이트
                                when (itemType) {
                                    "flight" -> {
                                        savedFlightDesc = newValue
                                        updateAirlineLogo(newValue)
                                    }
                                    "accommodation" -> {
                                        savedAccommodationDesc = newValue
                                        // 예약 사이트도 업데이트
                                        val bookingSite = getStringOrArray(result, "booking_site")
                                        if (bookingSite.isNotEmpty()) {
                                            savedBookingSite = bookingSite
                                            updateBookingSiteLogo(bookingSite)
                                        }
                                    }
                                    "restaurant" -> savedRestaurantDesc = newValue
                                }
                                Toast.makeText(this@SearchCountryResult, "${itemName} 추천이 변경되었습니다", Toast.LENGTH_SHORT).show()
                            } else {
                                targetView.text = originalText
                            }
                        } catch (e: Exception) {
                            Log.e("TravelResult", "Parse error: ${e.message}", e)
                            targetView.text = originalText
                            Toast.makeText(this@SearchCountryResult, "다시추천 실패", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        targetView.text = originalText
                        Toast.makeText(this@SearchCountryResult, "다시추천 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // 개별 항목 프롬프트 생성
    private fun buildSingleItemPrompt(itemType: String, itemName: String): String {
        val purposeText = if (savedPurposes.isNotEmpty()) savedPurposes.joinToString(", ") else "일반 여행"

        // 새미패스 여부 및 예산에 따라 추천 기준 변경
        val recommendationCriteria = if (!hasSemiPass) {
            "가장 인기 있고 많이 찾는 순으로 추천해주세요."
        } else {
            when (budgetLevel) {
                "매우 부족", "부족" -> "가격 대비 성능이 좋은 갓성비 위주로 추천해주세요."
                "여유로움", "매우 여유로움" -> "고급스럽고 럭셔리한 옵션 위주로 추천해주세요."
                else -> "가격과 품질의 균형이 좋은 옵션 위주로 추천해주세요."
            }
        }

        // 현재 추천된 것과 다른 것을 요청
        val currentValue = when (itemType) {
            "flight" -> savedFlightDesc
            "accommodation" -> savedAccommodationDesc
            "restaurant" -> savedRestaurantDesc
            else -> ""
        }

        val profileInfo = buildString {
            append("여행자 프로필:\n")
            if (savedGender.isNotEmpty()) append("- 성별: $savedGender\n")
            if (savedAgeDecade > 0) append("- 나이대: ${savedAgeDecade}대\n")
            if (savedCompanion.isNotEmpty()) append("- 동행: $savedCompanion\n")
            if (savedBudgetLabel.isNotEmpty()) append("- 예산: $savedBudgetLabel\n")
            if (savedEnergyLabel.isNotEmpty()) append("- 활동량: $savedEnergyLabel\n")
            if (purposeText.isNotEmpty()) append("- 여행 목적: $purposeText\n")
        }

        // 숙소의 경우 예약 사이트도 함께 추천
        val jsonFormat = if (itemType == "accommodation") {
            """{"$itemType":"추천 ${itemName} 이름","booking_site":"예약사이트(Hotels.com, Booking.com, Agoda, Airbnb, 야놀자, 여기어때, 트립닷컴, 익스피디아 중 하나)"}"""
        } else {
            """{"$itemType":"추천 ${itemName} 이름"}"""
        }

        return """
${country} 여행 ${itemName} 추천. $recommendationCriteria

$profileInfo

이전에 추천한 "${currentValue}"와는 다른 새로운 ${itemName}을(를) 추천해주세요.

아래 JSON 형식으로만 응답 (마크다운 없이, 값은 문자열):
$jsonFormat
        """.trimIndent()
    }

    // 하단 네비게이션 설정
    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.tab_country

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_country -> {
                    val intent = android.content.Intent(this, SearchActivity::class.java)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.tab_search -> {
                    val intent = android.content.Intent(this, StartActivity::class.java)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.tab_profile -> {
                    val intent = android.content.Intent(this, MyInfoActivity::class.java)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
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