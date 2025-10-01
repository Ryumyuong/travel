package com.hehe.travel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.hehe.travel.databinding.ActivityMainBinding
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
    private lateinit var nextButton: Button

    // 현재 선택된 옵션
    private var selectedOption: Int? = null

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
        nextButton = findViewById(R.id.btn_next)

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
            selectedOption = 1
        }

        option2Button.setOnClickListener {
            option1Button.isSelected = false
            option2Button.isSelected = true
            selectedOption = 2
        }

        prevButton.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateQuestion()
            }
        }

        nextButton.setOnClickListener {
            selectedOption?.let { option ->
                saveUserChoice(currentStep, option)

                if (currentStep < totalSteps) {
                    currentStep++
                    updateQuestion()
                } else {
                    navigateToCountryGuide()
                }
            }
        }
    }

    private fun saveUserChoice(questionNumber: Int, choice: Int) {
        val prefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("question_$questionNumber", choice)
        editor.apply()
    }

    private fun updateQuestion() {
        // 프로그레스 인디케이터 업데이트
        updateProgressIndicators()

        // 선택 상태 초기화
        selectedOption = null
        option1Button.isSelected = false
        option2Button.isSelected = false

        if (currentStep <= questions.size) {
            val question = questions[currentStep - 1]
            questionNumber.text = question.tag
            questionText.text = question.text
            option1Button.text = question.option1
            option2Button.text = question.option2
        }

        prevButton.isEnabled = currentStep > 1
        nextButton.text = if (currentStep == totalSteps) "완료" else "다음"
    }

    private fun updateProgressIndicators() {
        for (i in progressIndicators.indices) {
            progressIndicators[i].setBackgroundColor(
                if (i < currentStep) {
                    android.graphics.Color.parseColor("#3653AE")
                } else {
                    android.graphics.Color.parseColor("#E5E7EB")
                }
            )
        }
    }

    private fun navigateToCountryGuide() {
        val countries = listOf(
            "italy", "france", "japan", "korea", "usa", "spain", "germany", "uk",
            "thailand", "australia", "vietnam", "greece", "turkey", "switzerland",
            "portugal", "egypt"
        )

        // 랜덤 선택
        val randomCountry = countries.random()

        val intent = Intent(this, DestinationActivity::class.java)
        intent.putExtra("country", randomCountry)
        startActivity(intent)
        finish()
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