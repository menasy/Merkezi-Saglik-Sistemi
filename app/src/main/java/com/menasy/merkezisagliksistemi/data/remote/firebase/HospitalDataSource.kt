package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.model.District
import com.menasy.merkezisagliksistemi.data.model.Hospital
import com.menasy.merkezisagliksistemi.utils.toTurkishDisplayText
import kotlinx.coroutines.tasks.await

class HospitalDataSource(
    private val firestore: FirebaseFirestore
) {

    suspend fun getDistrictsByCityId(cityId: String): Result<List<District>> {
        return try {
            val snapshot = firestore.collection(DISTRICTS_COLLECTION)
                .whereEqualTo("cityId", cityId)
                .get()
                .await()

            val districts = snapshot.documents.mapNotNull { document ->
                document.toObject(District::class.java)?.let { district ->
                    // Use document ID if district.id is empty
                    val resolvedId = district.id.ifBlank { document.id }
                    district.copy(id = resolvedId)
                }
            }.sortedBy { it.name }

            Result.success(districts)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Gets hospitals filtered by city and optionally by district.
     * Branch filtering is NOT done here - it will be handled at doctor query level.
     * This approach prevents unnecessary Firestore queries to the doctors collection.
     */
    suspend fun getHospitals(
        cityId: String,
        districtId: String?
    ): Result<List<Hospital>> {
        return try {
            val hospitals = queryHospitalsByLocation(
                cityId = cityId,
                districtId = districtId
            )
            Result.success(hospitals.sortedBy { it.name })
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend fun queryHospitalsByLocation(
        cityId: String,
        districtId: String?
    ): List<Hospital> {
        var query = firestore.collection(HOSPITALS_COLLECTION)
            .whereEqualTo("cityId", cityId)

        if (!districtId.isNullOrBlank()) {
            query = query.whereEqualTo("districtId", districtId)
        }

        val snapshot = query.get().await()
        val hospitals = snapshot.documents.mapNotNull { document ->
            document.toObject(Hospital::class.java)?.let { hospital ->
                // Use document ID if hospital.id is empty
                val resolvedId = hospital.id.ifBlank { document.id }
                hospital.copy(
                    id = resolvedId,
                    name = toTurkishDisplayText(hospital.name)
                )
            }
        }

        if (hospitals.isNotEmpty() || districtId.isNullOrBlank()) {
            return hospitals
        }

        // Fallback: if no hospitals found with district filter, try city-only
        val fallbackSnapshot = firestore.collection(HOSPITALS_COLLECTION)
            .whereEqualTo("cityId", cityId)
            .get()
            .await()

        return fallbackSnapshot.documents.mapNotNull { document ->
            document.toObject(Hospital::class.java)?.let { hospital ->
                val resolvedId = hospital.id.ifBlank { document.id }
                hospital.copy(
                    id = resolvedId,
                    name = toTurkishDisplayText(hospital.name)
                )
            }
        }
    }

    private companion object {
        const val DISTRICTS_COLLECTION = "districts"
        const val HOSPITALS_COLLECTION = "hospitals"
    }
}
