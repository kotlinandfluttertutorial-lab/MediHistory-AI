package com.medihistory.ai.data.session

import com.medihistory.ai.data.security.SecurePreferences.Companion.KEY_ACCESS_TOKEN
import com.medihistory.ai.data.security.SecurePreferences.Companion.KEY_PATIENT_ID
import com.medihistory.ai.data.security.SecurePreferencesStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the current patient ID for the logged-in session.
 *
 * Reads from [SecurePreferencesStore] — never logs the value.
 * The patient ID is stored after login and cleared on logout/deletion.
 *
 * Privacy: patient ID is never exposed in UI strings or log messages.
 */
@Singleton
open class SessionManager @Inject constructor(
    private val securePreferences: SecurePreferencesStore,
) {
    companion object {
        /**
         * Fallback used only in tests and during onboarding before a patient ID is set.
         * Must never appear in production data queries.
         */
        const val STUB_PATIENT_ID = "current-patient"
    }

    /**
     * Returns the stored patient ID, or [STUB_PATIENT_ID] if none is set.
     * A null/blank value means the user has not completed setup or logged in.
     */
    open fun getPatientId(): String =
        securePreferences.getString(KEY_PATIENT_ID)
            ?.takeIf { it.isNotBlank() }
            ?: STUB_PATIENT_ID

    fun setPatientId(id: String) {
        securePreferences.putString(KEY_PATIENT_ID, id)
    }

    fun isSessionActive(): Boolean =
        securePreferences.getString(KEY_ACCESS_TOKEN)?.isNotBlank() == true

    fun clearSession() {
        securePreferences.remove(KEY_ACCESS_TOKEN)
        securePreferences.remove(KEY_PATIENT_ID)
    }
}
