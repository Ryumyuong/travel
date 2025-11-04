package com.hehe.travel

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RecommendApi {
    @POST("recommendTrip")
    suspend fun getRecommendation(@Body req: RecommendRequest): Response<RecommendResponse>
}