package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Branch
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository

class GetBranchesUseCase(
    private val doctorRepository: DoctorRepository
) {
    suspend operator fun invoke(): Result<List<Branch>> {
        return doctorRepository.getBranches()
    }
}
