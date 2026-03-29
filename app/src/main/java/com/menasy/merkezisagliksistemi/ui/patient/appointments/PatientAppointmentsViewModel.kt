package com.menasy.merkezisagliksistemi.ui.patient.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

enum class AppointmentTab {
    ACTIVE,
    PAST
}

data class PatientAppointmentsUiState(
    val isLoading: Boolean = false,
    val selectedTab: AppointmentTab = AppointmentTab.ACTIVE,
    val activeAppointments: List<PatientAppointmentItem> = emptyList(),
    val pastAppointments: List<PatientAppointmentItem> = emptyList(),
    val emptyMessage: String = ""
)

class PatientAppointmentsViewModel : ViewModel() {

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
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            // Simulated data - replace with actual repository call
            val mockAppointments = generateMockAppointments()

            val now = LocalDateTime.now()
            val (active, past) = mockAppointments.partition { appointment ->
                val appointmentDateTime = parseDateTime(appointment.dateMillis, appointment.timeLabel)
                appointmentDateTime.isAfter(now) && appointment.status == "SCHEDULED"
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                activeAppointments = active,
                pastAppointments = past,
                emptyMessage = "Henüz aktif randevunuz yok"
            )
        }
    }

    fun cancelAppointment(appointment: PatientAppointmentItem) {
        viewModelScope.launch {
            // TODO: Implement cancel logic with repository
            // For now, just remove from list
            val updatedActive = _uiState.value.activeAppointments.filter { it.id != appointment.id }
            _uiState.value = _uiState.value.copy(activeAppointments = updatedActive)
        }
    }

    private fun parseDateTime(dateMillis: Long, time: String): LocalDateTime {
        val date = LocalDate.ofInstant(
            java.time.Instant.ofEpochMilli(dateMillis),
            ZoneId.systemDefault()
        )
        val timeParts = time.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        return date.atTime(hour, minute)
    }

    private fun generateMockAppointments(): List<PatientAppointmentItem> {
        // Mock data for demonstration
        val now = LocalDateTime.now()

        return listOf(
            PatientAppointmentItem(
                id = "1",
                hospitalName = "İstanbul-(Avrupa)- Başakşehir Çam ve Sakura Şehir Hastanesi",
                branchName = "Endoskopi",
                doctorName = "Uzm. Dr. Elif Kaya",
                dateMillis = now.plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                timeLabel = "13:34",
                status = "SCHEDULED",
                isActive = true
            ),
            PatientAppointmentItem(
                id = "2",
                hospitalName = "Ankara Şehir Hastanesi",
                branchName = "Kardiyoloji",
                doctorName = "Prof. Dr. Ahmet Yılmaz",
                dateMillis = now.plusDays(3).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                timeLabel = "10:00",
                status = "SCHEDULED",
                isActive = true
            ),
            PatientAppointmentItem(
                id = "3",
                hospitalName = "İzmir Atatürk Eğitim ve Araştırma Hastanesi",
                branchName = "Göz Hastalıkları",
                doctorName = "Uzm. Dr. Elif Kaya",
                dateMillis = now.minusDays(10).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                timeLabel = "14:30",
                status = "COMPLETED",
                isActive = false
            ),
            PatientAppointmentItem(
                id = "4",
                hospitalName = "Bursa Yüksek İhtisas Eğitim ve Araştırma Hastanesi",
                branchName = "Ortopedi ve Travmatoloji",
                doctorName = "Doç. Dr. Mehmet Demir",
                dateMillis = now.minusDays(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                timeLabel = "11:15",
                status = "CANCELLED",
                isActive = false
            )
        )
    }
}
