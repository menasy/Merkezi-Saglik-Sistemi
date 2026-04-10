package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.di.SessionCache
import com.menasy.merkezisagliksistemi.domain.usecase.CreateAppointmentUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.ui.common.base.BaseViewModel
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import com.menasy.merkezisagliksistemi.utils.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AppointmentConfirmationUiState(
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isSuccess: Boolean = false,
    val appointmentId: String? = null,
    val doctorName: String = "",
    val hospitalName: String = "",
    val branchName: String = "",
    val dateLabel: String = "",
    val timeLabel: String = "",
    val patientName: String = "Yükleniyor..."
)

class AppointmentConfirmationViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val createAppointmentUseCase: CreateAppointmentUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AppointmentConfirmationUiState())
    val uiState: StateFlow<AppointmentConfirmationUiState> = _uiState.asStateFlow()

    private var isLoaded = false
    private var confirmationArgs: AppointmentConfirmationArgs? = null

    fun load(args: AppointmentConfirmationArgs) {
        if (isLoaded) return
        isLoaded = true
        confirmationArgs = args

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            doctorName = args.doctorName,
            hospitalName = args.hospitalName,
            branchName = args.branchName,
            dateLabel = formatDate(args.dateMillis),
            timeLabel = args.timeLabel
        )

        viewModelScope.launch {
            val patientNameResult = getCurrentUserUseCase.getCurrentUserFullName()
            patientNameResult.fold(
                onSuccess = { patientName ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        patientName = patientName
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        patientName = "Giriş yapan kullanıcı"
                    )
                    publishError(exception, OperationType.SESSION)
                }
            )
        }
    }

    fun confirm() {
        val args = confirmationArgs ?: run {
            publishError(AppErrorReason.APPOINTMENT_INFO_MISSING)
            return
        }

        val patientId = SessionCache.userId ?: run {
            publishError(AppErrorReason.NO_ACTIVE_SESSION)
            return
        }

        if (_uiState.value.isCreating) return

        _uiState.value = _uiState.value.copy(isCreating = true)

        viewModelScope.launch {
            val appointmentDate = formatDateForFirestore(args.dateMillis)
            if (!DateTimeUtils.isAppointmentInFuture(appointmentDate, args.timeLabel)) {
                _uiState.value = _uiState.value.copy(isCreating = false)
                publishError(AppErrorReason.PAST_APPOINTMENT_TIME_NOT_ALLOWED)
                return@launch
            }

            val appointment = Appointment(
                patientId = patientId,
                doctorId = args.doctorId,
                hospitalId = args.hospitalId,
                branchId = args.branchId,
                appointmentDate = appointmentDate,
                appointmentTime = args.timeLabel
            )

            val result = createAppointmentUseCase(appointment)

            result.fold(
                onSuccess = { appointmentId ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        isSuccess = true,
                        appointmentId = appointmentId
                    )
                    publishSuccess(
                        title = "Randevu Oluşturuldu",
                        description = "Randevunuz başarıyla oluşturuldu."
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(isCreating = false)
                    publishError(exception, OperationType.APPOINTMENT)
                }
            )
        }
    }

    private fun formatDate(millis: Long): String {
        val date = DateTimeUtils.millisToLocalDate(millis)
        return DATE_FORMATTER.format(date)
    }

    private fun formatDateForFirestore(millis: Long): String {
        val date = DateTimeUtils.millisToLocalDate(millis)
        return DATE_FIRESTORE_FORMATTER.format(date)
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy EEEE", Locale.forLanguageTag("tr-TR"))
        val DATE_FIRESTORE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
    }
}
