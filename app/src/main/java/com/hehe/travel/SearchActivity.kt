package com.hehe.travel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.hehe.travel.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var auth: FirebaseAuth
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupSearch()
        setupBottomNav(R.id.tab_country)
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            Firebase.firestore.collection("profiles").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nickname = document.getString("nickname") ?: ""
                        findViewById<TextView>(R.id.name1).text = nickname
                    }
                }
                .addOnFailureListener {
                    // 실패시 displayName 사용
                    findViewById<TextView>(R.id.name1).text =
                        auth.currentUser?.displayName ?: "여행자"
                }
        } else {
            findViewById<TextView>(R.id.name1).text =
                auth.currentUser?.displayName ?: "여행자"
        }

        findViewById<TextView>(R.id.travel).text = "어디로 떠나실래요?"
    }

    private fun setupSearch() {
        binding.btnSearchCountry.setOnClickListener {
            val country = binding.etSearchCountry.text?.toString().orEmpty().trim()
            if (country.isBlank()) {
                Toast.makeText(this, "나라 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()

            // TravelResultActivity로 이동 (AI 결과 화면)
            val intent = Intent(this, TravelResultActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
        }

        binding.etSearchCountry.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.btnSearchCountry.performClick()
                true
            } else false
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
    }

    private fun AppCompatActivity.setupBottomNav(selectedId: Int) {
        val bottom = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottom.selectedItemId = selectedId

        bottom.setOnItemSelectedListener { item ->
            if (item.itemId == selectedId) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.tab_country -> {
                    true
                }
                R.id.tab_search -> {
                    startActivity(Intent(this, StartActivity::class.java))
                    finish()
                    true
                }
                R.id.tab_profile -> {
                    startActivity(Intent(this, MyInfoActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            super.onBackPressed()
            finishAffinity()
        } else {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show()
        }
    }
}