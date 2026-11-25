package com.mursis.anycalc.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** API-ключи в EncryptedSharedPreferences, мастер-ключ — в Android Keystore. */
@Singleton
class SecureKeyStore @Inject constructor(@ApplicationContext context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "anycalc_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getKey(providerId: String): String = prefs.getString("key_$providerId", "").orEmpty()

    fun putKey(providerId: String, value: String) {
        prefs.edit().putString("key_$providerId", value.trim()).apply()
    }

    fun hasKey(providerId: String): Boolean = getKey(providerId).isNotBlank()
}
