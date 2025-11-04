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

    // ----- 2) 나라 데이터 (필요시 더 추가) -----
    private val countries: Map<String, CountryInfo> = mapOf(
        "italy" to CountryInfo(
            "이탈리아",
            "포지타노, 카프리섬, 폼페이, 나폴리",
            "마르게리타 피자, 라자냐, 티라미수, 프루티 디 마레",
            "첫째 날 - 도착과 첫 만남"
        ),
        "france" to CountryInfo(
            "프랑스",
            "에펠탑, 루브르 박물관, 몽마르트르, 베르사유",
            "크루아상, 에스카르고, 부야베스, 마카롱",
            "첫째 날 - 파리 시내 산책과 세느강 야경"
        ),
        "japan" to CountryInfo(
            "일본",
            "도쿄 스카이트리, 아사쿠사, 교토 아라시야마, 오사카 도톤보리",
            "스시, 라멘, 오코노미야키, 가이센동",
            "첫째 날 - 도쿄 도착 및 시부야 교차로"
        ),
        "korea" to CountryInfo(
            "대한민국",
            "경복궁, 북촌한옥마을, 남산타워, 홍대",
            "비빔밥, 불고기, 김치찌개, 떡볶이",
            "첫째 날 - 서울 도착과 전통시장 투어"
        ),
        "usa" to CountryInfo(
            "미국",
            "뉴욕 타임스퀘어, 센트럴파크, 메트로폴리탄 미술관, 브루클린 브리지",
            "버거, 핫도그, 스테이크, 클램 차우더",
            "첫째 날 - 뉴욕 도착 및 미드타운 산책"
        ),
        "spain" to CountryInfo(
            "스페인",
            "사그라다 파밀리아, 구엘 공원, 고딕 지구, 몬주익",
            "파에야, 타파스, 하몬, 츄로스",
            "첫째 날 - 바르셀로나 람블라스 산책"
        ),
        "germany" to CountryInfo(
            "독일",
            "브란덴부르크 문, 박물관 섬, 이스트사이드 갤러리, 샤를로텐부르크",
            "브라트부어스트, 슈니첼, 프레첼, 자우어크라우트",
            "첫째 날 - 베를린 미테 지구 투어"
        ),
        "uk" to CountryInfo(
            "영국",
            "버킹엄궁, 타워 브리지, 대영박물관, 웨스트엔드",
            "피시 앤 칩스, 선데이 로스트, 애프터눈 티, 풀 잉글리시",
            "첫째 날 - 런던 시티 하이라이트"
        ),
        "thailand" to CountryInfo(
            "태국",
            "왕궁, 왓 아룬, 짜오프라야 강, 차뚜짝 시장",
            "팟타이, 똠얌꿍, 솜탐, 망고 스티키라이스",
            "첫째 날 - 방콕 사원 & 야시장"
        ),
        "australia" to CountryInfo(
            "호주",
            "시드니 오페라하우스, 하버 브리지, 본다이 비치, 더 록스",
            "미트 파이, 람빙턴, 바비큐, 플랫 화이트",
            "첫째 날 - 서큘러 키 & 하버 워크"
        ),
        "vietnam" to CountryInfo(
            "베트남",
            "하노이 호안끼엠 호수, 하롱베이, 올드 쿼터, 닌빈",
            "퍼(Phở), 분짜, 반미, 분보남보",
            "첫째 날 - 하노이 구시가지 탐방"
        ),
        "greece" to CountryInfo(
            "그리스",
            "아크로폴리스, 플라카, 산토리니 오이아, 피라",
            "무사카, 수블라키, 그릭 샐러드, 바클라바",
            "첫째 날 - 아테네 파르테논 관람"
        ),
        "turkey" to CountryInfo(
            "튀르키예(터키)",
            "아야소피아, 블루 모스크, 그랜드 바자르, 보스포루스",
            "케밥, 로쿰, 바클라바, 메제",
            "첫째 날 - 술탄아흐메트 지구 산책"
        ),
        "switzerland" to CountryInfo(
            "스위스",
            "인터라켄, 융프라우요흐, 루체른 카펠교, 취리히 호숫가",
            "치즈 퐁듀, 라클렛, 로스티, 초콜릿",
            "첫째 날 - 루체른 호수 산책과 구시가지"
        ),
        "portugal" to CountryInfo(
            "포르투갈",
            "리스본 벨렝 탑, 제로니모스 수도원, 알파마, 신트라",
            "파스텔 데 나타, 바칼라우 요리, 삐리삐리 치킨, 카르코이스",
            "첫째 날 - 트램 28 탑승 & 전망대"
        ),
        "egypt" to CountryInfo(
            "이집트",
            "기자 피라미드, 스핑크스, 이집트 박물관, 칸 칼릴리 시장",
            "코샤리, 풀 메담스, 샤와르마, 바클라바",
            "첫째 날 - 카이로 이집트 박물관 관람"
        ),
    )

    // ----- 3) 별칭(한글/영문) → 키 매핑 -----
    private val aliasToKey: Map<String, String> = mapOf(
        "italy" to "italy", "이탈리아" to "italy", "italia" to "italy",
        "france" to "france", "프랑스" to "france",
        "japan" to "japan", "일본" to "japan", "nippon" to "japan",
        "korea" to "korea", "대한민국" to "korea", "한국" to "korea", "southkorea" to "korea",
        "usa" to "usa", "미국" to "usa", "unitedstates" to "usa",
        "spain" to "spain", "스페인" to "spain",
        "germany" to "germany", "독일" to "germany",
        "uk" to "uk", "영국" to "uk", "unitedkingdom" to "uk",
        "thailand" to "thailand", "태국" to "thailand",
        "australia" to "australia", "호주" to "australia",
        "vietnam" to "vietnam", "베트남" to "vietnam",
        "greece" to "greece", "그리스" to "greece",
        "turkey" to "turkey", "튀르키예" to "turkey", "터키" to "turkey",
        "switzerland" to "switzerland", "스위스" to "switzerland",
        "portugal" to "portugal", "포르투갈" to "portugal",
        "egypt" to "egypt", "이집트" to "egypt"
    )

    private fun norm(s: String) = s.lowercase().replace("\\s".toRegex(), "")

    private fun searchCountry(query: String): CountryInfo? {
        val key = aliasToKey[norm(query)]
        if (key != null) return countries[key]

        // 별칭에 없으면 displayName 부분 일치 검색
        val n = norm(query)
        return countries.values.firstOrNull { norm(it.displayName).contains(n) }
    }

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
            }
            val result = searchCountry(q)
            if (result != null) {
                applyCountryInfo(result)
                hideKeyboard()

                val intent = Intent(this, DateRangeActivity::class.java)
                intent.putExtra("country", q)
                intent.putExtra("login",auth.currentUser?.displayName)
                startActivity(intent)

            } else {
                Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
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