package com.medihistory.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Verifies that BuildConfig environment-config fields are present and have
 * correct defaults. Uses synthetic/build-time values only.
 */
class EnvironmentConfigTest {

    @Test
    fun `BACKEND_BASE_URL BuildConfig field is not null`() {
        // BuildConfig.BACKEND_BASE_URL is generated at compile time.
        // In a clean CI build with no local.properties and no env var it resolves to "".
        // An empty string is valid (network calls are disabled); null would be a build error.
        assertNotNull(BuildConfig.BACKEND_BASE_URL)
    }

    @Test
    fun `ENABLE_CLOUD_BACKUP defaults to false`() {
        assertFalse(
            "ENABLE_CLOUD_BACKUP must default to false — cloud backup is opt-in",
            BuildConfig.ENABLE_CLOUD_BACKUP
        )
    }

    @Test
    fun `ENABLE_INTERACTION_CHECK defaults to false`() {
        assertFalse(
            "ENABLE_INTERACTION_CHECK must default to false — interaction check is optional in MVP",
            BuildConfig.ENABLE_INTERACTION_CHECK
        )
    }
}
