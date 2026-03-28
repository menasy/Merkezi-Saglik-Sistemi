package com.menasy.merkezisagliksistemi.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.data.model.LoginResult
import com.menasy.merkezisagliksistemi.domain.usecase.LoginUserUseCase
import com.menasy.merkezisagliksistemi.ui.common.base.BaseViewModel
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import com.menasy.merkezisagliksistemi.ui.common.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUserUseCase: LoginUserUseCase
) : BaseViewModel() {

    private val _loginState = MutableStateFlow<UiState<LoginResult>>(UiState.Empty)
    val loginState: StateFlow<UiState<LoginResult>> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            publishError(AppErrorReason.EMAIL_AND_PASSWORD_REQUIRED)
            return
        }

        viewModelScope.launch {
            _loginState.value = UiState.Loading

            val result = loginUserUseCase(email, password)

            result.fold(
                onSuccess = { loginResult ->
                    _loginState.value = UiState.Success(loginResult)
                    publishSuccess(
                        title = "Giriş Başarılı",
                        description = "Hesabınıza yönlendiriliyorsunuz."
                    )
                },
                onFailure = { exception ->
                    _loginState.value = UiState.Empty
                    publishError(
                        throwable = exception,
                        operationType = OperationType.LOGIN
                    )
                }
            )
        }
    }

    fun clearState() {
        _loginState.value = UiState.Empty
    }
}
