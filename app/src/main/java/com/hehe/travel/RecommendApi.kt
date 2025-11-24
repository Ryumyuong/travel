package com.hehe.travel

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface RecommendApi {
    @POST("recommendTrip")
    suspend fun getRecommendation(
        @Body req: RecommendRequest,
        @Header("Authorization") bearer: String
    ): Response<RecommendCreateResponse>

    @POST("recommendTrip")
    suspend fun getRecommendationRaw(
        @Body req: RecommendRequest,
        @Header("Authorization") auth: String
    ): Response<LlmResultResponse>

    @POST("recommendTrip")
    suspend fun createPlan(
        @Body req: RecommendRequest,
        @Header("Authorization") bearer: String
    ): Response<RecommendCreateResponse>

    @POST("recommendTrip")
    suspend fun getRecommendationLegacy(
        @Body req: RecommendRequest,
        @Header("Authorization") auth: String
    ): Response<RawResultResponse>
}