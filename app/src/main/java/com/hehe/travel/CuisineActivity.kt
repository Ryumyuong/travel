package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hehe.travel.databinding.ActivityCuisineBinding

class CuisineActivity : AppCompatActivity() {

    private lateinit var country: String
    private lateinit var titleText: TextView
    private lateinit var cuisineRecyclerView: RecyclerView
    private lateinit var cuisineAdapter: CuisineAdapter
    private lateinit var binding: ActivityCuisineBinding
    private var allItems: List<CuisineItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCuisineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        country = intent.getStringExtra("country").toString()

        cuisineAdapter = CuisineAdapter()
        binding.rvCuisine.apply {
            layoutManager = LinearLayoutManager(this@CuisineActivity)
            adapter = cuisineAdapter
        }

        initViews()
        val data = getCuisineByCountry(country)
        cuisineAdapter.submitList(data)

        allItems = getCuisineByCountry(country)
        cuisineAdapter.submitList(allItems)

        binding.btnTraditional.isSelected = true

        binding.btnTraditional.setOnClickListener {
            binding.btnTraditional.isSelected = true
            binding.btnDessert.isSelected = false
            applyFilter(type = "전통음식")
        }
        binding.btnDessert.setOnClickListener {
            binding.btnTraditional.isSelected = false
            binding.btnDessert.isSelected = true
            applyFilter(type = "디저트")
        }


        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.home.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            finish()
        }


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

    private fun applyFilter(type: String) {
        val filtered = allItems.filter { it.type == type }
        cuisineAdapter.submitList(filtered)     // ListAdapter면 submitList
        // 선택 상태 UI(선택): 필요하면 버튼 enable/tint 등 토글
        updateCategoryUI(type)
    }

    private fun updateCategoryUI(selected: String) {
        val isDessert = selected == "디저트"
        binding.btnDessert.isSelected = isDessert
        binding.btnTraditional.isSelected = !isDessert
        // 선택 스타일 바꾸고 싶으면 background/tint 변경 로직 추가
    }


    // ======================== 공통 헬퍼 ========================
    private fun key(country: String) = country.lowercase().trim()

    // ===================== 1) 나라별 음식 묶음 =====================
    private fun getCuisineByCountry(country: String): List<CuisineItem> = when (key(country)) {
        "italy" -> listOf(
            CuisineItem("마르게리타 피자", "나폴리 스타일의 정통 피자", "전통음식"),
            CuisineItem("프루티 디 마레", "신선한 해산물 파스타/피자", "전통음식"),
            CuisineItem("라자냐", "층층이 겹친 면과 라구의 조화", "전통음식"),
            CuisineItem("티라미수", "에스프레소 풍미의 디저트", "디저트")
        )
        "france" -> listOf(
            CuisineItem("크루아상", "버터 풍미의 페이스트리", "디저트"),
            CuisineItem("에스카르고", "버터·마늘 달팽이 요리", "전통음식"),
            CuisineItem("부야베스", "지중해식 해산물 스튜", "전통음식"),
            CuisineItem("마카롱", "달콤한 머랭 과자", "디저트")
        )
        "japan" -> listOf(
            CuisineItem("스시", "신선한 생선과 샤리", "전통음식"),
            CuisineItem("라멘", "진한 국물의 면 요리", "전통음식"),
            CuisineItem("오코노미야키", "일본식 부침개", "전통음식"),
            CuisineItem("모찌", "찹쌀 디저트", "디저트")
        )
        "korea" -> listOf(
            CuisineItem("비빔밥", "채소·고기·고추장 비빔밥", "전통음식"),
            CuisineItem("불고기", "양념 소고기 구이", "전통음식"),
            CuisineItem("김치", "발효 채소 반찬", "전통음식"),
            CuisineItem("팥빙수", "빙수 디저트", "디저트")
        )
        "usa" -> listOf(
            CuisineItem("치즈버거", "대표적인 미국식 버거", "전통음식"),
            CuisineItem("바비큐 립", "훈연 소스 립", "전통음식"),
            CuisineItem("클램 차우더", "크리미한 조개 스프", "전통음식"),
            CuisineItem("애플 파이", "전통 디저트", "디저트")
        )
        "spain" -> listOf(
            CuisineItem("파에야", "사프란 해산물/고기 밥", "전통음식"),
            CuisineItem("타파스", "작은 접시 안주들", "전통음식"),
            CuisineItem("하몬", "이베리코 햄", "전통음식"),
            CuisineItem("츄로스", "초콜릿과 함께", "디저트")
        )
        "germany" -> listOf(
            CuisineItem("브라트부어스트", "독일식 소시지", "전통음식"),
            CuisineItem("슈니첼", "얇은 커틀릿", "전통음식"),
            CuisineItem("프레첼", "소금빵 스낵", "전통음식"),
            CuisineItem("자우어크라우트", "발효 양배추", "전통음식")
        )
        "uk" -> listOf(
            CuisineItem("피시 앤 칩스", "튀긴 생선과 감자", "전통음식"),
            CuisineItem("선데이 로스트", "일요일 구이 정식", "전통음식"),
            CuisineItem("풀 잉글리시 브렉퍼스트", "영국식 아침", "전통음식"),
            CuisineItem("스콘", "잼·클로티드 크림", "디저트")
        )
        "thailand" -> listOf(
            CuisineItem("팟타이", "볶음쌀국수", "전통음식"),
            CuisineItem("똠얌꿍", "매콤 새우 수프", "전통음식"),
            CuisineItem("솜탐", "파파야 샐러드", "전통음식"),
            CuisineItem("망고 스티키라이스", "망고 찹쌀 디저트", "디저트")
        )
        "australia" -> listOf(
            CuisineItem("미트 파이", "고기 파이", "전통음식"),
            CuisineItem("바비큐", "호주식 그릴", "전통음식"),
            CuisineItem("라밍턴", "코코넛 케이크", "디저트"),
            CuisineItem("플랫 화이트", "커피", "음료")
        )
        "vietnam" -> listOf(
            CuisineItem("퍼(Phở)", "쌀국수", "전통음식"),
            CuisineItem("분짜", "숯불고기 비빔면", "전통음식"),
            CuisineItem("반미", "바게트 샌드위치", "전통음식"),
            CuisineItem("분보남보", "소고기 비빔쌀국수", "전통음식")
        )
        "greece" -> listOf(
            CuisineItem("무사카", "가지 그라탱", "전통음식"),
            CuisineItem("수블라키", "꼬치구이", "전통음식"),
            CuisineItem("그릭 샐러드", "페타치즈 샐러드", "전통음식"),
            CuisineItem("바클라바", "견과류 페이스트리", "디저트")
        )
        "turkey", "튀르키예(터키)" -> listOf(
            CuisineItem("케밥", "대표적인 터키 그릴", "전통음식"),
            CuisineItem("메제", "전채요리 모듬", "전통음식"),
            CuisineItem("바클라바", "시럽 페이스트리", "디저트"),
            CuisineItem("로쿰", "터키시 딜라이트", "디저트")
        )
        "switzerland" -> listOf(
            CuisineItem("치즈 퐁듀", "치즈에 빵을 찍어 먹는 요리", "전통음식"),
            CuisineItem("라클렛", "녹인 치즈 요리", "전통음식"),
            CuisineItem("로스티", "감자전", "전통음식"),
            CuisineItem("초콜릿", "스위스 초콜릿", "디저트")
        )
        "portugal", "포르투갈", "포르투칼" -> listOf(
            CuisineItem("파스텔 드 나타", "에그 타르트", "디저트"),
            CuisineItem("바칼라우", "대구 요리", "전통음식"),
            CuisineItem("삐리삐리 치킨", "매콤 치킨", "전통음식"),
            CuisineItem("칼도 베르데", "케일 감자 수프", "전통음식")
        )
        "egypt" -> listOf(
            CuisineItem("코샤리", "렌틸·파스타 혼합 요리", "전통음식"),
            CuisineItem("풀 메담스", "삶은 잠두콩 요리", "전통음식"),
            CuisineItem("샤와르마", "중동식 랩", "전통음식"),
            CuisineItem("바클라바", "디저트", "디저트")
        )
        else -> emptyList()
    }
}