package com.menasy.merkezisagliksistemi.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.domain.usecase.RegisterPatientUseCase
import com.menasy.merkezisagliksistemi.ui.common.base.BaseViewModel
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.OperationType
import com.menasy.merkezisagliksistemi.ui.common.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerPatientUseCase: RegisterPatientUseCase
) : BaseViewModel() {

    private val _registerState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val registerState: StateFlow<UiState<Unit>> = _registerState.asStateFlow()

    fun registerPatient(
        fullName: String,
        email: String,
        password: String,
        tcNo: String,
        birthDate: String,
        gender: String
    ) {
        if (
            fullName.isBlank() ||
            email.isBlank() ||
            password.isBlank() ||
            tcNo.isBlank() ||
            birthDate.isBlank() ||
            gender.isBlank()
        ) {
            publishError(AppErrorReason.REQUIRED_FIELDS)
            return
        }

        if (tcNo.length != 11) {
            publishError(AppErrorReason.INVALID_TC_NO)
            return
        }

        viewModelScope.launch {
            _registerState.value = UiState.Loading

            val result = registerPatientUseCase(
                fullName = fullName,
                email = email,
                password = password,
                tcNo = tcNo,
                birthDate = birthDate,
                gender = gender
            )

            result.fold(
                onSuccess = {
                    _registerState.value = UiState.Success(Unit)
                    publishSuccess(
                        title = "Kayıt Başarılı",
                        description = "Hesabınız oluşturuldu. Giriş yapabilirsiniz."
                    )
                },
                onFailure = { exception ->
                    _registerState.value = UiState.Empty
                    publishError(
                        throwable = exception,
                        operationType = OperationType.REGISTER
                    )
                }
            )
        }
    }

    fun clearState() {
        _registerState.value = UiState.Empty
    }
}
