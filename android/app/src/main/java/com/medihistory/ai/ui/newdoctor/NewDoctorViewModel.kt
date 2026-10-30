package com.medihistory.ai.ui.newdoctor

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [NewDoctorModeScreen].
 *
 * Loads reviewed prescriptions and exposes record selection state.
 *
 * Medical safety invariants:
 * - Only [OcrStatus.REVIEWED] prescriptions are included in the record list.
 *   Unreviewed prescriptions are excluded so the patient is not sharing uncertain data.
 * - PDF generation is only triggered AFTER the patient explicitly confirms the
 *   consent dialog. [generatePdfAndShare] must never be called without prior consent.
 * - PDF contains ONLY selected records. Patient controls selection entirely.
 *
 * Privacy: never log patient ID, doctor names, medicine names, or any patient data.
 * The [onPdfReady] callback delivers a FileProvider URI — never a direct file path.
 */
@HiltViewModel
class NewDoctorViewModel @Inject constructor(
    private val prescriptionRepository: PrescriptionRepository,
    private val medicineRepository: MedicineRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewDoctorSelectionUiState())
    val uiState: StateFlow<NewDoctorSelectionUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    /**
     * Loads all reviewed prescriptions for [patientId] and builds the initial selection list.
     *
     * Only [OcrStatus.REVIEWED] prescriptions are loaded. Unreviewed records are excluded
     * so the patient is not inadvertently sharing unconfirmed OCR output.
     */
    fun loadRecords(patientId: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val prescriptions = prescriptionRepository.observeByPatient(patientId)
                    .first()
                    .filter { it.ocrStatus == OcrStatus.REVIEWED }

                val records = prescriptions.map { prescription ->
                    val medicineCount = medicineRepository
                        .observeByPatient(patientId)
                        .first()
                        .count { it.prescriptionId == prescription.id }

                    RecordSelection(
                        prescriptionId = prescription.id,
                        doctorName = prescription.doctorId ?: "Not recorded",
                        prescriptionDate = prescription.prescriptionDate
                            ?.format(dateFormatter)
                            ?: "Not recorded",
                        medicineCount = medicineCount,
                        isSelected = true,
                    )
                }

                _uiState.update {
                    it.copy(
                        records = records,
                        isLoading = false,
                        selectedCount = records.count { r -> r.isSelected },
                        error = null,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load doctor summary records")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load records. Please try again.",
                    )
                }
            }
        }
    }

    /**
     * Toggles the selection state of the record identified by [prescriptionId].
     *
     * The patient controls which records are included in the summary.
     */
    fun onToggleRecord(prescriptionId: String) {
        _uiState.update { state ->
            val updated = state.records.map { record ->
                if (record.prescriptionId == prescriptionId) {
                    record.copy(isSelected = !record.isSelected)
                } else {
                    record
                }
            }
            state.copy(
                records = updated,
                selectedCount = updated.count { it.isSelected },
            )
        }
    }

    /** Marks all records as selected. */
    fun onSelectAll() {
        _uiState.update { state ->
            val updated = state.records.map { it.copy(isSelected = true) }
            state.copy(records = updated, selectedCount = updated.size)
        }
    }

    /** Marks all records as not selected. */
    fun onDeselectAll() {
        _uiState.update { state ->
            val updated = state.records.map { it.copy(isSelected = false) }
            state.copy(records = updated, selectedCount = 0)
        }
    }

    /**
     * Generates the PDF from currently selected records and delivers a FileProvider URI via
     * [onPdfReady].
     *
     * CONSENT PRECONDITION: this function MUST only be called AFTER the patient has confirmed
     * the consent dialog in the UI. The UI is responsible for enforcing this gate.
     *
     * The PDF is written to [Context.getCacheDir] (app-private). A content URI is created via
     * [FileProvider] so the share sheet can access the file without exposing the file path.
     *
     * @param context     Used to access app-private cache and FileProvider.
     * @param patientId   Used only as the display name fallback for the summary header.
     * @param onPdfReady  Called with the FileProvider URI once the file is ready to share.
     */
    fun generatePdfAndShare(
        context: Context,
        patientId: String,
        onPdfReady: (Uri) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val selectedRecords = _uiState.value.records.filter { it.isSelected }
                val pdfFile = PdfGenerator.generate(
                    context = context,
                    selections = selectedRecords,
                    patientName = patientId,
                )

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile,
                )
                onPdfReady(uri)
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate summary PDF")
                _uiState.update {
                    it.copy(error = "Failed to generate summary. Please try again.")
                }
            }
        }
    }
}
