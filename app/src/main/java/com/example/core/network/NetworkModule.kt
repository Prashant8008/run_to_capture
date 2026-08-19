package com.example.core.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // Default production HTTPS API endpoint
    var baseUrl: String = "https://api.run2capture.com/v1/"

    // Injected token provider for Authorization Bearer header
    var authTokenProvider: (() -> String?)? = null

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = authTokenProvider?.invoke()
        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")
            .header("User-Agent", "Run2Capture-Android/${BuildConfig.VERSION_NAME}")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
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

