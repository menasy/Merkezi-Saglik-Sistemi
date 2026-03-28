package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.City
import com.menasy.merkezisagliksistemi.data.repository.CityRepository

class GetCitiesUseCase(
    private val cityRepository: CityRepository
) {
    operator fun invoke(): List<City> {
        return cityRepository.getCities()
    }
}
