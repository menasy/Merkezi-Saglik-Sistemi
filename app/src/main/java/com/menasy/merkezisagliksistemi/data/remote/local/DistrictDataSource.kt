package com.menasy.merkezisagliksistemi.data.remote.local

import com.menasy.merkezisagliksistemi.data.model.District
import com.menasy.merkezisagliksistemi.data.model.seedData.districts as seededDistricts

class DistrictDataSource {

    fun getDistrictsByCityId(cityId: String): List<District> {
        return districtsByCityId[cityId]?.sortedBy { it.name } ?: emptyList()
    }

    fun getDistrictById(districtId: String): District? {
        return districtsById[districtId]
    }

    private companion object {
        val districtsByCityId: Map<String, List<District>> = seededDistricts.groupBy { it.cityId }
        val districtsById: Map<String, District> = seededDistricts.associateBy { it.id }
    }
}
