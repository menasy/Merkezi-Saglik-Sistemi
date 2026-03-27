package com.menasy.merkezisagliksistemi.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.data.model.LoginResult
import com.menasy.merkezisagliksistemi.domain.usecase.LoginUserUseCase
import com.menasy.merkezisagliksistemi.ui.common.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUserUseCase: LoginUserUseCase
) : ViewModel() {

    private val _loginState = MutableStateFlow<UiState<LoginResult>>(UiState.Empty)
    val loginState: StateFlow<UiState<LoginResult>> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = UiState.Error("E-posta ve şifre boş bırakılamaz")
            return
        }

        viewModelScope.launch {
            _loginState.value = UiState.Loading

            val result = loginUserUseCase(email, password)

            _loginState.value = result.fold(
                onSuccess = { loginResult ->
                    UiState.Success(loginResult)
                },
                onFailure = { exception ->
                    UiState.Error(exception.message ?: "Giriş işlemi başarısız")
                }
            )
        }
    }

    fun clearState() {
        _loginState.value = UiState.Empty
    }
}