package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.City
import com.menasy.merkezisagliksistemi.data.remote.firebase.CityDataSource

class CityRepository(
    private val cityDataSource: CityDataSource
) {
    suspend fun getCities(): Result<List<City>> {
        return cityDataSource.getCities()
    }
}
