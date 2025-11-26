package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
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
        // Step 1: 동행자
        findViewById<RadioGroup>(R.id.rgCompanion).setOnCheckedChangeListener { _, checkedId ->
            companion = when (checkedId) {
                R.id.rbAlone -> "혼자"
                R.id.rbCouple -> "연인"
                R.id.rbFriend -> "친구"
                R.id.rbFamily -> "가족"
                R.id.rbFamilyWithKids -> "가족(아이 동반)"
                else -> "혼자"
            }
        }

        // Step 2: 예산 (5단계)
        val seekBarBudget = findViewById<SeekBar>(R.id.seekBarBudget)
        seekBarBudget.max = 4
        seekBarBudget.progress = 2
        seekBarBudget.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                budgetLabel = arrayOf("매우 부족", "부족", "적당함", "여유로움", "매우 여유로움")[progress]
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Step 3: 에너지 (5단계)
        val seekBarEnergy = findViewById<SeekBar>(R.id.seekBarEnergy)
        seekBarEnergy.max = 4
        seekBarEnergy.progress = 2
        seekBarEnergy.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                energyLabel = arrayOf("많이 쉬고 싶음", "쉬고 싶음", "보통", "활동적", "매우 활동적")[progress]
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun onNextClicked() {
        if (currentStep < totalSteps - 1) {
            currentStep++
            updateUI()
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

    private fun updateUI() {

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