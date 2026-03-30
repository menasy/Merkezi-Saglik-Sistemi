package com.menasy.merkezisagliksistemi.ui.splash

import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.di.SessionCache
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
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
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : BaseViewModel() {

    private val _navigationState =
        MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    fun initializeApp() {
        viewModelScope.launch {
            checkSession()
        }
    }

    private suspend fun checkSession() {
        val currentUserId = getCurrentUserUseCase.getCurrentUserId()
        if (currentUserId == null) {
            _navigationState.value = SplashNavigationState.GoToLogin
            return
        }

        if (SessionCache.isPopulated && SessionCache.userId == currentUserId) {
            navigateByRole(SessionCache.role)
            return
        }

        val roleResult = getCurrentUserUseCase.getCurrentUserRole()
        val fullNameResult = getCurrentUserUseCase.getCurrentUserFullName()

        roleResult.fold(
            onSuccess = { role ->
                val fullName = fullNameResult.getOrElse { "" }
                val isSessionReady = populateSessionCache(
                    userId = currentUserId,
                    role = role,
                    fullName = fullName
                )
                if (isSessionReady) {
                    navigateByRole(role)
                } else {
                    _navigationState.value = SplashNavigationState.GoToLogin
                }
            },
            onFailure = { exception ->
                publishError(
                    throwable = exception,
                    operationType = OperationType.SESSION
                )
                _navigationState.value = SplashNavigationState.GoToLogin
            }
        )
    }

    private fun populateSessionCache(userId: String, role: String, fullName: String): Boolean {
        when (role) {
            "doctor" -> {
                val doctorId = getCurrentUserUseCase.getDoctorIdByUserId(userId)
                if (doctorId != null) {
                    SessionCache.populateDoctor(
                        userId = userId,
                        role = role,
                        fullName = fullName,
                        doctorId = doctorId
                    )
                    return true
                } else {
                    publishError(
                        reason = if (getCurrentUserUseCase.hasDoctorProfileByUserId(userId)) {
                            AppErrorReason.DOCTOR_LOGIN_NOT_ALLOWED
                        } else {
                            AppErrorReason.DOCTOR_UID_MISMATCH
                        }
                    )
                    SessionCache.clear()
                    return false
                }
            }
            "patient" -> {
                SessionCache.populate(
                    userId = userId,
                    role = role,
                    fullName = fullName
                )
                return true
            }
            else -> {
                publishError(AppErrorReason.INVALID_USER_ROLE)
                SessionCache.clear()
                return false
            }
        }
    }

    private fun navigateByRole(role: String?) {
        _navigationState.value = when (role) {
            "patient" -> SplashNavigationState.GoToPatientHome
            "doctor" -> {
                // Doktor profili kontrolü
                if (SessionCache.doctorId != null) {
                    SplashNavigationState.GoToDoctorHome
                } else {
                    publishError(AppErrorReason.DOCTOR_PROFILE_NOT_FOUND)
                    SplashNavigationState.GoToLogin
                }
            }
            else -> {
                publishError(AppErrorReason.INVALID_USER_ROLE)
                SplashNavigationState.GoToLogin
            }
        }
    }
}
