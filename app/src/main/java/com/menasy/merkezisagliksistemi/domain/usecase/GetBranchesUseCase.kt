package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Branch
import com.menasy.merkezisagliksistemi.data.repository.BranchRepository

class GetBranchesUseCase(
    private val branchRepository: BranchRepository
) {
    operator fun invoke(): List<Branch> {
        return branchRepository.getBranches()
    }
}
