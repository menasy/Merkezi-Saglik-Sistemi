package com.menasy.merkezisagliksistemi.data.remote.local

import com.menasy.merkezisagliksistemi.data.model.City
import com.menasy.merkezisagliksistemi.data.model.seedData.cities as seededCities

class CityDataSource {

    fun getCities(): List<City> {
        return seededCities.sortedBy { it.name }
    }

    fun getCityById(cityId: String): City? {
        return citiesById[cityId]
    }

    private companion object {
        val citiesById: Map<String, City> = seededCities.associateBy { it.id }
    }
}
