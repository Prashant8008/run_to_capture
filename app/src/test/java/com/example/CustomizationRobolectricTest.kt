package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.Run2CaptureApiService
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
import com.example.core.security.SecureStorage
import com.example.data.repository.CustomizationRepositoryImpl
import com.example.domain.model.Faction
import com.example.domain.model.FlagBackground
import com.example.domain.model.FlagBorder
import com.example.domain.model.FlagConfig
import com.example.domain.model.FlagEmblem
import com.example.domain.model.FlagPattern
import com.example.domain.model.MapContrastValidator
import com.example.domain.model.PlayerCustomization
import com.example.domain.model.StandardTerritoryColor
import com.example.feature.customization.components.FlagCanvas
import com.example.feature.customization.components.LiveFlagPreview
import com.example.feature.customization.components.LivePlayerPreview
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

class FakeRun2CaptureApiService : Run2CaptureApiService {
    override suspend fun checkHealth(): Response<HealthResponseDto> = throw NotImplementedError()
    override suspend fun register(request: UserRegisterRequestDto): Response<ApiResponseDto<TokenPairDto>> = throw NotImplementedError()
    override suspend fun login(request: UserLoginRequestDto): Response<ApiResponseDto<TokenPairDto>> = throw NotImplementedError()
    override suspend fun authWithGoogle(request: GoogleAuthRequestDto): Response<ApiResponseDto<TokenPairDto>> = throw NotImplementedError()
    override suspend fun refreshToken(request: RefreshTokenRequestDto): Response<ApiResponseDto<TokenPairDto>> = throw NotImplementedError()
    override suspend fun logout(authorization: String, request: LogoutRequestDto): Response<ApiResponseDto<Map<String, Boolean>>> = throw NotImplementedError()
    override suspend fun getCurrentUser(authorization: String): Response<ApiResponseDto<UserDto>> = throw NotImplementedError()
    override suspend fun getCustomizationOptions(): Response<ApiResponseDto<CustomizationOptionsDto>> = throw NotImplementedError()
    override suspend fun getCustomization(authorization: String): Response<ApiResponseDto<CustomizationResponseDto>> = throw NotImplementedError()
    override suspend fun updateCustomization(
        authorization: String,
        request: CustomizationUpdateRequestDto
    ): Response<ApiResponseDto<CustomizationResponseDto>> {
        return Response.success(
            ApiResponseDto(
                success = true,
                message = "Saved",
                data = CustomizationResponseDto(
                    territoryColor = request.territoryColor,
                    territoryColorHex = "#9D00FF",
                    isCustomColor = false,
                    flag = request.flag
                )
            )
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomizationRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test standard territory colors coverage`() {
        val expectedColors = listOf(
            "blue", "purple", "red", "orange", "cyan", "pink", "gold", "green", "indigo"
        )
        
        expectedColors.forEach { id ->
            val color = StandardTerritoryColor.fromId(id)
            assertNotNull("Standard territory color '$id' should exist", color)
            assertEquals(id, color!!.id)
        }

        // Case insensitivity check
        assertEquals(StandardTerritoryColor.CYAN, StandardTerritoryColor.fromId("CyAn"))
        assertEquals(StandardTerritoryColor.GOLD, StandardTerritoryColor.fromId("GOLD"))
        assertEquals(StandardTerritoryColor.RED, StandardTerritoryColor.fromId("red "))
    }

    @Test
    fun `test map contrast validation on bright colors`() {
        // High visibility bright colors should pass
        val cyanResult = MapContrastValidator.validate("#00F0FF")
        assertTrue(cyanResult.isValid)
        assertTrue(cyanResult.brightness >= 0.22f)

        val limeResult = MapContrastValidator.validate("#CCFF00")
        assertTrue(limeResult.isValid)

        val goldResult = MapContrastValidator.validate("#FFD700")
        assertTrue(goldResult.isValid)

        val redResult = MapContrastValidator.validate("#FF3B30")
        assertTrue(redResult.isValid)
    }

    @Test
    fun `test map contrast validation rejects low contrast dark colors`() {
        // Black / very dark colors that blend into the map should fail
        val blackResult = MapContrastValidator.validate("#000000")
        assertFalse(blackResult.isValid)
        assertTrue(blackResult.brightness < 0.22f)

        // Near-map background color #0E121A should fail
        val nearMapResult = MapContrastValidator.validate("#0E121A")
        assertFalse(nearMapResult.isValid)

        // Invalid formatting
        val invalidHex = MapContrastValidator.validate("XYZ123")
        assertFalse(invalidHex.isValid)
    }

    @Test
    fun `test structured flag configuration models`() {
        val flag = FlagConfig(
            background = "crimson",
            pattern = "diagonal",
            emblem = "wolf",
            border = "gold"
        )

        assertEquals(FlagBackground.CRIMSON, flag.backgroundEnum)
        assertEquals(FlagPattern.DIAGONAL, flag.patternEnum)
        assertEquals(FlagEmblem.WOLF, flag.emblemEnum)
        assertEquals(FlagBorder.GOLD, flag.borderEnum)
    }

    @Test
    fun `test local customization persistence in repository`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val secureStorage = SecureStorage(context)
        val moshi = Moshi.Builder().build()
        val fakeApi = FakeRun2CaptureApiService()
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val repository = CustomizationRepositoryImpl(
            apiService = fakeApi,
            secureStorage = secureStorage,
            moshi = moshi,
            ioDispatcher = testDispatcher
        )

        val newFlag = FlagConfig(
            background = "navy",
            pattern = "cross",
            emblem = "shield",
            border = "neon_cyan"
        )

        val result = repository.saveCustomization(
            territoryColor = "purple",
            flag = newFlag
        )

        testScheduler.advanceUntilIdle()

        assertTrue(result.isSuccess)
        val saved = repository.customizationState.value
        assertEquals("purple", saved.territoryColor)
        assertEquals("cross", saved.flag.pattern)
        assertEquals("shield", saved.flag.emblem)
        assertEquals("neon_cyan", saved.flag.border)
    }

    @Test
    fun `test live player preview renders username and flag`() {
        val testFlag = FlagConfig(
            background = "navy",
            pattern = "diagonal",
            emblem = "wolf",
            border = "gold"
        )

        composeTestRule.setContent {
            LivePlayerPreview(
                username = "SHADOW_RUNNER_99",
                faction = Faction.CIPHER,
                territoryColor = "cyan",
                flag = testFlag
            )
        }

        composeTestRule.onNodeWithTag("live_player_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preview_username").assertIsDisplayed()
        composeTestRule.onNodeWithText("SHADOW_RUNNER_99").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preview_flag_canvas").assertIsDisplayed()
    }

    @Test
    fun `test live flag preview renders flag canvas and components`() {
        val testFlag = FlagConfig(
            background = "crimson",
            pattern = "cross",
            emblem = "eagle",
            border = "double_gold"
        )

        composeTestRule.setContent {
            LiveFlagPreview(flag = testFlag)
        }

        composeTestRule.onNodeWithTag("live_flag_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("flag_canvas").assertIsDisplayed()
    }
}
