package com.menasy.merkezisagliksistemi.ui.patient.appointmentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.model.AppointmentStatus
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.di.SessionCache
import com.menasy.merkezisagliksistemi.domain.usecase.CancelAppointmentUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetPrescriptionPreviewsByAppointmentIdsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.ObservePatientAppointmentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AppointmentTab {
    ACTIVE,
    PAST
}

data class PatientAppointmentsUiState(
    val isLoading: Boolean = false,
    val isPrescriptionLoading: Boolean = false,
    val selectedTab: AppointmentTab = AppointmentTab.ACTIVE,
    val activeAppointments: List<PatientAppointmentItem> = emptyList(),
    val pastAppointments: List<PatientAppointmentItem> = emptyList(),
    val emptyMessage: String = "",
    val errorMessage: String? = null,
    val isCancelling: Boolean = false
)

class PatientAppointmentsViewModel(
    private val observePatientAppointmentsUseCase: ObservePatientAppointmentsUseCase,
    private val cancelAppointmentUseCase: CancelAppointmentUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getPrescriptionPreviewsByAppointmentIdsUseCase: GetPrescriptionPreviewsByAppointmentIdsUseCase,
    private val appointmentMapper: AppointmentMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientAppointmentsUiState())
    val uiState: StateFlow<PatientAppointmentsUiState> = _uiState.asStateFlow()
    private val prescriptionCache = mutableMapOf<String, Prescription?>()
    private var latestAppointments: List<Appointment> = emptyList()

    init {
        loadAppointments()
    }

    fun selectTab(tab: AppointmentTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            emptyMessage = when (tab) {
                AppointmentTab.ACTIVE -> "Henüz aktif randevunuz yok"
                AppointmentTab.PAST -> "Geçmiş randevunuz bulunmuyor"
            }
        )

        if (tab == AppointmentTab.PAST) {
            viewModelScope.launch {
                ensurePastPrescriptionPreviews(latestAppointments)
                updateSections()
            }
        } else {
            updateSections()
        }
    }

    private fun loadAppointments() {
        val patientId = getCurrentUserUseCase.getCurrentUserId()
        
        if (patientId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Kullanıcı oturumu bulunamadı"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            observePatientAppointmentsUseCase(patientId)
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Randevular yüklenemedi: ${exception.message}"
                    )
                }
                .collectLatest { appointments ->
                    latestAppointments = appointments

                    if (_uiState.value.selectedTab == AppointmentTab.PAST) {
                        ensurePastPrescriptionPreviews(appointments)
                    }

                    updateSections(isLoading = false)
                }
        }
    }

    private suspend fun ensurePastPrescriptionPreviews(appointments: List<Appointment>) {
        val pastAppointmentIds = appointments
            .asSequence()
            .filter {
                it.status == AppointmentStatus.COMPLETED.name ||
                    it.status == AppointmentStatus.MISSED.name ||
                    it.status == AppointmentStatus.CANCELLED.name
            }
            .map { it.id }
            .filter { it.isNotBlank() }
            .toSet()

        prescriptionCache.keys.retainAll(pastAppointmentIds)

        val missingAppointmentIds = pastAppointmentIds
            .filterNot { prescriptionCache.containsKey(it) }
            .toSet()

        if (missingAppointmentIds.isEmpty()) return

        _uiState.value = _uiState.value.copy(isPrescriptionLoading = true)

        getPrescriptionPreviewsByAppointmentIdsUseCase(missingAppointmentIds)
            .onSuccess { previews ->
                previews.forEach { (appointmentId, preview) ->
                    prescriptionCache[appointmentId] = preview
                }
                missingAppointmentIds
                    .filterNot { previews.containsKey(it) }
                    .forEach { prescriptionCache[it] = null }
            }
            .onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Reçete bilgileri alınamadı: ${exception.message}"
                )
            }

        _uiState.value = _uiState.value.copy(isPrescriptionLoading = false)
    }

    private fun updateSections(isLoading: Boolean = _uiState.value.isLoading) {
        val state = _uiState.value
        val prescriptions = prescriptionCache
            .mapNotNull { (appointmentId, preview) ->
                preview?.let { appointmentId to it }
            }
            .toMap()

        val (active, past) = appointmentMapper.partitionAppointments(
            appointments = latestAppointments,
            prescriptionByAppointmentId = prescriptions
        )

        _uiState.value = state.copy(
            isLoading = isLoading,
            activeAppointments = active,
            pastAppointments = past,
            emptyMessage = when (state.selectedTab) {
                AppointmentTab.ACTIVE -> "Henüz aktif randevunuz yok"
                AppointmentTab.PAST -> "Geçmiş randevunuz bulunmuyor"
            }
        )
    }

    fun cancelAppointment(appointment: PatientAppointmentItem) {
        if (_uiState.value.isCancelling) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true)

            val patientId = SessionCache.userId
            if (patientId.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    isCancelling = false,
                    errorMessage = "Kullanıcı oturumu bulunamadı"
                )
                return@launch
            }

            cancelAppointmentUseCase(
                appointmentId = appointment.id,
                patientId = patientId
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isCancelling = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isCancelling = false,
                        errorMessage = "Randevu iptal edilemedi: ${exception.message}"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(
        private val observePatientAppointmentsUseCase: ObservePatientAppointmentsUseCase,
        private val cancelAppointmentUseCase: CancelAppointmentUseCase,
        private val getCurrentUserUseCase: GetCurrentUserUseCase,
        private val getPrescriptionPreviewsByAppointmentIdsUseCase: GetPrescriptionPreviewsByAppointmentIdsUseCase,
        private val appointmentMapper: AppointmentMapper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PatientAppointmentsViewModel::class.java)) {
                return PatientAppointmentsViewModel(
                    observePatientAppointmentsUseCase,
                    cancelAppointmentUseCase,
                    getCurrentUserUseCase,
                    getPrescriptionPreviewsByAppointmentIdsUseCase,
                    appointmentMapper
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
