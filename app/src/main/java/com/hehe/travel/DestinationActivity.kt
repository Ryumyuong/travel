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
import com.hehe.travel.databinding.ActivityDestinationBinding

class DestinationActivity : AppCompatActivity() {

    private lateinit var country: String
    private lateinit var titleText: TextView
    private lateinit var destinationRecyclerView: RecyclerView
    private lateinit var destinationAdapter: DestinationAdapter
    private lateinit var binding:ActivityDestinationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDestinationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        country = intent.getStringExtra("country") ?: "italy"

        initViews()
        loadDestinations()

        binding.next.setOnClickListener {
            val intent = Intent(this, CuisineActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
            finish()
        }

    }

    private fun initViews() {
        titleText = findViewById(R.id.tv_destination_title)
        destinationRecyclerView = findViewById(R.id.rv_destinations)

        titleText.text = "추천 관광지"

        destinationAdapter = DestinationAdapter()
        destinationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DestinationActivity)
            adapter = destinationAdapter
        }
    }

    private fun loadDestinations() {
        val destinations = when (country) {
            "italy" -> getItalyDestinations()
            "france" -> getFranceDestinations()
            "japan" -> getJapanDestinations()
            "korea" -> getKoreaDestinations()
            "usa" -> getUsaDestinations()
            "spain" -> getSpainDestinations()
            "germany" -> getGermanyDestinations()
            "uk" -> getUkDestinations()
            "thailand" -> getThailandDestinations()
            "australia" -> getAustraliaDestinations()
            "vietnam" -> getVietnamDestinations()
            "greece" -> getGreeceDestinations()
            "turkey" -> getTurkeyDestinations()
            "switzerland" -> getSwitzerlandDestinations()
            "portugal" -> getPortugalDestinations()
            "egypt" -> getEgyptDestinations()
            else -> emptyList()
        }
        destinationAdapter.submitList(destinations)
    }

    private fun getItalyDestinations(): List<Destination> {
        return listOf(
            Destination(
                name = "포지타노 (Positano)",
                description = "아말피 해안의 아름다운 마을",
                category = "관광지"
            ),
            Destination(
                name = "폼페이 (Pompeii)",
                description = "고대 로마 유적지",
                category = "관광지"
            ),
            Destination(
                name = "카프리섬 (Capri)",
                description = "아름다운 경치와 고급 부티크로 유명한 섬",
                category = "관광지"
            ),
            Destination(
                name = "나폴리 (Naples)",
                description = "활기찬 도시와 맛있는 음식으로 유명",
                category = "관광지"
            )
        )
    }

    private fun getFranceDestinations(): List<Destination> {
        return listOf(
            Destination("에펠탑 (Eiffel Tower)", "파리의 상징적인 랜드마크", "관광지"),
            Destination("루브르 박물관 (Louvre Museum)", "세계 최대 규모의 미술관", "관광지"),
            Destination("몽마르트르 (Montmartre)", "예술가의 마을과 사크레쾨르 대성당", "관광지"),
            Destination("베르사유 궁전 (Palace of Versailles)", "화려한 궁전과 정원", "관광지")
        )
    }

    private fun getJapanDestinations(): List<Destination> {
        return listOf(
            Destination("도쿄 스카이트리 (Tokyo Skytree)", "세계에서 가장 높은 타워 중 하나", "관광지"),
            Destination("아사쿠사 (Asakusa)", "센소지 사원이 있는 전통 거리", "관광지"),
            Destination("교토 아라시야마 (Arashiyama, Kyoto)", "대나무 숲과 절경이 아름다운 지역", "관광지"),
            Destination("오사카 도톤보리 (Dotonbori, Osaka)", "네온사인과 길거리 음식으로 유명한 거리", "관광지")
        )
    }

    private fun getKoreaDestinations(): List<Destination> {
        return listOf(
            Destination("경복궁 (Gyeongbokgung Palace)", "조선 시대의 대표적인 궁궐", "관광지"),
            Destination("북촌한옥마을 (Bukchon Hanok Village)", "전통 한옥이 보존된 마을", "관광지"),
            Destination("남산타워 (Namsan Tower)", "서울 전경을 한눈에 볼 수 있는 전망대", "관광지"),
            Destination("홍대 (Hongdae)", "젊음과 예술, 음악의 거리", "관광지")
        )
    }

    private fun getUsaDestinations(): List<Destination> {
        return listOf(
            Destination("뉴욕 타임스퀘어 (Times Square)", "불야성을 이루는 미국의 중심 거리", "관광지"),
            Destination("센트럴파크 (Central Park)", "뉴욕의 대표적인 도시 공원", "관광지"),
            Destination("메트로폴리탄 미술관 (Metropolitan Museum of Art)", "세계 3대 박물관 중 하나", "관광지"),
            Destination("브루클린 브리지 (Brooklyn Bridge)", "뉴욕의 대표적인 다리", "관광지")
        )
    }

    private fun getSpainDestinations(): List<Destination> {
        return listOf(
            Destination("사그라다 파밀리아 (Sagrada Família)", "가우디의 미완성 대성당", "관광지"),
            Destination("구엘 공원 (Park Güell)", "가우디의 예술적 공원", "관광지"),
            Destination("고딕 지구 (Gothic Quarter)", "중세 시대의 거리와 건축물", "관광지"),
            Destination("몬주익 (Montjuïc)", "전망대와 역사적인 요새", "관광지")
        )
    }

    private fun getGermanyDestinations(): List<Destination> {
        return listOf(
            Destination("브란덴부르크 문 (Brandenburg Gate)", "베를린의 상징적인 문", "관광지"),
            Destination("박물관 섬 (Museum Island)", "유네스코 세계문화유산", "관광지"),
            Destination("이스트사이드 갤러리 (East Side Gallery)", "베를린 장벽 예술 거리", "관광지"),
            Destination("샤를로텐부르크 궁전 (Charlottenburg Palace)", "화려한 바로크 양식 궁전", "관광지")
        )
    }

    private fun getUkDestinations(): List<Destination> {
        return listOf(
            Destination("버킹엄궁 (Buckingham Palace)", "영국 왕실의 공식 거주지", "관광지"),
            Destination("타워 브리지 (Tower Bridge)", "런던의 대표적인 다리", "관광지"),
            Destination("대영박물관 (British Museum)", "세계적인 역사 박물관", "관광지"),
            Destination("웨스트엔드 (West End)", "뮤지컬과 공연의 중심지", "관광지")
        )
    }

    private fun getThailandDestinations(): List<Destination> {
        return listOf(
            Destination("왕궁 (Grand Palace)", "방콕의 대표적인 궁전", "관광지"),
            Destination("왓 아룬 (Wat Arun)", "새벽 사원으로 불리는 불교 사원", "관광지"),
            Destination("짜오프라야 강 (Chao Phraya River)", "방콕의 주요 수상 교통로", "관광지"),
            Destination("차뚜짝 시장 (Chatuchak Market)", "세계 최대 규모의 주말 시장", "관광지")
        )
    }

    private fun getAustraliaDestinations(): List<Destination> {
        return listOf(
            Destination("시드니 오페라하우스 (Sydney Opera House)", "세계적인 공연 예술의 상징", "관광지"),
            Destination("하버 브리지 (Harbour Bridge)", "시드니의 랜드마크 다리", "관광지"),
            Destination("본다이 비치 (Bondi Beach)", "호주의 대표적인 해변", "관광지"),
            Destination("더 록스 (The Rocks)", "시드니의 역사적인 거리", "관광지")
        )
    }

    private fun getVietnamDestinations(): List<Destination> {
        return listOf(
            Destination("하노이 호안끼엠 호수 (Hoàn Kiếm Lake)", "하노이의 중심 호수", "관광지"),
            Destination("하롱베이 (Hạ Long Bay)", "유네스코 세계자연유산", "관광지"),
            Destination("올드 쿼터 (Old Quarter)", "전통과 현대가 공존하는 거리", "관광지"),
            Destination("닌빈 (Ninh Bình)", "아름다운 자연경관과 절경", "관광지")
        )
    }

    private fun getGreeceDestinations(): List<Destination> {
        return listOf(
            Destination("아크로폴리스 (Acropolis)", "고대 그리스 문명의 상징", "관광지"),
            Destination("플라카 (Plaka)", "아테네의 전통 거리", "관광지"),
            Destination("산토리니 오이아 (Oia, Santorini)", "세계적으로 유명한 석양", "관광지"),
            Destination("피라 (Fira)", "산토리니의 중심 마을", "관광지")
        )
    }

    private fun getTurkeyDestinations(): List<Destination> {
        return listOf(
            Destination("아야소피아 (Hagia Sophia)", "동서양 문화가 융합된 건축물", "관광지"),
            Destination("블루 모스크 (Blue Mosque)", "이스탄불의 대표적인 사원", "관광지"),
            Destination("그랜드 바자르 (Grand Bazaar)", "세계에서 가장 큰 전통 시장 중 하나", "관광지"),
            Destination("보스포루스 해협 (Bosphorus Strait)", "유럽과 아시아를 가르는 해협", "관광지")
        )
    }

    private fun getSwitzerlandDestinations(): List<Destination> {
        return listOf(
            Destination("인터라켄 (Interlaken)", "호수와 알프스 산맥 사이에 위치한 휴양지", "관광지"),
            Destination("융프라우요흐 (Jungfraujoch)", "유럽의 정상이라 불리는 고산 지대", "관광지"),
            Destination("루체른 카펠교 (Chapel Bridge)", "유서 깊은 목조 다리", "관광지"),
            Destination("취리히 호숫가 (Lake Zurich)", "휴식과 산책을 즐기기 좋은 호숫가", "관광지")
        )
    }

    private fun getPortugalDestinations(): List<Destination> {
        return listOf(
            Destination("리스본 벨렝 탑 (Belém Tower)", "포르투갈의 대표적인 요새", "관광지"),
            Destination("제로니모스 수도원 (Jerónimos Monastery)", "마누엘 양식의 걸작", "관광지"),
            Destination("알파마 (Alfama)", "리스본의 오래된 거리", "관광지"),
            Destination("신트라 (Sintra)", "동화 속 같은 마을", "관광지")
        )
    }

    private fun getEgyptDestinations(): List<Destination> {
        return listOf(
            Destination("기자 피라미드 (Pyramids of Giza)", "고대 이집트의 대표 유적", "관광지"),
            Destination("스핑크스 (Sphinx)", "수수께끼의 석상", "관광지"),
            Destination("이집트 박물관 (Egyptian Museum)", "고대 유물의 보고", "관광지"),
            Destination("칸 칼릴리 시장 (Khan el-Khalili)", "전통 시장과 기념품 거리", "관광지")
        )
    }

}