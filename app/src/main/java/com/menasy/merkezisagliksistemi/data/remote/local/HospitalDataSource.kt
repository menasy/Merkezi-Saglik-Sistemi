package com.menasy.merkezisagliksistemi.data.remote.local

import com.menasy.merkezisagliksistemi.data.model.Hospital
import com.menasy.merkezisagliksistemi.data.model.seedData.hospitals as seededHospitals

class HospitalDataSource {

    fun getHospitals(cityId: String, districtId: String?): List<Hospital> {
        val hospitalsByCityId = hospitalsByCityId[cityId] ?: emptyList()

        val filteredHospitals = if (!districtId.isNullOrBlank()) {
            hospitalsByCityId.filter { it.districtId == districtId }
        } else {
            hospitalsByCityId
        }

        return if (filteredHospitals.isNotEmpty() || districtId.isNullOrBlank()) {
            filteredHospitals.sortedBy { it.name }
        } else {
            hospitalsByCityId.sortedBy { it.name }
        }
    }

    fun getHospitalById(hospitalId: String): Hospital? {
        return hospitalsById[hospitalId]
    }

    fun getHospitalsByIds(hospitalIds: List<String>): List<Hospital> {
        return hospitalIds.mapNotNull { hospitalsById[it] }
    }

    private companion object {
        val hospitalsByCityId: Map<String, List<Hospital>> = seededHospitals.groupBy { it.cityId }
        val hospitalsById: Map<String, Hospital> = seededHospitals.associateBy { it.id }
    }
}
