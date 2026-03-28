package com.menasy.merkezisagliksistemi.ui.patient.appointment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DoctorAvailabilityViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DoctorAvailabilityViewModel::class.java)) {
            return DoctorAvailabilityViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
