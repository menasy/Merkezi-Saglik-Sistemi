package com.menasy.merkezisagliksistemi.data.remote.local

import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.data.model.seedData.branches as seededBranches
import com.menasy.merkezisagliksistemi.data.model.seedData.doctors as seededDoctors
import java.text.Normalizer
import java.util.Locale

class DoctorDataSource {

    fun getDoctors(hospitalId: String?, branchId: String?): List<Doctor> {
        if (hospitalId.isNullOrBlank()) {
            return emptyList()
        }

        val hospitalDoctors = doctorsByHospitalId[hospitalId] ?: return emptyList()

        return if (!branchId.isNullOrBlank()) {
            val aliasBranchIds = resolveBranchAliasIds(branchId)
            hospitalDoctors.filter { it.branchId in aliasBranchIds }
        } else {
            hospitalDoctors
        }.sortedBy { it.fullName }
    }

    fun getDoctorsByHospitalIds(hospitalIds: List<String>, branchId: String?): List<Doctor> {
        if (hospitalIds.isEmpty()) {
            return emptyList()
        }

        val aliasBranchIds = if (!branchId.isNullOrBlank()) {
            resolveBranchAliasIds(branchId)
        } else {
            emptySet()
        }

        val allDoctors = hospitalIds.flatMap { hospitalId ->
            doctorsByHospitalId[hospitalId] ?: emptyList()
        }

        val filteredDoctors = if (aliasBranchIds.isNotEmpty()) {
            allDoctors.filter { it.branchId in aliasBranchIds }
        } else {
            allDoctors
        }

        return filteredDoctors.sortedBy { it.fullName }
    }

    fun getDoctorById(doctorId: String): Doctor? {
        return doctorsById[doctorId]
    }

    /**
     * Firebase Auth UID ile doktor profilini bulur.
     *
     * @param userId Firebase Auth UID
     * @return Eşleşen doktor profili veya null
     */
    fun getDoctorByUserId(userId: String): Doctor? {
        return loginEnabledDoctorsByUserId[userId]
    }

    /**
     * Firebase Auth UID ile seed'de doktor profili var mı kontrol eder.
     * Login yetkisi kontrolü yapmaz.
     */
    fun hasDoctorProfileByUserId(userId: String): Boolean {
        return allDoctorsByUserId.containsKey(userId)
    }

    private fun resolveBranchAliasIds(branchId: String): Set<String> {
        val seedBranch = seedBranchById[branchId]
        if (seedBranch != null) {
            val normalizedName = normalizeForComparison(seedBranch.name)
            val aliases = seededBranches
                .filter { normalizeForComparison(it.name) == normalizedName }
                .map { it.id }
                .toMutableSet()
            aliases.add(branchId)
            return aliases
        }
        return setOf(branchId)
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

    private companion object {
        val doctorsByHospitalId: Map<String, List<Doctor>> = seededDoctors.groupBy { it.hospitalId }
        val doctorsById: Map<String, Doctor> = seededDoctors.associateBy { it.id }
        val seedBranchById = seededBranches.associateBy { it.id }

        // userId'si olan tüm doktorları index'le (canLogin true/false fark etmez)
        val allDoctorsByUserId: Map<String, Doctor> = seededDoctors
            .filter { !it.userId.isNullOrBlank() }
            .associateBy { it.userId!! }

        // Login yetkisi olan doktorları index'le
        val loginEnabledDoctorsByUserId: Map<String, Doctor> = seededDoctors
            .filter { !it.userId.isNullOrBlank() && it.canLogin }
            .associateBy { it.userId!! }
    }
}
