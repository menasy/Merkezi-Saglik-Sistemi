package com.menasy.merkezisagliksistemi.ui.doctor.examination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.data.model.AppointmentStatus
import com.menasy.merkezisagliksistemi.data.model.Medicine
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.di.SessionCache
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorExaminationAppointmentUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetMedicinesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.UpdateDoctorAppointmentResultUseCase
import com.menasy.merkezisagliksistemi.ui.common.base.BaseViewModel
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PrescriptionDecision {
    REQUIRED,
    NOT_REQUIRED
}

data class DoctorExaminationUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isAccessible: Boolean = false,
    val appointmentId: String = "",
    val patientName: String = "",
    val doctorName: String = "",
    val hospitalName: String = "",
    val branchName: String = "",
    val appointmentDateLabel: String = "",
    val appointmentTimeLabel: String = "",
    val examinationNote: String = "",
    val doctorNote: String = "",
    val prescriptionDecision: PrescriptionDecision? = null,
    val showMedicineSection: Boolean = false,
    val availableMedicines: List<Medicine> = emptyList(),
    val selectedMedicines: List<Medicine> = emptyList(),
    val shouldCloseScreen: Boolean = false
)

class DoctorExaminationViewModel(
    private val getDoctorExaminationAppointmentUseCase: GetDoctorExaminationAppointmentUseCase,
    private val updateDoctorAppointmentResultUseCase: UpdateDoctorAppointmentResultUseCase,
    private val getMedicinesUseCase: GetMedicinesUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DoctorExaminationUiState())
    val uiState: StateFlow<DoctorExaminationUiState> = _uiState.asStateFlow()

    private val medicinesById = mutableMapOf<String, Medicine>()
    private var isInitialized = false

    fun initialize(
        appointmentId: String?,
        prefetchedPatientName: String?
    ) {
        if (isInitialized) return
        isInitialized = true

        val resolvedAppointmentId = appointmentId?.takeIf { it.isNotBlank() } ?: run {
            _uiState.update { it.copy(isLoading = false, isAccessible = false, shouldCloseScreen = true) }
            publishError(AppErrorReason.APPOINTMENT_INFO_MISSING)
            return
        }

        val doctorId = SessionCache.doctorId?.takeIf { it.isNotBlank() } ?: run {
            _uiState.update { it.copy(isLoading = false, isAccessible = false, shouldCloseScreen = true) }
            publishError(AppErrorReason.NO_ACTIVE_SESSION)
            return
        }

        val medicines = getMedicinesUseCase()
        medicinesById.clear()
        medicinesById.putAll(medicines.associateBy { it.medicineId })

        _uiState.update {
            it.copy(
                isLoading = true,
                availableMedicines = medicines
            )
        }

        viewModelScope.launch {
            getDoctorExaminationAppointmentUseCase(
                appointmentId = resolvedAppointmentId,
                doctorId = doctorId,
                prefetchedPatientName = prefetchedPatientName
            ).onSuccess { appointment ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAccessible = true,
                        appointmentId = appointment.appointmentId,
                        patientName = appointment.patientName,
                        doctorName = appointment.doctorName,
                        hospitalName = appointment.hospitalName,
                        branchName = appointment.branchName,
                        appointmentDateLabel = formatDateLabel(appointment.appointmentDate),
                        appointmentTimeLabel = appointment.appointmentTime
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAccessible = false,
                        shouldCloseScreen = true
                    )
                }
                publishError(throwable, OperationType.APPOINTMENT)
            }
        }
    }

    fun onExaminationNoteChanged(value: String) {
        _uiState.update { it.copy(examinationNote = value) }
    }

    fun onDoctorNoteChanged(value: String) {
        _uiState.update { it.copy(doctorNote = value) }
    }

    fun onPrescriptionDecisionChanged(decision: PrescriptionDecision) {
        _uiState.update {
            it.copy(
                prescriptionDecision = decision,
                showMedicineSection = decision == PrescriptionDecision.REQUIRED
            )
        }
    }

    fun addMedicine(medicineId: String) {
        val state = _uiState.value
        if (!state.isAccessible || state.isSubmitting) return

        val medicine = medicinesById[medicineId]
            ?: state.availableMedicines.firstOrNull { it.medicineId == medicineId }
            ?: return

        if (state.selectedMedicines.any { it.medicineId == medicineId }) {
            return
        }

        _uiState.update {
            it.copy(
                selectedMedicines = (it.selectedMedicines + medicine)
                    .sortedBy { selected -> selected.medicineName }
            )
        }
    }

    fun removeMedicine(medicineId: String) {
        _uiState.update { state ->
            state.copy(
                selectedMedicines = state.selectedMedicines.filterNot { it.medicineId == medicineId }
            )
        }
    }

    fun updateMedicineDoctorNote(medicineId: String, doctorNote: String) {
        _uiState.update { state ->
            state.copy(
                selectedMedicines = state.selectedMedicines.map { medicine ->
                    if (medicine.medicineId == medicineId) {
                        medicine.copy(doctorNote = doctorNote)
                    } else {
                        medicine
                    }
                }
            )
        }
    }

    fun getSelectableMedicines(): List<Medicine> {
        val state = _uiState.value
        val selectedIds = state.selectedMedicines.map { it.medicineId }.toSet()
        return state.availableMedicines.filterNot { it.medicineId in selectedIds }
    }

    fun markPatientMissed() {
        submitResult(
            targetStatus = AppointmentStatus.MISSED,
            prescription = null
        )
    }

    fun completeExamination() {
        val state = _uiState.value

        val decision = state.prescriptionDecision ?: run {
            publishError(AppErrorReason.PRESCRIPTION_DECISION_REQUIRED)
            return
        }

        val prescription = when (decision) {
            PrescriptionDecision.NOT_REQUIRED -> null
            PrescriptionDecision.REQUIRED -> {
                if (state.selectedMedicines.isEmpty()) {
                    publishError(AppErrorReason.PRESCRIPTION_MEDICINE_REQUIRED)
                    return
                }

                Prescription(
                    appointmentId = state.appointmentId,
                    note = state.doctorNote.trim(),
                    medicines = state.selectedMedicines
                )
            }
        }

        submitResult(
            targetStatus = AppointmentStatus.COMPLETED,
            prescription = prescription
        )
    }

    fun consumeCloseSignal() {
        _uiState.update { it.copy(shouldCloseScreen = false) }
    }

    private fun submitResult(
        targetStatus: AppointmentStatus,
        prescription: Prescription?
    ) {
        val state = _uiState.value
        if (state.isSubmitting || !state.isAccessible || state.appointmentId.isBlank()) return

        val doctorId = SessionCache.doctorId?.takeIf { it.isNotBlank() } ?: run {
            publishError(AppErrorReason.NO_ACTIVE_SESSION)
            return
        }
        val normalizedExaminationNote = state.examinationNote.trim()

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            updateDoctorAppointmentResultUseCase(
                appointmentId = state.appointmentId,
                doctorId = doctorId,
                targetStatus = targetStatus,
                prescription = if (targetStatus == AppointmentStatus.MISSED) null else prescription,
                examinationNote = normalizedExaminationNote
            ).onSuccess {
                _uiState.update { current ->
                    current.copy(isSubmitting = false, shouldCloseScreen = true)
                }

                when (targetStatus) {
                    AppointmentStatus.MISSED -> publishSuccess(
                        title = "Hasta Gelmedi İşaretlendi",
                        description = "Randevu MISSED durumuna alındı."
                    )

                    AppointmentStatus.COMPLETED -> {
                        if (prescription != null) {
                            publishSuccess(
                                title = "Muayene ve Reçete Kaydedildi",
                                description = "Randevu tamamlandı ve reçete oluşturuldu."
                            )
                        } else {
                            publishSuccess(
                                title = "Muayene Tamamlandı",
                                description = "Randevu COMPLETED durumuna alındı."
                            )
                        }
                    }

                    else -> Unit
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSubmitting = false) }
                publishError(throwable, OperationType.APPOINTMENT)
            }
        }
    }

    private fun formatDateLabel(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString, FIRESTORE_DATE_FORMATTER)
            date.format(DISPLAY_DATE_FORMATTER)
        } catch (_: Exception) {
            dateString
        }
    }

    class Factory(
        private val getDoctorExaminationAppointmentUseCase: GetDoctorExaminationAppointmentUseCase,
        private val updateDoctorAppointmentResultUseCase: UpdateDoctorAppointmentResultUseCase,
        private val getMedicinesUseCase: GetMedicinesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DoctorExaminationViewModel::class.java)) {
                return DoctorExaminationViewModel(
                    getDoctorExaminationAppointmentUseCase = getDoctorExaminationAppointmentUseCase,
                    updateDoctorAppointmentResultUseCase = updateDoctorAppointmentResultUseCase,
                    getMedicinesUseCase = getMedicinesUseCase
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        private val FIRESTORE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
        private val DISPLAY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy EEEE", Locale.forLanguageTag("tr-TR"))
    }
}
