package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.District
import com.menasy.merkezisagliksistemi.data.repository.DistrictRepository

class GetDistrictsByCityUseCase(
    private val districtRepository: DistrictRepository
) {
    operator fun invoke(cityId: String): List<District> {
        return districtRepository.getDistrictsByCityId(cityId)
    }
}
