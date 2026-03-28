package com.menasy.merkezisagliksistemi

import com.menasy.merkezisagliksistemi.data.model.Hospital
import com.menasy.merkezisagliksistemi.data.model.seedData.resolveHospitalBranchIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HospitalBranchAllocatorTest {

    private val branchIds = (1..17).map { index -> "branch_$index" }

    @Test
    fun `bed capacity should map to expected branch counts`() {
        val cases = listOf(
            Hospital(id = "h_0_500", bedCount = 500) to 3,
            Hospital(id = "h_501", bedCount = 501) to 5,
            Hospital(id = "h_1000", bedCount = 1000) to 5,
            Hospital(id = "h_1001", bedCount = 1001) to 10,
            Hospital(id = "h_1300", bedCount = 1300) to 10,
            Hospital(id = "h_1301_plus", bedCount = 1301) to 17
        )

        cases.forEach { (hospital, expectedCount) ->
            val assigned = resolveHospitalBranchIds(hospital, branchIds)
            assertEquals("${hospital.id} branch count mismatch", expectedCount, assigned.size)
            assertTrue("${hospital.id} contains invalid branch id", branchIds.containsAll(assigned))
        }
    }

    @Test
    fun `assignment should be deterministic for same hospital id`() {
        val hospital = Hospital(id = "deterministic_hospital", bedCount = 420)

        val first = resolveHospitalBranchIds(hospital, branchIds)
        val second = resolveHospitalBranchIds(hospital, branchIds)

        assertEquals(first, second)
    }
}
