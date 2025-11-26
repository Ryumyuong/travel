package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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
        // Step 1: 닉네임
        findViewById<EditText>(R.id.etNickname).setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                nickname = findViewById<EditText>(R.id.etNickname).text.toString()
            }
        }

        // Step 2: 성별
        findViewById<RadioGroup>(R.id.rgGender).setOnCheckedChangeListener { _, checkedId ->
            gender = if (checkedId == R.id.rbMale) "남자" else "여자"
        }

        // Step 3: 연령대
        val seekBarAge = findViewById<SeekBar>(R.id.seekBarAge)
        seekBarAge.max = 4
        seekBarAge.progress = 2
        seekBarAge.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ageDecade = (progress + 1) * 10
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Step 4: 여행 목적 - 새로고침 버튼
        findViewById<ImageButton>(R.id.btnRefresh)?.setOnClickListener { resetPurposes() }
        findViewById<TextView>(R.id.tvRefresh)?.setOnClickListener { resetPurposes() }
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
        // 현재 스텝 데이터 저장
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

        if (currentStep < totalSteps - 1) {
            currentStep++
            updateUI()
        } else {
            // 마지막 스텝 - 저장 후 이동
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

    private fun updateUI() {

        // ⭐ 버튼 텍스트 변경 (수정 모드면 "수정 완료")
        btnNext.text = when {
            currentStep == totalSteps - 1 && isEditMode -> "수정 완료"
            currentStep == totalSteps - 1 -> "완료"
            else -> "다음"
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