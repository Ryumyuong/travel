package com.hehe.travel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ItineraryActivity : AppCompatActivity() {

    private lateinit var country: String
    private lateinit var titleText: TextView
    private lateinit var itineraryRecyclerView: RecyclerView
    private lateinit var itineraryAdapter: ItineraryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        country = intent.getStringExtra("country").toString()

        initViews()
        getItineraryByCountry(country)
    }

    private fun initViews() {
        titleText = findViewById(R.id.tv_itinerary_title)
        itineraryRecyclerView = findViewById(R.id.rv_itinerary)

        titleText.text = "2박 3일 럭셔리 시간대별 일정"

        itineraryAdapter = ItineraryAdapter()
        itineraryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ItineraryActivity)
            adapter = itineraryAdapter
        }
    }

    private fun key(country: String) = country.lowercase().trim()

    private fun getItineraryByCountry(country: String): List<ItineraryDay> = when (key(country)) {
        "italy" -> listOf(
            ItineraryDay(1, "첫째 날 - 아말피 해안", listOf(
                Activity("09:00","요트 크루즈","포지타노 출발 럭셔리 크루즈","포지타노 항구"),
                Activity("12:00","해산물 점심","미슐랭 가이드 추천","Ristorante Max"),
                Activity("15:00","포지타노 산책","해안 마을 탐방","포지타노 센터")
            )),
            ItineraryDay(2, "둘째 날 - 폼페이", listOf(
                Activity("10:00","폼페이 가이드 투어","고대 로마 유적","폼페이")
            )),
            ItineraryDay(3, "셋째 날 - 카프리섬", listOf(
                Activity("09:00","카프리 당일치기","청의 동굴/쇼핑","카프리섬")
            ))
        )
        "france" -> listOf(
            ItineraryDay(1,"파리 시내", listOf(
                Activity("09:00","에펠탑","전망대 감상","에펠탑"),
                Activity("15:00","루브르","명작 감상","루브르")
            )),
            ItineraryDay(2,"예술 산책", listOf(
                Activity("10:00","몽마르트르","사크레쾨르 대성당","몽마르트르")
            )),
            ItineraryDay(3,"베르사유", listOf(
                Activity("09:00","궁전/정원 투어","화려한 정원","베르사유")
            ))
        )
        "japan" -> listOf(
            ItineraryDay(1,"도쿄", listOf(
                Activity("09:00","스카이트리","도쿄 전경","스카이트리"),
                Activity("12:00","스시 오마카세","긴자 식사","긴자")
            )),
            ItineraryDay(2,"교토", listOf(
                Activity("10:00","아라시야마 대나무숲","자연 산책","아라시야마")
            )),
            ItineraryDay(3,"오사카", listOf(
                Activity("09:00","도톤보리","길거리 음식","도톤보리")
            ))
        )
        "korea" -> listOf(
            ItineraryDay(1,"서울 고궁", listOf(
                Activity("09:00","경복궁","전통 궁궐","경복궁"),
                Activity("13:00","북촌","한옥 골목","북촌")
            )),
            ItineraryDay(2,"서울 전망", listOf(
                Activity("10:00","남산타워","시내 전경","남산")
            )),
            ItineraryDay(3,"쇼핑", listOf(
                Activity("09:00","명동","쇼핑 거리","명동")
            ))
        )
        "usa" -> listOf( // ← UsaItinerary 포함해 모두 이 함수 하나로!
            ItineraryDay(1,"뉴욕 하이라이트", listOf(
                Activity("09:00","타임스퀘어","도심 산책","타임스퀘어"),
                Activity("12:00","센트럴파크","피크닉/산책","센트럴파크")
            )),
            ItineraryDay(2,"문화의 날", listOf(
                Activity("10:00","메트로폴리탄 미술관","명작 감상","MET"),
                Activity("19:00","브로드웨이","뮤지컬 관람","시어터 디스트릭트")
            )),
            ItineraryDay(3,"브루클린", listOf(
                Activity("09:00","브루클린 브리지","도보 건너기","브루클린 브리지")
            ))
        )
        "spain" -> listOf(
            ItineraryDay(1,"바르셀로나", listOf(
                Activity("10:00","사그라다 파밀리아","가우디 성당","사그라다"),
                Activity("14:00","구엘 공원","가우디 공원","구엘 공원")
            )),
            ItineraryDay(2,"고딕지구", listOf(
                Activity("10:00","바리 고틱","중세 골목","고딕 지구")
            )),
            ItineraryDay(3,"몬주익", listOf(
                Activity("11:00","전망대/요새","전망 감상","몬주익")
            ))
        )
        "germany" -> listOf(
            ItineraryDay(1,"베를린 중심", listOf(
                Activity("10:00","브란덴부르크 문","포토스팟","파리저 광장"),
                Activity("14:00","박물관 섬","유네스코 문화유산","슈프레 강")
            )),
            ItineraryDay(2,"장벽의 흔적", listOf(
                Activity("11:00","이스트사이드 갤러리","그래피티 벽","프리드리히샤인")
            )),
            ItineraryDay(3,"궁전", listOf(
                Activity("10:00","샤를로텐부르크","바로크 궁전","샤를로텐부르크")
            ))
        )
        "uk" -> listOf(
            ItineraryDay(1,"왕실과 역사", listOf(
                Activity("10:30","버킹엄궁","근위병 교대식","버킹엄궁"),
                Activity("14:00","대영박물관","세계 유물","대영박물관")
            )),
            ItineraryDay(2,"템즈 강변", listOf(
                Activity("11:00","타워 브리지","전망 통로","타워 브리지")
            )),
            ItineraryDay(3,"웨스트엔드", listOf(
                Activity("19:00","뮤지컬 관람","공연의 밤","웨스트엔드")
            ))
        )
        "thailand" -> listOf(
            ItineraryDay(1,"방콕 왕실", listOf(
                Activity("09:30","왕궁","태국 왕실 유산","그랜드 팰리스"),
                Activity("13:00","왓 아룬","새벽 사원","왓 아룬")
            )),
            ItineraryDay(2,"수상도시", listOf(
                Activity("11:00","짜오프라야 보트","강 유람","짜오프라야")
            )),
            ItineraryDay(3,"시장 탐험", listOf(
                Activity("10:00","차뚜짝 시장","주말 마켓","차뚜짝")
            ))
        )
        "australia" -> listOf(
            ItineraryDay(1,"시드니 랜드마크", listOf(
                Activity("10:00","오페라하우스","외관/투어","서큘러 키"),
                Activity("12:00","하버 브리지 워크","전망 보행","하버 브리지")
            )),
            ItineraryDay(2,"해변의 날", listOf(
                Activity("11:00","본다이 비치","서핑/산책","본다이")
            )),
            ItineraryDay(3,"더 록스", listOf(
                Activity("14:00","역사 지구 산책","카페/펍","더 록스")
            ))
        )
        "vietnam" -> listOf(
            ItineraryDay(1,"하노이", listOf(
                Activity("10:00","호안끼엠 호수","시내 산책","호수"),
                Activity("13:00","올드 쿼터","골목 투어","구시가지")
            )),
            ItineraryDay(2,"하롱베이", listOf(
                Activity("08:00","크루즈 당일","경관 감상","하롱베이")
            )),
            ItineraryDay(3,"닌빈", listOf(
                Activity("09:00","보트 투어","카르스트 절경","닌빈")
            ))
        )
        "greece" -> listOf(
            ItineraryDay(1,"아테네", listOf(
                Activity("10:00","아크로폴리스","파르테논","아크로폴리스"),
                Activity("14:00","플라카","전통 거리","플라카")
            )),
            ItineraryDay(2,"산토리니", listOf(
                Activity("17:00","오이아 석양","세계적인 일몰","오이아")
            )),
            ItineraryDay(3,"피라", listOf(
                Activity("11:00","중심 마을 산책","카페/쇼핑","피라")
            ))
        )
        "turkey", "튀르키예(터키)" -> listOf(
            ItineraryDay(1,"이스탄불 핵심", listOf(
                Activity("10:00","아야소피아","비잔틴/오스만 유산","술탄아흐메트"),
                Activity("13:00","블루 모스크","청색 타일 사원","술탄아흐메트")
            )),
            ItineraryDay(2,"시장과 해협", listOf(
                Activity("11:00","그랜드 바자르","전통 시장","바이아지트"),
                Activity("16:00","보스포루스 크루즈","유럽·아시아 경계","보스포루스")
            )),
            ItineraryDay(3,"카페 투어", listOf(
                Activity("11:00","갈라타 주변","현지 카페","카라쾨이")
            ))
        )
        "switzerland" -> listOf(
            ItineraryDay(1,"루체른", listOf(
                Activity("10:00","카펠교","호수 풍경","루체른"),
                Activity("14:00","호수 산책","구시가지","루체른 호숫가")
            )),
            ItineraryDay(2,"인터라켄", listOf(
                Activity("09:00","융프라우요흐","알프스 설경","융프라우요흐")
            )),
            ItineraryDay(3,"취리히", listOf(
                Activity("11:00","호수&구시가지","카페 산책","취리히")
            ))
        )
        "portugal", "포르투갈", "포르투칼" -> listOf(
            ItineraryDay(1,"리스본", listOf(
                Activity("10:00","벨렝 탑","요새/포토스팟","벨렝"),
                Activity("13:00","제로니모스 수도원","마누엘 양식","벨렝")
            )),
            ItineraryDay(2,"알파마", listOf(
                Activity("11:00","트램 28","전통 트램","알파마")
            )),
            ItineraryDay(3,"신트라", listOf(
                Activity("10:00","동화 같은 성","페냐 궁전","신트라")
            ))
        )
        "egypt" -> listOf(
            ItineraryDay(1,"기자", listOf(
                Activity("09:00","피라미드/스핑크스","고대 유산","기자")
            )),
            ItineraryDay(2,"카이로", listOf(
                Activity("11:00","이집트 박물관","투탕카멘 유물","타흐리르")
            )),
            ItineraryDay(3,"시장", listOf(
                Activity("14:00","칸 칼릴리","전통 시장","올드 카이로")
            ))
        )
        else -> emptyList()
    }

}