package com.menasy.merkezisagliksistemi.ui.patient.appointment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menasy.merkezisagliksistemi.di.ServiceLocator

class AppointmentConfirmationViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppointmentConfirmationViewModel::class.java)) {
            return AppointmentConfirmationViewModel(
                getCurrentUserUseCase = ServiceLocator.provideGetCurrentUserUseCase(),
                createAppointmentUseCase = ServiceLocator.provideCreateAppointmentUseCase()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
