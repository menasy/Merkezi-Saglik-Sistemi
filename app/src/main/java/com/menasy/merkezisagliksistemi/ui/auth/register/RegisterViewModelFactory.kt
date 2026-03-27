package com.menasy.merkezisagliksistemi.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menasy.merkezisagliksistemi.di.ServiceLocator

class RegisterViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel(
                registerPatientUseCase = ServiceLocator.provideRegisterPatientUseCase()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}