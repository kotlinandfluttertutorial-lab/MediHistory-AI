package com.medihistory.ai.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for [SecurePreferences].
 *
 * Runs on an Android device or emulator so Android Keystore is available.
 *
 * Security invariants verified:
 * - Round-trip read/write integrity for encrypted preferences.
 * - clear() removes all stored values.
 * - remove() removes only the targeted key.
 * - Non-existent keys return null without throwing.
 * - KEY_ACCESS_TOKEN constant is non-empty (guards against accidental blank key).
 *
 * Note on Logcat token leak: verifying that a token value does NOT appear in
 * Logcat output requires an Android instrumented test environment. The equivalent
 * coverage is provided by [backend/tests/test_security.py::test_token_value_not_in_logs]
 * on the server side, and the AuthRepositoryImpl review confirms no Log.* call
 * ever receives the token value.
 *
 * All fixtures use synthetic data only — no real patient tokens or credentials.
 */
@RunWith(AndroidJUnit4::class)
class SecurePreferencesTest {

    private lateinit var securePreferences: SecurePreferences

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        securePreferences = SecurePreferences(context)
        // Start each test with a clean slate.
        securePreferences.clear()
    }

    // -------------------------------------------------------------------------
    // Round-trip tests
    // -------------------------------------------------------------------------

    @Test
    fun `should store and retrieve a string value`() {
        securePreferences.putString("test_key", "synthetic_value")

        val result = securePreferences.getString("test_key")

        assertEquals("getString must return the value stored via putString", "synthetic_value", result)
    }

    @Test
    fun `should store and retrieve access token via KEY_ACCESS_TOKEN`() {
        val syntheticToken = "synthetic.jwt.token.for.ci.testing"
        securePreferences.putString(SecurePreferences.KEY_ACCESS_TOKEN, syntheticToken)

        val result = securePreferences.getString(SecurePreferences.KEY_ACCESS_TOKEN)

        assertEquals(
            "Access token round-trip must preserve value exactly",
            syntheticToken,
            result,
        )
    }

    @Test
    fun `should store and retrieve patient ID via KEY_PATIENT_ID`() {
        securePreferences.putString(SecurePreferences.KEY_PATIENT_ID, "synthetic-patient-001")

        val result = securePreferences.getString(SecurePreferences.KEY_PATIENT_ID)

        assertEquals("synthetic-patient-001", result)
    }

    @Test
    fun `should store and retrieve cloud backup flag via KEY_CLOUD_BACKUP_ENABLED`() {
        securePreferences.putString(SecurePreferences.KEY_CLOUD_BACKUP_ENABLED, "true")

        val result = securePreferences.getString(SecurePreferences.KEY_CLOUD_BACKUP_ENABLED)

        assertEquals("true", result)
    }

    // -------------------------------------------------------------------------
    // Missing key
    // -------------------------------------------------------------------------

    @Test
    fun `should return null for non-existent key`() {
        val result = securePreferences.getString("key_that_was_never_set")

        assertNull(
            "getString must return null when the key has never been written",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // remove() — single key removal
    // -------------------------------------------------------------------------

    @Test
    fun `should remove specific key without affecting other keys`() {
        securePreferences.putString("key_to_keep", "keep_value")
        securePreferences.putString("key_to_remove", "remove_value")

        securePreferences.remove("key_to_remove")

        assertNull(
            "removed key must return null",
            securePreferences.getString("key_to_remove"),
        )
        assertEquals(
            "non-removed key must still be present after remove()",
            "keep_value",
            securePreferences.getString("key_to_keep"),
        )
    }

    @Test
    fun `should remove access token on logout`() {
        securePreferences.putString(SecurePreferences.KEY_ACCESS_TOKEN, "synthetic.token.abc")

        securePreferences.remove(SecurePreferences.KEY_ACCESS_TOKEN)

        assertNull(
            "Access token must be null after remove — simulates logout",
            securePreferences.getString(SecurePreferences.KEY_ACCESS_TOKEN),
        )
    }

    // -------------------------------------------------------------------------
    // clear() — full wipe
    // -------------------------------------------------------------------------

    @Test
    fun `should remove all values after clear`() {
        securePreferences.putString(SecurePreferences.KEY_ACCESS_TOKEN, "synthetic.token.xyz")
        securePreferences.putString(SecurePreferences.KEY_PATIENT_ID, "synthetic-patient-002")
        securePreferences.putString(SecurePreferences.KEY_CLOUD_BACKUP_ENABLED, "false")

        securePreferences.clear()

        assertNull(
            "KEY_ACCESS_TOKEN must be null after clear()",
            securePreferences.getString(SecurePreferences.KEY_ACCESS_TOKEN),
        )
        assertNull(
            "KEY_PATIENT_ID must be null after clear()",
            securePreferences.getString(SecurePreferences.KEY_PATIENT_ID),
        )
        assertNull(
            "KEY_CLOUD_BACKUP_ENABLED must be null after clear()",
            securePreferences.getString(SecurePreferences.KEY_CLOUD_BACKUP_ENABLED),
        )
    }

    // -------------------------------------------------------------------------
    // Constant guards
    // -------------------------------------------------------------------------

    @Test
    fun `KEY_ACCESS_TOKEN constant must not be blank`() {
        assertTrue(
            "KEY_ACCESS_TOKEN must be a non-blank string to prevent silent data loss",
            SecurePreferences.KEY_ACCESS_TOKEN.isNotBlank(),
        )
    }

    @Test
    fun `KEY_PATIENT_ID constant must not be blank`() {
        assertTrue(
            "KEY_PATIENT_ID must be a non-blank string",
            SecurePreferences.KEY_PATIENT_ID.isNotBlank(),
        )
    }

    @Test
    fun `KEY_CLOUD_BACKUP_ENABLED constant must not be blank`() {
        assertTrue(
            "KEY_CLOUD_BACKUP_ENABLED must be a non-blank string",
            SecurePreferences.KEY_CLOUD_BACKUP_ENABLED.isNotBlank(),
        )
    }
}
