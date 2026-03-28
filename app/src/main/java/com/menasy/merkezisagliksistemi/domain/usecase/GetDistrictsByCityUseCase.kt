package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.District
import com.menasy.merkezisagliksistemi.data.repository.HospitalRepository

class GetDistrictsByCityUseCase(
    private val hospitalRepository: HospitalRepository
) {
    suspend operator fun invoke(cityId: String): Result<List<District>> {
        return hospitalRepository.getDistrictsByCityId(cityId)
    }
}
