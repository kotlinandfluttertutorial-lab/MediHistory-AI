package com.medihistory.ai.data.database

import androidx.room.TypeConverter
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.model.DoseRecord
import com.medihistory.ai.data.model.MatchType
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.ocr.ReviewState
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    // --- LocalDate <-> Long (epoch day) ---

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? =
        value?.let { LocalDate.ofEpochDay(it) }

    // --- List<String> <-> String (comma-delimited, backslash-escaped) ---

    @TypeConverter
    fun stringListToString(list: List<String>?): String? =
        list?.joinToString(",") { it.escape() }

    @TypeConverter
    fun stringToStringList(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return null
        return value.splitEscaped(',')
    }

    // --- DoctorSpecialty <-> String ---

    @TypeConverter
    fun fromDoctorSpecialty(specialty: DoctorSpecialty): String = specialty.name

    @TypeConverter
    fun toDoctorSpecialty(value: String): DoctorSpecialty = DoctorSpecialty.valueOf(value)

    // --- MedicineStatus <-> String ---

    @TypeConverter
    fun fromMedicineStatus(status: MedicineStatus): String = status.name

    @TypeConverter
    fun toMedicineStatus(value: String): MedicineStatus = MedicineStatus.valueOf(value)

    // --- OcrStatus <-> String ---

    @TypeConverter
    fun fromOcrStatus(status: OcrStatus): String = status.name

    @TypeConverter
    fun toOcrStatus(value: String): OcrStatus = OcrStatus.valueOf(value)

    // --- ReviewState <-> String ---

    @TypeConverter
    fun fromReviewState(state: ReviewState): String = state.name

    @TypeConverter
    fun toReviewState(value: String): ReviewState = ReviewState.valueOf(value)

    // --- MatchType <-> String ---

    @TypeConverter
    fun fromMatchType(matchType: MatchType): String = matchType.name

    @TypeConverter
    fun toMatchType(value: String): MatchType = MatchType.valueOf(value)

    // --- LocalTime <-> Int (seconds of day) ---

    @TypeConverter
    fun fromLocalTime(t: LocalTime): Int = t.toSecondOfDay()

    @TypeConverter
    fun toLocalTime(s: Int): LocalTime = LocalTime.ofSecondOfDay(s.toLong())

    // --- DoseRecord <-> String ---

    @TypeConverter
    fun fromDoseRecord(record: DoseRecord): String = record.name

    @TypeConverter
    fun toDoseRecord(value: String): DoseRecord = DoseRecord.valueOf(value)

    // --- private helpers ---

    private fun String.escape(): String = replace("\\", "\\\\").replace(",", "\\,")

    private fun String.splitEscaped(delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < length) {
            val c = this[i]
            when {
                c == '\\' && i + 1 < length -> {
                    current.append(this[i + 1])
                    i += 2
                }
                c == delimiter -> {
                    result.add(current.toString())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(c)
                    i++
                }
            }
        }
        result.add(current.toString())
        return result
    }
}
