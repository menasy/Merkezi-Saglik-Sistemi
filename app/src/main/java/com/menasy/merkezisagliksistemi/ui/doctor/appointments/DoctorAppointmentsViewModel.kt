package com.menasy.merkezisagliksistemi.ui.doctor.appointments

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
import com.menasy.merkezisagliksistemi.ui.patient.appointmentlist.AppointmentMapper
import com.menasy.merkezisagliksistemi.ui.patient.appointmentlist.DoctorAppointmentsSections
import com.menasy.merkezisagliksistemi.ui.patient.appointmentlist.PatientAppointmentItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class DoctorAppointmentTab {
    PENDING,
    UPCOMING,
    PAST
}

data class DoctorAppointmentsUiState(
    val isLoading: Boolean = true,
    val isPrescriptionLoading: Boolean = false,
    val selectedTab: DoctorAppointmentTab = DoctorAppointmentTab.PENDING,
    val selectedDate: LocalDate = LocalDate.now(),
    val pendingAppointments: List<PatientAppointmentItem> = emptyList(),
    val upcomingAppointments: List<PatientAppointmentItem> = emptyList(),
    val pastAppointments: List<PatientAppointmentItem> = emptyList(),
    val pendingCount: Int = 0,
    val upcomingCount: Int = 0,
    val pastCount: Int = 0,
    val errorMessage: String? = null
)

class DoctorAppointmentsViewModel(
    private val observeDoctorAppointmentsUseCase: ObserveDoctorAppointmentsUseCase,
    private val getUserFullNamesByIdsUseCase: GetUserFullNamesByIdsUseCase,
    private val getPrescriptionPreviewsByAppointmentIdsUseCase: GetPrescriptionPreviewsByAppointmentIdsUseCase,
    private val appointmentMapper: AppointmentMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorAppointmentsUiState())
    val uiState: StateFlow<DoctorAppointmentsUiState> = _uiState.asStateFlow()

    private val patientNameCache = mutableMapOf<String, String>()
    private val prescriptionCache = mutableMapOf<String, Prescription?>()
    private var latestAppointments: List<Appointment> = emptyList()

    init {
        observeDoctorAppointments()
    }

    fun selectTab(tab: DoctorAppointmentTab) {
        if (_uiState.value.selectedTab == tab) return

        _uiState.update { it.copy(selectedTab = tab) }

        if (tab == DoctorAppointmentTab.PAST) {
            viewModelScope.launch {
                ensurePastPrescriptionPreviews(latestAppointments)
                updateSections()
            }
        } else {
            updateSections()
        }
    }

    fun onDateSelected(utcMillis: Long) {
        val selectedDate = Instant.ofEpochMilli(utcMillis)
            .atOffset(ZoneOffset.UTC)
            .toLocalDate()

        _uiState.update { it.copy(selectedDate = selectedDate) }
        updateSections()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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
                            errorMessage = "Randevular yüklenemedi: ${throwable.message}"
                        )
                    }
                }
                .collectLatest { appointments ->
                    latestAppointments = appointments

                    ensurePatientNames(appointments)
                    if (_uiState.value.selectedTab == DoctorAppointmentTab.PAST) {
                        ensurePastPrescriptionPreviews(appointments)
                    }

                    updateSections(isLoading = false)
                }
        }
    }

    private suspend fun ensurePatientNames(appointments: List<Appointment>) {
        val missingPatientIds = appointments
            .asSequence()
            .map { it.patientId }
            .filter { it.isNotBlank() && !patientNameCache.containsKey(it) }
            .toSet()

        if (missingPatientIds.isEmpty()) return

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

    private suspend fun ensurePastPrescriptionPreviews(appointments: List<Appointment>) {
        val pastAppointmentIds = appointments
            .filter {
                it.status == AppointmentStatus.COMPLETED.name ||
                    it.status == AppointmentStatus.MISSED.name ||
                    it.status == AppointmentStatus.CANCELLED.name
            }
            .map { it.id }
            .filter { it.isNotBlank() }
            .toSet()

        val missingAppointmentIds = pastAppointmentIds.filterNot { prescriptionCache.containsKey(it) }.toSet()
        if (missingAppointmentIds.isEmpty()) return

        _uiState.update { it.copy(isPrescriptionLoading = true) }

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
                    it.copy(errorMessage = "Reçete bilgileri alınamadı: ${throwable.message}")
                }
            }

        _uiState.update { it.copy(isPrescriptionLoading = false) }
    }

    private fun updateSections(isLoading: Boolean = _uiState.value.isLoading) {
        val state = _uiState.value
        val prescriptions = prescriptionCache
            .mapNotNull { (appointmentId, preview) ->
                preview?.let { appointmentId to it }
            }
            .toMap()

        val sections: DoctorAppointmentsSections = appointmentMapper.partitionDoctorAppointments(
            appointments = latestAppointments,
            selectedDate = state.selectedDate,
            patientNamesById = patientNameCache,
            prescriptionByAppointmentId = prescriptions
        )

        _uiState.update {
            it.copy(
                isLoading = isLoading,
                pendingAppointments = sections.pendingAppointments,
                upcomingAppointments = sections.upcomingAppointments,
                pastAppointments = sections.pastAppointments,
                pendingCount = sections.pendingAppointments.size,
                upcomingCount = sections.upcomingAppointments.size,
                pastCount = sections.pastAppointments.size
            )
        }
    }

    class Factory(
        private val observeDoctorAppointmentsUseCase: ObserveDoctorAppointmentsUseCase,
        private val getUserFullNamesByIdsUseCase: GetUserFullNamesByIdsUseCase,
        private val getPrescriptionPreviewsByAppointmentIdsUseCase: GetPrescriptionPreviewsByAppointmentIdsUseCase,
        private val appointmentMapper: AppointmentMapper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DoctorAppointmentsViewModel::class.java)) {
                return DoctorAppointmentsViewModel(
                    observeDoctorAppointmentsUseCase = observeDoctorAppointmentsUseCase,
                    getUserFullNamesByIdsUseCase = getUserFullNamesByIdsUseCase,
                    getPrescriptionPreviewsByAppointmentIdsUseCase = getPrescriptionPreviewsByAppointmentIdsUseCase,
                    appointmentMapper = appointmentMapper
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
