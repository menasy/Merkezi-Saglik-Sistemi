package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.repository.AuthRepository

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }

    suspend fun getCurrentUserRole(): Result<String> {
        return authRepository.getCurrentUserRole()
    }

    suspend fun getCurrentUserFullName(): Result<String> {
        return authRepository.getCurrentUserFullName()
    }
}
