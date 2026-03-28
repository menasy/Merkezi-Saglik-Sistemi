package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.repository.ReferenceDataRepository

class InitializeReferenceDataUseCase(
    private val referenceDataRepository: ReferenceDataRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return referenceDataRepository.ensureReferenceDataInitialized()
    }
}
