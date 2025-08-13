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
    private val totalSteps = 10
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
            text = "휴가 첫날, 당신의 이상적인 하루는?",
            option1 = "아무 계획 없이 카페에 앉아 조용히 하루를 보내고 싶다.",
            option2 = "하루에 몇 군데라도 더 돌아다니며 알차게 시간을 쓰고 싶다."
        ),
        Question(
            text = "여행지에서 가장 중요하게 생각하는 것은?",
            option1 = "현지 문화와 역사를 깊이 있게 체험하는 것",
            option2 = "인스타그램에 올릴 멋진 사진을 찍는 것"
        ),
        Question(
            text = "숙소를 선택할 때 가장 우선순위는?",
            option1 = "편의시설과 서비스가 완벽한 고급 호텔",
            option2 = "현지 분위기를 느낄 수 있는 독특한 숙소"
        ),
        Question(
            text = "음식 체험에서 중요한 것은?",
            option1 = "미슐랭 가이드에 나온 유명 레스토랑",
            option2 = "현지인들이 자주 가는 로컬 맛집"
        ),
        Question(
            text = "여행 예산 범위는?",
            option1 = "1인당 300만원 이상 (프리미엄)",
            option2 = "1인당 150-300만원 (스탠다드)"
        ),
        Question(
            text = "여행 스타일은?",
            option1 = "여유롭게 휴식 중심의 여행",
            option2 = "액티비티와 체험 중심의 여행"
        ),
        Question(
            text = "쇼핑에 대한 관심도는?",
            option1 = "명품 쇼핑과 면세점이 중요",
            option2 = "현지 기념품과 수공예품 선호"
        ),
        Question(
            text = "교통수단 선호도는?",
            option1 = "편안한 개인 차량이나 프리미엄 교통",
            option2 = "현지 대중교통으로 현지 문화 체험"
        ),
        Question(
            text = "여행 동반자는?",
            option1 = "연인 또는 배우자와 로맨틱한 여행",
            option2 = "가족 또는 친구들과 함께하는 여행"
        ),
        Question(
            text = "가장 기대하는 여행 경험은?",
            option1 = "평생 잊지 못할 특별한 순간",
            option2 = "일상에서 벗어난 완전한 휴식"
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

        // 로그아웃 버튼 클릭 리스너
        binding.btnLogout.setOnClickListener {
            signOut() // 또는 disconnectGoogle()
        }
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

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            auth.signOut()
            goToLogin()
        }.addOnFailureListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}