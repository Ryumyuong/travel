package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import com.google.android.material.chip.ChipGroup
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class SammyFirstQuestionActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    // 현재 스텝 (0부터 시작)
    private var currentStep = 0
    private val totalSteps = 4

    // 수집할 데이터
    private var nickname = ""
    private var gender = "남자"
    private var ageDecade = 30
    private var selectedPurposes = arrayListOf<String>()

    // ⭐ 수정 모드 플래그
    private var isEditMode = false

    // Step Containers
    private lateinit var stepNickname: View
    private lateinit var stepGender: View
    private lateinit var stepAge: View
    private lateinit var stepPurpose: View

    private lateinit var btnNext: AppCompatButton
    private lateinit var completedQuestionsContainer: LinearLayout
    private lateinit var scrollViewQuestions: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sammy_first_question)

        auth = FirebaseAuth.getInstance()

        // ⭐ 수정 모드 확인
        isEditMode = intent.getBooleanExtra("isEditMode", false)

        initViews()
        setupStepViews()

        // ⭐ 수정 모드면 기존 데이터 로드
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

        stepNickname = findViewById(R.id.stepNickname)
        stepGender = findViewById(R.id.stepGender)
        stepAge = findViewById(R.id.stepAge)
        stepPurpose = findViewById(R.id.stepPurpose)

        btnNext = findViewById(R.id.btnNext)
        completedQuestionsContainer = findViewById(R.id.completedQuestionsContainer)
        scrollViewQuestions = findViewById(R.id.scrollViewQuestions)

        // 기본 닉네임 설정 (수정 모드가 아닐 때만)
        if (!isEditMode) {
            auth.currentUser?.displayName?.let {
                nickname = it
                findViewById<EditText>(R.id.etNickname).setText(it)
            }
        }

        btnNext.setOnClickListener { onNextClicked() }
    }

    // ⭐ 기존 프로필 데이터 로드
    private fun loadExistingProfile() {
        val uid = auth.currentUser?.uid ?: return

        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // 닉네임
                    nickname = doc.getString("nickname") ?: ""
                    findViewById<EditText>(R.id.etNickname).setText(nickname)

                    // 성별
                    gender = doc.getString("gender") ?: "남자"
                    val rgGender = findViewById<RadioGroup>(R.id.rgGender)
                    if (gender == "남자") {
                        rgGender.check(R.id.rbMale)
                    } else {
                        rgGender.check(R.id.rbFemale)
                    }

                    // 연령대
                    ageDecade = doc.getLong("ageDecade")?.toInt() ?: 30
                    val seekBarAge = findViewById<SeekBar>(R.id.seekBarAge)
                    seekBarAge.progress = (ageDecade / 10) - 1  // 10대=0, 20대=1, 30대=2...

                    // 여행 목적
                    val purposes = doc.get("purposes") as? List<String> ?: emptyList()
                    selectedPurposes = ArrayList(purposes)

                    // ChipGroup에서 해당 목적들 체크
                    val chipGroup = findViewById<ChipGroup>(R.id.chipGroupPurpose)
                    for (i in 0 until chipGroup.childCount) {
                        val chip = chipGroup.getChildAt(i) as? Chip
                        chip?.let {
                            it.isChecked = purposes.contains(it.text.toString())
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "프로필 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupStepViews() {
        // Step 1: 닉네임 - 엔터/완료 키 입력 시 다음 단계로
        val etNickname = findViewById<EditText>(R.id.etNickname)
        etNickname.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                onNextClicked()
                true
            } else {
                false
            }
        }

        // Step 2: 성별 - 선택하면 자동으로 다음 스텝
        findViewById<RadioGroup>(R.id.rgGender).setOnCheckedChangeListener { _, checkedId ->
            gender = if (checkedId == R.id.rbMale) "남자" else "여자"

            // 자동으로 다음 스텝 이동
            if (checkedId != -1) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        }

        // Step 3: 연령대 - 터치 끊기면 자동으로 다음 스텝
        val seekBarAge = findViewById<SeekBar>(R.id.seekBarAge)
        seekBarAge.max = 4
        seekBarAge.progress = 2
        seekBarAge.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ageDecade = (progress + 1) * 10
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 자동으로 다음 스텝 이동
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onNextClicked()
                }, 300)
            }
        })

        // Step 4: 여행 목적 - 새로고침 버튼
        findViewById<ImageButton>(R.id.btnRefresh)?.setOnClickListener { resetPurposes() }
        findViewById<TextView>(R.id.tvRefresh)?.setOnClickListener { resetPurposes() }
        findViewById<ImageButton>(R.id.btnReset)?.setOnClickListener { resetPurposes() }
        findViewById<TextView>(R.id.tvReset)?.setOnClickListener { resetPurposes() }
    }

    private fun resetPurposes() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupPurpose)
        for (i in 0 until chipGroup.childCount) {
            (chipGroup.getChildAt(i) as? Chip)?.isChecked = false
        }
        Toast.makeText(this, "선택이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun collectPurposes() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupPurpose)
        selectedPurposes = (0 until chipGroup.childCount)
            .mapNotNull { chipGroup.getChildAt(it) as? Chip }
            .filter { it.isChecked }
            .map { it.text.toString() }
            .toCollection(ArrayList())
    }

    private fun onNextClicked() {
        // 현재 스텝 데이터 저장 및 검증
        when (currentStep) {
            0 -> {
                nickname = findViewById<EditText>(R.id.etNickname).text.toString()
                if (nickname.isBlank()) {
                    Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            3 -> collectPurposes()
        }

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
            // 마지막 스텝 - 저장 후 이동
            collectPurposes()
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
                questionText = "Q. 제가 뭐라고 부르면 될까요?"
                answerText = nickname
            }
            1 -> {
                questionText = "Q. 성별을 알려주세요"
                answerText = gender
            }
            2 -> {
                questionText = "Q. 연령대를 알려주세요"
                answerText = "${ageDecade}대"
            }
            3 -> {
                questionText = "Q. 이번 여행의 목적은 무엇인가요?"
                answerText = selectedPurposes.joinToString(", ")
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

        // 버튼 visibility - 여행 목적(step 3)에서만 보이기
        btnNext.visibility = if (currentStep == totalSteps - 1) View.VISIBLE else View.GONE

        // ScrollView 하단 패딩 조정 (버튼이 보일 때만 여백)
        val bottomPadding = if (currentStep == totalSteps - 1) dpToPx(120) else dpToPx(40)
        scrollViewQuestions.setPadding(
            scrollViewQuestions.paddingLeft,
            scrollViewQuestions.paddingTop,
            scrollViewQuestions.paddingRight,
            bottomPadding
        )

        // ⭐ 버튼 텍스트 변경 (수정 모드면 "수정 완료")
        btnNext.text = when {
            currentStep == totalSteps - 1 && isEditMode -> "수정 완료"
            currentStep == totalSteps - 1 -> "다음으로"
            else -> "다음으로"
        }

        // 모든 스텝 숨기기
        listOf(stepNickname, stepGender, stepAge, stepPurpose)
            .forEach { it.visibility = View.GONE }

        // 현재 스텝만 표시 (애니메이션 적용)
        val currentView = when (currentStep) {
            0 -> stepNickname
            1 -> stepGender
            2 -> stepAge
            3 -> stepPurpose
            else -> stepNickname
        }

        currentView.visibility = View.VISIBLE
        currentView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
    }

    private fun saveAndNavigate() {
        val uid = auth.currentUser?.uid ?: return

        val data = mapOf(
            "nickname" to nickname,
            "gender" to gender,
            "ageDecade" to ageDecade,
            "purposes" to selectedPurposes.toList()
        )

        // 로딩 표시
        btnNext.isEnabled = false
        btnNext.text = "저장 중..."

        Firebase.firestore.collection("profiles").document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                // ⭐ 수정 모드면 MyInfoActivity로, 아니면 SemiPassChoiceActivity로
                if (isEditMode) {
                    Toast.makeText(this, "프로필이 수정되었습니다! ✅", Toast.LENGTH_SHORT).show()
                    // MyInfoActivity로 돌아가기 (스택 정리)
                    val intent = Intent(this, MyInfoActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                } else {
                    // 세미패스 선택 화면으로 이동
                    startActivity(Intent(this, SemiPassChoiceActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener { e ->
                btnNext.isEnabled = true
                btnNext.text = if (isEditMode) "수정 완료" else "완료"
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