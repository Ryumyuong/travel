package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class SammyPassMainActivity : AppCompatActivity() {

    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sammy_pass_main)
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

        // 수정 모드 플래그 확인 (MyInfoActivity에서 진입 시)
        isEditMode = intent.getBooleanExtra("isEditMode", false)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnBrowse = findViewById<Button>(R.id.btnBrowse)

        btnStart.setOnClickListener {
            // SammySecondQuestionActivity로 이동 (새미패스 질문)
            val intent = Intent(this, SammySecondQuestionActivity::class.java)
            intent.putExtra("isEditMode", isEditMode)  // 수정 모드 전달
            startActivity(intent)
            if (isEditMode) finish()  // 수정 모드에서는 뒤로가기 시 MyInfo로 돌아가도록
        }

        btnBrowse.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            // 비회원인 경우: MainContainerActivity로 바로 이동
            if (uid == null) {
                val intent = Intent(this, MainContainerActivity::class.java)
                intent.putExtra("isGuest", true)
                startActivity(intent)
                return@setOnClickListener
            }

            // 회원인 경우
            if (!isEditMode) {
                // 수정 모드가 아닐 때: 새미패스 미작성 상태 저장
                val data = mapOf("hasSemiPass" to false)
                Firebase.firestore.collection("profiles").document(uid)
                    .set(data, SetOptions.merge())
            }

            // SammyFirstQuestionActivity로 이동 (프로필 질문)
            val intent = Intent(this, SammyFirstQuestionActivity::class.java)
            intent.putExtra("fromBrowse", true)  // 둘러보기에서 진입 표시
            intent.putExtra("isEditMode", isEditMode)  // 수정 모드 전달
            startActivity(intent)
            if (isEditMode) finish()  // 수정 모드에서는 뒤로가기 시 MyInfo로 돌아가도록
        }
    }
}