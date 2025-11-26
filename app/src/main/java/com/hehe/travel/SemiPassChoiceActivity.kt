package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class SemiPassChoiceActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_semi_pass_choice)

        auth = FirebaseAuth.getInstance()

        val btnStartSemiPass = findViewById<AppCompatButton>(R.id.btnStartSemiPass)
        val btnSkip = findViewById<AppCompatButton>(R.id.btnSkip)

        // 세미패스 작성하기 클릭
        btnStartSemiPass.setOnClickListener {
            // 세미패스 작성 화면으로 이동
            startActivity(Intent(this, SammySecondQuestionActivity::class.java))
            finish()
        }

        // 그냥 둘러만 볼게요 클릭
        btnSkip.setOnClickListener {
            // 세미패스 미작성 상태 저장
            val uid = auth.currentUser?.uid
            if (uid != null) {
                val data = mapOf("hasSemiPass" to false)
                Firebase.firestore.collection("profiles").document(uid)
                    .set(data, SetOptions.merge())
            }

            // SearchActivity로 이동
            startActivity(Intent(this, SearchActivity::class.java))
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}