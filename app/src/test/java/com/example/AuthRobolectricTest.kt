package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.SecureStorage
import com.example.domain.model.Faction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthRobolectricTest {

    @Test
    fun `test secure storage token encryption and retrieval`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val secureStorage = SecureStorage(context)

        secureStorage.accessToken = "test_jwt_access_token_12345"
        secureStorage.refreshToken = "test_jwt_refresh_token_67890"

        assertEquals("test_jwt_access_token_12345", secureStorage.accessToken)
        assertEquals("test_jwt_refresh_token_67890", secureStorage.refreshToken)

        secureStorage.clearAll()
        assertNull(secureStorage.accessToken)
        assertNull(secureStorage.refreshToken)
    }

    @Test
    fun `test faction enum mapping`() {
        assertEquals(Faction.APEX, Faction.fromId("APEX"))
        assertEquals(Faction.CIPHER, Faction.fromId("CIPHER"))
        assertEquals(Faction.SOLARIS, Faction.fromId("SOLARIS"))
        assertEquals(Faction.CIPHER, Faction.fromId("UNKNOWN"))
    }
}
