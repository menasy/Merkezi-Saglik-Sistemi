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

    /**
     * Gets doctors for a specific hospital.
     * @param hospitalId Required - if null, returns empty list
     * @param branchId Optional - filters by branch if provided
     */
    suspend fun getDoctors(
        hospitalId: String?,
        branchId: String?
    ): Result<List<Doctor>> {
        return doctorDataSource.getDoctors(
            hospitalId = hospitalId,
            branchId = branchId
        )
    }

    /**
     * Gets doctors from multiple hospitals.
     * Used for search results when no specific hospital is selected.
     */
    suspend fun getDoctorsByHospitalIds(
        hospitalIds: List<String>,
        branchId: String?
    ): Result<List<Doctor>> {
        return doctorDataSource.getDoctorsByHospitalIds(
            hospitalIds = hospitalIds,
            branchId = branchId
        )
    }
}
