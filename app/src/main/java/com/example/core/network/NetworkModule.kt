package com.example.core.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // Default development base URL for local testing or android emulator host
    var baseUrl: String = "http://10.0.2.2:8000/"

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun createApiService(customBaseUrl: String = baseUrl): Run2CaptureApiService {
        val effectiveUrl = if (customBaseUrl.endsWith("/")) customBaseUrl else "$customBaseUrl/"
        return Retrofit.Builder()
            .baseUrl(effectiveUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(Run2CaptureApiService::class.java)
    }

    val apiService: Run2CaptureApiService by lazy {
        createApiService(baseUrl)
    }
}
