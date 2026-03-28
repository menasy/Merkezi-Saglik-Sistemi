package com.menasy.merkezisagliksistemi.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.InitializeReferenceDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashNavigationState {
    data object Loading : SplashNavigationState()
    data object GoToLogin : SplashNavigationState()
    data object GoToPatientHome : SplashNavigationState()
    data object GoToDoctorHome : SplashNavigationState()
    data class Error(val message: String) : SplashNavigationState()
}

class SplashViewModel(
    private val initializeReferenceDataUseCase: InitializeReferenceDataUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _navigationState =
        MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    fun initializeApp() {
        viewModelScope.launch {
            val seedResult = initializeReferenceDataUseCase()

            if (seedResult.isFailure) {
                _navigationState.value = SplashNavigationState.Error(
                    seedResult.exceptionOrNull()?.message ?: "Başlangıç verileri yüklenemedi"
                )
                return@launch
            }

            checkSession()
        }
    }

    private suspend fun checkSession() {
        val currentUserId = getCurrentUserUseCase.getCurrentUserId()
        if (currentUserId == null) {
            _navigationState.value = SplashNavigationState.GoToLogin
            return
        }

        val roleResult = getCurrentUserUseCase.getCurrentUserRole()

        _navigationState.value = roleResult.fold(
            onSuccess = { role ->
                when (role) {
                    "patient" -> SplashNavigationState.GoToPatientHome
                    "doctor" -> SplashNavigationState.GoToDoctorHome
                    else -> SplashNavigationState.Error("Geçersiz kullanıcı rolü")
                }
            },
            onFailure = { exception ->
                SplashNavigationState.Error(
                    exception.message ?: "Oturum kontrolü sırasında hata oluştu"
                )
            }
        )
    }
}
