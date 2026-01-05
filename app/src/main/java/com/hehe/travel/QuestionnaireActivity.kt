package com.hehe.travel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.hehe.travel.databinding.ActivityQuestionnaireBinding

class QuestionnaireActivity : AppCompatActivity() {

    private var currentStep = 1
    private val totalSteps = 12
    private lateinit var binding: ActivityQuestionnaireBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Views
    private val progressIndicators = mutableListOf<android.view.View>()
    private lateinit var questionNumber: TextView
    private lateinit var questionText: TextView
    private lateinit var option1Button: Button
    private lateinit var option2Button: Button
    private lateinit var prevButton: Button

    // 답변 저장 배열
    private val answers = mutableListOf<Int>()

    // 진입 경로 (semipass: 새미패스 경유, taste: 취향맞춤)
    private var flowType: String = "taste"

    // 질문 리스트 정의
    data class QuestionData(
        val tag: String,
        val text: String,
        val option1: String,
        val option2: String
    )

    private val questions = listOf(
        QuestionData(
            tag = "퇴사 D-1",
            text = "사직서 던지고 내일 출국하는 당신,\n오늘은?",
            option1 = "여행 전 체크리스트를 만들어 하나 씩 짐을 챙긴다.",
            option2 = "짐은 여행 출발 전에만 챙기면 되지~"
        ),
        QuestionData(
            tag = "출국 당일",
            text = "설렘 가득한 공항, 비행기 안에서 당신은?",
            option1 = "우선 도착했을 때 해야 할 것들 체크하기",
            option2 = "도착하면 일단 뭐 먹을까? (아무 생각 없음)"
        ),
        QuestionData(
            tag = "여행 첫날 아침",
            text = "드디어 도착! 첫날 계획은?",
            option1 = "'아 몰랑~' 첫날은 호텔에서 푹 쉰다.",
            option2 = "여행까지 왔는데 근처 맛집과 카페를 찾아 떠난다."
        ),
        QuestionData(
            tag = "이동 시간",
            text = "체크인 하러 호텔 가는 중 교통편을 고른다면?",
            option1 = "편하고 안전한 교통수단이 최고!",
            option2 = "조금 불편해도 현지 느낌 나는 방법이 좋다."
        ),
        QuestionData(
            tag = "함께 온 친구의 완벽한 일정표",
            text = "그때 당신의 반응은?",
            option1 = "와, 귀찮았을 텐데 대단하다! 그냥 따라가야지~",
            option2 = "여행은 그때그때 분위기 따라 가야지."
        ),
        QuestionData(
            tag = "밤이 찾아오고",
            text = "친구가 핫한 술집에 가자고 한다면?",
            option1 = "힝… 오늘은 조용히 쉬고 싶어.",
            option2 = "좋아! 사람 많으면 나야 좋지~"
        ),
        QuestionData(
            tag = "예상 못 한 제안",
            text = "친구가 새로운 곳을 가자고 한다면?",
            option1 = "익숙한 곳이 좋아… 안정감이 최고야.",
            option2 = "여행 아니면 언제 해보겠어? 당연히 가야지!"
        ),
        QuestionData(
            tag = "맛집 투어",
            text = "친구가 하루에 2~3곳을 가자고 한다면?",
            option1 = "진짜 맛있는 집 하나만 가도 만족해.",
            option2 = "다양한 곳을 가봐야지~ 먹방 여행이잖아."
        ),
        QuestionData(
            tag = "사진 찍기 타임",
            text = "가는 곳마다 사진 찍자는 친구",
            option1 = "그만.. 사진 지옥 그만..",
            option2 = "SNS 없으면 무슨 재미? 바로 스토리 + 게시물 올린다. "
        ),
        QuestionData(
            tag = "날씨 변수",
            text = "비가 와서 일정이 모두 취소됐다면?",
            option1 = "비가 와도 여행엔 문제 없어! 플랜 B로 가야지 ㄱㄱ",
            option2 = "그냥 우리 호텔에서 쉴까?"
        ),
        QuestionData(
            tag = "쇼핑 스타일",
            text = "쇼핑을 하러 현지 시장에 가면 당신은?",
            option1 = "여행 오기 전 계획한 것만 사고 바로 나온다.",
            option2 = "구경하다가 마음에 드는 건 다 사야지~"
        ),
        QuestionData(
            tag = "여행 마지막 날",
            text = "친구가 호텔에 있자고 한다면?",
            option1 = "호캉스도 여행이지~ 호텔에서 여유 즐긴다.",
            option2 = "숙소는 잠만 자는 곳이지… 마지막까지 밖에서 즐겨야지."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionnaireBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // 진입 경로 확인 (TravelResultActivity에서 온 경우 "semipass")
        flowType = intent.getStringExtra("flowType") ?: "taste"

        repeat(totalSteps) { answers.add(0) }

        initViews()
        updateQuestion()
        setupClickListeners()

        initAuthAndGoogleClient()
        setupBottomNav(R.id.tab_search)
    }

    private fun initViews() {
        // 프로그레스 인디케이터 초기화
        for (i in 1..12) {
            val indicatorId = resources.getIdentifier("indicator_$i", "id", packageName)
            progressIndicators.add(findViewById(indicatorId))
        }

        questionNumber = findViewById(R.id.tv_question_number)
        questionText = findViewById(R.id.tv_question)
        option1Button = findViewById(R.id.btn_option1)
        option2Button = findViewById(R.id.btn_option2)
        prevButton = findViewById(R.id.btn_prev)

        // 버튼 초기 선택 상태 설정
        option1Button.isSelected = false
        option2Button.isSelected = false

        // 뒤로가기 버튼 설정
        findViewById<android.widget.ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        option1Button.setOnClickListener {
            option1Button.isSelected = true
            option2Button.isSelected = false
            onOptionSelected(1)
        }

        option2Button.setOnClickListener {
            option1Button.isSelected = false
            option2Button.isSelected = true
            onOptionSelected(2)
        }

        prevButton.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateQuestion()
            }
        }
    }

    // 옵션 선택 시 바로 다음으로 이동
    private fun onOptionSelected(option: Int) {
        // 답변 저장
        answers[currentStep - 1] = option
        saveUserChoice(currentStep, option)

        // 약간의 딜레이 후 다음으로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentStep < totalSteps) {
                currentStep++
                updateQuestion()
            } else {
                // 마지막 질문 완료 - Firebase에 저장 후 이동
                saveMbtiToFirebase()
            }
        }, 300)
    }

    private fun saveUserChoice(questionNumber: Int, choice: Int) {
        val prefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("question_$questionNumber", choice)
        editor.apply()
    }

    private fun updateQuestion() {
        updateProgressIndicators()

        val previousAnswer = answers[currentStep - 1]
        option1Button.isSelected = previousAnswer == 1
        option2Button.isSelected = previousAnswer == 2

        if (currentStep <= questions.size) {
            val question = questions[currentStep - 1]
            questionNumber.text = question.tag
            questionText.text = question.text
            option1Button.text = question.option1
            option2Button.text = question.option2
        }

        prevButton.isEnabled = currentStep > 1
    }

    private fun updateProgressIndicators() {
        for (i in progressIndicators.indices) {
            progressIndicators[i].setBackgroundResource(
                if (i < currentStep) {
                    R.drawable.progress_indicator_active
                } else {
                    R.drawable.progress_indicator_bg
                }
            )
        }
    }

    // Firebase mbti 컬렉션에 저장
    private fun saveMbtiToFirebase() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            navigateToCountryGuide()
            return
        }

        // 먼저 profiles에서 닉네임 가져오기
        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { profileDoc ->
                val nickname = profileDoc.getString("nickname") ?: "익명"

                // 선택한 질문과 답변을 텍스트로 저장
                val selectedAnswers = answers.mapIndexed { index, answer ->
                    val question = questions[index]
                    mapOf(
                        "questionTag" to question.tag,
                        "questionText" to question.text,
                        "selectedOption" to answer,
                        "selectedText" to if (answer == 1) question.option1 else question.option2
                    )
                }

                // 여행 스타일 요약 (선택한 답변 기반)
                val travelTraits = mutableListOf<String>()

                // Q1, Q2, Q5, Q11: 계획 vs 즉흥
                val planCount = listOf(0, 1, 4, 10).count { answers[it] == 1 }
                travelTraits.add(if (planCount >= 2) "계획적인 여행자" else "즉흥적인 여행자")

                // Q3, Q6, Q8, Q12: 휴식 vs 활동
                val activeCount = listOf(2, 5, 7, 11).count { answers[it] == 2 }
                travelTraits.add(if (activeCount >= 2) "활동적인 여행자" else "여유로운 여행자")

                // Q4, Q7: 안정 vs 모험
                val adventureCount = listOf(3, 6).count { answers[it] == 2 }
                travelTraits.add(if (adventureCount >= 1) "모험을 즐기는 여행자" else "안정을 추구하는 여행자")

                // Q9, Q10: 개인 vs 사교
                val socialCount = listOf(8, 9).count { answers[it] == 2 }
                travelTraits.add(if (socialCount >= 1) "사람들과 어울리는 여행자" else "조용히 즐기는 여행자")

                val mbtiData = hashMapOf(
                    "uid" to uid,
                    "nickname" to nickname,                               // ⭐ 닉네임 추가
                    "selectedAnswers" to selectedAnswers,
                    "travelTraits" to travelTraits,
                    "travelStyle" to travelTraits.joinToString(" • "),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                Firebase.firestore.collection("mbti")
                    .document(uid)
                    .set(mbtiData)
                    .addOnSuccessListener {
                        Log.d("MBTI", "여행 취향 저장 완료: $nickname")
                        navigateToCountryGuide()
                    }
                    .addOnFailureListener { e ->
                        Log.e("MBTI", "저장 실패: ${e.message}")
                        Toast.makeText(this, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        navigateToCountryGuide()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MBTI", "프로필 로드 실패: ${e.message}")
                // 프로필 로드 실패해도 닉네임 없이 저장 진행
                saveWithoutNickname(uid)
            }
    }

    // 닉네임 없이 저장 (프로필 로드 실패 시)
    private fun saveWithoutNickname(uid: String) {
        val selectedAnswers = answers.mapIndexed { index, answer ->
            val question = questions[index]
            mapOf(
                "questionTag" to question.tag,
                "questionText" to question.text,
                "selectedOption" to answer,
                "selectedText" to if (answer == 1) question.option1 else question.option2
            )
        }

        val travelTraits = mutableListOf<String>()
        val planCount = listOf(0, 1, 4, 10).count { answers[it] == 1 }
        travelTraits.add(if (planCount >= 2) "계획적인 여행자" else "즉흥적인 여행자")
        val activeCount = listOf(2, 5, 7, 11).count { answers[it] == 2 }
        travelTraits.add(if (activeCount >= 2) "활동적인 여행자" else "여유로운 여행자")
        val adventureCount = listOf(3, 6).count { answers[it] == 2 }
        travelTraits.add(if (adventureCount >= 1) "모험을 즐기는 여행자" else "안정을 추구하는 여행자")
        val socialCount = listOf(8, 9).count { answers[it] == 2 }
        travelTraits.add(if (socialCount >= 1) "사람들과 어울리는 여행자" else "조용히 즐기는 여행자")

        val mbtiData = hashMapOf(
            "uid" to uid,
            "nickname" to "익명",
            "selectedAnswers" to selectedAnswers,
            "travelTraits" to travelTraits,
            "travelStyle" to travelTraits.joinToString(" • "),
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        Firebase.firestore.collection("mbti")
            .document(uid)
            .set(mbtiData)
            .addOnSuccessListener {
                Log.d("MBTI", "여행 취향 저장 완료 (닉네임 없음)")
                navigateToCountryGuide()
            }
            .addOnFailureListener { e ->
                Log.e("MBTI", "저장 실패: ${e.message}")
                navigateToCountryGuide()
            }
    }

    private fun navigateToCountryGuide() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            navigateWithRandomCountry()
            return
        }

        // Firebase에서 비행시간 선호도 가져오기
        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val flightTimeValue = doc.getDouble("flightTimeValue") ?: 10.0
                val filteredCountries = getCountriesByFlightTime(flightTimeValue)
                val selectedCountry = filteredCountries.random()

                val intent = Intent(this, DateRangeActivity::class.java)
                intent.putExtra("country", selectedCountry)
                intent.putExtra("flowType", flowType)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                navigateWithRandomCountry()
            }
    }

    private fun navigateWithRandomCountry() {
        val countries = listOf(
            "japan", "thailand", "vietnam", "taiwan", "philippines",
            "italy", "france", "spain", "germany", "uk",
            "australia", "usa", "switzerland", "greece", "turkey"
        )
        val selectedCountry = countries.random()

        val intent = Intent(this, DateRangeActivity::class.java)
        intent.putExtra("country", selectedCountry)
        intent.putExtra("flowType", flowType)
        startActivity(intent)
        finish()
    }

    // 한국에서 각 나라까지 비행시간 기준 필터링
    private fun getCountriesByFlightTime(preferredHours: Double): List<String> {
        // 나라별 대략적인 비행시간 (한국 출발 기준)
        val countryFlightTimes = mapOf(
            "japan" to 2.0,
            "taiwan" to 2.5,
            "hongkong" to 3.5,
            "philippines" to 4.0,
            "guam" to 4.0,
            "vietnam" to 5.0,
            "thailand" to 5.5,
            "singapore" to 6.5,
            "malaysia" to 6.5,
            "indonesia" to 7.0,
            "india" to 8.0,
            "australia" to 10.0,
            "dubai" to 10.0,
            "turkey" to 11.0,
            "greece" to 12.0,
            "italy" to 12.0,
            "switzerland" to 12.0,
            "germany" to 12.0,
            "france" to 12.5,
            "spain" to 13.0,
            "uk" to 13.0,
            "portugal" to 14.0,
            "egypt" to 14.0,
            "usa" to 14.0
        )

        // 선호 비행시간 ±3시간 범위 내의 나라들 필터링
        val tolerance = 3.0
        val filtered = countryFlightTimes.filter { (_, hours) ->
            hours >= (preferredHours - tolerance) && hours <= (preferredHours + tolerance)
        }.keys.toList()

        // 필터링된 나라가 없으면 가장 가까운 비행시간의 나라들 반환
        return if (filtered.isNotEmpty()) {
            filtered
        } else {
            countryFlightTimes.entries
                .sortedBy { kotlin.math.abs(it.value - preferredHours) }
                .take(5)
                .map { it.key }
        }
    }

    private fun initAuthAndGoogleClient() {
        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun AppCompatActivity.setupBottomNav(selectedId: Int) {
        val bottom = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottom.selectedItemId = selectedId

        bottom.setOnItemSelectedListener { item ->
            if (item.itemId == selectedId) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.tab_country -> {
                    startActivity(
                        Intent(this, SearchActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
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