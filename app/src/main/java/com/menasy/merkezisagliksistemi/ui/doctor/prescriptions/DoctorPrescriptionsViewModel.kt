package com.menasy.merkezisagliksistemi.ui.doctor.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.model.AppointmentStatus
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.di.SessionCache
import com.menasy.merkezisagliksistemi.domain.usecase.GetPrescriptionPreviewsByAppointmentIdsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetUserFullNamesByIdsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.ObserveDoctorAppointmentsUseCase
import com.menasy.merkezisagliksistemi.ui.common.adapter.PrescriptionListItem
import com.menasy.merkezisagliksistemi.ui.patient.appointmentlist.AppointmentMapper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DoctorPrescriptionFilter {
    TODAY,
    RANGE,
    ALL
}

data class DoctorPrescriptionsUiState(
    val isLoading: Boolean = true,
    val selectedFilter: DoctorPrescriptionFilter = DoctorPrescriptionFilter.ALL,
    val activeFilterLabel: String = "Tüm Tarihler",
    val prescriptions: List<PrescriptionListItem> = emptyList(),
    val emptyMessage: String = "Kayıtlı reçete bulunmuyor.",
    val rangeStartUtcMillis: Long? = null,
    val rangeEndUtcMillis: Long? = null,
    val errorMessage: String? = null
)

class DoctorPrescriptionsViewModel(
    private val observeDoctorAppointmentsUseCase: ObserveDoctorAppointmentsUseCase,
    private val getUserFullNamesByIdsUseCase: GetUserFullNamesByIdsUseCase,
    private val getPrescriptionPreviewsByAppointmentIdsUseCase: GetPrescriptionPreviewsByAppointmentIdsUseCase,
    private val appointmentMapper: AppointmentMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorPrescriptionsUiState())
    val uiState: StateFlow<DoctorPrescriptionsUiState> = _uiState.asStateFlow()

    private val patientNameCache = mutableMapOf<String, String>()
    private val prescriptionCache = mutableMapOf<String, Prescription?>()
    private var allPrescriptionItems: List<PrescriptionListItem> = emptyList()

    init {
        observeDoctorAppointments()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun selectTodayFilter() {
        _uiState.update {
            it.copy(
                selectedFilter = DoctorPrescriptionFilter.TODAY,
                rangeStartUtcMillis = null,
                rangeEndUtcMillis = null
            )
        }
        applyFilter()
    }

    fun selectAllFilter() {
        _uiState.update {
            it.copy(
                selectedFilter = DoctorPrescriptionFilter.ALL,
                rangeStartUtcMillis = null,
                rangeEndUtcMillis = null
            )
        }
        applyFilter()
    }

    fun selectRangeFilter(startUtcMillis: Long, endUtcMillis: Long) {
        _uiState.update {
            it.copy(
                selectedFilter = DoctorPrescriptionFilter.RANGE,
                rangeStartUtcMillis = startUtcMillis,
                rangeEndUtcMillis = endUtcMillis
            )
        }
        applyFilter()
    }

    private fun observeDoctorAppointments() {
        val doctorId = SessionCache.doctorId
        if (doctorId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Doktor oturumu bulunamadı"
                )
            }
            return
        }

        viewModelScope.launch {
            observeDoctorAppointmentsUseCase(doctorId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Reçete verileri yüklenemedi: ${throwable.message}"
                        )
                    }
                }
                .collectLatest { appointments ->
                    synchronizeCaches(appointments)
                    rebuildPrescriptionItems(appointments)
                    applyFilter(isLoading = false)
                }
        }
    }

    private suspend fun synchronizeCaches(appointments: List<Appointment>) {
        val missingPatientIds = appointments
            .asSequence()
            .map { it.patientId }
            .filter { it.isNotBlank() && !patientNameCache.containsKey(it) }
            .toSet()

        if (missingPatientIds.isNotEmpty()) {
            getUserFullNamesByIdsUseCase(missingPatientIds)
                .onSuccess { names ->
                    patientNameCache.putAll(names)
                    missingPatientIds
                        .filterNot { names.containsKey(it) }
                        .forEach { patientNameCache[it] = "Bilinmeyen Hasta" }
                }
                .onFailure {
                    missingPatientIds.forEach { patientNameCache[it] = "Bilinmeyen Hasta" }
                }
        }

        val relevantAppointmentIds = appointments
            .asSequence()
            .filter {
                it.status == AppointmentStatus.COMPLETED.name ||
                    it.status == AppointmentStatus.MISSED.name ||
                    it.status == AppointmentStatus.CANCELLED.name
            }
            .map { it.id }
            .filter { it.isNotBlank() }
            .toSet()

        prescriptionCache.keys.retainAll(relevantAppointmentIds)
        if (relevantAppointmentIds.isEmpty()) {
            return
        }

        val missingAppointmentIds = relevantAppointmentIds
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

    private fun rebuildPrescriptionItems(appointments: List<Appointment>) {
        val prescriptions = prescriptionCache
            .mapNotNull { (appointmentId, preview) -> preview?.let { appointmentId to it } }
            .toMap()

        allPrescriptionItems = appointmentMapper.mapDoctorPrescriptionItems(
            appointments = appointments,
            patientNamesById = patientNameCache,
            prescriptionByAppointmentId = prescriptions
        )
    }

    private fun applyFilter(isLoading: Boolean = _uiState.value.isLoading) {
        val state = _uiState.value

        val filteredItems = when (state.selectedFilter) {
            DoctorPrescriptionFilter.TODAY -> {
                val today = LocalDate.now()
                allPrescriptionItems.filter { item ->
                    item.createdAtMillis > 0L &&
                        millisToLocalDate(item.createdAtMillis) == today
                }
            }

            DoctorPrescriptionFilter.RANGE -> {
                val startUtc = state.rangeStartUtcMillis
                val endUtc = state.rangeEndUtcMillis
                if (startUtc == null || endUtc == null) {
                    emptyList()
                } else {
                    val startDate = utcMillisToLocalDate(startUtc)
                    val endDate = utcMillisToLocalDate(endUtc)
                    allPrescriptionItems.filter { item ->
                        if (item.createdAtMillis <= 0L) {
                            false
                        } else {
                            val createdDate = millisToLocalDate(item.createdAtMillis)
                            !createdDate.isBefore(startDate) && !createdDate.isAfter(endDate)
                        }
                    }
                }
            }

            DoctorPrescriptionFilter.ALL -> allPrescriptionItems
        }

        val activeFilterLabel = when (state.selectedFilter) {
            DoctorPrescriptionFilter.TODAY -> "Bugün"
            DoctorPrescriptionFilter.ALL -> "Tüm Tarihler"
            DoctorPrescriptionFilter.RANGE -> {
                val startUtc = state.rangeStartUtcMillis
                val endUtc = state.rangeEndUtcMillis
                if (startUtc == null || endUtc == null) {
                    "Tarih Aralığı"
                } else {
                    val startDate = utcMillisToLocalDate(startUtc)
                    val endDate = utcMillisToLocalDate(endUtc)
                    "${DATE_FORMATTER.format(startDate)} - ${DATE_FORMATTER.format(endDate)}"
                }
            }
        }

        val emptyMessage = when (state.selectedFilter) {
            DoctorPrescriptionFilter.TODAY -> "Bugün için reçete bulunmuyor."
            DoctorPrescriptionFilter.RANGE -> "Seçilen tarih aralığında reçete bulunmuyor."
            DoctorPrescriptionFilter.ALL -> "Kayıtlı reçete bulunmuyor."
        }

        _uiState.update {
            it.copy(
                isLoading = isLoading,
                activeFilterLabel = activeFilterLabel,
                prescriptions = filteredItems,
                emptyMessage = emptyMessage
            )
        }
    }

    private fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private fun utcMillisToLocalDate(utcMillis: Long): LocalDate {
        return Instant.ofEpochMilli(utcMillis)
            .atOffset(ZoneOffset.UTC)
            .toLocalDate()
    }

    class Factory(
        private val observeDoctorAppointmentsUseCase: ObserveDoctorAppointmentsUseCase,
        private val getUserFullNamesByIdsUseCase: GetUserFullNamesByIdsUseCase,
        private val getPrescriptionPreviewsByAppointmentIdsUseCase: GetPrescriptionPreviewsByAppointmentIdsUseCase,
        private val appointmentMapper: AppointmentMapper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DoctorPrescriptionsViewModel::class.java)) {
                return DoctorPrescriptionsViewModel(
                    observeDoctorAppointmentsUseCase = observeDoctorAppointmentsUseCase,
                    getUserFullNamesByIdsUseCase = getUserFullNamesByIdsUseCase,
                    getPrescriptionPreviewsByAppointmentIdsUseCase = getPrescriptionPreviewsByAppointmentIdsUseCase,
                    appointmentMapper = appointmentMapper
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private companion object {
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))
    }
}
