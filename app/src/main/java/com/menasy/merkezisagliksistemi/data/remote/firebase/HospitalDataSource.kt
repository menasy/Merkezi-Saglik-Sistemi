package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.model.District
import com.menasy.merkezisagliksistemi.data.model.Hospital
import kotlinx.coroutines.tasks.await

class HospitalDataSource(
    private val firestore: FirebaseFirestore
) {

    suspend fun getDistrictsByCityId(cityId: String): Result<List<District>> {
        return try {
            val districts = firestore.collection(DISTRICTS_COLLECTION)
                .whereEqualTo("cityId", cityId)
                .get()
                .await()
                .toObjects(District::class.java)
                .sortedBy { it.name }

            Result.success(districts)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getHospitals(
        cityId: String,
        districtId: String?,
        branchId: String?
    ): Result<List<Hospital>> {
        return try {
            var query = firestore.collection(HOSPITALS_COLLECTION)
                .whereEqualTo("cityId", cityId)

            if (!districtId.isNullOrBlank()) {
                query = query.whereEqualTo("districtId", districtId)
            }

            val hospitals = query.get()
                .await()
                .toObjects(Hospital::class.java)

            if (branchId.isNullOrBlank()) {
                return Result.success(hospitals.sortedBy { it.name })
            }

            val hospitalIdsWithBranch = firestore.collection(DOCTORS_COLLECTION)
                .whereEqualTo("branchId", branchId)
                .get()
                .await()
                .documents
                .mapNotNull { document -> document.getString("hospitalId") }
                .toSet()

            val filteredHospitals = hospitals
                .filter { hospital -> hospital.id in hospitalIdsWithBranch }
                .sortedBy { it.name }

            Result.success(filteredHospitals)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private companion object {
        const val DISTRICTS_COLLECTION = "districts"
        const val HOSPITALS_COLLECTION = "hospitals"
        const val DOCTORS_COLLECTION = "doctors"
    }
}
