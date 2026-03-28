package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Hospital
import com.menasy.merkezisagliksistemi.data.repository.HospitalRepository

class GetHospitalsByDistrictUseCase(
    private val hospitalRepository: HospitalRepository
) {
    suspend operator fun invoke(
        cityId: String,
        districtId: String?
    ): Result<List<Hospital>> {
        return hospitalRepository.getHospitals(
            cityId = cityId,
            districtId = districtId
        )
    }
}
