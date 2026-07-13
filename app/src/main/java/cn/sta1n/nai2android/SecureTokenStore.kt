package cn.sta1n.nai2android

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    init {
        ensureKey()
    }

    fun save(token: String) {
        if (token.isBlank()) {
            preferences.edit().remove(TOKEN_KEY).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val ciphertext = Base64.encodeToString(
            cipher.doFinal(token.toByteArray(StandardCharsets.UTF_8)),
            Base64.NO_WRAP
        )
        preferences.edit().putString(TOKEN_KEY, "$iv:$ciphertext").apply()
    }

    fun read(): String {
        val stored = preferences.getString(TOKEN_KEY, null) ?: return ""
        val parts = stored.split(':', limit = 2)
        if (parts.size != 2) return ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(GCM_TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8)
        }.getOrDefault("")
    }

    private fun ensureKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) return
        val generator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE)
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .build())
        generator.generateKey()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private companion object {
        const val PREFERENCES = "nai2android_secure_settings"
        const val TOKEN_KEY = "encrypted_access_token"
        const val KEY_ALIAS = "nai2android_access_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    var baseUrl: String
        get() = preferences.getString(BASE_URL_KEY, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) {
            preferences.edit().putString(BASE_URL_KEY, value.trim().trimEnd('/')).apply()
        }

    private companion object {
        const val PREFERENCES = "nai2android_app_settings"
        const val BASE_URL_KEY = "base_url"
    }
}


