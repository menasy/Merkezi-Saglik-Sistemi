package com.menasy.merkezisagliksistemi.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility object for date and time operations related to appointments.
 */
object DateTimeUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val systemZoneId: ZoneId
        get() = ZoneId.systemDefault()

    /**
     * Parses appointment date and time strings into LocalDateTime.
     * 
     * @param dateStr Date string in "yyyy-MM-dd" format
     * @param timeStr Time string in "HH:mm" format
     * @return LocalDateTime representing the appointment date and time
     */
    fun parseAppointmentDateTime(dateStr: String, timeStr: String): LocalDateTime? {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            val time = LocalTime.parse(timeStr, timeFormatter)
            LocalDateTime.of(date, time)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses appointment time label into LocalTime.
     */
    fun parseAppointmentTime(timeStr: String): LocalTime? {
        return try {
            LocalTime.parse(timeStr, timeFormatter)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Determines if an appointment date/time is in the future.
     * 
     * @param dateStr Date string in "yyyy-MM-dd" format
     * @param timeStr Time string in "HH:mm" format
     * @return true if the appointment is in the future, false otherwise
     */
    fun isAppointmentInFuture(
        dateStr: String,
        timeStr: String,
        referenceDateTime: LocalDateTime = currentLocalDateTime()
    ): Boolean {
        val appointmentDateTime = parseAppointmentDateTime(dateStr, timeStr)
        return appointmentDateTime?.isAfter(referenceDateTime) == true
    }

    /**
     * Determines if slot time is strictly after the reference time.
     */
    fun isAppointmentInFuture(
        date: LocalDate,
        timeStr: String,
        referenceDateTime: LocalDateTime = currentLocalDateTime()
    ): Boolean {
        val appointmentTime = parseAppointmentTime(timeStr) ?: return false
        val appointmentDateTime = LocalDateTime.of(date, appointmentTime)
        return appointmentDateTime.isAfter(referenceDateTime)
    }

    /**
     * Filters slot labels to keep only strictly future slots.
     */
    fun filterFutureSlotLabels(
        date: LocalDate,
        slotLabels: Collection<String>,
        referenceDateTime: LocalDateTime = currentLocalDateTime()
    ): List<String> {
        return slotLabels.filter { slotLabel ->
            isAppointmentInFuture(
                date = date,
                timeStr = slotLabel,
                referenceDateTime = referenceDateTime
            )
        }
    }

    /**
     * Converts date string to millis for UI display purposes.
     * 
     * @param dateStr Date string in "yyyy-MM-dd" format
     * @return Epoch millis for the date at start of day
     */
    fun dateStringToMillis(dateStr: String): Long {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            localDateToStartOfDayMillis(date)
        } catch (e: Exception) {
            0L
        }
    }

    fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis)
            .atZone(systemZoneId)
            .toLocalDate()
    }

    fun localDateToStartOfDayMillis(date: LocalDate): Long {
        return date.atStartOfDay(systemZoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun currentLocalDate(): LocalDate {
        return LocalDate.now(systemZoneId)
    }

    fun currentLocalDateTime(): LocalDateTime {
        return LocalDateTime.now(systemZoneId)
    }

    fun todayStartMillis(): Long {
        return localDateToStartOfDayMillis(currentLocalDate())
    }
}
