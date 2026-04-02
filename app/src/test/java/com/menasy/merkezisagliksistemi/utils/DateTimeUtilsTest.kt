package com.menasy.merkezisagliksistemi.utils

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeUtilsTest {

    @Test
    fun `isAppointmentInFuture should return false when appointment time equals now`() {
        val now = LocalDateTime.of(2026, 4, 2, 13, 15)

        val result = DateTimeUtils.isAppointmentInFuture(
            dateStr = "2026-04-02",
            timeStr = "13:15",
            referenceDateTime = now
        )

        assertFalse(result)
    }

    @Test
    fun `isAppointmentInFuture should return false when appointment time is before now`() {
        val now = LocalDateTime.of(2026, 4, 2, 13, 15)

        val result = DateTimeUtils.isAppointmentInFuture(
            dateStr = "2026-04-02",
            timeStr = "13:10",
            referenceDateTime = now
        )

        assertFalse(result)
    }

    @Test
    fun `isAppointmentInFuture should return true when appointment time is after now`() {
        val now = LocalDateTime.of(2026, 4, 2, 13, 15)

        val result = DateTimeUtils.isAppointmentInFuture(
            date = LocalDate.of(2026, 4, 2),
            timeStr = "13:20",
            referenceDateTime = now
        )

        assertTrue(result)
    }

    @Test
    fun `filterFutureSlotLabels should only keep slots strictly after now`() {
        val now = LocalDateTime.of(2026, 4, 2, 13, 15)

        val filtered = DateTimeUtils.filterFutureSlotLabels(
            date = LocalDate.of(2026, 4, 2),
            slotLabels = listOf("13:00", "13:10", "13:20", "13:30"),
            referenceDateTime = now
        )

        assertEquals(listOf("13:20", "13:30"), filtered)
    }
}
