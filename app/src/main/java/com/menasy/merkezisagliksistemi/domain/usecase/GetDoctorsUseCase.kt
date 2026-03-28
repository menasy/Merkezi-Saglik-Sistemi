package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository

class GetDoctorsUseCase(
    private val doctorRepository: DoctorRepository
) {
    suspend operator fun invoke(
        cityId: String,
        branchId: String,
        districtId: String?,
        hospitalId: String?
    ): Result<List<Doctor>> {
        return doctorRepository.getDoctors(
            cityId = cityId,
            branchId = branchId,
            districtId = districtId,
            hospitalId = hospitalId
        )
    }
}
