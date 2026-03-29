package com.menasy.merkezisagliksistemi

import com.menasy.merkezisagliksistemi.data.model.seedData.branches
import com.menasy.merkezisagliksistemi.data.model.seedData.doctors
import com.menasy.merkezisagliksistemi.data.model.seedData.hospitals
import org.junit.Assert.*
import org.junit.Test

class DoctorSeedDataTest {

    @Test
    fun `all doctors should have valid hospitalId`() {
        val hospitalIds = hospitals.map { it.id }.toSet()
        val invalidDoctors = doctors.filter { it.hospitalId !in hospitalIds }
        assertTrue(
            "Found ${invalidDoctors.size} doctors with invalid hospitalId: ${invalidDoctors.take(5).map { it.hospitalId }}",
            invalidDoctors.isEmpty()
        )
    }

    @Test
    fun `all doctors should have valid branchId`() {
        val branchIds = branches.map { it.id }.toSet()
        val invalidDoctors = doctors.filter { it.branchId !in branchIds }
        assertTrue(
            "Found ${invalidDoctors.size} doctors with invalid branchId: ${invalidDoctors.take(5).map { it.branchId }}",
            invalidDoctors.isEmpty()
        )
    }

    @Test
    fun `no duplicate doctor IDs`() {
        val duplicates = doctors.groupingBy { it.id }.eachCount().filter { it.value > 1 }
        assertTrue(
            "Found ${duplicates.size} duplicate doctor IDs: ${duplicates.keys.take(5)}",
            duplicates.isEmpty()
        )
    }

    @Test
    fun `no duplicate user IDs`() {
        val duplicates = doctors.groupingBy { it.userId }.eachCount().filter { it.value > 1 }
        assertTrue(
            "Found ${duplicates.size} duplicate user IDs: ${duplicates.keys.take(5)}",
            duplicates.isEmpty()
        )
    }

    @Test
    fun `all doctors should have non-blank fields`() {
        val invalidDoctors = doctors.filter {
            it.id.isBlank() || it.userId.isNullOrBlank() || it.fullName.isBlank() ||
                it.hospitalId.isBlank() || it.branchId.isBlank() || it.roomInfo.isBlank()
        }
        assertTrue(
            "Found ${invalidDoctors.size} doctors with blank fields",
            invalidDoctors.isEmpty()
        )
    }

    @Test
    fun `every hospital should have at least one doctor`() {
        val doctorsByHospital = doctors.groupBy { it.hospitalId }
        val hospitalsWithoutDoctors = hospitals.filter { it.id !in doctorsByHospital }
        assertTrue(
            "Found ${hospitalsWithoutDoctors.size} hospitals without doctors: ${hospitalsWithoutDoctors.take(5).map { it.name }}",
            hospitalsWithoutDoctors.isEmpty()
        )
    }

    @Test
    fun `hospital branch assignments should match capacity rules`() {
        val allBranchIds = branches.map { it.id }.toSet()
        val violations = mutableListOf<String>()

        hospitals.forEach { hospital ->
            val hospitalBranchIds = hospital.branchIds.toSet()
            val expectedCount = when {
                hospital.bedCount <= 500 -> 3
                hospital.bedCount <= 1000 -> 5
                hospital.bedCount <= 1300 -> 10
                else -> allBranchIds.size
            }

            if (hospitalBranchIds.size != expectedCount) {
                violations.add("${hospital.name}: ${hospitalBranchIds.size} branches, expected $expectedCount")
            }

            if (!allBranchIds.containsAll(hospitalBranchIds)) {
                violations.add("${hospital.name}: has invalid branch ids")
            }
        }

        assertTrue(
            "Found ${violations.size} branch assignment violations:\n${violations.take(5).joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun `doctors per branch should be exactly two`() {
        val countsByHospitalBranch = doctors.groupingBy { it.hospitalId to it.branchId }.eachCount()

        val violations = mutableListOf<String>()
        countsByHospitalBranch.forEach { (hospitalBranch, count) ->
            val required = 2
            if (count != required) {
                violations.add("Hospital ${hospitalBranch.first}, branch ${hospitalBranch.second}: $count doctors != $required required")
            }
        }

        assertTrue(
            "Found ${violations.size} violations:\n${violations.take(10).joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun `doctor branches should be subset of hospital branch assignments`() {
        val hospitalById = hospitals.associateBy { it.id }
        val violations = doctors.filter { doctor ->
            val hospital = hospitalById[doctor.hospitalId] ?: return@filter true
            doctor.branchId !in hospital.branchIds
        }

        assertTrue(
            "Found ${violations.size} doctors with branch not assigned to hospital: ${violations.take(5).map { it.id }}",
            violations.isEmpty()
        )
    }

    @Test
    fun `print doctor distribution summary`() {
        val doctorsByHospital = doctors.groupBy { it.hospitalId }

        println("\n=== Doctor Seed Data Summary ===")
        println("Total hospitals: ${hospitals.size}")
        println("Total branches: ${branches.size}")
        println("Total doctors: ${doctors.size}")

        data class SizeCategory(val name: String, val min: Int, val max: Int)
        val categories = listOf(
            SizeCategory("0-50 beds", 0, 50),
            SizeCategory("51-150 beds", 51, 150),
            SizeCategory("151-300 beds", 151, 300),
            SizeCategory("301-600 beds", 301, 600),
            SizeCategory("601-1000 beds", 601, 1000),
            SizeCategory("1001+ beds", 1001, Int.MAX_VALUE)
        )

        println("\n=== Distribution by Hospital Size ===")
        categories.forEach { cat ->
            val hospitalsInCat = hospitals.filter { it.bedCount in cat.min..cat.max }
            val doctorsInCat = hospitalsInCat.sumOf { h -> doctorsByHospital[h.id]?.size ?: 0 }
            val avgDoctors = if (hospitalsInCat.isNotEmpty()) doctorsInCat.toDouble() / hospitalsInCat.size else 0.0
            println("${cat.name}: ${hospitalsInCat.size} hospitals, $doctorsInCat doctors, avg: %.1f".format(avgDoctors))
        }

        println("\n=== Largest Hospitals ===")
        hospitals.sortedByDescending { it.bedCount }.take(5).forEach { h ->
            val docs = doctorsByHospital[h.id] ?: emptyList()
            val branchCount = docs.map { it.branchId }.distinct().size
            println("${h.name} (${h.bedCount} beds): ${docs.size} doctors, $branchCount branches, assigned=${h.branchIds.size}")
        }

        assertTrue(true)
    }
}
