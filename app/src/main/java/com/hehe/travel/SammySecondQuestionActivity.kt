package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class SammySecondQuestionActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    // 현재 스텝 (0부터 시작)
    private var currentStep = 0
    private val totalSteps = 3

    // 수집할 데이터
    private var companion = "혼자"
    private var budgetLabel = "적당함"
    private var energyLabel = "보통"

    private lateinit var tvHeader: TextView

    // Step Containers
    private lateinit var stepCompanion: View
    private lateinit var stepBudget: View
    private lateinit var stepEnergy: View

    private lateinit var btnNext: AppCompatButton
    private lateinit var completedQuestionsContainer: LinearLayout
    private lateinit var scrollViewQuestions: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sammy_second_question)

        auth = FirebaseAuth.getInstance()

        initViews()
        setupStepViews()
        loadNicknameAndUpdateHeader()
        updateUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        tvHeader = findViewById(R.id.tvHeader)

        stepCompanion = findViewById(R.id.stepCompanion)
        stepBudget = findViewById(R.id.stepBudget)
        stepEnergy = findViewById(R.id.stepEnergy)

        btnNext = findViewById(R.id.btnNext)
        completedQuestionsContainer = findViewById(R.id.completedQuestionsContainer)
        scrollViewQuestions = findViewById(R.id.scrollViewQuestions)

        btnNext.setOnClickListener { onNextClicked() }
    }

    private fun loadNicknameAndUpdateHeader() {
        val uid = auth.currentUser?.uid ?: return
        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val nickname = document.getString("nickname") ?: "여행자"
                tvHeader.text = "${nickname}님께 여행 일정을 추천드리기 전,\n여행 상태는 어때요?"
            }
    }

    private fun setupStepViews() {
        // Step 1: 동행자 - 선택하면 자동으로 다음 스텝
        findViewById<RadioGroup>(R.id.rgCompanion).setOnCheckedChangeListener { _, checkedId ->
            companion = when (checkedId) {
                R.id.rbAlone -> "혼자"
                R.id.rbCouple -> "연인"
                R.id.rbFriend -> "친구"
                R.id.rbFamily -> "가족"
                R.id.rbFamilyWithKids -> "가족(아이 동반)"
                else -> "혼자"
            }

            // 자동으로 다음 스텝 이동
            if (checkedId != -1) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        }

        // Step 2: 예산 (5단계) - 터치 끊기면 자동으로 다음 스텝
        val seekBarBudget = findViewById<SeekBar>(R.id.seekBarBudget)
        seekBarBudget.max = 4
        seekBarBudget.progress = 2
        seekBarBudget.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                budgetLabel = arrayOf("매우 부족", "부족", "적당함", "여유로움", "매우 여유로움")[progress]
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 자동으로 다음 스텝 이동
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        })

        // Step 3: 에너지 (5단계) - 버튼 없이 마지막은 저장
        val seekBarEnergy = findViewById<SeekBar>(R.id.seekBarEnergy)
        seekBarEnergy.max = 4
        seekBarEnergy.progress = 2
        seekBarEnergy.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                energyLabel = arrayOf("많이 쉬고 싶음", "쉬고 싶음", "보통", "활동적", "매우 활동적")[progress]
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 버튼을 표시
                btnNext.visibility = View.VISIBLE
            }
        })
    }

    private fun onNextClicked() {
        // 완료된 질문을 상단에 추가
        addCompletedQuestion(currentStep)

        if (currentStep < totalSteps - 1) {
            currentStep++
            updateUI()

            // 스크롤을 하단으로 이동 (새 질문이 보이도록)
            scrollViewQuestions.post {
                scrollViewQuestions.fullScroll(View.FOCUS_DOWN)
            }
        } else {
            // 마지막 스텝 - 저장 후 SearchActivity로
            saveAndNavigate()
        }
    }

    private fun onBackClicked() {
        if (currentStep > 0) {
            currentStep--
            updateUI()
        } else {
            finish()
        }
    }

    private fun addCompletedQuestion(step: Int) {
        val questionText: String
        val answerText: String

        when (step) {
            0 -> {
                questionText = "누구와 함께 가나요?"
                answerText = companion
            }
            1 -> {
                questionText = "여행 예산이 궁금해요!"
                answerText = budgetLabel
            }
            2 -> {
                questionText = "여행 갔을 때 에너지는?"
                answerText = energyLabel
            }
            else -> return
        }

        // 완료된 질문 컨테이너
        val completedView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(40)
            }
            tag = step // 스텝 번호 저장
            isClickable = true
            isFocusable = true

            // 클릭 효과 추가
            background = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)

            // 클릭하면 해당 스텝으로 돌아가기
            setOnClickListener {
                editCompletedQuestion(step)
            }
        }

        // 질문
        val questionTextView = TextView(this).apply {
            text = questionText
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // 답변
        val answerTextView = TextView(this).apply {
            text = answerText
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
        }

        completedView.addView(questionTextView)
        completedView.addView(answerTextView)

        completedQuestionsContainer.addView(completedView)
    }

    private fun editCompletedQuestion(targetStep: Int) {
        // 해당 스텝 이후의 완료된 질문들 삭제
        val childCount = completedQuestionsContainer.childCount
        val viewsToRemove = mutableListOf<View>()

        for (i in 0 until childCount) {
            val child = completedQuestionsContainer.getChildAt(i)
            val step = child.tag as? Int ?: continue
            if (step >= targetStep) {
                viewsToRemove.add(child)
            }
        }

        viewsToRemove.forEach {
            completedQuestionsContainer.removeView(it)
        }

        // 해당 스텝으로 돌아가기
        currentStep = targetStep
        updateUI()

        // 스크롤을 하단으로 이동
        scrollViewQuestions.post {
            scrollViewQuestions.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun updateUI() {

        // 버튼 visibility - 마지막 스텝에서만 보이기 (에너지 SeekBar 터치 후)
        btnNext.visibility = View.GONE

        // ScrollView 하단 패딩 조정 (버튼이 보일 때만 여백)
        val bottomPadding = if (currentStep == totalSteps - 1) dpToPx(120) else dpToPx(40)
        scrollViewQuestions.setPadding(
            scrollViewQuestions.paddingLeft,
            scrollViewQuestions.paddingTop,
            scrollViewQuestions.paddingRight,
            bottomPadding
        )

        // 버튼 텍스트 변경
        btnNext.text = if (currentStep == totalSteps - 1) "여행 시작하기" else "다음"

        // 모든 스텝 숨기기
        listOf(stepCompanion, stepBudget, stepEnergy)
            .forEach { it.visibility = View.GONE }

        // 현재 스텝만 표시 (애니메이션 적용)
        val currentView = when (currentStep) {
            0 -> stepCompanion
            1 -> stepBudget
            2 -> stepEnergy
            else -> stepCompanion
        }

        currentView.visibility = View.VISIBLE
        currentView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
    }

    private fun saveAndNavigate() {
        val uid = auth.currentUser?.uid ?: return

        val data = mapOf(
            "companion" to companion,
            "budgetLabel" to budgetLabel,
            "energyLabel" to energyLabel,
            "hasSemiPass" to true
        )

        // 로딩 표시
        btnNext.isEnabled = false
        btnNext.text = "저장 중..."

        Firebase.firestore.collection("profiles").document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                startActivity(Intent(this, SearchActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                btnNext.isEnabled = true
                btnNext.text = "여행 시작하기"
                Toast.makeText(this, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (currentStep > 0) {
            onBackClicked()
        } else {
            super.onBackPressed()
        }
    }
}