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

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnBrowse = findViewById<Button>(R.id.btnBrowse)

        btnStart.setOnClickListener {
            val intent = Intent(this, SammyFirstQuestionActivity::class.java)
            startActivity(intent)
        }

        btnBrowse.setOnClickListener {
            // 새미패스 미작성 상태 저장
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val data = mapOf("hasSemiPass" to false)
                Firebase.firestore.collection("profiles").document(uid)
                    .set(data, SetOptions.merge())
            }

            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
    }
}