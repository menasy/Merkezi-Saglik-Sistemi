package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository

class GetDoctorsUseCase(
    private val doctorRepository: DoctorRepository
) {
    /**
     * Gets doctors for a specific hospital.
     * @param hospitalId Required - if null, returns empty list (no Firestore call)
     * @param branchId Optional - filters by branch if provided
     */
    suspend operator fun invoke(
        hospitalId: String?,
        branchId: String?
    ): Result<List<Doctor>> {
        return doctorRepository.getDoctors(
            hospitalId = hospitalId,
            branchId = branchId
        )
    }

    /**
     * Gets doctors from multiple hospitals.
     * Used for search results when no specific hospital is selected.
     * @param hospitalIds List of hospital IDs to search
     * @param branchId Optional - filters by branch if provided
     */
    suspend fun byHospitalIds(
        hospitalIds: List<String>,
        branchId: String?
    ): Result<List<Doctor>> {
        return doctorRepository.getDoctorsByHospitalIds(
            hospitalIds = hospitalIds,
            branchId = branchId
        )
    }
}
