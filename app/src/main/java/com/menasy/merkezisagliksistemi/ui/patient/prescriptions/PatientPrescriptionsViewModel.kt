package com.menasy.merkezisagliksistemi.ui.patient.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetPrescriptionPreviewsByAppointmentIdsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.ObservePatientAppointmentsUseCase
import com.menasy.merkezisagliksistemi.ui.common.adapter.PrescriptionListItem
import com.menasy.merkezisagliksistemi.ui.patient.appointmentlist.AppointmentMapper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PatientPrescriptionFilter {
    TODAY,
    ALL,
    RANGE
}

data class PatientPrescriptionsUiState(
    val isLoading: Boolean = true,
    val prescriptions: List<PrescriptionListItem> = emptyList(),
    val selectedFilter: PatientPrescriptionFilter = PatientPrescriptionFilter.ALL,
    val activeFilterLabel: String = "Tüm Reçeteler",
    val emptyMessage: String = "Kayıtlı reçete bulunmuyor.",
    val errorMessage: String? = null
)

class PatientPrescriptionsViewModel(
    private val observePatientAppointmentsUseCase: ObservePatientAppointmentsUseCase,
    private val getPrescriptionPreviewsByAppointmentIdsUseCase: GetPrescriptionPreviewsByAppointmentIdsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val appointmentMapper: AppointmentMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientPrescriptionsUiState())
    val uiState: StateFlow<PatientPrescriptionsUiState> = _uiState.asStateFlow()

    private val prescriptionCache = mutableMapOf<String, Prescription?>()
    private var allAppointments: List<Appointment> = emptyList()
    private var rangeStartMillis: Long? = null
    private var rangeEndMillis: Long? = null

    init {
        observePatientAppointments()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun selectTodayFilter() {
        val today = LocalDate.now()
        val todayLabel = today.format(FILTER_LABEL_FORMATTER)
        rangeStartMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        rangeEndMillis = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        _uiState.update {
            it.copy(
                selectedFilter = PatientPrescriptionFilter.TODAY,
                activeFilterLabel = "Bugün • $todayLabel"
            )
        }
        renderPrescriptions(allAppointments)
    }

    fun selectAllFilter() {
        rangeStartMillis = null
        rangeEndMillis = null
        _uiState.update {
            it.copy(
                selectedFilter = PatientPrescriptionFilter.ALL,
                activeFilterLabel = "Tüm Reçeteler"
            )
        }
        renderPrescriptions(allAppointments)
    }

    fun selectRangeFilter(startMillis: Long, endMillis: Long) {
        rangeStartMillis = startMillis
        rangeEndMillis = endMillis

        val startDate = Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val label = if (startDate == endDate) {
            startDate.format(FILTER_LABEL_FORMATTER)
        } else {
            "${startDate.format(FILTER_LABEL_FORMATTER)} - ${endDate.format(FILTER_LABEL_FORMATTER)}"
        }

        _uiState.update {
            it.copy(
                selectedFilter = PatientPrescriptionFilter.RANGE,
                activeFilterLabel = label
            )
        }
        renderPrescriptions(allAppointments)
    }

    private fun observePatientAppointments() {
        val patientId = getCurrentUserUseCase.getCurrentUserId()
        if (patientId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Kullanıcı oturumu bulunamadı"
                )
            }
            return
        }

        viewModelScope.launch {
            observePatientAppointmentsUseCase(patientId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Reçete verileri yüklenemedi: ${throwable.message}"
                        )
                    }
                }
                .collectLatest { appointments ->
                    allAppointments = appointments
                    synchronizePrescriptionCache(appointments)
                    renderPrescriptions(appointments)
                }
        }
    }

    private suspend fun synchronizePrescriptionCache(appointments: List<Appointment>) {
        val appointmentIds = appointments
            .asSequence()
            .map { it.id }
            .filter { it.isNotBlank() }
            .toSet()

        prescriptionCache.keys.retainAll(appointmentIds)
        if (appointmentIds.isEmpty()) {
            return
        }

        val missingAppointmentIds = appointmentIds
            .filterNot { prescriptionCache.containsKey(it) }
            .toSet()

        if (missingAppointmentIds.isNotEmpty()) {
            getPrescriptionPreviewsByAppointmentIdsUseCase(missingAppointmentIds)
                .onSuccess { previews ->
                    previews.forEach { (appointmentId, preview) ->
                        prescriptionCache[appointmentId] = preview
                    }
                    missingAppointmentIds
                        .filterNot { previews.containsKey(it) }
                        .forEach { prescriptionCache[it] = null }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = "Reçete verileri alınamadı: ${throwable.message}")
                    }
                }
        }
    }

    private fun renderPrescriptions(appointments: List<Appointment>) {
        val prescriptions = prescriptionCache
            .mapNotNull { (appointmentId, preview) -> preview?.let { appointmentId to it } }
            .toMap()

        val filteredAppointments = filterAppointmentsByDate(appointments)

        val items = appointmentMapper.mapPatientPrescriptionItems(
            appointments = filteredAppointments,
            prescriptionByAppointmentId = prescriptions
        )

        val emptyMessage = when (_uiState.value.selectedFilter) {
            PatientPrescriptionFilter.TODAY -> "Bugün için reçete bulunmuyor."
            PatientPrescriptionFilter.ALL -> "Kayıtlı reçete bulunmuyor."
            PatientPrescriptionFilter.RANGE -> "Seçilen tarih aralığında reçete bulunmuyor."
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                prescriptions = items,
                emptyMessage = if (items.isEmpty()) emptyMessage else ""
            )
        }
    }

    private fun filterAppointmentsByDate(appointments: List<Appointment>): List<Appointment> {
        val startMillis = rangeStartMillis
        val endMillis = rangeEndMillis

        if (startMillis == null || endMillis == null) {
            return appointments
        }

        val startOfDayMillis = Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val endOfDayMillis = Instant.ofEpochMilli(endMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return appointments.filter { appointment ->
            val prescriptionCreatedAt = prescriptionCache[appointment.id]?.createdAtMillis ?: 0L
            prescriptionCreatedAt in startOfDayMillis until endOfDayMillis
        }
    }

    class Factory(
        private val observePatientAppointmentsUseCase: ObservePatientAppointmentsUseCase,
        private val getPrescriptionPreviewsByAppointmentIdsUseCase: GetPrescriptionPreviewsByAppointmentIdsUseCase,
        private val getCurrentUserUseCase: GetCurrentUserUseCase,
        private val appointmentMapper: AppointmentMapper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PatientPrescriptionsViewModel::class.java)) {
                return PatientPrescriptionsViewModel(
                    observePatientAppointmentsUseCase = observePatientAppointmentsUseCase,
                    getPrescriptionPreviewsByAppointmentIdsUseCase = getPrescriptionPreviewsByAppointmentIdsUseCase,
                    getCurrentUserUseCase = getCurrentUserUseCase,
                    appointmentMapper = appointmentMapper
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private companion object {
        val FILTER_LABEL_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr-TR"))
    }
}
