package com.menasy.merkezisagliksistemi.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menasy.merkezisagliksistemi.di.ServiceLocator

class LoginViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                loginUserUseCase = ServiceLocator.provideLoginUserUseCase()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}