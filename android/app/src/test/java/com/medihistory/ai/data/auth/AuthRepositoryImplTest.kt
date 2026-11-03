package com.medihistory.ai.data.auth

import com.medihistory.ai.data.network.MediHistoryApiService
import com.medihistory.ai.data.network.dto.RegisterResponse
import com.medihistory.ai.data.network.dto.TokenResponse
import com.medihistory.ai.data.security.SecurePreferences
import com.medihistory.ai.data.security.SecurePreferencesStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for [AuthRepositoryImpl] — backend JWT auth implementation.
 *
 * Uses [FakeApiService] and [FakeSecurePreferences] — no real network or Keystore.
 * Synthetic credentials only.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private lateinit var fakeApi: FakeApiService
    private lateinit var fakePrefs: FakeSecurePreferences
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        fakeApi = FakeApiService()
        fakePrefs = FakeSecurePreferences()
        repository = AuthRepositoryImpl(fakeApi, fakePrefs)
    }

    @Test
    fun `login success stores access token`() = runTest {
        fakeApi.loginResponse = Response.success(
            TokenResponse(accessToken = "synthetic-token-abc", refreshToken = "refresh-xyz")
        )
        val result = repository.login("test@synthetic.medihistory", "SyntheticPass123!")
        assertTrue("login must succeed", result.isSuccess)
        assertNotNull(fakePrefs.getString(SecurePreferences.KEY_ACCESS_TOKEN))
        assertTrue(fakePrefs.getString(SecurePreferences.KEY_ACCESS_TOKEN)!!.isNotBlank())
    }

    @Test
    fun `login failure returns failure result`() = runTest {
        fakeApi.loginStatusCode = 401
        val result = repository.login("wrong@synthetic.medihistory", "WrongPass!")
        assertTrue("login must fail for 401", result.isFailure)
        assertFalse("token must not be stored on failure",
            fakePrefs.getString(SecurePreferences.KEY_ACCESS_TOKEN)?.isNotBlank() == true)
    }

    @Test
    fun `register success then login stores token`() = runTest {
        fakeApi.registerResponse = Response.success(
            RegisterResponse(id = "uuid-test-001", email = "new@synthetic.medihistory")
        )
        fakeApi.loginResponse = Response.success(
            TokenResponse(accessToken = "synthetic-reg-token", refreshToken = "refresh-reg")
        )
        val result = repository.register("new@synthetic.medihistory", "SyntheticNew123!")
        assertTrue("register+login must succeed", result.isSuccess)
        assertNotNull(fakePrefs.getString(SecurePreferences.KEY_ACCESS_TOKEN))
    }

    @Test
    fun `register 409 returns failure with message about existing account`() = runTest {
        fakeApi.registerStatusCode = 409
        val result = repository.register("existing@synthetic.medihistory", "SyntheticPass123!")
        assertTrue("register must fail for 409", result.isFailure)
        assertTrue("error message must mention existing account",
            result.exceptionOrNull()?.message?.contains("already exists", ignoreCase = true) == true)
    }

    @Test
    fun `logout clears access token`() = runTest {
        fakePrefs.putString(SecurePreferences.KEY_ACCESS_TOKEN, "some-token")
        repository.logout()
        assertFalse(repository.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns true when token present`() {
        fakePrefs.putString(SecurePreferences.KEY_ACCESS_TOKEN, "valid-token")
        assertTrue(repository.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns false when no token`() {
        assertFalse(repository.isLoggedIn())
    }

    @Test
    fun `network error returns user-friendly failure`() = runTest {
        fakeApi.throwNetworkError = true
        val result = repository.login("test@synthetic.medihistory", "SyntheticPass123!")
        assertTrue("network error must return failure", result.isFailure)
        assertTrue("error message must be user-friendly",
            result.exceptionOrNull()?.message?.contains("server", ignoreCase = true) == true ||
            result.exceptionOrNull()?.message?.contains("connection", ignoreCase = true) == true)
    }
}

// ---------------------------------------------------------------------------
// Fake implementations — no real network or Keystore
// ---------------------------------------------------------------------------

private class FakeApiService : MediHistoryApiService {

    var loginResponse: Response<TokenResponse>? = null
    var loginStatusCode: Int = 200
    var registerResponse: Response<RegisterResponse>? = null
    var registerStatusCode: Int = 201
    var throwNetworkError: Boolean = false

    override suspend fun getOcrFields(
        prescriptionId: String,
    ): Response<com.medihistory.ai.data.network.dto.OcrFieldsResponse> =
        Response.error(503, "{}".toResponseBody())

    override suspend fun register(
        request: com.medihistory.ai.data.network.dto.RegisterRequest
    ): Response<RegisterResponse> {
        if (throwNetworkError) throw java.io.IOException("Synthetic network error")
        return registerResponse
            ?: Response.error(registerStatusCode, "{}".toResponseBody())
    }

    override suspend fun login(
        request: com.medihistory.ai.data.network.dto.LoginRequest
    ): Response<TokenResponse> {
        if (throwNetworkError) throw java.io.IOException("Synthetic network error")
        return loginResponse
            ?: Response.error(loginStatusCode, "{}".toResponseBody())
    }

    override suspend fun processReview(
        prescriptionId: String,
        request: com.medihistory.ai.data.network.dto.ProcessReviewRequest,
    ): Response<com.medihistory.ai.data.network.dto.ProcessReviewResponse> =
        Response.error(501, "{}".toResponseBody())

    override suspend fun syncPrescriptions(request: com.medihistory.ai.data.network.dto.SyncPrescriptionsRequest): Response<com.medihistory.ai.data.network.dto.SyncResponse> = Response.error(501, "{}".toResponseBody())
    override suspend fun syncDoctors(request: com.medihistory.ai.data.network.dto.SyncDoctorsRequest): Response<com.medihistory.ai.data.network.dto.SyncResponse> = Response.error(501, "{}".toResponseBody())
    override suspend fun syncMedicines(request: com.medihistory.ai.data.network.dto.SyncMedicinesRequest): Response<com.medihistory.ai.data.network.dto.SyncResponse> = Response.error(501, "{}".toResponseBody())
}

private class FakeSecurePreferences : SecurePreferencesStore {
    private val store = mutableMapOf<String, String>()
    override fun getString(key: String): String? = store[key]
    override fun putString(key: String, value: String) { store[key] = value }
    override fun remove(key: String) { store.remove(key) }
    override fun clear() { store.clear() }
}

