package com.menasy.merkezisagliksistemi.ui.doctor.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.di.SessionCache
import com.menasy.merkezisagliksistemi.domain.usecase.DoctorHomeSummary
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorHomeSummaryUseCase
import com.menasy.merkezisagliksistemi.ui.common.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for Doctor Home screen.
 */
data class DoctorHomeUiState(
    val summaryState: UiState<DoctorHomeSummary> = UiState.Loading
)

/**
 * ViewModel for the Doctor Home screen.
 * Manages UI state and data loading for the doctor dashboard.
 */
class DoctorHomeViewModel(
    private val getDoctorHomeSummaryUseCase: GetDoctorHomeSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorHomeUiState())
    val uiState: StateFlow<DoctorHomeUiState> = _uiState.asStateFlow()

    init {
        loadSummary()
    }

    fun loadSummary() {
        val doctorId = SessionCache.doctorId

        if (doctorId.isNullOrBlank()) {
            _uiState.value = DoctorHomeUiState(
                summaryState = UiState.Error("Doktor oturumu bulunamadı")
            )
            return
        }

        _uiState.value = DoctorHomeUiState(summaryState = UiState.Loading)

        viewModelScope.launch {
            getDoctorHomeSummaryUseCase(doctorId)
                .onSuccess { summary ->
                    _uiState.value = DoctorHomeUiState(
                        summaryState = UiState.Success(summary)
                    )
                }
                .onFailure { exception ->
                    _uiState.value = DoctorHomeUiState(
                        summaryState = UiState.Error(
                            exception.message ?: "Veriler yüklenirken hata oluştu"
                        )
                    )
                }
        }
    }

    fun refresh() {
        loadSummary()
    }

    class Factory(
        private val getDoctorHomeSummaryUseCase: GetDoctorHomeSummaryUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DoctorHomeViewModel::class.java)) {
                return DoctorHomeViewModel(getDoctorHomeSummaryUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
