package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.Hospital
import com.menasy.merkezisagliksistemi.data.remote.local.HospitalDataSource

class HospitalRepository(
    private val hospitalDataSource: HospitalDataSource
) {
    fun getHospitals(cityId: String, districtId: String?): List<Hospital> {
        return hospitalDataSource.getHospitals(
            cityId = cityId,
            districtId = districtId
        )
    }

    fun getHospitalById(hospitalId: String): Hospital? {
        return hospitalDataSource.getHospitalById(hospitalId)
    }

    fun getHospitalsByIds(hospitalIds: List<String>): List<Hospital> {
        return hospitalDataSource.getHospitalsByIds(hospitalIds)
    }
}
