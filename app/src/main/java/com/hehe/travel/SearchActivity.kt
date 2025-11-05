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

    // ----- 1) 데이터 모델 -----
    data class CountryInfo(
        val displayName: String,
        val places: String,
        val foods: String,
        val firstDayPlan: String
    )


    private fun norm(s: String) = s.lowercase().replace("\\s".toRegex(), "")


    private fun applyCountryInfo(info: CountryInfo) {

    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
    }

    private fun setupSearch() {
        binding.btnSearchCountry.setOnClickListener {
            val q = binding.etSearchCountry.text?.toString().orEmpty()
            if (q.isBlank()) {
                Toast.makeText(this, "나라 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                val intent = Intent(this, DateRangeActivity::class.java)
                intent.putExtra("country", q)
                intent.putExtra("login",auth.currentUser?.displayName)
                startActivity(intent)
            }


        }

        binding.etSearchCountry.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.btnSearchCountry.performClick()
                true
            } else false
        }
    }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSearch()
        setupBottomNav(R.id.tab_country)
        auth = FirebaseAuth.getInstance()


        findViewById<TextView>(R.id.travel).text =
            auth.currentUser?.displayName?.let { "어디로 떠나실래요?" } ?: "어디로 떠나실래요?"

        val uid = auth.currentUser?.uid
        if (uid != null) {
            val docRef = Firebase.firestore.collection("profiles").document(uid)
            docRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: ""

                    // ✅ 값들을 UI에 반영
                    findViewById<TextView>(R.id.name1).setText(nickname)

                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "데이터 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {

            findViewById<TextView>(R.id.name1).text =
                auth.currentUser?.displayName?.let { "$it" }
        }


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
                    true
                }
                R.id.tab_profile -> {
                    startActivity(Intent(this, MyInfoActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}