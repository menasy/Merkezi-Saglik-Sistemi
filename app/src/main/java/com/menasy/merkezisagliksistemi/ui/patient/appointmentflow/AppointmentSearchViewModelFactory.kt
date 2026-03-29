package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menasy.merkezisagliksistemi.di.ServiceLocator

class AppointmentSearchViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppointmentSearchViewModel::class.java)) {
            return AppointmentSearchViewModel(
                getCitiesUseCase = ServiceLocator.provideGetCitiesUseCase(),
                getDistrictsByCityUseCase = ServiceLocator.provideGetDistrictsByCityUseCase(),
                getHospitalsByDistrictUseCase = ServiceLocator.provideGetHospitalsByDistrictUseCase(),
                getBranchesUseCase = ServiceLocator.provideGetBranchesUseCase(),
                getDoctorsUseCase = ServiceLocator.provideGetDoctorsUseCase()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
