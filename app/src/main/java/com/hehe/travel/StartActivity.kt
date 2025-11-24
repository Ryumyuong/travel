package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hehe.travel.databinding.ActivityMainBinding
import com.hehe.travel.databinding.ActivityStartBinding




class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav(R.id.tab_search)

        binding.start.setOnClickListener {
            startActivity(Intent(this, QuestionnaireActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            true
        }

    }


    private fun AppCompatActivity.setupBottomNav(selectedId: Int) {
        val bottom = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottom.selectedItemId = selectedId

        bottom.setOnItemSelectedListener { item ->
            if (item.itemId == selectedId) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.tab_country -> {
                    startActivity(
                        Intent(this, SearchActivity::class.java))
                    true
                }
                R.id.tab_search -> {
                    startActivity(Intent(this, StartActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
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