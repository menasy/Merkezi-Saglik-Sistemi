package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.model.seedData.branches
import com.menasy.merkezisagliksistemi.data.model.seedData.cities
import com.menasy.merkezisagliksistemi.data.model.seedData.doctors
import com.menasy.merkezisagliksistemi.data.model.seedData.hospitals
import kotlinx.coroutines.tasks.await

class ReferenceDataSource(
    private val firestore: FirebaseFirestore
) {

    suspend fun ensureReferenceDataInitialized(): Result<Unit> {
        return try {
            val currentState = readCollectionState()

            if (currentState.allCollectionsExist()) {
                Result.success(Unit)
            } else {
                seedMissingCollections(currentState)
                Result.success(Unit)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend fun readCollectionState(): CollectionState {
        val citiesExist = firestore.collection(CITIES_COLLECTION)
            .limit(1)
            .get()
            .await()
            .isEmpty
            .not()

        val hospitalsExist = firestore.collection(HOSPITALS_COLLECTION)
            .limit(1)
            .get()
            .await()
            .isEmpty
            .not()

        val branchesExist = firestore.collection(BRANCHES_COLLECTION)
            .limit(1)
            .get()
            .await()
            .isEmpty
            .not()

        val doctorsExist = firestore.collection(DOCTORS_COLLECTION)
            .limit(1)
            .get()
            .await()
            .isEmpty
            .not()

        return CollectionState(
            citiesExist = citiesExist,
            hospitalsExist = hospitalsExist,
            branchesExist = branchesExist,
            doctorsExist = doctorsExist
        )
    }

    private suspend fun seedMissingCollections(state: CollectionState) {
        val batch = firestore.batch()

        if (!state.citiesExist) {
            cities.forEach { city ->
                batch.set(
                    firestore.collection(CITIES_COLLECTION).document(city.id),
                    city
                )
            }
        }

        if (!state.hospitalsExist) {
            hospitals.forEach { hospital ->
                batch.set(
                    firestore.collection(HOSPITALS_COLLECTION).document(hospital.id),
                    hospital
                )
            }
        }

        if (!state.branchesExist) {
            branches.forEach { branch ->
                batch.set(
                    firestore.collection(BRANCHES_COLLECTION).document(branch.id),
                    branch
                )
            }
        }

        if (!state.doctorsExist) {
            doctors.forEach { doctor ->
                batch.set(
                    firestore.collection(DOCTORS_COLLECTION).document(doctor.id),
                    doctor
                )
            }
        }

        batch.commit().await()
    }

    private data class CollectionState(
        val citiesExist: Boolean,
        val hospitalsExist: Boolean,
        val branchesExist: Boolean,
        val doctorsExist: Boolean
    ) {
        fun allCollectionsExist(): Boolean {
            return citiesExist && hospitalsExist && branchesExist && doctorsExist
        }
    }

    private companion object {
        const val CITIES_COLLECTION = "cities"
        const val HOSPITALS_COLLECTION = "hospitals"
        const val BRANCHES_COLLECTION = "branches"
        const val DOCTORS_COLLECTION = "doctors"
    }
}
