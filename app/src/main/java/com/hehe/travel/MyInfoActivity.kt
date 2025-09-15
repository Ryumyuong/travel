package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.hehe.travel.databinding.ActivityMainBinding
import com.hehe.travel.databinding.ActivityMyInfoBinding

class MyInfoActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var binding: ActivityMyInfoBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

                auth = FirebaseAuth.getInstance()
        findViewById<TextView>(R.id.tvGreeting).text =
            auth.currentUser?.displayName?.let { "$it 님" } ?: "내 정보"

        findViewById<View>(R.id.cardHistory).setOnClickListener {
            startActivity(Intent(this, TravelHistoryActivity::class.java))
        }
//        findViewById<View>(R.id.cardMap).setOnClickListener {
//            startActivity(Intent(this, MapActivity::class.java))
//        }
        findViewById<View>(R.id.cardPref).setOnClickListener {
            startActivity(Intent(this, PreferenceRecommendActivity::class.java))
        }

        // (선택) 하단 3탭 네비 유지 중이면:
         setupBottomNav(R.id.tab_profile)

        binding.btnLogout.setOnClickListener {
            signOut() // 또는 disconnectGoogle()
        }

        initAuthAndGoogleClient()

    }

    private fun initAuthAndGoogleClient() {
        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun AppCompatActivity.setupBottomNav(selectedId: Int) {
        val bottom = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottom.selectedItemId = selectedId

        bottom.setOnItemSelectedListener { item ->
            if (item.itemId == selectedId) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.tab_country -> {
                    startActivity(
                        Intent(this, SearchActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                R.id.tab_search -> {
                    startActivity(Intent(this, StartActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                R.id.tab_profile -> {
                    startActivity(Intent(this, MyInfoActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                else -> false
            }
        }
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            auth.signOut()
            goToLogin()
        }.addOnFailureListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}