package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.Branch
import com.menasy.merkezisagliksistemi.data.remote.local.BranchDataSource

class BranchRepository(
    private val branchDataSource: BranchDataSource
) {
    fun getBranches(): List<Branch> {
        return branchDataSource.getBranches()
    }

    fun getBranchById(branchId: String): Branch? {
        return branchDataSource.getBranchById(branchId)
    }
}
