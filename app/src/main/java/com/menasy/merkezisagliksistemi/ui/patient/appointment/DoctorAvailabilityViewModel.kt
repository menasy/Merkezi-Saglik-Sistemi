package com.menasy.merkezisagliksistemi.ui.patient.appointment

import androidx.lifecycle.ViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SlotUiModel(
    val timeLabel: String,
    val isAvailable: Boolean
)

data class HourBlockUiModel(
    val hourLabel: String,
    val isEnabled: Boolean,
    val slots: List<SlotUiModel>
)

data class DayAvailabilityUiModel(
    val dateMillis: Long,
    val dayTitle: String,
    val daySubtitle: String,
    val hourBlocks: List<HourBlockUiModel>,
    val selectedHourIndex: Int? = null,
    val selectedSlotLabel: String? = null
)

data class DoctorAvailabilityUiState(
    val isLoading: Boolean = false,
    val doctorName: String = "",
    val hospitalName: String = "",
    val branchName: String = "",
    val dayAvailabilities: List<DayAvailabilityUiModel> = emptyList(),
    val selectedDateMillis: Long? = null,
    val selectedTimeLabel: String? = null,
    val selectedSummaryText: String? = null,
    val errorMessage: String? = null
)

class DoctorAvailabilityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorAvailabilityUiState())
    val uiState: StateFlow<DoctorAvailabilityUiState> = _uiState.asStateFlow()

    private var args: DoctorAvailabilityArgs? = null

    fun load(availabilityArgs: DoctorAvailabilityArgs) {
        if (args != null) return
        args = availabilityArgs

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            doctorName = availabilityArgs.doctorName,
            hospitalName = availabilityArgs.hospitalName,
            branchName = availabilityArgs.branchName,
            errorMessage = null
        )

        val generatedDays = generateDayAvailabilities(availabilityArgs)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            dayAvailabilities = generatedDays
        )
    }

    fun onHourSelected(dayIndex: Int, hourIndex: Int) {
        val currentState = _uiState.value
        val selectedDay = currentState.dayAvailabilities.getOrNull(dayIndex) ?: return
        val selectedHour = selectedDay.hourBlocks.getOrNull(hourIndex) ?: return
        if (!selectedHour.isEnabled) return

        val updatedDays = currentState.dayAvailabilities.mapIndexed { index, day ->
            if (index == dayIndex) {
                day.copy(selectedHourIndex = hourIndex, selectedSlotLabel = null)
            } else {
                day.copy(selectedHourIndex = null, selectedSlotLabel = null)
            }
        }

        _uiState.value = currentState.copy(
            dayAvailabilities = updatedDays,
            selectedDateMillis = null,
            selectedTimeLabel = null,
            selectedSummaryText = null,
            errorMessage = null
        )
    }

    fun onSlotSelected(dayIndex: Int, slotTimeLabel: String) {
        val currentState = _uiState.value
        val selectedDay = currentState.dayAvailabilities.getOrNull(dayIndex) ?: return
        val selectedHourIndex = selectedDay.selectedHourIndex ?: return
        val selectedHour = selectedDay.hourBlocks.getOrNull(selectedHourIndex) ?: return
        val selectedSlot = selectedHour.slots.find { slot -> slot.timeLabel == slotTimeLabel } ?: return
        if (!selectedSlot.isAvailable) return

        val updatedDays = currentState.dayAvailabilities.mapIndexed { index, day ->
            if (index == dayIndex) {
                day.copy(selectedSlotLabel = slotTimeLabel)
            } else {
                day.copy(selectedHourIndex = null, selectedSlotLabel = null)
            }
        }

        val summaryText = "${selectedDay.dayTitle} - $slotTimeLabel"

        _uiState.value = currentState.copy(
            dayAvailabilities = updatedDays,
            selectedDateMillis = selectedDay.dateMillis,
            selectedTimeLabel = slotTimeLabel,
            selectedSummaryText = summaryText,
            errorMessage = null
        )
    }

    fun buildConfirmationArgs(): Result<AppointmentConfirmationArgs> {
        val currentArgs = args ?: return Result.failure(Exception("Randevu bilgisi bulunamadi"))
        val currentState = _uiState.value
        val selectedDateMillis = currentState.selectedDateMillis
            ?: return Result.failure(Exception("Lutfen bir saat secin"))
        val selectedTimeLabel = currentState.selectedTimeLabel
            ?: return Result.failure(Exception("Lutfen bir saat secin"))

        return Result.success(
            AppointmentConfirmationArgs(
                doctorId = currentArgs.doctorId,
                doctorName = currentArgs.doctorName,
                hospitalId = currentArgs.hospitalId,
                hospitalName = currentArgs.hospitalName,
                branchId = currentArgs.branchId,
                branchName = currentArgs.branchName,
                dateMillis = selectedDateMillis,
                timeLabel = selectedTimeLabel
            )
        )
    }

    private fun generateDayAvailabilities(availabilityArgs: DoctorAvailabilityArgs): List<DayAvailabilityUiModel> {
        val startDate = millisToLocalDate(availabilityArgs.searchArgs.startDateMillis)
        val endDate = millisToLocalDate(availabilityArgs.searchArgs.endDateMillis)
        val dates = buildDateRange(startDate, endDate).take(MAX_DAY_COUNT)

        return dates.map { localDate ->
            val dayMillis = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val hourBlocks = buildHourBlocks(
                localDate = localDate,
                doctorId = availabilityArgs.doctorId,
                slotStartHour = availabilityArgs.slotStartHour,
                slotEndHour = availabilityArgs.slotEndHour,
                slotDurationMinutes = availabilityArgs.slotDurationMinutes
            )

            val openSlotCount = hourBlocks.sumOf { hour -> hour.slots.count { slot -> slot.isAvailable } }

            DayAvailabilityUiModel(
                dateMillis = dayMillis,
                dayTitle = DAY_TITLE_FORMATTER.format(localDate),
                daySubtitle = if (openSlotCount > 0) {
                    "$openSlotCount uygun seans"
                } else {
                    "Bu gun uygun seans yok"
                },
                hourBlocks = hourBlocks
            )
        }
    }

    private fun buildDateRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        if (endDate.isBefore(startDate)) return listOf(startDate)

        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        return (0 until totalDays).map { dayOffset ->
            startDate.plusDays(dayOffset.toLong())
        }
    }

    private fun buildHourBlocks(
        localDate: LocalDate,
        doctorId: String,
        slotStartHour: Int,
        slotEndHour: Int,
        slotDurationMinutes: Int
    ): List<HourBlockUiModel> {
        val normalizedStartHour = slotStartHour.coerceIn(0, 23)
        val normalizedEndHour = slotEndHour.coerceIn(normalizedStartHour + 1, 24)
        val normalizedSlotDuration = slotDurationMinutes.coerceAtLeast(5)

        return (normalizedStartHour until normalizedEndHour).map { hour ->
            val slots = mutableListOf<SlotUiModel>()
            var minute = 0
            while (minute < 60) {
                val timeLabel = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                val isAvailable = isSlotAvailable(
                    doctorId = doctorId,
                    date = localDate,
                    timeLabel = timeLabel
                )
                slots.add(
                    SlotUiModel(
                        timeLabel = timeLabel,
                        isAvailable = isAvailable
                    )
                )
                minute += normalizedSlotDuration
            }

            HourBlockUiModel(
                hourLabel = String.format(Locale.ROOT, "%02d:00", hour),
                isEnabled = slots.any { slot -> slot.isAvailable },
                slots = slots
            )
        }
    }

    private fun isSlotAvailable(
        doctorId: String,
        date: LocalDate,
        timeLabel: String
    ): Boolean {
        val hashInput = "$doctorId|$date|$timeLabel"
        val hashValue = abs(hashInput.hashCode())
        return hashValue % 4 != 0
    }

    private fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private companion object {
        const val MAX_DAY_COUNT = 5
        val DAY_TITLE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMMM EEEE", Locale.forLanguageTag("tr-TR"))
    }
}
