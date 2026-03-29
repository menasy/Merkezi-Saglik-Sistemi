package com.menasy.merkezisagliksistemi.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Utility object for date and time operations related to appointments.
 */
object DateTimeUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Parses appointment date and time strings into LocalDateTime.
     * 
     * @param dateStr Date string in "yyyy-MM-dd" format
     * @param timeStr Time string in "HH:mm" format
     * @return LocalDateTime representing the appointment date and time
     */
    fun parseAppointmentDateTime(dateStr: String, timeStr: String): LocalDateTime {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            val time = LocalTime.parse(timeStr, timeFormatter)
            LocalDateTime.of(date, time)
        } catch (e: Exception) {
            LocalDateTime.MIN
        }
    }

    /**
     * Determines if an appointment date/time is in the future.
     * 
     * @param dateStr Date string in "yyyy-MM-dd" format
     * @param timeStr Time string in "HH:mm" format
     * @return true if the appointment is in the future, false otherwise
     */
    fun isAppointmentInFuture(dateStr: String, timeStr: String): Boolean {
        val appointmentDateTime = parseAppointmentDateTime(dateStr, timeStr)
        return appointmentDateTime.isAfter(LocalDateTime.now())
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
            date.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }
}
