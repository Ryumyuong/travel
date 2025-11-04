package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class TravelHistoryActivity : AppCompatActivity() {
    data class Trip(val country: String, val period: String, val memo: String)
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_travel_history)
        setupBottomNav(R.id.tab_profile)

        auth = FirebaseAuth.getInstance()

        val uid = auth.currentUser?.uid
        if (uid != null) {
            val docRef = Firebase.firestore.collection("profiles").document(uid)
            docRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: ""
                    val name = nickname +" 님"
                    val nick = name.substring(1)

                    // ✅ 값들을 UI에 반영
                    findViewById<TextView>(R.id.name).setText(nick)

                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "데이터 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {

            findViewById<TextView>(R.id.name).text =
                auth.currentUser?.displayName?.substring(1)?.let { "$it 님" }
        }


            val rv = findViewById<RecyclerView>(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(this)
        val data = listOf(
            Trip("이탈리아", "2024.05 · 7일", "남부 해안 드라이브"),
            Trip("일본", "2023.11 · 4일", "교토 단풍"),
            Trip("태국", "2023.02 · 5일", "푸켓 휴양")
        )
        rv.adapter = object : RecyclerView.Adapter<HistoryVH>() {
            override fun onCreateViewHolder(p: ViewGroup, vType: Int) =
                HistoryVH(
                    LayoutInflater.from(p.context)
                    .inflate(android.R.layout.simple_list_item_2, p, false))
            override fun onBindViewHolder(h: HistoryVH, i: Int) {
                val t = data[i]
                h.t1.text = t.country
                h.t2.text = "${t.period} · ${t.memo}"
            }
            override fun getItemCount() = data.size
        }
    }
    class HistoryVH(v: View): RecyclerView.ViewHolder(v){
        val t1: TextView = v.findViewById(android.R.id.text1)
        val t2: TextView = v.findViewById(android.R.id.text2)
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
                    startActivity(
                        Intent(this, StartActivity::class.java))
                    true
                }
                R.id.tab_profile -> {
                    startActivity(
                        Intent(this, MyInfoActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                else -> false
            }
        }
    }
}