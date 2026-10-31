package com.medihistory.ai.data.session

import com.medihistory.ai.data.security.SecurePreferencesStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    private class InMemorySecurePreferencesStore : SecurePreferencesStore {
        private val values = mutableMapOf<String, String>()

        override fun putString(
            key: String,
            value: String,
        ) {
            values[key] = value
        }

        override fun getString(key: String): String? = values[key]

        override fun remove(key: String) {
            values.remove(key)
        }

        override fun clear() {
            values.clear()
        }
    }

    @Test
    fun `uses stub patient id when no patient id is stored`() {
        val manager = SessionManager(InMemorySecurePreferencesStore())

        assertEquals(SessionManager.STUB_PATIENT_ID, manager.getPatientId())
    }

    @Test
    fun `uses stub patient id when stored patient id is blank`() {
        val preferences = InMemorySecurePreferencesStore()
        val manager = SessionManager(preferences)
        manager.setPatientId("  ")

        assertEquals(SessionManager.STUB_PATIENT_ID, manager.getPatientId())
    }

    @Test
    fun `returns stored patient id and session activity follows nonblank token`() {
        val preferences = InMemorySecurePreferencesStore()
        val manager = SessionManager(preferences)

        manager.setPatientId("patient-42")
        assertEquals("patient-42", manager.getPatientId())
        assertFalse(manager.isSessionActive())

        preferences.putString(com.medihistory.ai.data.security.SecurePreferences.KEY_ACCESS_TOKEN, "token")
        assertTrue(manager.isSessionActive())
        preferences.putString(com.medihistory.ai.data.security.SecurePreferences.KEY_ACCESS_TOKEN, " ")
        assertFalse(manager.isSessionActive())
    }

    @Test
    fun `clear session removes access token and patient id`() {
        val preferences = InMemorySecurePreferencesStore()
        val manager = SessionManager(preferences)
        manager.setPatientId("patient-42")
        preferences.putString(com.medihistory.ai.data.security.SecurePreferences.KEY_ACCESS_TOKEN, "token")

        manager.clearSession()

        assertFalse(manager.isSessionActive())
        assertEquals(SessionManager.STUB_PATIENT_ID, manager.getPatientId())
    }

    @Test
    fun `session module creates manager backed by supplied preferences`() {
        val preferences = InMemorySecurePreferencesStore()

        val manager = SessionModule.provideSessionManager(preferences)

        manager.setPatientId("patient-module")
        assertEquals("patient-module", manager.getPatientId())
    }
}
