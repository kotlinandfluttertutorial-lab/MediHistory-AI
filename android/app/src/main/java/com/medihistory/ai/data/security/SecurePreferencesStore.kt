package com.medihistory.ai.data.security

/**
 * Interface for the encrypted preference store.
 *
 * Extracted so that production code depending on secure preferences can be tested
 * with an in-memory fake without requiring the AndroidKeyStore.
 *
 * The only production implementation is [SecurePreferences].
 */
interface SecurePreferencesStore {
    fun putString(
        key: String,
        value: String,
    )

    fun getString(key: String): String?

    fun remove(key: String)

    /** Clears all stored values. Called during account deletion. */
    fun clear()
}
