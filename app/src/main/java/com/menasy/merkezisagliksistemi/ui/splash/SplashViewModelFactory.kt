package com.menasy.merkezisagliksistemi.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menasy.merkezisagliksistemi.di.ServiceLocator

class SplashViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            return SplashViewModel(
                getCurrentUserUseCase = ServiceLocator.provideGetCurrentUserUseCase()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}