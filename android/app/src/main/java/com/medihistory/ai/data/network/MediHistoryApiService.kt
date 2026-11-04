package com.medihistory.ai.data.network

import com.medihistory.ai.data.network.dto.LoginRequest
import com.medihistory.ai.data.network.dto.OcrFieldsResponse
import com.medihistory.ai.data.network.dto.ProcessReviewRequest
import com.medihistory.ai.data.network.dto.ProcessReviewResponse
import com.medihistory.ai.data.network.dto.RegisterRequest
import com.medihistory.ai.data.network.dto.RegisterResponse
import com.medihistory.ai.data.network.dto.SyncDoctorsRequest
import com.medihistory.ai.data.network.dto.SyncMedicinesRequest
import com.medihistory.ai.data.network.dto.SyncPrescriptionsRequest
import com.medihistory.ai.data.network.dto.SyncResponse
import com.medihistory.ai.data.network.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for the MediHistory AI FastAPI backend.
 *
 * Security:
 * - Bearer token is added by [AuthInterceptor] — never passed here directly.
 * - All HTTPS; no plain HTTP allowed in production.
 * - Every response is checked for HTTP errors before use (see ApiResult helpers).
 *
 * Privacy:
 * - Endpoint paths never contain patient identifiers.
 * - Request/response bodies are never logged (OkHttp logging is DEBUG only, disabled in release).
 */
interface MediHistoryApiService {

    // ── Auth (no token required) ──────────────────────────────────────────────

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    // ── Prescriptions (Bearer token required) ────────────────────────────────

    /**
     * Server-side OCR: extracts fields from the prescription document using Gemini Vision.
     * Returns 503 when Gemini is unavailable — caller falls back to on-device ML Kit.
     */
    @GET("api/v1/patients/me/prescriptions/{prescriptionId}/ocr/fields")
    suspend fun getOcrFields(
        @Path("prescriptionId") prescriptionId: String,
    ): Response<OcrFieldsResponse>

    /**
     * Server-side PostReviewProcessor: writes doctor, prescription date, medicines,
     * and runs duplicate detection. Returns counts of created records.
     */
    @POST("api/v1/patients/me/prescriptions/{prescriptionId}/process-review")
    suspend fun processReview(
        @Path("prescriptionId") prescriptionId: String,
        @Body request: ProcessReviewRequest,
    ): Response<ProcessReviewResponse>

    // ── Sync endpoints (Bearer token required) ───────────────────────────────

    @POST("api/v1/patients/me/sync/prescriptions")
    suspend fun syncPrescriptions(@Body request: SyncPrescriptionsRequest): Response<SyncResponse>

    @POST("api/v1/patients/me/sync/doctors")
    suspend fun syncDoctors(@Body request: SyncDoctorsRequest): Response<SyncResponse>

    @POST("api/v1/patients/me/sync/medicines")
    suspend fun syncMedicines(@Body request: SyncMedicinesRequest): Response<SyncResponse>
}
