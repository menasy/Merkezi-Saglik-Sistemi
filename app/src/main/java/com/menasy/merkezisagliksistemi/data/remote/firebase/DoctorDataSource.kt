package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.model.Branch
import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.data.model.seedData.branches as seededBranches
import com.menasy.merkezisagliksistemi.utils.toTurkishDisplayText
import kotlinx.coroutines.tasks.await
import java.text.Normalizer
import java.util.Locale

class DoctorDataSource(
    private val firestore: FirebaseFirestore
) {

    suspend fun getBranches(): Result<List<Branch>> {
        return try {
            val firestoreBranches = firestore.collection(BRANCHES_COLLECTION)
                .get()
                .await()
                .toObjects(Branch::class.java)

            if (firestoreBranches.isEmpty()) {
                return Result.success(seedBranchList.sortedBy { it.name })
            }

            val deduplicatedBranches = linkedMapOf<String, Branch>()
            firestoreBranches.forEach { rawBranch ->
                val normalizedName = normalizeForComparison(rawBranch.name)
                val canonicalBranch = seedBranchById[rawBranch.id] ?: seedBranchByNormalizedName[normalizedName]
                if (!isAppointmentSuitable(rawBranch.id, normalizedName, canonicalBranch != null)) {
                    return@forEach
                }

                val displayName = canonicalBranch?.name ?: toTurkishDisplayText(rawBranch.name)
                val candidate = Branch(id = rawBranch.id, name = displayName)
                val existing = deduplicatedBranches[normalizedName]
                if (existing == null || isBetterBranchCandidate(candidate.id, existing.id)) {
                    deduplicatedBranches[normalizedName] = candidate
                }
            }

            val result = deduplicatedBranches.values
                .sortedBy { it.name }

            if (result.isNotEmpty()) {
                Result.success(result)
            } else {
                Result.success(seedBranchList.sortedBy { it.name })
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Gets doctors filtered by the given criteria.
     * IMPORTANT: hospitalId is required for fetching doctors.
     * If hospitalId is null (user selected "Fark etmez"), returns empty list.
     * This prevents fetching all doctors unnecessarily.
     */
    suspend fun getDoctors(
        hospitalId: String?,
        branchId: String?
    ): Result<List<Doctor>> {
        return try {
            if (hospitalId.isNullOrBlank()) {
                return Result.success(emptyList())
            }

            val doctors = queryDoctorsByHospital(hospitalId, branchId)
            Result.success(doctors.map(::normalizeDoctorDisplay).sortedBy { it.fullName })
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Gets doctors from multiple hospitals.
     * Used for search results when no specific hospital is selected.
     * Uses Firestore whereIn query (max 30 hospitals per batch).
     */
    suspend fun getDoctorsByHospitalIds(
        hospitalIds: List<String>,
        branchId: String?
    ): Result<List<Doctor>> {
        return try {
            if (hospitalIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val aliasBranchIds = if (!branchId.isNullOrBlank()) {
                resolveBranchAliasIdsLocally(branchId)
            } else {
                emptySet()
            }

            val allDoctors = mutableListOf<Doctor>()
            val batches = hospitalIds.take(MAX_HOSPITAL_BATCH_SIZE).chunked(FIRESTORE_WHERE_IN_LIMIT)

            for (batch in batches) {
                val query = firestore.collection(DOCTORS_COLLECTION)
                    .whereIn("hospitalId", batch)

                val doctors = query.get().await().toObjects(Doctor::class.java)
                allDoctors.addAll(doctors)
            }

            // If no doctors found, return empty
            if (allDoctors.isEmpty()) {
                return Result.success(emptyList())
            }

            // Apply branch filter if specified
            val finalDoctors = if (aliasBranchIds.isNotEmpty()) {
                allDoctors.filter { it.branchId in aliasBranchIds }
            } else {
                allDoctors
            }

            Result.success(finalDoctors.map(::normalizeDoctorDisplay).sortedBy { it.fullName })
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend fun queryDoctorsByHospital(
        hospitalId: String,
        branchId: String?
    ): List<Doctor> {
        // Single query by hospitalId only - avoids composite index requirement
        val query = firestore.collection(DOCTORS_COLLECTION)
            .whereEqualTo("hospitalId", hospitalId)

        val allDoctors = query.get().await().toObjects(Doctor::class.java)

        // If no doctors found, return empty immediately
        if (allDoctors.isEmpty()) {
            return emptyList()
        }

        // If branchId specified, filter by branch aliases
        if (!branchId.isNullOrBlank()) {
            val aliasBranchIds = resolveBranchAliasIdsLocally(branchId)
            return allDoctors.filter { it.branchId in aliasBranchIds }
        }

        // No branch filter - return all doctors from this hospital
        return allDoctors
    }

    /**
     * Resolves branch alias IDs using local seed data.
     * No Firestore call needed - uses normalized name matching from seed data.
     */
    private fun resolveBranchAliasIdsLocally(branchId: String): Set<String> {
        val seedBranch = seedBranchById[branchId]
        if (seedBranch != null) {
            val normalizedName = normalizeForComparison(seedBranch.name)
            val aliases = seedBranchList
                .filter { normalizeForComparison(it.name) == normalizedName }
                .map { it.id }
                .toMutableSet()
            aliases.add(branchId)
            return aliases
        }
        return setOf(branchId)
    }

    private fun isAppointmentSuitable(
        branchId: String,
        normalizedName: String,
        hasCanonicalMatch: Boolean
    ): Boolean {
        if (branchId in seedBranchById) return true
        if (hasCanonicalMatch) return true
        return nonAppointmentKeywords.none { keyword -> keyword in normalizedName }
    }

    private fun isBetterBranchCandidate(
        candidateId: String,
        existingId: String
    ): Boolean {
        val candidateScore = when {
            candidateId in seedBranchById -> 0
            candidateId.matches(Regex(".*_\\d+$")) -> 2
            else -> 1
        }
        val existingScore = when {
            existingId in seedBranchById -> 0
            existingId.matches(Regex(".*_\\d+$")) -> 2
            else -> 1
        }

        return candidateScore < existingScore
    }

    private fun normalizeForComparison(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace("ı", "i")
            .replace("İ", "i")
            .lowercase(Locale.ROOT)
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun normalizeDoctorDisplay(doctor: Doctor): Doctor {
        return doctor.copy(
            fullName = toTurkishDisplayText(doctor.fullName)
        )
    }

    private companion object {
        const val BRANCHES_COLLECTION = "branches"
        const val DOCTORS_COLLECTION = "doctors"
        const val FIRESTORE_WHERE_IN_LIMIT = 30
        const val MAX_HOSPITAL_BATCH_SIZE = 60

        val seedBranchList = seededBranches
        val seedBranchById: Map<String, Branch> = seededBranches.associateBy { branch -> branch.id }
        val seedBranchByNormalizedName: Map<String, Branch> = seededBranches.associateBy { branch ->
            Normalizer.normalize(branch.name, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .replace("ı", "i")
                .replace("İ", "i")
                .lowercase(Locale.ROOT)
                .replace(Regex("\\([^)]*\\)"), " ")
                .replace(Regex("[^a-z0-9]+"), " ")
                .trim()
                .replace(Regex("\\s+"), " ")
        }

        val nonAppointmentKeywords = listOf(
            "yogun bakim",
            "asi uygulama",
            "pandemi",
            "saglik kurulu",
            "toplum sagligi",
            "geleneksel tamamlayici",
            "amatem",
            "cematem",
            "bagimlilik",
            "sigarayi biraktirma",
            "palyatif bakim",
            "askeri",
            "hava ve uzay hekimligi",
            "cevre sagligi",
            "epidemiyoloji",
            "fizyoloji",
            "anatomi",
            "histoloji",
            "sitopatoloji",
            "temel immunoloji",
            "harp cerrahisi",
            "genel klinik",
            "tani merkezi",
            "kurulu"
        )
    }
}
