package com.menasy.merkezisagliksistemi.ui.patient.appointment

import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.ui.common.base.BaseViewModel
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AppointmentConfirmationUiState(
    val isLoading: Boolean = false,
    val doctorName: String = "",
    val hospitalName: String = "",
    val branchName: String = "",
    val dateLabel: String = "",
    val timeLabel: String = "",
    val patientName: String = "Yükleniyor..."
)

class AppointmentConfirmationViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AppointmentConfirmationUiState())
    val uiState: StateFlow<AppointmentConfirmationUiState> = _uiState.asStateFlow()

    private var isLoaded = false

    fun load(args: AppointmentConfirmationArgs) {
        if (isLoaded) return
        isLoaded = true

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
        publishInfo(
            title = "Bilgilendirme",
            description = "Randevu oluşturma işlemi bir sonraki aşamada aktifleştirilecek."
        )
    }

    private fun formatDate(millis: Long): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return DATE_FORMATTER.format(date)
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR"))
    }
}
