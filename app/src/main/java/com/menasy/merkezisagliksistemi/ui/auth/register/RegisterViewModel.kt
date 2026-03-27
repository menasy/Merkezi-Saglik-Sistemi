package com.menasy.merkezisagliksistemi.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menasy.merkezisagliksistemi.domain.usecase.RegisterPatientUseCase
import com.menasy.merkezisagliksistemi.ui.common.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerPatientUseCase: RegisterPatientUseCase
) : ViewModel() {

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
            _registerState.value = UiState.Error("Tüm alanlar doldurulmalıdır")
            return
        }

        if (tcNo.length != 11) {
            _registerState.value = UiState.Error("TC kimlik numarası 11 haneli olmalıdır")
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

            _registerState.value = result.fold(
                onSuccess = {
                    UiState.Success(Unit)
                },
                onFailure = { exception ->
                    UiState.Error(exception.message ?: "Kayıt işlemi başarısız")
                }
            )
        }
    }

    fun clearState() {
        _registerState.value = UiState.Empty
    }
}