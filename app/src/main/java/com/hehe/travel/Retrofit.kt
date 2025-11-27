package com.hehe.travel

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object Retrofit {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(logging)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://us-central1-travel-5e270.cloudfunctions.net/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

}
