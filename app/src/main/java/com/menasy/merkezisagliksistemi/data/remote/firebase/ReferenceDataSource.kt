package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.model.seedData.branches
import com.menasy.merkezisagliksistemi.data.model.seedData.cities
import com.menasy.merkezisagliksistemi.data.model.seedData.doctors
import com.menasy.merkezisagliksistemi.data.model.seedData.districts
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

        val districtsExist = firestore.collection(DISTRICTS_COLLECTION)
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
            districtsExist = districtsExist,
            hospitalsExist = hospitalsExist,
            branchesExist = branchesExist,
            doctorsExist = doctorsExist
        )
    }

    private suspend fun seedMissingCollections(state: CollectionState) {
        if (!state.citiesExist) {
            writeCollectionInBatches(CITIES_COLLECTION, cities) { city -> city.id }
        }

        if (!state.districtsExist) {
            writeCollectionInBatches(DISTRICTS_COLLECTION, districts) { district -> district.id }
        }

        if (!state.hospitalsExist) {
            writeCollectionInBatches(HOSPITALS_COLLECTION, hospitals) { hospital -> hospital.id }
        }

        if (!state.branchesExist) {
            writeCollectionInBatches(BRANCHES_COLLECTION, branches) { branch -> branch.id }
        }

        if (!state.doctorsExist) {
            writeCollectionInBatches(DOCTORS_COLLECTION, doctors) { doctor -> doctor.id }
        }
    }

    private suspend fun <T : Any> writeCollectionInBatches(
        collectionName: String,
        items: List<T>,
        documentIdProvider: (T) -> String
    ) {
        items.chunked(MAX_BATCH_WRITE_SIZE).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { item ->
                batch.set(
                    firestore.collection(collectionName).document(documentIdProvider(item)),
                    item
                )
            }
            batch.commit().await()
        }
    }

    private data class CollectionState(
        val citiesExist: Boolean,
        val districtsExist: Boolean,
        val hospitalsExist: Boolean,
        val branchesExist: Boolean,
        val doctorsExist: Boolean
    ) {
        fun allCollectionsExist(): Boolean {
            return citiesExist && districtsExist && hospitalsExist && branchesExist && doctorsExist
        }
    }

    private companion object {
        const val CITIES_COLLECTION = "cities"
        const val DISTRICTS_COLLECTION = "districts"
        const val HOSPITALS_COLLECTION = "hospitals"
        const val BRANCHES_COLLECTION = "branches"
        const val DOCTORS_COLLECTION = "doctors"
        const val MAX_BATCH_WRITE_SIZE = 450
    }
}
