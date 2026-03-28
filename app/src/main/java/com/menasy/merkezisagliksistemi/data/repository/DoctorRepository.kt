package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.Branch
import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.data.remote.firebase.DoctorDataSource

class DoctorRepository(
    private val doctorDataSource: DoctorDataSource
) {
    suspend fun getBranches(): Result<List<Branch>> {
        return doctorDataSource.getBranches()
    }

    suspend fun getDoctors(
        cityId: String,
        branchId: String,
        districtId: String?,
        hospitalId: String?
    ): Result<List<Doctor>> {
        return doctorDataSource.getDoctors(
            cityId = cityId,
            branchId = branchId,
            districtId = districtId,
            hospitalId = hospitalId
        )
    }
}
