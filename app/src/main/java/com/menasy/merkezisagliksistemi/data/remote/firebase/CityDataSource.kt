package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.model.City
import com.menasy.merkezisagliksistemi.data.model.seedData.cities as seededCities
import kotlinx.coroutines.tasks.await

class CityDataSource(
    private val firestore: FirebaseFirestore
) {

    suspend fun getCities(): Result<List<City>> {
        return try {
            val snapshot = firestore.collection(CITIES_COLLECTION)
                .get()
                .await()

            val cities = snapshot.documents.mapNotNull { document ->
                document.toObject(City::class.java)?.let { city ->
                    // Use document ID if city.id is empty
                    val resolvedId = city.id.ifBlank { document.id }
                    city.copy(
                        id = resolvedId,
                        name = cityNameOverrides[resolvedId] ?: city.name
                    )
                }
            }.sortedBy { it.name }

            Result.success(cities)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private companion object {
        const val CITIES_COLLECTION = "cities"
        val cityNameOverrides: Map<String, String> = seededCities.associate { city ->
            city.id to city.name
        }
    }
}
