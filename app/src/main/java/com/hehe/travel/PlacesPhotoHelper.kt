package com.hehe.travel

import android.content.Context
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object PlacesPhotoHelper {

    private const val TAG = "PlacesPhotoHelper"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val PLACES_API_KEY = BuildConfig.PLACES_API_KEY

    // 캐시 (장소명 -> 사진 URL)
    private val photoCache = mutableMapOf<String, String?>()

    // 병렬 로딩을 위한 스레드풀 (동시에 5개 요청)
    private val executor = Executors.newFixedThreadPool(5)

    /**
     * 나라/도시 대표 이미지를 로드 (헤더 배경용)
     */
    fun loadCountryPhoto(country: String, imageView: ImageView, defaultResId: Int = R.drawable.top_picture) {
        val cacheKey = "country|$country"

        // 캐시 확인
        if (photoCache.containsKey(cacheKey)) {
            val cachedUrl = photoCache[cacheKey]
            if (cachedUrl != null) {
                Glide.with(imageView.context)
                    .load(cachedUrl)
                    .centerCrop()
                    .placeholder(defaultResId)
                    .error(defaultResId)
                    .into(imageView)
            }
            return
        }

        // "나라명 landmark"로 검색
        val query = "$country landmark"
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

        val url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json" +
                "?input=$encodedQuery" +
                "&inputtype=textquery" +
                "&fields=photos" +
                "&key=$PLACES_API_KEY"

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Country 사진 검색 실패: ${e.message}")
                photoCache[cacheKey] = null
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val json = JSONObject(responseBody)
                        val candidates = json.optJSONArray("candidates")

                        if (candidates != null && candidates.length() > 0) {
                            val photos = candidates.getJSONObject(0).optJSONArray("photos")
                            if (photos != null && photos.length() > 0) {
                                val photoRef = photos.getJSONObject(0).optString("photo_reference")
                                val photoUrl = getPhotoUrl(photoRef, 800)
                                photoCache[cacheKey] = photoUrl

                                imageView.post {
                                    Glide.with(imageView.context)
                                        .load(photoUrl)
                                        .centerCrop()
                                        .placeholder(defaultResId)
                                        .error(defaultResId)
                                        .into(imageView)
                                }
                                return
                            }
                        }
                    }
                    photoCache[cacheKey] = null
                } catch (e: Exception) {
                    Log.e(TAG, "Country 사진 파싱 실패: ${e.message}")
                    photoCache[cacheKey] = null
                }
            }
        })
    }

    /**
     * 장소명으로 사진을 검색하여 ImageView에 로드
     * @param placeName 장소명 (예: "The Seminyak Beach")
     * @param country 나라명 (검색 정확도 향상용)
     * @param imageView 이미지를 로드할 ImageView
     */
    fun loadPlacePhoto(placeName: String, country: String, imageView: ImageView) {
        val cacheKey = "$placeName|$country"

        // 캐시 확인
        if (photoCache.containsKey(cacheKey)) {
            val cachedUrl = photoCache[cacheKey]
            if (cachedUrl != null) {
                loadImageWithGlide(cachedUrl, imageView)
            }
            return
        }

        // Find Place API 호출
        findPlace(placeName, country) { photoReference ->
            if (photoReference != null) {
                val photoUrl = getPhotoUrl(photoReference)
                photoCache[cacheKey] = photoUrl

                imageView.post {
                    loadImageWithGlide(photoUrl, imageView)
                }
            } else {
                photoCache[cacheKey] = null
                Log.d(TAG, "사진 없음: $placeName")
            }
        }
    }

    // Find Place API로 place_id와 photo_reference 가져오기
    private fun findPlace(placeName: String, country: String, callback: (String?) -> Unit) {
        val query = "$placeName $country"
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

        val url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json" +
                "?input=$encodedQuery" +
                "&inputtype=textquery" +
                "&fields=photos" +
                "&key=$PLACES_API_KEY"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Find Place 실패: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val json = JSONObject(responseBody)
                        val candidates = json.optJSONArray("candidates")

                        if (candidates != null && candidates.length() > 0) {
                            val place = candidates.getJSONObject(0)
                            val photos = place.optJSONArray("photos")

                            if (photos != null && photos.length() > 0) {
                                val photoRef = photos.getJSONObject(0)
                                    .optString("photo_reference")
                                callback(photoRef)
                                return
                            }
                        }
                    }
                    callback(null)
                } catch (e: Exception) {
                    Log.e(TAG, "JSON 파싱 실패: ${e.message}")
                    callback(null)
                }
            }
        })
    }

    // Find Place API 동기 버전 (병렬 로딩용)
    private fun findPlaceSync(placeName: String, country: String): String? {
        val query = "$placeName $country"
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

        val url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json" +
                "?input=$encodedQuery" +
                "&inputtype=textquery" +
                "&fields=photos" +
                "&key=$PLACES_API_KEY"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")

                if (candidates != null && candidates.length() > 0) {
                    val place = candidates.getJSONObject(0)
                    val photos = place.optJSONArray("photos")

                    if (photos != null && photos.length() > 0) {
                        return photos.getJSONObject(0).optString("photo_reference")
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Find Place Sync 실패: ${e.message}")
            null
        }
    }

    // photo_reference로 사진 URL 생성
    private fun getPhotoUrl(photoReference: String, maxWidth: Int = 400): String {
        return "https://maps.googleapis.com/maps/api/place/photo" +
                "?maxwidth=$maxWidth" +
                "&photo_reference=$photoReference" +
                "&key=$PLACES_API_KEY"
    }

    // Glide로 이미지 로드
    private fun loadImageWithGlide(url: String, imageView: ImageView) {
        Glide.with(imageView.context)
            .load(url)
            .transform(CenterCrop(), RoundedCorners(30))
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .into(imageView)
    }

    // 캐시 초기화
    fun clearCache() {
        photoCache.clear()
    }

    /**
     * 병렬로 여러 장소의 사진을 미리 로드
     * @param context Context
     * @param places 장소명 리스트
     * @param country 나라명
     * @param onComplete 완료 콜백 (메인 스레드에서 호출)
     * @param onProgress 진행률 콜백 (현재/전체)
     */
    fun preloadPhotosParallel(
        context: Context,
        places: List<String>,
        country: String,
        onComplete: () -> Unit,
        onProgress: ((Int, Int) -> Unit)? = null
    ) {
        if (places.isEmpty()) {
            onComplete()
            return
        }

        val total = places.size
        val completed = AtomicInteger(0)
        val latch = CountDownLatch(total)

        places.forEach { placeName ->
            executor.submit {
                try {
                    val cacheKey = "$placeName|$country"

                    // 이미 캐시에 있으면 스킵
                    if (!photoCache.containsKey(cacheKey)) {
                        val photoReference = findPlaceSync(placeName, country)
                        if (photoReference != null) {
                            val photoUrl = getPhotoUrl(photoReference)
                            synchronized(photoCache) {
                                photoCache[cacheKey] = photoUrl
                            }

                            // Glide로 이미지 미리 다운로드
                            try {
                                Glide.with(context.applicationContext)
                                    .load(photoUrl)
                                    .preload()
                            } catch (e: Exception) {
                                Log.e(TAG, "Glide preload 실패: ${e.message}")
                            }
                        } else {
                            synchronized(photoCache) {
                                photoCache[cacheKey] = null
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Preload 실패 ($placeName): ${e.message}")
                } finally {
                    val current = completed.incrementAndGet()
                    onProgress?.let { callback ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            callback(current, total)
                        }
                    }
                    latch.countDown()
                }
            }
        }

        // 백그라운드 스레드에서 완료 대기
        Thread {
            latch.await(60, TimeUnit.SECONDS) // 최대 60초 대기
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onComplete()
            }
        }.start()
    }

    /**
     * 1일차 우선 로드 + 나머지 백그라운드 로드
     * @param context Context
     * @param allDays 모든 일차 데이터
     * @param country 나라명
     * @param onFirstDayComplete 1일차 로드 완료 콜백 (화면 표시용)
     * @param onAllComplete 전체 완료 콜백
     * @param onProgress 진행률 콜백
     */
    fun preloadWithPriority(
        context: Context,
        allDays: List<DayPlanData>,
        country: String,
        onFirstDayComplete: () -> Unit,
        onAllComplete: (() -> Unit)? = null,
        onProgress: ((Int, Int) -> Unit)? = null
    ) {
        if (allDays.isEmpty()) {
            onFirstDayComplete()
            onAllComplete?.invoke()
            return
        }

        // 1일차 장소들
        val firstDayPlaces = allDays.firstOrNull()?.places?.map { it.name } ?: emptyList()
        // 나머지 일차 장소들
        val restPlaces = allDays.drop(1).flatMap { day -> day.places.map { it.name } }

        val totalPlaces = firstDayPlaces.size + restPlaces.size
        val progressCounter = AtomicInteger(0)

        // 1일차 먼저 병렬 로드
        preloadPhotosParallel(
            context = context,
            places = firstDayPlaces,
            country = country,
            onComplete = {
                // 1일차 완료 → 화면 표시
                onFirstDayComplete()

                // 나머지 백그라운드 로드
                if (restPlaces.isNotEmpty()) {
                    preloadPhotosParallel(
                        context = context,
                        places = restPlaces,
                        country = country,
                        onComplete = {
                            onAllComplete?.invoke()
                        },
                        onProgress = { current, _ ->
                            val total = progressCounter.addAndGet(1)
                            onProgress?.invoke(firstDayPlaces.size + current, totalPlaces)
                        }
                    )
                } else {
                    onAllComplete?.invoke()
                }
            },
            onProgress = { current, _ ->
                onProgress?.invoke(current, totalPlaces)
            }
        )
    }

    /**
     * 모든 일차의 장소 사진을 미리 로드 (병렬)
     */
    fun preloadAllDays(
        context: Context,
        allDays: List<DayPlanData>,
        country: String,
        onComplete: () -> Unit,
        onProgress: ((Int, Int) -> Unit)? = null
    ) {
        // 1일차 우선 로드 방식 사용
        preloadWithPriority(
            context = context,
            allDays = allDays,
            country = country,
            onFirstDayComplete = onComplete, // 1일차 완료시 화면 표시
            onAllComplete = null, // 나머지는 백그라운드에서 조용히 완료
            onProgress = onProgress
        )
    }
}
