package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.utils.DateTimeUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class GetNearestAvailableDateUseCase(
    private val observeOccupiedTimesUseCase: ObserveOccupiedTimesUseCase
) {

    suspend operator fun invoke(
        doctorId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        slotStartHour: Int,
        slotEndHour: Int,
        slotDurationMinutes: Int
    ): LocalDate? {
        val referenceNow = DateTimeUtils.currentLocalDateTime()
        val normalizedStartDate = if (startDate.isBefore(referenceNow.toLocalDate())) {
            referenceNow.toLocalDate()
        } else {
            startDate
        }
        if (endDate.isBefore(normalizedStartDate)) return null

        val slotLabels = buildSlotLabels(
            slotStartHour = slotStartHour,
            slotEndHour = slotEndHour,
            slotDurationMinutes = slotDurationMinutes
        )
        if (slotLabels.isEmpty()) return null

        val totalDays = ChronoUnit.DAYS.between(normalizedStartDate, endDate).toInt() + 1
        for (offset in 0 until totalDays) {
            val date = normalizedStartDate.plusDays(offset.toLong())
            val dateString = date.format(DATE_STRING_FORMATTER)
            val occupiedSlots = observeOccupiedTimesUseCase(doctorId, dateString).first()
            val futureSlots = DateTimeUtils.filterFutureSlotLabels(
                date = date,
                slotLabels = slotLabels,
                referenceDateTime = referenceNow
            )
            val hasAtLeastOneOpenSlot = futureSlots.any { slotLabel -> slotLabel !in occupiedSlots }
            if (hasAtLeastOneOpenSlot) {
                return date
            }
        }

        return null
    }

    private fun buildSlotLabels(
        slotStartHour: Int,
        slotEndHour: Int,
        slotDurationMinutes: Int
    ): List<String> {
        val normalizedStartHour = slotStartHour.coerceIn(0, 23)
        val normalizedEndHour = slotEndHour.coerceIn(normalizedStartHour + 1, 24)
        val normalizedDuration = slotDurationMinutes.coerceAtLeast(5)
        val labels = mutableListOf<String>()

        for (hour in normalizedStartHour until normalizedEndHour) {
            var minute = 0
            while (minute < 60) {
                labels.add(String.format(Locale.ROOT, "%02d:%02d", hour, minute))
                minute += normalizedDuration
            }
        }

        return labels
    }

    private companion object {
        val DATE_STRING_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
    }
}
