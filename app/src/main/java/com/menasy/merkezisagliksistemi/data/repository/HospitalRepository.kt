package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.District
import com.menasy.merkezisagliksistemi.data.model.Hospital
import com.menasy.merkezisagliksistemi.data.remote.firebase.HospitalDataSource

class HospitalRepository(
    private val hospitalDataSource: HospitalDataSource
) {
    suspend fun getDistrictsByCityId(cityId: String): Result<List<District>> {
        return hospitalDataSource.getDistrictsByCityId(cityId)
    }

    suspend fun getHospitals(
        cityId: String,
        districtId: String?,
        branchId: String?
    ): Result<List<Hospital>> {
        return hospitalDataSource.getHospitals(
            cityId = cityId,
            districtId = districtId,
            branchId = branchId
        )
    }
}
