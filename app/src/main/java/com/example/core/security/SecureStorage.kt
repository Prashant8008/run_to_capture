package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Secure token and credential storage using hardware-backed Android KeyStore + AES-256-GCM.
 * Includes graceful JVM/Robolectric test fallback when hardware KeyStore provider is absent.
 * Ensures tokens and credentials are never stored in plaintext.
 */
class SecureStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var keyStore: KeyStore? = null
    private var jvmFallbackKey: SecretKey? = null

    init {
        try {
            val ks = KeyStore.getInstance(ANDROID_KEY_STORE)
            ks.load(null)
            keyStore = ks
            ensureKeyExists()
        } catch (e: Throwable) {
            // AndroidKeyStore is not supported on pure JVM/Robolectric environments
            keyStore = null
            initJvmFallbackKey()
        }
    }

    private fun ensureKeyExists() {
        val ks = keyStore ?: return
        try {
            if (!ks.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE
                )
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Throwable) {
            keyStore = null
            initJvmFallbackKey()
        }
    }

    private fun initJvmFallbackKey() {
        val raw = "Run2CaptureDevKeyRun2CaptureDevKey32B!".toByteArray(Charsets.UTF_8).copyOf(32)
        jvmFallbackKey = SecretKeySpec(raw, "AES")
    }

    private fun getSecretKey(): SecretKey {
        val ks = keyStore
        return if (ks != null && ks.containsAlias(KEY_ALIAS)) {
            (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            jvmFallbackKey ?: synchronized(this) {
                jvmFallbackKey ?: SecretKeySpec("Run2CaptureDevKeyRun2CaptureDevKey32B!".toByteArray(Charsets.UTF_8).copyOf(32), "AES").also {
                    jvmFallbackKey = it
                }
            }
        }
    }

    @Synchronized
    fun encryptAndSave(key: String, plainText: String?) {
        if (plainText.isNullOrEmpty()) {
            prefs.edit().remove(key).remove("${key}_iv").apply()
            return
        }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

            prefs.edit()
                .putString(key, encryptedBase64)
                .putString("${key}_iv", ivBase64)
                .apply()
        } catch (e: Exception) {
            try {
                ensureKeyExists()
            } catch (_: Exception) {}
        }
    }

    @Synchronized
    fun getDecrypted(key: String): String? {
        val encryptedBase64 = prefs.getString(key, null) ?: return null
        val ivBase64 = prefs.getString("${key}_iv", null) ?: return null

        return try {
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // High level properties
    var accessToken: String?
        get() = getDecrypted(KEY_ACCESS_TOKEN)
        set(value) = encryptAndSave(KEY_ACCESS_TOKEN, value)

    var refreshToken: String?
        get() = getDecrypted(KEY_REFRESH_TOKEN)
        set(value) = encryptAndSave(KEY_REFRESH_TOKEN, value)

    var userJson: String?
        get() = getDecrypted(KEY_USER_JSON)
        set(value) = encryptAndSave(KEY_USER_JSON, value)

    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrEmpty()

    companion object {
        private const val PREFS_NAME = "run2capture_secure_vault"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "run2capture_master_sec_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        private const val KEY_ACCESS_TOKEN = "sec_access_token"
        private const val KEY_REFRESH_TOKEN = "sec_refresh_token"
        private const val KEY_USER_JSON = "sec_user_json"
    }
}
