package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.City
import com.menasy.merkezisagliksistemi.data.repository.CityRepository

class GetCitiesUseCase(
    private val cityRepository: CityRepository
) {
    suspend operator fun invoke(): Result<List<City>> {
        return cityRepository.getCities()
    }
}
