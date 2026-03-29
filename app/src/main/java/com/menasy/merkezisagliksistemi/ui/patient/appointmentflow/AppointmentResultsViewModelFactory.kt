package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menasy.merkezisagliksistemi.di.ServiceLocator

class AppointmentResultsViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppointmentResultsViewModel::class.java)) {
            return AppointmentResultsViewModel(
                getDoctorsUseCase = ServiceLocator.provideGetDoctorsUseCase(),
                getHospitalsByDistrictUseCase = ServiceLocator.provideGetHospitalsByDistrictUseCase(),
                getBranchesUseCase = ServiceLocator.provideGetBranchesUseCase()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
