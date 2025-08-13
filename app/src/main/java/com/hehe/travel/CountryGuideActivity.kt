package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hehe.travel.databinding.ActivityCountryGuideBinding

class CountryGuideActivity : AppCompatActivity() {

    private lateinit var country: String
    private lateinit var binding: ActivityCountryGuideBinding

    // Views
    private lateinit var countryTitle: TextView
    private lateinit var destinationCard: CardView
    private lateinit var cuisineCard: CardView
    private lateinit var itineraryCard: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountryGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        country = intent.getStringExtra("country").toString()

        initViews()
        loadCountryData()
    }

    private fun initViews() {
        countryTitle = findViewById(R.id.tv_country_title)
        destinationCard = findViewById(R.id.card_destinations)
        cuisineCard = findViewById(R.id.card_cuisine)
        itineraryCard = findViewById(R.id.card_itinerary)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        destinationCard.setOnClickListener {
            val intent = Intent(this, DestinationActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
        }

        cuisineCard.setOnClickListener {
            val intent = Intent(this, CuisineActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
        }
    }

    private fun loadCountryData() {
        when (country.lowercase()) {
            "italy" -> {
                countryTitle.text = "이탈리아"
                binding.place.text = "포지타노, 카프리섬, 폼페이, 나폴리"
                binding.food.text = "마르게리타 피자, 라자냐, 티라미수, 프루티 디 마레"
                binding.trip.text = "첫째 날 - 도착과 첫 만남"
            }
            "france" -> {
                countryTitle.text = "프랑스"
                binding.place.text = "에펠탑, 루브르 박물관, 몽마르트르, 베르사유"
                binding.food.text = "크루아상, 에스카르고, 부야베스, 마카롱"
                binding.trip.text = "첫째 날 - 파리 시내 산책과 세느강 야경"
            }
            "japan" -> {
                countryTitle.text = "일본"
                binding.place.text = "도쿄 스카이트리, 아사쿠사, 교토 아라시야마, 오사카 도톤보리"
                binding.food.text = "스시, 라멘, 오코노미야키, 가이센동"
                binding.trip.text = "첫째 날 - 도쿄 도착 및 시부야 교차로"
            }
            "korea" -> {
                countryTitle.text = "대한민국"
                binding.place.text = "경복궁, 북촌한옥마을, 남산타워, 홍대"
                binding.food.text = "비빔밥, 불고기, 김치찌개, 떡볶이"
                binding.trip.text = "첫째 날 - 서울 도착과 전통시장 투어"
            }
            "usa" -> {
                countryTitle.text = "미국"
                binding.place.text = "뉴욕 타임스퀘어, 센트럴파크, 메트로폴리탄 미술관, 브루클린 브리지"
                binding.food.text = "버거, 핫도그, 스테이크, 클램 차우더"
                binding.trip.text = "첫째 날 - 뉴욕 도착 및 미드타운 산책"
            }
            "spain" -> {
                countryTitle.text = "스페인"
                binding.place.text = "사그라다 파밀리아, 구엘 공원, 고딕 지구, 몬주익"
                binding.food.text = "파에야, 타파스, 하몬, 츄로스"
                binding.trip.text = "첫째 날 - 바르셀로나 람블라스 산책"
            }
            "germany" -> {
                countryTitle.text = "독일"
                binding.place.text = "브란덴부르크 문, 박물관 섬, 이스트사이드 갤러리, 샤를로텐부르크"
                binding.food.text = "브라트부어스트, 슈니첼, 프레첼, 자우어크라우트"
                binding.trip.text = "첫째 날 - 베를린 미테 지구 투어"
            }
            "uk" -> {
                countryTitle.text = "영국"
                binding.place.text = "버킹엄궁, 타워 브리지, 대영박물관, 웨스트엔드"
                binding.food.text = "피시 앤 칩스, 선데이 로스트, 애프터눈 티, 풀 잉글리시"
                binding.trip.text = "첫째 날 - 런던 시티 하이라이트"
            }
            "thailand" -> {
                countryTitle.text = "태국"
                binding.place.text = "왕궁, 왓 아룬, 짜오프라야 강, 차뚜짝 시장"
                binding.food.text = "팟타이, 똠얌꿍, 솜탐, 망고 스티키라이스"
                binding.trip.text = "첫째 날 - 방콕 사원 & 야시장"
            }
            "australia" -> {
                countryTitle.text = "호주"
                binding.place.text = "시드니 오페라하우스, 하버 브리지, 본다이 비치, 더 록스"
                binding.food.text = "미트 파이, 람빙턴, 바비큐, 플랫 화이트"
                binding.trip.text = "첫째 날 - 서큘러 키 & 하버 워크"
            }
            "vietnam" -> {
                countryTitle.text = "베트남"
                binding.place.text = "하노이 호안끼엠 호수, 하롱베이, 올드 쿼터, 닌빈"
                binding.food.text = "퍼(Phở), 분짜, 반미, 분보남보"
                binding.trip.text = "첫째 날 - 하노이 구시가지 탐방"
            }
            "greece" -> {
                countryTitle.text = "그리스"
                binding.place.text = "아크로폴리스, 플라카, 산토리니 오이아, 피라"
                binding.food.text = "무사카, 수블라키, 그릭 샐러드, 바클라바"
                binding.trip.text = "첫째 날 - 아테네 파르테논 관람"
            }
            "turkey" -> {
                countryTitle.text = "튀르키예(터키)"
                binding.place.text = "아야소피아, 블루 모스크, 그랜드 바자르, 보스포루스"
                binding.food.text = "케밥, 로쿰, 바클라바, 메제"
                binding.trip.text = "첫째 날 - 술탄아흐메트 지구 산책"
            }
            "switzerland" -> {
                countryTitle.text = "스위스"
                binding.place.text = "인터라켄, 융프라우요흐, 루체른 카펠교, 취리히 호숫가"
                binding.food.text = "치즈 퐁듀, 라클렛, 로스티, 초콜릿"
                binding.trip.text = "첫째 날 - 루체른 호수 산책과 구시가지"
            }
            "portugal" -> {
                countryTitle.text = "포르투갈"
                binding.place.text = "리스본 벨렝 탑, 제로니모스 수도원, 알파마, 신트라"
                binding.food.text = "파스텔 데 나타, 바칼라우 요리, 카탈라나? 아니고 삐리삐리 치킨, 카르코이스(달팽이)"
                binding.trip.text = "첫째 날 - 트램 28 탑승 & 전망대"
            }
            "egypt" -> {
                countryTitle.text = "이집트"
                binding.place.text = "기자 피라미드, 스핑크스, 이집트 박물관, 칸 칼릴리 시장"
                binding.food.text = "코샤리, 풀 메담스, 샤와르마, 바클라바"
                binding.trip.text = "첫째 날 - 카이로 이집트 박물관 관람"
            }
            else -> {
                countryTitle.text = "알 수 없는 국가"
                binding.place.text = "—"
                binding.food.text = "—"
                binding.trip.text = "—"
            }
        }
    }
}