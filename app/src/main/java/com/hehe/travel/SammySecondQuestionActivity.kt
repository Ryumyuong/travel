package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_sammy_second_question)

        auth = FirebaseAuth.getInstance()

        val nickname = intent.getStringExtra("nickname") ?: ""
        val gender = intent.getStringExtra("gender") ?: ""
        val ageDecade = intent.getIntExtra("ageDecade", 0)
        val tags = intent.getStringArrayListExtra("tags") ?: arrayListOf()

        val rgCompanion = findViewById<RadioGroup>(R.id.rgCompanion)
        val seekBarBudget = findViewById<SeekBar>(R.id.seekBarBudget)
        val seekBarEnergy = findViewById<SeekBar>(R.id.seekBarEnergy)
        val btnStart = findViewById<AppCompatButton>(R.id.btnStart)

        btnStart.setOnClickListener {
            // 1) 선택된 칩 텍스트 모으기
            val companion = when (rgCompanion.checkedRadioButtonId) {
                R.id.rbAlone -> "혼자"
                R.id.rbCouple -> "연인"
                R.id.rbFriend -> "친구"
                R.id.rbFamily -> "가족"
                R.id.rbFamilyWithKids -> "가족(아이 동반)"
                else -> "혼자"
            }

            // 2) 예산, 에너지 (원하면 라벨로 저장해도 됨)
            val budgetLevel = seekBarBudget.progress              // 0:없음,1:적당함,2:여유로움
            val energyLevel = seekBarEnergy.progress              // 0:쉬고싶음,1:보통,2:쌩쌩함
            val budgetLabel = arrayOf("없음", "적당함", "여유로움")[budgetLevel]
            val energyLabel = arrayOf("쉬고 싶음", "보통", "액티비티 좋아함")[energyLevel]

            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val data = mapOf(
                "nickname" to nickname,
                "gender" to gender,
                "ageDecade" to ageDecade,
                "tags" to tags.toList(),
                "companion" to companion,
                "budgetLabel" to budgetLabel,   // 보기 좋게 라벨도 함께 저장(선택)
                "energyLabel" to energyLabel
            )
            Firebase.firestore.collection("profiles").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    startActivity(Intent(this, SearchActivity::class.java))
                }
                .addOnFailureListener { e ->
                }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // XML에서 설정한 패딩 값 유지하면서 시스템 바 insets 추가
            val originalPaddingLeft = v.paddingLeft
            val originalPaddingTop = v.paddingTop
            val originalPaddingRight = v.paddingRight
            val originalPaddingBottom = v.paddingBottom
            v.setPadding(
                originalPaddingLeft + systemBars.left,
                originalPaddingTop + systemBars.top,
                originalPaddingRight + systemBars.right,
                originalPaddingBottom + systemBars.bottom
            )
            insets
        }


    }
}