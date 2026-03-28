package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.model.Branch
import com.menasy.merkezisagliksistemi.data.model.Doctor
import kotlinx.coroutines.tasks.await

class DoctorDataSource(
    private val firestore: FirebaseFirestore
) {

    suspend fun getBranches(): Result<List<Branch>> {
        return try {
            val branches = firestore.collection(BRANCHES_COLLECTION)
                .get()
                .await()
                .toObjects(Branch::class.java)
                .sortedBy { it.name }

            Result.success(branches)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getDoctors(
        cityId: String,
        branchId: String,
        districtId: String?,
        hospitalId: String?
    ): Result<List<Doctor>> {
        return try {
            var doctorQuery = firestore.collection(DOCTORS_COLLECTION)
                .whereEqualTo("branchId", branchId)

            if (!hospitalId.isNullOrBlank()) {
                doctorQuery = doctorQuery.whereEqualTo("hospitalId", hospitalId)
            }

            val doctors = doctorQuery.get()
                .await()
                .toObjects(Doctor::class.java)

            if (!hospitalId.isNullOrBlank()) {
                return Result.success(doctors.sortedBy { it.fullName })
            }

            var hospitalQuery = firestore.collection(HOSPITALS_COLLECTION)
                .whereEqualTo("cityId", cityId)

            if (!districtId.isNullOrBlank()) {
                hospitalQuery = hospitalQuery.whereEqualTo("districtId", districtId)
            }

            val allowedHospitalIds = hospitalQuery.get()
                .await()
                .documents
                .map { document -> document.id }
                .toSet()

            val filteredDoctors = doctors
                .filter { doctor -> doctor.hospitalId in allowedHospitalIds }
                .sortedBy { it.fullName }

            Result.success(filteredDoctors)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private companion object {
        const val BRANCHES_COLLECTION = "branches"
        const val DOCTORS_COLLECTION = "doctors"
        const val HOSPITALS_COLLECTION = "hospitals"
    }
}
