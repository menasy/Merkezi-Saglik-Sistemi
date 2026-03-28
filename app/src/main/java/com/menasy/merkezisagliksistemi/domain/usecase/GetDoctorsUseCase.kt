package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository

class GetDoctorsUseCase(
    private val doctorRepository: DoctorRepository
) {
    operator fun invoke(hospitalId: String?, branchId: String?): List<Doctor> {
        return doctorRepository.getDoctors(
            hospitalId = hospitalId,
            branchId = branchId
        )
    }

    fun byHospitalIds(hospitalIds: List<String>, branchId: String?): List<Doctor> {
        return doctorRepository.getDoctorsByHospitalIds(
            hospitalIds = hospitalIds,
            branchId = branchId
        )
    }
}
