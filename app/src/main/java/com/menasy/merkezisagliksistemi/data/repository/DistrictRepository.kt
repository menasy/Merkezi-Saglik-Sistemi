package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.District
import com.menasy.merkezisagliksistemi.data.remote.local.DistrictDataSource

class DistrictRepository(
    private val districtDataSource: DistrictDataSource
) {
    fun getDistrictsByCityId(cityId: String): List<District> {
        return districtDataSource.getDistrictsByCityId(cityId)
    }

    fun getDistrictById(districtId: String): District? {
        return districtDataSource.getDistrictById(districtId)
    }
}
