package com.menasy.merkezisagliksistemi.data.model.seedData

import com.menasy.merkezisagliksistemi.data.model.Hospital
import kotlin.random.Random

private const val SMALL_HOSPITAL_BRANCH_COUNT = 3
private const val MEDIUM_HOSPITAL_BRANCH_COUNT = 5
private const val LARGE_HOSPITAL_BRANCH_COUNT = 10

fun assignBranchIdsToHospitals(
    hospitals: List<Hospital>,
    branchIds: List<String>
): List<Hospital> {
    val uniqueBranchIds = branchIds.distinct()
    if (uniqueBranchIds.isEmpty()) return hospitals

    return hospitals.map { hospital ->
        hospital.copy(
            branchIds = resolveHospitalBranchIds(
                hospital = hospital,
                allBranchIds = uniqueBranchIds
            )
        )
    }
}

fun resolveHospitalBranchIds(
    hospital: Hospital,
    allBranchIds: List<String>
): List<String> {
    val uniqueBranchIds = allBranchIds.distinct()
    if (uniqueBranchIds.isEmpty()) return emptyList()

    val existing = hospital.branchIds
        .filter { it in uniqueBranchIds }
        .distinct()
    if (existing.isNotEmpty()) {
        return existing.sorted()
    }

    val targetCount = targetBranchCountByBed(
        bedCount = hospital.bedCount,
        totalBranchCount = uniqueBranchIds.size
    )

    return deterministicBranchPick(
        hospitalId = hospital.id,
        branchIds = uniqueBranchIds,
        count = targetCount
    )
}

private fun targetBranchCountByBed(
    bedCount: Int,
    totalBranchCount: Int
): Int {
    val normalizedBedCount = bedCount.coerceAtLeast(0)

    val target = when {
        normalizedBedCount <= 500 -> SMALL_HOSPITAL_BRANCH_COUNT
        normalizedBedCount <= 1000 -> MEDIUM_HOSPITAL_BRANCH_COUNT
        normalizedBedCount <= 1300 -> LARGE_HOSPITAL_BRANCH_COUNT
        else -> totalBranchCount
    }

    return target.coerceAtMost(totalBranchCount)
}

private fun deterministicBranchPick(
    hospitalId: String,
    branchIds: List<String>,
    count: Int
): List<String> {
    if (count <= 0) return emptyList()
    if (count >= branchIds.size) return branchIds.sorted()

    val seed = hospitalId.hashCode().let { hash ->
        if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash)
    }

    return branchIds
        .shuffled(Random(seed))
        .take(count)
        .sorted()
}
