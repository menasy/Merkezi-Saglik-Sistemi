package com.menasy.merkezisagliksistemi.ui.patient.appointmentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.domain.usecase.CancelAppointmentUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.ObservePatientAppointmentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

enum class AppointmentTab {
    ACTIVE,
    PAST
}

data class PatientAppointmentsUiState(
    val isLoading: Boolean = false,
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
    private val appointmentMapper: AppointmentMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientAppointmentsUiState())
    val uiState: StateFlow<PatientAppointmentsUiState> = _uiState.asStateFlow()

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
                .collect { appointments ->
                    val (active, past) = appointmentMapper.partitionAppointments(appointments)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeAppointments = active,
                        pastAppointments = past,
                        emptyMessage = when (_uiState.value.selectedTab) {
                            AppointmentTab.ACTIVE -> "Henüz aktif randevunuz yok"
                            AppointmentTab.PAST -> "Geçmiş randevunuz bulunmuyor"
                        },
                        errorMessage = null
                    )
                }
        }
    }

    fun cancelAppointment(appointment: PatientAppointmentItem) {
        if (_uiState.value.isCancelling) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true)

            cancelAppointmentUseCase(appointment.id)
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
        private val appointmentMapper: AppointmentMapper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PatientAppointmentsViewModel::class.java)) {
                return PatientAppointmentsViewModel(
                    observePatientAppointmentsUseCase,
                    cancelAppointmentUseCase,
                    getCurrentUserUseCase,
                    appointmentMapper
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
