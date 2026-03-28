package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.City
import com.menasy.merkezisagliksistemi.data.remote.local.CityDataSource

class CityRepository(
    private val cityDataSource: CityDataSource
) {
    fun getCities(): List<City> {
        return cityDataSource.getCities()
    }

    fun getCityById(cityId: String): City? {
        return cityDataSource.getCityById(cityId)
    }
}
