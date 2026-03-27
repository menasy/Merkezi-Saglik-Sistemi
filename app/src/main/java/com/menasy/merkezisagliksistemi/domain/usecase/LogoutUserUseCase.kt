package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.repository.AuthRepository

class LogoutUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke() {
        authRepository.logout()
    }
}