package com.medihistory.ai.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Centralised wrapper for EncryptedSharedPreferences.
 *
 * All sensitive values (tokens, user IDs, settings) go through this class.
 * NEVER use plaintext SharedPreferences for sensitive data.
 *
 * Key generation uses Android Keystore via [MasterKey] with AES256_GCM scheme.
 * Preference keys are encrypted with AES256_SIV; values with AES256_GCM.
 *
 * Security invariants:
 * - Values stored here are NEVER written to any log at any level.
 * - The backing file is app-private; other apps cannot read it.
 */
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String): String? = prefs.getString(key, null)

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "medihistory_secure_prefs"

        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_PATIENT_ID = "patient_id"
        const val KEY_CLOUD_BACKUP_ENABLED = "cloud_backup_enabled"
    }
}
