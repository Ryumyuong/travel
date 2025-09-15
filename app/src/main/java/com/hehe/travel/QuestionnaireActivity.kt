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
    private lateinit var progressBar: ProgressBar
    private lateinit var stepText: TextView
    private lateinit var questionNumber: TextView
    private lateinit var questionText: TextView
    private lateinit var option1Button: Button
    private lateinit var option2Button: Button
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    // 질문 리스트 정의
    private val questions = listOf(
        Question(
            text = "[퇴사 D-1] 사직서 던지고 내일 출국하는 당신, 오늘은?",
            option1 = "여행 전 체크리스트를 만들어 하나 씩 짐을 챙긴다.",
            option2 = "짐은 여행 출발 전에만 챙기면 되지~"
        ),
        Question(
            text = "[출국 당일] 설렘 가득한 공항, 비행기 안에서 당신은?",
            option1 = "우선 도착했을 때 해야 할 것들 체크하기",
            option2 = "도착하면 일단 뭐 먹을까? (아무 생각 없음)"
        ),
        Question(
            text = "[여행 첫날 아침] 드디어 도착! 첫날 계획은?",
            option1 = "'아 몰랑~' 첫날은 호텔에서 푹 쉰다.",
            option2 = "여행까지 왔는데 근처 맛집과 카페를 찾아 떠난다."
        ),
        Question(
            text = "[이동 시간] 체크인 하러 호텔 가는 중 교통편을 고른다면?",
            option1 = "편하고 안전한 교통수단이 최고!",
            option2 = "조금 불편해도 현지 느낌 나는 방법이 좋다."
        ),
        Question(
            text = "[함께 온 친구의 완벽한 일정표] 그때 당신의 반응은?",
            option1 = "와, 귀찮았을 텐데 대단하다! 그냥 따라가야지~",
            option2 = "여행은 그때그때 분위기 따라 가야지."
        ),
        Question(
            text = "[밤이 찾아오고] 친구가 핫한 술집에 가자고 한다면?",
            option1 = "힝… 오늘은 조용히 쉬고 싶어.",
            option2 = "좋아! 사람 많으면 나야 좋지~"
        ),
        Question(
            text = "[예상 못 한 제안] 친구가 새로운 곳을 가자고 한다면?",
            option1 = "익숙한 곳이 좋아… 안정감이 최고야.",
            option2 = "여행 아니면 언제 해보겠어? 당연히 가야지!"
        ),
        Question(
            text = "[맛집 투어] 친구가 하루에 2~3곳을 가자고 한다면?",
            option1 = "진짜 맛있는 집 하나만 가도 만족해.",
            option2 = "다양한 곳을 가봐야지~ 먹방 여행이잖아."
        ),
        Question(
            text = "[사진 찍기 타임] 가는 곳마다 사진 찍자는 친구",
            option1 = "그만.. 사진 지옥 그만..",
            option2 = "SNS 없으면 무슨 재미? 바로 스토리 + 게시물 올린다. "
        ),
        Question(
            text = "[날씨 변수] 비가 와서 일정이 모두 취소됐다면?",
            option1 = "비가 와도 여행엔 문제 없어! 플랜 B로 가야지 ㄱㄱ",
            option2 = "그냥 우리 호텔에서 쉴까?"
        ),

        Question(
            text = "[쇼핑 스타일] 쇼핑을 하러 현지 시장에 가면 당신은?",
            option1 = "여행 오기 전 계획한 것만 사고 바로 나온다.",
            option2 = "구경하다가 마음에 드는 건 다 사야지~"
        ),

        Question(
            text = "[여행 마지막 날] 친구가 호텔에 있자고 한다면?",
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
        progressBar = findViewById(R.id.progress_bar)
        stepText = findViewById(R.id.tv_step)
        questionNumber = findViewById(R.id.tv_question_number)
        questionText = findViewById(R.id.tv_question)
        option1Button = findViewById(R.id.btn_option1)
        option2Button = findViewById(R.id.btn_option2)
        prevButton = findViewById(R.id.btn_prev)
        nextButton = findViewById(R.id.btn_next)
    }

    private fun setupClickListeners() {
        option1Button.setOnClickListener {
            selectOption(1)
        }

        option2Button.setOnClickListener {
            selectOption(2)
        }

        prevButton.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateQuestion()
            }
        }

        nextButton.setOnClickListener {
            if (currentStep < totalSteps) {
                currentStep++
                updateQuestion()
            } else {
                navigateToCountryGuide()
            }
        }
    }

    private fun selectOption(option: Int) {
        saveUserChoice(currentStep, option)

        if (currentStep < totalSteps) {
            currentStep++
            updateQuestion()
        } else {
            navigateToCountryGuide()
        }
    }

    private fun saveUserChoice(questionNumber: Int, choice: Int) {
        val prefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("question_$questionNumber", choice)
        editor.apply()
    }

    private fun updateQuestion() {
        val progress = (currentStep.toFloat() / totalSteps * 100).toInt()
        progressBar.progress = progress
        stepText.text = "$currentStep/$totalSteps 질문"
        questionNumber.text = "질문 $currentStep"

        if (currentStep <= questions.size) {
            val question = questions[currentStep - 1]
            questionText.text = question.text
            option1Button.text = question.option1
            option2Button.text = question.option2
        }

        prevButton.isEnabled = currentStep > 1
        nextButton.text = if (currentStep == totalSteps) "완료" else "다음 →"
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