package com.medihistory.ai.data.database

import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.model.DoseRecord
import com.medihistory.ai.data.model.MatchType
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.ocr.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `converts nullable dates to and from epoch days`() {
        val date = LocalDate.of(2024, 2, 29)

        assertEquals(date.toEpochDay(), converters.localDateToEpochDay(date))
        assertEquals(date, converters.epochDayToLocalDate(date.toEpochDay()))
        assertNull(converters.localDateToEpochDay(null))
        assertNull(converters.epochDayToLocalDate(null))
    }

    @Test
    fun `round trips string lists with commas and backslashes`() {
        val values = listOf("plain", "comma,value", "slash\\value", "\\,")

        val encoded = converters.stringListToString(values)

        assertEquals(values, converters.stringToStringList(encoded))
        assertNull(converters.stringListToString(null))
        assertNull(converters.stringToStringList(null))
        assertNull(converters.stringToStringList(""))
    }

    @Test
    fun `round trips all persisted enum types`() {
        DoctorSpecialty.values().forEach {
            assertEquals(it, converters.toDoctorSpecialty(converters.fromDoctorSpecialty(it)))
        }
        MedicineStatus.values().forEach {
            assertEquals(it, converters.toMedicineStatus(converters.fromMedicineStatus(it)))
        }
        OcrStatus.values().forEach {
            assertEquals(it, converters.toOcrStatus(converters.fromOcrStatus(it)))
        }
        ReviewState.values().forEach {
            assertEquals(it, converters.toReviewState(converters.fromReviewState(it)))
        }
        MatchType.values().forEach {
            assertEquals(it, converters.toMatchType(converters.fromMatchType(it)))
        }
        DoseRecord.values().forEach {
            assertEquals(it, converters.toDoseRecord(converters.fromDoseRecord(it)))
        }
    }

    @Test
    fun `round trips local time as seconds after midnight`() {
        val time = LocalTime.of(23, 59, 58)

        assertEquals(86_398, converters.fromLocalTime(time))
        assertEquals(time, converters.toLocalTime(converters.fromLocalTime(time)))
    }
}
