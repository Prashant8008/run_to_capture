package com.example.core.network

import com.example.core.network.model.ApiResponseDto
import com.example.core.network.model.CustomizationOptionsDto
import com.example.core.network.model.CustomizationResponseDto
import com.example.core.network.model.CustomizationUpdateRequestDto
import com.example.core.network.model.GoogleAuthRequestDto
import com.example.core.network.model.HealthResponseDto
import com.example.core.network.model.LogoutRequestDto
import com.example.core.network.model.RefreshTokenRequestDto
import com.example.core.network.model.TokenPairDto
import com.example.core.network.model.UserDto
import com.example.core.network.model.UserLoginRequestDto
import com.example.core.network.model.UserRegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface Run2CaptureApiService {

    @GET("api/v1/health")
    suspend fun checkHealth(): Response<HealthResponseDto>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: UserRegisterRequestDto
    ): Response<ApiResponseDto<TokenPairDto>>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: UserLoginRequestDto
    ): Response<ApiResponseDto<TokenPairDto>>

    @POST("api/v1/auth/google")
    suspend fun authWithGoogle(
        @Body request: GoogleAuthRequestDto
    ): Response<ApiResponseDto<TokenPairDto>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequestDto
    ): Response<ApiResponseDto<TokenPairDto>>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String,
        @Body request: LogoutRequestDto
    ): Response<ApiResponseDto<Map<String, Boolean>>>

    @GET("api/v1/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String
    ): Response<ApiResponseDto<UserDto>>

    @GET("api/v1/customization/options")
    suspend fun getCustomizationOptions(): Response<ApiResponseDto<CustomizationOptionsDto>>

    @GET("api/v1/users/me/customization")
    suspend fun getCustomization(
        @Header("Authorization") authorization: String
    ): Response<ApiResponseDto<CustomizationResponseDto>>

    @PUT("api/v1/users/me/customization")
    suspend fun updateCustomization(
        @Header("Authorization") authorization: String,
        @Body request: CustomizationUpdateRequestDto
    ): Response<ApiResponseDto<CustomizationResponseDto>>
}
