package com.medihistory.ai.data.security

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.lang.reflect.Proxy

class SecurePreferencesTest {
    @Test
    fun `delegates preference reads writes removal and clearing`() {
        val preferences = newSecurePreferences()

        preferences.putString(SecurePreferences.KEY_PATIENT_ID, "patient-1")
        assertEquals("patient-1", preferences.getString(SecurePreferences.KEY_PATIENT_ID))

        preferences.putString(SecurePreferences.KEY_PATIENT_ID, "patient-2")
        assertEquals("patient-2", preferences.getString(SecurePreferences.KEY_PATIENT_ID))

        preferences.remove(SecurePreferences.KEY_PATIENT_ID)
        assertNull(preferences.getString(SecurePreferences.KEY_PATIENT_ID))

        preferences.putString(SecurePreferences.KEY_ACCESS_TOKEN, "token")
        preferences.clear()
        assertNull(preferences.getString(SecurePreferences.KEY_ACCESS_TOKEN))
        assertNull(preferences.getString("missing"))
    }

    @Test
    fun `security module exposes the secure preferences store`() {
        val preferences = newSecurePreferences()

        assertSame(preferences, SecurityModule.provideSecurePreferencesStore(preferences))
    }

    private fun newSecurePreferences(): SecurePreferences {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null)
        val preferences =
            unsafeClass.getMethod("allocateInstance", Class::class.java)
                .invoke(unsafe, SecurePreferences::class.java) as SecurePreferences
        val values = mutableMapOf<String, String?>()
        lateinit var editor: SharedPreferences.Editor
        editor =
            Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java),
            ) { _, method, args ->
                when (method.name) {
                    "putString" -> {
                        values[args!![0] as String] = args[1] as String?
                        editor
                    }
                    "remove" -> {
                        values.remove(args!![0] as String)
                        editor
                    }
                    "clear" -> {
                        values.clear()
                        editor
                    }
                    "apply" -> null
                    "commit" -> true
                    else -> throw UnsupportedOperationException("Unexpected editor call: ${method.name}")
                }
            } as SharedPreferences.Editor
        val sharedPreferences =
            Proxy.newProxyInstance(
                SharedPreferences::class.java.classLoader,
                arrayOf(SharedPreferences::class.java),
            ) { _, method, args ->
                when (method.name) {
                    "edit" -> editor
                    "getString" -> values[args!![0] as String] ?: args[1] as String?
                    else -> throw UnsupportedOperationException("Unexpected preference call: ${method.name}")
                }
            } as SharedPreferences

        SecurePreferences::class.java.getDeclaredField("prefs").apply { isAccessible = true }
            .set(preferences, sharedPreferences)
        return preferences
    }
}
