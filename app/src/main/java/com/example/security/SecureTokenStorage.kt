package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureTokenStorage {
    private const val KEY_ALIAS = "SangamNexusPlatformKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS_NAME = "SangamNexusSecureStore"

    init {
        initKeystore()
    }

    private fun initKeystore() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(context: Context, key: String, value: String) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val base64Encrypted = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
            val base64Iv = Base64.encodeToString(iv, Base64.DEFAULT)

            sharedPref.edit()
                .putString("${key}_encrypted", base64Encrypted)
                .putString("${key}_iv", base64Iv)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun decrypt(context: Context, key: String): String? {
        try {
            val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val base64Encrypted = sharedPref.getString("${key}_encrypted", null) ?: return null
            val base64Iv = sharedPref.getString("${key}_iv", null) ?: return null

            val encryptedBytes = Base64.decode(base64Encrypted, Base64.DEFAULT)
            val iv = Base64.decode(base64Iv, Base64.DEFAULT)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            return null
        }
    }

    fun remove(context: Context, key: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit()
            .remove("${key}_encrypted")
            .remove("${key}_iv")
            .apply()
    }
}
