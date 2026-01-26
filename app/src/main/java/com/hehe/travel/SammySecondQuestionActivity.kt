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
    private val totalSteps = 8

    // 비회원 여부
    private var isGuest = false

    // 수정 모드 플래그
    private var isEditMode = false

    // 수집할 데이터
    private var companion = "혼자"
    private var flightTimeValue = 10.0      // 1~20시간
    private var flightTimeLabel = "10시간"
    private var budgetValue = 50            // 0~100
    private var budgetLabel = "적당함"
    private var energyValue = 50            // 0~100
    private var energyLabel = "보통"
    private var shoppingValue = 50          // 0~100
    private var shoppingLabel = "보통"
    private var personalityValue = 50       // 0~100
    private var personalityLabel = "보통"
    private var sleepValue = 50             // 0~100
    private var sleepLabel = "보통"
    private var accommodationValue = 5.0    // 1~10
    private var accommodationLabel = "5"

    // TextView 참조
    private lateinit var tvFlightTimeValue: TextView
    private lateinit var tvBudgetValue: TextView
    private lateinit var tvEnergyValue: TextView
    private lateinit var tvShoppingValue: TextView
    private lateinit var tvPersonalityValue: TextView
    private lateinit var tvSleepValue: TextView
    private lateinit var tvAccommodationValue: TextView

    private lateinit var tvHeader: TextView

    // Step Containers
    private lateinit var stepCompanion: View
    private lateinit var stepFlightTime: View
    private lateinit var stepBudget: View
    private lateinit var stepEnergy: View
    private lateinit var stepShopping: View
    private lateinit var stepPersonality: View
    private lateinit var stepSleep: View
    private lateinit var stepAccommodation: View

    private lateinit var btnNext: AppCompatButton
    private lateinit var completedQuestionsContainer: LinearLayout
    private lateinit var scrollViewQuestions: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sammy_second_question)

        auth = FirebaseAuth.getInstance()

        // 비회원 여부 확인
        isGuest = auth.currentUser == null

        // 수정 모드 확인 (MyInfoActivity에서 진입 시)
        isEditMode = intent.getBooleanExtra("isEditMode", false)

        initViews()
        setupStepViews()
        loadNicknameAndUpdateHeader()

        // 수정 모드면 기존 데이터 로드
        if (isEditMode) {
            loadExistingProfile()
        }

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
        stepFlightTime = findViewById(R.id.stepFlightTime)
        stepBudget = findViewById(R.id.stepBudget)
        stepEnergy = findViewById(R.id.stepEnergy)
        stepShopping = findViewById(R.id.stepShopping)
        stepPersonality = findViewById(R.id.stepPersonality)
        stepSleep = findViewById(R.id.stepSleep)
        stepAccommodation = findViewById(R.id.stepAccommodation)

        // TextView 초기화
        tvFlightTimeValue = findViewById(R.id.tvFlightTimeValue)
        tvBudgetValue = findViewById(R.id.tvBudgetValue)
        tvEnergyValue = findViewById(R.id.tvEnergyValue)
        tvShoppingValue = findViewById(R.id.tvShoppingValue)
        tvPersonalityValue = findViewById(R.id.tvPersonalityValue)
        tvSleepValue = findViewById(R.id.tvSleepValue)
        tvAccommodationValue = findViewById(R.id.tvAccommodationValue)

        btnNext = findViewById(R.id.btnNext)
        completedQuestionsContainer = findViewById(R.id.completedQuestionsContainer)
        scrollViewQuestions = findViewById(R.id.scrollViewQuestions)

        btnNext.setOnClickListener { onNextClicked() }
    }

    private fun loadNicknameAndUpdateHeader() {
        // 비회원인 경우 GuestProfileData에서 닉네임 가져오기
        if (isGuest) {
            val nickname = GuestProfileData.nickname.ifEmpty { "여행자" }
            tvHeader.text = "${nickname}님께 여행 일정을 추천드리기 전,\n여행 상태는 어때요?"
            return
        }

        val uid = auth.currentUser?.uid ?: return
        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val nickname = document.getString("nickname") ?: "여행자"
                tvHeader.text = "${nickname}님께 여행 일정을 추천드리기 전,\n여행 상태는 어때요?"
            }
    }

    // 기존 프로필 데이터 로드 (수정 모드용)
    private fun loadExistingProfile() {
        val uid = auth.currentUser?.uid ?: return

        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // 동행자
                    companion = doc.getString("companion") ?: "혼자"
                    val rgCompanion = findViewById<RadioGroup>(R.id.rgCompanion)
                    when (companion) {
                        "혼자" -> rgCompanion.check(R.id.rbAlone)
                        "연인" -> rgCompanion.check(R.id.rbCouple)
                        "친구" -> rgCompanion.check(R.id.rbFriend)
                        "가족" -> rgCompanion.check(R.id.rbFamily)
                        "가족(아이 동반)" -> rgCompanion.check(R.id.rbFamilyWithKids)
                    }

                    // 비행 시간
                    flightTimeValue = doc.getDouble("flightTimeValue") ?: 10.0
                    flightTimeLabel = doc.getString("flightTimeLabel") ?: "10시간"
                    val seekBarFlightTime = findViewById<SeekBar>(R.id.seekBarFlightTime)
                    seekBarFlightTime.progress = ((flightTimeValue - 1.0) / 19.0 * 100).toInt()
                    updateFlightTimeDisplay(seekBarFlightTime.progress)

                    // 예산
                    budgetValue = doc.getLong("budgetValue")?.toInt() ?: 50
                    budgetLabel = doc.getString("budgetLabel") ?: "적당함"
                    val seekBarBudget = findViewById<SeekBar>(R.id.seekBarBudget)
                    seekBarBudget.progress = budgetValue
                    updateBudgetDisplay(budgetValue)

                    // 에너지
                    energyValue = doc.getLong("energyValue")?.toInt() ?: 50
                    energyLabel = doc.getString("energyLabel") ?: "보통"
                    val seekBarEnergy = findViewById<SeekBar>(R.id.seekBarEnergy)
                    seekBarEnergy.progress = energyValue
                    updateEnergyDisplay(energyValue)

                    // 쇼핑
                    shoppingValue = doc.getLong("shoppingValue")?.toInt() ?: 50
                    shoppingLabel = doc.getString("shoppingLabel") ?: "보통"
                    val seekBarShopping = findViewById<SeekBar>(R.id.seekBarShopping)
                    seekBarShopping.progress = shoppingValue
                    updateShoppingDisplay(shoppingValue)

                    // 성향
                    personalityValue = doc.getLong("personalityValue")?.toInt() ?: 50
                    personalityLabel = doc.getString("personalityLabel") ?: "보통"
                    val seekBarPersonality = findViewById<SeekBar>(R.id.seekBarPersonality)
                    seekBarPersonality.progress = personalityValue
                    updatePersonalityDisplay(personalityValue)

                    // 잠
                    sleepValue = doc.getLong("sleepValue")?.toInt() ?: 50
                    sleepLabel = doc.getString("sleepLabel") ?: "보통"
                    val seekBarSleep = findViewById<SeekBar>(R.id.seekBarSleep)
                    seekBarSleep.progress = sleepValue
                    updateSleepDisplay(sleepValue)

                    // 숙소 컨디션
                    accommodationValue = doc.getDouble("accommodationValue") ?: 5.0
                    accommodationLabel = doc.getString("accommodationLabel") ?: "5"
                    val seekBarAccommodation = findViewById<SeekBar>(R.id.seekBarAccommodation)
                    seekBarAccommodation.progress = ((accommodationValue - 1.0) / 9.0 * 100).toInt()
                    updateAccommodationDisplay(seekBarAccommodation.progress)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "프로필 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
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
            if (checkedId != -1) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        }

        // Step 2: 비행 시간 (1~20시간)
        val seekBarFlightTime = findViewById<SeekBar>(R.id.seekBarFlightTime)
        // 초기값 표시
        updateFlightTimeDisplay(seekBarFlightTime.progress)
        seekBarFlightTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateFlightTimeDisplay(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        })

        // Step 3: 예산 (매우 부족 ~ 매우 여유로움)
        val seekBarBudget = findViewById<SeekBar>(R.id.seekBarBudget)
        updateBudgetDisplay(seekBarBudget.progress)
        seekBarBudget.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateBudgetDisplay(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        })

        // Step 4: 에너지 (쉬고 싶음 ~ 활동적)
        val seekBarEnergy = findViewById<SeekBar>(R.id.seekBarEnergy)
        updateEnergyDisplay(seekBarEnergy.progress)
        seekBarEnergy.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateEnergyDisplay(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        })

        // Step 5: 물욕 (없음 ~ 많음)
        val seekBarShopping = findViewById<SeekBar>(R.id.seekBarShopping)
        updateShoppingDisplay(seekBarShopping.progress)
        seekBarShopping.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateShoppingDisplay(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        })

        // Step 6: 성향 (I ~ E)
        val seekBarPersonality = findViewById<SeekBar>(R.id.seekBarPersonality)
        updatePersonalityDisplay(seekBarPersonality.progress)
        seekBarPersonality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updatePersonalityDisplay(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        })

        // Step 7: 잠 (바로 잠 ~ 잠이 중요)
        val seekBarSleep = findViewById<SeekBar>(R.id.seekBarSleep)
        updateSleepDisplay(seekBarSleep.progress)
        seekBarSleep.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSleepDisplay(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        })

        // Step 8: 숙소 컨디션 (1~10점)
        val seekBarAccommodation = findViewById<SeekBar>(R.id.seekBarAccommodation)
        updateAccommodationDisplay(seekBarAccommodation.progress)
        seekBarAccommodation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateAccommodationDisplay(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                btnNext.visibility = View.VISIBLE
            }
        })
    }

    // ===== 각 SeekBar 값 업데이트 함수 =====

    private fun updateFlightTimeDisplay(progress: Int) {
        // 0~100 → 1~20시간
        flightTimeValue = 1.0 + (progress / 100.0) * 19.0
        val displayValue = String.format("%.1f", flightTimeValue)
        tvFlightTimeValue.text = "${displayValue}시간"
        flightTimeLabel = "${displayValue}시간"
    }

    private fun updateBudgetDisplay(progress: Int) {
        budgetValue = progress
        budgetLabel = when {
            progress < 20 -> "매우 부족"
            progress < 40 -> "부족"
            progress < 60 -> "적당함"
            progress < 80 -> "여유로움"
            else -> "매우 여유로움"
        }
        tvBudgetValue.text = budgetLabel
    }

    private fun updateEnergyDisplay(progress: Int) {
        energyValue = progress
        energyLabel = when {
            progress < 25 -> "많이 쉬고 싶음"
            progress < 45 -> "쉬고 싶음"
            progress < 55 -> "보통"
            progress < 75 -> "활동적"
            else -> "매우 활동적"
        }
        tvEnergyValue.text = "$energyLabel (${progress}%)"
    }

    private fun updateShoppingDisplay(progress: Int) {
        shoppingValue = progress
        shoppingLabel = when {
            progress < 25 -> "거의 없음"
            progress < 45 -> "조금"
            progress < 55 -> "보통"
            progress < 75 -> "많음"
            else -> "매우 많음"
        }
        tvShoppingValue.text = "$shoppingLabel (${progress}%)"
    }

    private fun updatePersonalityDisplay(progress: Int) {
        personalityValue = progress
        personalityLabel = when {
            progress < 20 -> "완전 I"
            progress < 40 -> "I 성향"
            progress < 60 -> "중간"
            progress < 80 -> "E 성향"
            else -> "완전 E"
        }
        tvPersonalityValue.text = personalityLabel
    }

    private fun updateSleepDisplay(progress: Int) {
        sleepValue = progress
        sleepLabel = when {
            progress < 25 -> "바로 잠"
            progress < 45 -> "잠 잘 옴"
            progress < 55 -> "보통"
            progress < 75 -> "잠이 중요"
            else -> "숙면 필수"
        }
        tvSleepValue.text = sleepLabel
    }

    private fun updateAccommodationDisplay(progress: Int) {
        // 0~100 → 1.0~10.0
        accommodationValue = 1.0 + (progress / 100.0) * 9.0
        val displayValue = String.format("%.1f", accommodationValue)
        tvAccommodationValue.text = displayValue
        accommodationLabel = displayValue
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
                questionText = "비행 시간"
                answerText = flightTimeLabel
            }
            2 -> {
                questionText = "여행 예산이 궁금해요!"
                answerText = budgetLabel
            }
            3 -> {
                questionText = "여행 갔을 때 에너지는?"
                answerText = energyLabel
            }
            4 -> {
                questionText = "물욕"
                answerText = shoppingLabel
            }
            5 -> {
                questionText = "성향"
                answerText = personalityLabel
            }
            6 -> {
                questionText = "잠"
                answerText = sleepLabel
            }
            7 -> {
                questionText = "숙소 컨디션"
                answerText = accommodationLabel
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
        // 버튼 visibility - 마지막 스텝에서만 보이기
        btnNext.visibility = View.GONE

        // ScrollView 하단 패딩 조정
        val bottomPadding = if (currentStep == totalSteps - 1) dpToPx(120) else dpToPx(40)
        scrollViewQuestions.setPadding(
            scrollViewQuestions.paddingLeft,
            scrollViewQuestions.paddingTop,
            scrollViewQuestions.paddingRight,
            bottomPadding
        )

        // 버튼 텍스트 변경 (수정 모드면 "수정 완료")
        btnNext.text = when {
            currentStep == totalSteps - 1 && isEditMode -> "수정 완료"
            currentStep == totalSteps - 1 -> "여행 시작하기"
            else -> "다음"
        }

        // 모든 스텝 숨기기
        listOf(stepCompanion, stepFlightTime, stepBudget, stepEnergy,
               stepShopping, stepPersonality, stepSleep, stepAccommodation)
            .forEach { it.visibility = View.GONE }

        // 현재 스텝만 표시 (애니메이션 적용)
        val currentView = when (currentStep) {
            0 -> stepCompanion
            1 -> stepFlightTime
            2 -> stepBudget
            3 -> stepEnergy
            4 -> stepShopping
            5 -> stepPersonality
            6 -> stepSleep
            7 -> stepAccommodation
            else -> stepCompanion
        }

        currentView.visibility = View.VISIBLE
        currentView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
    }

    private fun saveAndNavigate() {
        // 비회원인 경우: GuestProfileData에 저장 후 TravelResultActivity로 이동
        if (isGuest) {
            GuestProfileData.companion = companion
            GuestProfileData.flightTimeValue = flightTimeValue
            GuestProfileData.flightTimeLabel = flightTimeLabel
            GuestProfileData.budgetValue = budgetValue
            GuestProfileData.budgetLabel = budgetLabel
            GuestProfileData.energyValue = energyValue
            GuestProfileData.energyLabel = energyLabel
            GuestProfileData.shoppingValue = shoppingValue
            GuestProfileData.shoppingLabel = shoppingLabel
            GuestProfileData.personalityValue = personalityValue
            GuestProfileData.personalityLabel = personalityLabel
            GuestProfileData.sleepValue = sleepValue
            GuestProfileData.sleepLabel = sleepLabel
            GuestProfileData.accommodationValue = accommodationValue
            GuestProfileData.accommodationLabel = accommodationLabel

            // SearchCountryResult로 이동 (AI 추천)
            val intent = Intent(this, SearchCountryResult::class.java)
            intent.putExtra("isGuest", true)
            intent.putExtra("hasSemiPass", true)
            startActivity(intent)
            finish()
            return
        }

        val uid = auth.currentUser?.uid ?: return

        val data = mapOf(
            "companion" to companion,
            "flightTimeValue" to flightTimeValue,
            "flightTimeLabel" to flightTimeLabel,
            "budgetValue" to budgetValue,
            "budgetLabel" to budgetLabel,
            "energyValue" to energyValue,
            "energyLabel" to energyLabel,
            "shoppingValue" to shoppingValue,
            "shoppingLabel" to shoppingLabel,
            "personalityValue" to personalityValue,
            "personalityLabel" to personalityLabel,
            "sleepValue" to sleepValue,
            "sleepLabel" to sleepLabel,
            "accommodationValue" to accommodationValue,
            "accommodationLabel" to accommodationLabel,
            "hasSemiPass" to true
        )

        // 로딩 표시
        btnNext.isEnabled = false
        btnNext.text = "저장 중..."

        Firebase.firestore.collection("profiles").document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                // 수정 모드면 MainContainerActivity의 내정보 탭으로
                if (isEditMode) {
                    Toast.makeText(this, "프로필이 수정되었습니다!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainContainerActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    intent.putExtra("initialTab", R.id.tab_profile)
                    startActivity(intent)
                } else {
                    // SearchCountryResult로 이동 (AI 추천)
                    val intent = Intent(this, SearchCountryResult::class.java)
                    intent.putExtra("hasSemiPass", true)
                    startActivity(intent)
                }
                finish()
            }
            .addOnFailureListener { e ->
                btnNext.isEnabled = true
                btnNext.text = if (isEditMode) "수정 완료" else "여행 시작하기"
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