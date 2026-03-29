package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.data.remote.local.DoctorDataSource

class DoctorRepository(
    private val doctorDataSource: DoctorDataSource
) {
    fun getDoctors(hospitalId: String?, branchId: String?): List<Doctor> {
        return doctorDataSource.getDoctors(
            hospitalId = hospitalId,
            branchId = branchId
        )
    }

    fun getDoctorsByHospitalIds(hospitalIds: List<String>, branchId: String?): List<Doctor> {
        return doctorDataSource.getDoctorsByHospitalIds(
            hospitalIds = hospitalIds,
            branchId = branchId
        )
    }

    fun getDoctorById(doctorId: String): Doctor? {
        return doctorDataSource.getDoctorById(doctorId)
    }

    /**
     * Firebase Auth UID ile doktor profilini bulur.
     *
     * @param userId Firebase Auth UID
     * @return Eşleşen doktor profili veya null
     */
    fun getDoctorByUserId(userId: String): Doctor? {
        return doctorDataSource.getDoctorByUserId(userId)
    }
}
