package com.medihistory.ai.data.database

import androidx.room.TypeConverter
import com.medihistory.ai.data.model.DoctorSpecialty
import java.time.LocalDate

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
