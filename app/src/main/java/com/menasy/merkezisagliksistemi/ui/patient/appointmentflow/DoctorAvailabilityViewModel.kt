package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.domain.usecase.ObserveOccupiedTimesUseCase
import com.menasy.merkezisagliksistemi.ui.common.base.BaseViewModel
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import com.menasy.merkezisagliksistemi.utils.DateTimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

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
    val dateString: String,
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
    val selectedSummaryText: String? = null
)

class DoctorAvailabilityViewModel(
    private val observeOccupiedTimesUseCase: ObserveOccupiedTimesUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DoctorAvailabilityUiState())
    val uiState: StateFlow<DoctorAvailabilityUiState> = _uiState.asStateFlow()

    private var args: DoctorAvailabilityArgs? = null
    private var occupiedSlotsJobs: MutableMap<String, Job> = mutableMapOf()
    private var occupiedSlotsCache: MutableMap<String, Set<String>> = mutableMapOf()

    fun load(availabilityArgs: DoctorAvailabilityArgs) {
        if (args != null) return
        args = availabilityArgs

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            doctorName = availabilityArgs.doctorName,
            hospitalName = availabilityArgs.hospitalName,
            branchName = availabilityArgs.branchName
        )

        val generatedDays = generateDayAvailabilities(availabilityArgs)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            dayAvailabilities = generatedDays
        )

        startObservingOccupiedSlots(availabilityArgs, generatedDays)
    }

    private fun startObservingOccupiedSlots(
        availabilityArgs: DoctorAvailabilityArgs,
        days: List<DayAvailabilityUiModel>
    ) {
        days.forEach { day ->
            val dateString = day.dateString
            val job = viewModelScope.launch {
                observeOccupiedTimesUseCase(availabilityArgs.doctorId, dateString)
                    .catch { error ->
                        publishError(error, OperationType.FETCH_DATA)
                    }
                    .collect { occupiedSlots ->
                        occupiedSlotsCache[dateString] = occupiedSlots
                        updateDayAvailability(dateString, occupiedSlots)
                    }
            }
            occupiedSlotsJobs[dateString] = job
        }
    }

    private fun updateDayAvailability(dateString: String, occupiedSlots: Set<String>) {
        if (args == null) return
        val currentState = _uiState.value

        val updatedDays = currentState.dayAvailabilities.map { day ->
            if (day.dateString == dateString) {
                rebuildDayWithOccupiedSlots(day, occupiedSlots)
            } else {
                day
            }
        }

        val selectedDate = currentState.selectedDateMillis?.let(::millisToLocalDate)
        val selectedDateString = selectedDate?.format(DATE_STRING_FORMATTER)
        val selectedTimeLabel = currentState.selectedTimeLabel

        val isSelectedSlotStillAvailable = if (selectedDateString == dateString && selectedTimeLabel != null) {
            selectedDate?.let { date ->
                selectedTimeLabel !in occupiedSlots &&
                    isSlotBookable(date, selectedTimeLabel)
            } == true
        } else {
            true
        }

        if (!isSelectedSlotStillAvailable) {
            val isPastSlot = selectedDate?.let { date ->
                selectedTimeLabel != null && !isSlotBookable(date, selectedTimeLabel)
            } == true

            if (isPastSlot) {
                publishError(AppErrorReason.PAST_APPOINTMENT_TIME_NOT_ALLOWED)
            } else {
                publishWarning(
                    title = "Saat Doldu",
                    description = "Seçtiğiniz saat başka bir hasta tarafından alındı. Lütfen başka bir saat seçin."
                )
            }
            _uiState.value = currentState.copy(
                dayAvailabilities = updatedDays,
                selectedDateMillis = null,
                selectedTimeLabel = null,
                selectedSummaryText = null
            )
        } else {
            _uiState.value = currentState.copy(dayAvailabilities = updatedDays)
        }
    }

    private fun rebuildDayWithOccupiedSlots(
        day: DayAvailabilityUiModel,
        occupiedSlots: Set<String>
    ): DayAvailabilityUiModel {
        val dayDate = millisToLocalDate(day.dateMillis)
        val referenceNow = DateTimeUtils.currentLocalDateTime()
        val selectedHourLabel = day.selectedHourIndex?.let { index ->
            day.hourBlocks.getOrNull(index)?.hourLabel
        }

        val updatedHourBlocks = day.hourBlocks.mapNotNull { hourBlock ->
            val updatedSlots = hourBlock.slots.mapNotNull { slot ->
                val isFutureSlot = isSlotBookable(
                    date = dayDate,
                    timeLabel = slot.timeLabel,
                    referenceDateTime = referenceNow
                )
                if (!isFutureSlot) {
                    null
                } else {
                    slot.copy(isAvailable = slot.timeLabel !in occupiedSlots)
                }
            }

            if (updatedSlots.isEmpty()) {
                null
            } else {
                hourBlock.copy(
                    slots = updatedSlots,
                    isEnabled = updatedSlots.any { it.isAvailable }
                )
            }
        }

        val openSlotCount = updatedHourBlocks.sumOf { hour -> hour.slots.count { slot -> slot.isAvailable } }

        val updatedSelectedHourIndex = selectedHourLabel?.let { label ->
            val newIndex = updatedHourBlocks.indexOfFirst { hourBlock ->
                hourBlock.hourLabel == label
            }
            if (newIndex >= 0 && updatedHourBlocks[newIndex].isEnabled) newIndex else null
        }

        val updatedSelectedSlotLabel = if (updatedSelectedHourIndex != null && day.selectedSlotLabel != null) {
            val hourBlock = updatedHourBlocks.getOrNull(updatedSelectedHourIndex)
            val slot = hourBlock?.slots?.find { it.timeLabel == day.selectedSlotLabel }
            if (slot?.isAvailable == true) day.selectedSlotLabel else null
        } else {
            null
        }

        return day.copy(
            hourBlocks = updatedHourBlocks,
            daySubtitle = if (openSlotCount > 0) {
                "$openSlotCount uygun seans"
            } else {
                "Bu gün uygun seans yok"
            },
            selectedHourIndex = updatedSelectedHourIndex,
            selectedSlotLabel = updatedSelectedSlotLabel
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
            selectedSummaryText = null
        )
    }

    fun onSlotSelected(dayIndex: Int, slotTimeLabel: String) {
        val currentState = _uiState.value
        val selectedDay = currentState.dayAvailabilities.getOrNull(dayIndex) ?: return
        val selectedDate = millisToLocalDate(selectedDay.dateMillis)
        val selectedHourIndex = selectedDay.selectedHourIndex ?: return
        val selectedHour = selectedDay.hourBlocks.getOrNull(selectedHourIndex) ?: return
        val selectedSlot = selectedHour.slots.find { slot -> slot.timeLabel == slotTimeLabel } ?: return
        if (!selectedSlot.isAvailable) {
            refreshDayAvailability(selectedDay.dateString)
            return
        }
        if (!isSlotBookable(selectedDate, slotTimeLabel)) {
            publishError(AppErrorReason.PAST_APPOINTMENT_TIME_NOT_ALLOWED)
            refreshDayAvailability(selectedDay.dateString)
            return
        }

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
            selectedSummaryText = summaryText
        )
    }

    fun buildConfirmationArgs(): AppointmentConfirmationArgs? {
        val currentArgs = args ?: run {
            publishError(AppErrorReason.APPOINTMENT_INFO_MISSING)
            return null
        }
        val currentState = _uiState.value
        val selectedDateMillis = currentState.selectedDateMillis ?: run {
            publishError(AppErrorReason.SLOT_SELECTION_REQUIRED)
            return null
        }
        val selectedTimeLabel = currentState.selectedTimeLabel ?: run {
            publishError(AppErrorReason.SLOT_SELECTION_REQUIRED)
            return null
        }
        val selectedDate = millisToLocalDate(selectedDateMillis)
        val selectedDateString = selectedDate.format(DATE_STRING_FORMATTER)

        if (!isSlotBookable(selectedDate, selectedTimeLabel)) {
            publishError(AppErrorReason.PAST_APPOINTMENT_TIME_NOT_ALLOWED)
            refreshDayAvailability(selectedDateString)
            return null
        }

        val occupiedSlots = occupiedSlotsCache[selectedDateString].orEmpty()
        if (selectedTimeLabel in occupiedSlots) {
            publishError(AppErrorReason.SLOT_ALREADY_TAKEN)
            refreshDayAvailability(selectedDateString)
            return null
        }

        return AppointmentConfirmationArgs(
            doctorId = currentArgs.doctorId,
            doctorName = currentArgs.doctorName,
            hospitalId = currentArgs.hospitalId,
            hospitalName = currentArgs.hospitalName,
            branchId = currentArgs.branchId,
            branchName = currentArgs.branchName,
            dateMillis = selectedDateMillis,
            timeLabel = selectedTimeLabel
        )
    }

    private fun generateDayAvailabilities(availabilityArgs: DoctorAvailabilityArgs): List<DayAvailabilityUiModel> {
        val startDate = millisToLocalDate(availabilityArgs.searchArgs.startDateMillis)
        val endDate = millisToLocalDate(availabilityArgs.searchArgs.endDateMillis)
        val today = DateTimeUtils.currentLocalDate()
        val normalizedStartDate = if (startDate.isBefore(today)) today else startDate
        val normalizedEndDate = if (endDate.isBefore(normalizedStartDate)) normalizedStartDate else endDate
        val referenceNow = DateTimeUtils.currentLocalDateTime()
        val dates = buildDateRange(normalizedStartDate, normalizedEndDate).take(MAX_DAY_COUNT)

        return dates.map { localDate ->
            val dayMillis = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dateString = localDate.format(DATE_STRING_FORMATTER)
            val hourBlocks = buildHourBlocks(
                slotStartHour = availabilityArgs.slotStartHour,
                slotEndHour = availabilityArgs.slotEndHour,
                slotDurationMinutes = availabilityArgs.slotDurationMinutes,
                slotDate = localDate,
                referenceDateTime = referenceNow
            )

            val openSlotCount = hourBlocks.sumOf { hour -> hour.slots.count { slot -> slot.isAvailable } }

            DayAvailabilityUiModel(
                dateMillis = dayMillis,
                dateString = dateString,
                dayTitle = DAY_TITLE_FORMATTER.format(localDate),
                daySubtitle = if (openSlotCount > 0) {
                    "$openSlotCount uygun seans"
                } else {
                    "Bu gün uygun seans yok"
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
        slotStartHour: Int,
        slotEndHour: Int,
        slotDurationMinutes: Int,
        slotDate: LocalDate,
        referenceDateTime: LocalDateTime
    ): List<HourBlockUiModel> {
        val normalizedStartHour = slotStartHour.coerceIn(0, 23)
        val normalizedEndHour = slotEndHour.coerceIn(normalizedStartHour + 1, 24)
        val normalizedSlotDuration = slotDurationMinutes.coerceAtLeast(5)

        return (normalizedStartHour until normalizedEndHour).mapNotNull { hour ->
            val slots = mutableListOf<SlotUiModel>()
            var minute = 0
            while (minute < 60) {
                val timeLabel = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                val isFutureSlot = isSlotBookable(
                    date = slotDate,
                    timeLabel = timeLabel,
                    referenceDateTime = referenceDateTime
                )
                if (isFutureSlot) {
                    slots.add(
                        SlotUiModel(
                            timeLabel = timeLabel,
                            isAvailable = true
                        )
                    )
                }
                minute += normalizedSlotDuration
            }

            if (slots.isEmpty()) {
                null
            } else {
                HourBlockUiModel(
                    hourLabel = String.format(Locale.ROOT, "%02d:00", hour),
                    isEnabled = true,
                    slots = slots
                )
            }
        }
    }

    private fun refreshDayAvailability(dateString: String) {
        val occupiedSlots = occupiedSlotsCache[dateString].orEmpty()
        updateDayAvailability(dateString, occupiedSlots)
    }

    private fun isSlotBookable(
        date: LocalDate,
        timeLabel: String,
        referenceDateTime: LocalDateTime = DateTimeUtils.currentLocalDateTime()
    ): Boolean {
        return DateTimeUtils.isAppointmentInFuture(
            date = date,
            timeStr = timeLabel,
            referenceDateTime = referenceDateTime
        )
    }

    private fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    override fun onCleared() {
        super.onCleared()
        occupiedSlotsJobs.values.forEach { it.cancel() }
        occupiedSlotsJobs.clear()
        occupiedSlotsCache.clear()
    }

    private companion object {
        const val MAX_DAY_COUNT = 5
        val DAY_TITLE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMMM EEEE", Locale.forLanguageTag("tr-TR"))
        val DATE_STRING_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
    }
}
