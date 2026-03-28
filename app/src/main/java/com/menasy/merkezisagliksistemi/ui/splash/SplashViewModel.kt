package com.menasy.merkezisagliksistemi.ui.splash

import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.InitializeReferenceDataUseCase
import com.menasy.merkezisagliksistemi.ui.common.base.BaseViewModel
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashNavigationState {
    data object Loading : SplashNavigationState()
    data object GoToLogin : SplashNavigationState()
    data object GoToPatientHome : SplashNavigationState()
    data object GoToDoctorHome : SplashNavigationState()
}

class SplashViewModel(
    private val initializeReferenceDataUseCase: InitializeReferenceDataUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : BaseViewModel() {

    private val _navigationState =
        MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    fun initializeApp() {
        viewModelScope.launch {
            val seedResult = initializeReferenceDataUseCase()

            if (seedResult.isFailure) {
                publishError(
                    throwable = seedResult.exceptionOrNull(),
                    operationType = OperationType.FETCH_DATA
                )
                _navigationState.value = SplashNavigationState.GoToLogin
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
                    else -> {
                        publishError(AppErrorReason.INVALID_USER_ROLE)
                        SplashNavigationState.GoToLogin
                    }
                }
            },
            onFailure = { exception ->
                publishError(
                    throwable = exception,
                    operationType = OperationType.SESSION
                )
                SplashNavigationState.GoToLogin
            }
        )
    }
}
