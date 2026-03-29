package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menasy.merkezisagliksistemi.di.ServiceLocator

class DoctorAvailabilityViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DoctorAvailabilityViewModel::class.java)) {
            return DoctorAvailabilityViewModel(
                observeOccupiedTimesUseCase = ServiceLocator.provideObserveOccupiedTimesUseCase()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
