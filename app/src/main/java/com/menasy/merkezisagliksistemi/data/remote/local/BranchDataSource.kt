package com.menasy.merkezisagliksistemi.data.remote.local

import com.menasy.merkezisagliksistemi.data.model.Branch
import com.menasy.merkezisagliksistemi.data.model.seedData.branches as seededBranches

class BranchDataSource {

    fun getBranches(): List<Branch> {
        return seededBranches.sortedBy { it.name }
    }

    fun getBranchById(branchId: String): Branch? {
        return branchesById[branchId]
    }

    private companion object {
        val branchesById: Map<String, Branch> = seededBranches.associateBy { it.id }
    }
}
