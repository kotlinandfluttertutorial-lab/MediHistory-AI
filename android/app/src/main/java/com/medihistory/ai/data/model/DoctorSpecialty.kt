package com.medihistory.ai.data.model

/**
 * Medical specialty for a [Doctor] record.
 *
 * [OTHER] must be accompanied by a non-blank [Doctor.specialtyOther] value.
 * Persisted as its [name] string via [com.medihistory.ai.data.database.Converters].
 */
enum class DoctorSpecialty {
    ORTHOPEDICS,
    CARDIOLOGY,
    GENERAL_MEDICINE,
    NEUROLOGY,
    DERMATOLOGY,
    PSYCHIATRY,
    OTHER,
}
