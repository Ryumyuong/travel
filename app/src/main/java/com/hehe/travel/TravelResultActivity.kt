package com.hehe.travel

import android.util.Log
import android.os.Bundle
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

class TravelResultActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val client = OkHttpClient()

    // Gemini API Key
    private val GEMINI_API_KEY = "AIzaSyBnuHvwMS-h7v_i8oRgfE_4iLfEBPAtjXI"

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
    private var hasCompanion = false  // ⭐ companion 유무 저장

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
            if (hasCompanion) {
                // companion 있으면 → 여행 다시 시작 (SearchActivity로)
                val intent = android.content.Intent(this, QuestionnaireActivity::class.java)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            } else {
                // companion 없으면 → 세미 패스 작성하기
                val intent = android.content.Intent(this, SammySecondQuestionActivity::class.java)
                startActivity(intent)
            }
            finish()
        }
    }

    private fun loadProfileAndGenerateResult() {
        val uid = auth.currentUser?.uid ?: return

        // 로딩 표시
        loadingContainer.visibility = View.VISIBLE
        resultContainer.visibility = View.GONE

        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    nickname = document.getString("nickname") ?: "여행자"
                    val gender = document.getString("gender") ?: ""
                    val ageDecade = document.getLong("ageDecade")?.toInt() ?: 30
                    val companion = document.getString("companion") ?: ""
                    val budgetLabel = document.getString("budgetLabel") ?: ""
                    val energyLabel = document.getString("energyLabel") ?: ""
                    val purposes = document.get("purposes") as? List<String> ?: emptyList()

                    // 헤더 업데이트
                    tvHeader.text = "${nickname}님의 $country 여행,\n갓성비 있게 안내드려요"

                    // ⭐ companion 유무에 따라 버튼 텍스트 변경
                    hasCompanion = companion.isNotEmpty()
                    if (hasCompanion) {
                        btnRestart.text = "여행 다시 시작하기 ✈️"
                    } else {
                        btnRestart.text = "세미 패스 작성하기 ✔️"
                    }

                    // AI 호출
                    generateTravelRecommendation(
                        country = country,
                        nickname = nickname,
                        gender = gender,
                        ageDecade = ageDecade,
                        companion = companion,
                        budgetLabel = budgetLabel,
                        energyLabel = energyLabel,
                        purposes = purposes
                    )
                } else {
                    tvHeader.text = "${country} 여행 추천"
                    // 프로필이 없으면 세미 패스 작성하기
                    btnRestart.text = "세미 패스 작성하기 ✔️"
                    generateTravelRecommendation(country = country)
                }
            }
            .addOnFailureListener {
                loadingContainer.visibility = View.GONE
                btnRestart.text = "세미 패스 작성하기 ✔️"
                Toast.makeText(this, "프로필 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateTravelRecommendation(
        country: String,
        nickname: String = "여행자",
        gender: String = "",
        ageDecade: Int = 30,
        companion: String = "",
        budgetLabel: String = "",
        energyLabel: String = "",
        purposes: List<String> = emptyList()
    ) {
        val prompt = buildPrompt(country, nickname, gender, ageDecade, companion, budgetLabel, energyLabel, purposes)

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
                put("maxOutputTokens", 2048)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$GEMINI_API_KEY")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TravelResult", "Network failure: ${e.message}", e)
                runOnUiThread {
                    loadingContainer.visibility = View.GONE
                    resultContainer.visibility = View.VISIBLE
                    Toast.makeText(this@TravelResultActivity, "AI 응답 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    tvFlightTitle.text = "네트워크 오류"
                    tvFlightDesc.text = "네트워크 오류가 발생했습니다.\n다시 시도해주세요.\n\n에러: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("TravelResult", "Response code: ${response.code}")
                Log.d("TravelResult", "Response body: $responseBody")

                runOnUiThread {
                    loadingContainer.visibility = View.GONE
                    resultContainer.visibility = View.VISIBLE

                    if (response.isSuccessful && responseBody != null) {
                        parseAndDisplayResult(responseBody)
                    } else {
                        Log.e("TravelResult", "API Error: ${response.code}")
                        Toast.makeText(this@TravelResultActivity, "AI 응답 오류: ${response.code}", Toast.LENGTH_SHORT).show()
                        tvFlightTitle.text = "오류 발생"
                        tvFlightDesc.text = "응답 오류: ${response.code}\n\n${responseBody ?: "응답 없음"}"
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
        purposes: List<String>
    ): String {
        val purposeText = if (purposes.isNotEmpty()) purposes.joinToString(", ") else "일반 여행"

        return """
당신은 전문 여행 플래너입니다. 다음 사용자 정보를 바탕으로 ${country} 여행을 추천해주세요.

[사용자 정보]
- 이름: $nickname
- 성별: $gender
- 연령대: ${ageDecade}대
- 동행: $companion
- 예산: $budgetLabel
- 에너지/활동성: $energyLabel
- 여행 목적: $purposeText

[요청 형식]
다음 JSON 형식으로만 응답해주세요. 다른 텍스트 없이 JSON만 출력하세요:

{
    "flight": {
        "title": "최저가 best 비행기",
        "description": "추천 항공편 정보 (항공사, 예상 가격대, 팁)"
    },
    "accommodation": {
        "title": "${country}의 갓성비 best 숙소",
        "description": "예산과 스타일에 맞는 숙소 추천 3개 (이름, 가격대, 특징)"
    },
    "restaurant": {
        "title": "가격도 저렴하지만 맛까지 챙긴 실속 맛집",
        "description": "현지 맛집 추천 3개 (이름, 대표 메뉴, 가격대)"
    },
    "itinerary": {
        "title": "추천 일정",
        "description": "3박 4일 추천 일정 (일차별 간단 요약)"
    }
}
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

            // JSON 파싱 (```json ... ``` 제거)
            val cleanJson = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.d("TravelResult", "Clean JSON: $cleanJson")

            val result = JSONObject(cleanJson)

            // 비행기 (문장마다 줄바꿈)
            val flight = result.getJSONObject("flight")
            tvFlightTitle.text = flight.getString("title")
            savedFlightDesc = flight.getString("description")
            tvFlightDesc.text = formatFlight(savedFlightDesc)

            // 숙소 (1, 2, 3 숫자마다 줄바꿈)
            val accommodation = result.getJSONObject("accommodation")
            tvAccommodationTitle.text = accommodation.getString("title")
            savedAccommodationDesc = accommodation.getString("description")
            tvAccommodationDesc.text = formatNumberedList(savedAccommodationDesc)

            // 맛집 (1, 2, 3 숫자마다 줄바꿈)
            val restaurant = result.getJSONObject("restaurant")
            tvRestaurantTitle.text = restaurant.getString("title")
            savedRestaurantDesc = restaurant.getString("description")
            tvRestaurantDesc.text = formatNumberedList(savedRestaurantDesc)

            // 일정 (* 제거, 일차별 줄바꿈)
            val itinerary = result.getJSONObject("itinerary")
            tvItineraryTitle.text = itinerary.getString("title")
            savedItineraryDesc = itinerary.getString("description")
            tvItineraryDesc.text = formatItinerary(savedItineraryDesc)

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

    // Firebase history에 저장
    private fun saveToHistory() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 저장할 데이터가 없으면 리턴
        if (savedFlightDesc.isEmpty()) {
            Toast.makeText(this, "저장할 여행 정보가 없습니다.", Toast.LENGTH_SHORT).show()
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
            "savedAt" to com.google.firebase.Timestamp.now()
        )

        Firebase.firestore.collection("history")
            .add(historyData)
            .addOnSuccessListener { documentRef ->
                Log.d("TravelResult", "History saved with ID: ${documentRef.id}")
                Toast.makeText(this, "여행이 저장되었습니다! ❤️", Toast.LENGTH_SHORT).show()
                btnSaveTrip.text = "저장 완료 ✅"
                btnSaveTrip.isEnabled = false
            }
            .addOnFailureListener { e ->
                Log.e("TravelResult", "Error saving history: ${e.message}", e)
                Toast.makeText(this, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}