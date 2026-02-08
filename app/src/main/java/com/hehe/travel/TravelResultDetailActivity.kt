package com.hehe.travel

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hehe.travel.databinding.ActivityTravelResultDetailBinding

class TravelResultDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTravelResultDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTravelResultDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 데이터 받기
        val country = intent.getStringExtra("country") ?: ""
        val nickname = intent.getStringExtra("nickname") ?: "여행자"
        val flight = intent.getStringExtra("flight") ?: ""
        val accommodation = intent.getStringExtra("accommodation") ?: ""
        val restaurant = intent.getStringExtra("restaurant") ?: ""
        val hasSemiPass = intent.getBooleanExtra("hasSemiPass", false)
        val budgetLabel = intent.getStringExtra("budgetLabel") ?: ""

        setupUI(country, nickname, flight, accommodation, restaurant, hasSemiPass, budgetLabel)

        // 헤더 배경 이미지 로드
        if (country.isNotEmpty()) {
            PlacesPhotoHelper.loadCountryPhoto(country, binding.ivHeaderBackground)
        }
    }

    private fun setupUI(
        country: String,
        nickname: String,
        flight: String,
        accommodation: String,
        restaurant: String,
        hasSemiPass: Boolean,
        budgetLabel: String
    ) {
        // 새미패스 뱃지
        if (hasSemiPass) {
            binding.tvBadge.visibility = View.VISIBLE
            binding.tvBadge.text = "새미패스"
        } else {
            binding.tvBadge.visibility = View.GONE
        }

        // 헤더 텍스트 (예산에 따라 다른 문구)
        val headerText = when {
            !hasSemiPass -> "${nickname}님의 $country 여행,\n가장 많이 찾은 순으로 안내드려요!"
            budgetLabel in listOf("매우 부족", "부족") -> "${nickname}님의 $country 여행,\n갓성비 있게 안내드려요"
            budgetLabel in listOf("여유로움", "매우 여유로움") -> "${nickname}님의 $country 여행,\n고급스럽게 안내드려요"
            else -> "${nickname}님의 $country 여행,\n균형 잡힌 일정으로 안내드려요"
        }
        binding.tvHeader.text = headerText

        // 섹션 타이틀 (예산에 따라 다른 문구)
        when {
            !hasSemiPass -> {
                binding.tvFlightTitle.text = "best 비행기 추천"
                binding.tvAccommodationTitle.text = "${country}의 best 숙소 추천"
                binding.tvRestaurantTitle.text = "best 맛집 추천"
            }
            budgetLabel in listOf("매우 부족", "부족") -> {
                binding.tvFlightTitle.text = "최저가 best 비행기"
                binding.tvAccommodationTitle.text = "${country}의 갓성비 best 숙소"
                binding.tvRestaurantTitle.text = "가격도 저렴하지만 맛까지 챙긴 실속 맛집"
            }
            budgetLabel in listOf("여유로움", "매우 여유로움") -> {
                binding.tvFlightTitle.text = "프리미엄 비행기 추천"
                binding.tvAccommodationTitle.text = "${country}의 럭셔리 best 숙소"
                binding.tvRestaurantTitle.text = "분위기와 맛 모두 잡은 프리미엄 맛집"
            }
            else -> {
                binding.tvFlightTitle.text = "가성비 좋은 비행기 추천"
                binding.tvAccommodationTitle.text = "${country}의 인기 숙소 추천"
                binding.tvRestaurantTitle.text = "현지인이 추천하는 맛집"
            }
        }

        // 내용 표시
        binding.tvFlightDesc.text = flight.ifEmpty { "정보 없음" }
        binding.tvAccommodationDesc.text = accommodation.ifEmpty { "정보 없음" }
        binding.tvRestaurantDesc.text = restaurant.ifEmpty { "정보 없음" }
    }
}
