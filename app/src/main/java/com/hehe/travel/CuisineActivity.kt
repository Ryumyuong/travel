package com.hehe.travel

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CuisineActivity : AppCompatActivity() {

    private lateinit var country: String
    private lateinit var titleText: TextView
    private lateinit var cuisineRecyclerView: RecyclerView
    private lateinit var cuisineAdapter: CuisineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cuisine)

        country = intent.getStringExtra("country") ?: "italy"

        initViews()
        loadCuisine()
    }

    private fun initViews() {
        titleText = findViewById(R.id.tv_cuisine_title)
        cuisineRecyclerView = findViewById(R.id.rv_cuisine)

        titleText.text = "미식 체험"

        cuisineAdapter = CuisineAdapter()
        cuisineRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CuisineActivity)
            adapter = cuisineAdapter
        }
    }

    private fun loadCuisine() {
        val cuisine = when (country) {
            "italy" -> getItalyCuisine()
            else -> emptyList()
        }
        cuisineAdapter.submitList(cuisine)
    }

    private fun getItalyCuisine(): List<CuisineItem> {
        return listOf(
            CuisineItem(
                name = "마르게리타 피자 (Margherita Pizza)",
                description = "나폴리 스타일의 정통 피자",
                type = "전통음식"
            ),
            CuisineItem(
                name = "프루티 디 마레 (Frutti di Mare)",
                description = "신선한 해산물 요리",
                type = "전통음식"
            ),
            CuisineItem(
                name = "라자냐 (Lasagna)",
                description = "이탈리아식 밀푸유",
                type = "전통음식"
            ),
            CuisineItem(
                name = "티라미수 (Tiramisu)",
                description = "이탈리아 대표 디저트",
                type = "디저트"
            )
        )
    }
}