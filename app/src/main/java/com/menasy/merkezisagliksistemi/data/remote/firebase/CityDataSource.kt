package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.model.City
import kotlinx.coroutines.tasks.await

class CityDataSource(
    private val firestore: FirebaseFirestore
) {

    suspend fun getCities(): Result<List<City>> {
        return try {
            val cities = firestore.collection(CITIES_COLLECTION)
                .get()
                .await()
                .toObjects(City::class.java)
                .sortedBy { it.name }

            Result.success(cities)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private companion object {
        const val CITIES_COLLECTION = "cities"
    }
}
