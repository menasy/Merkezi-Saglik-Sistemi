package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.repository.AuthRepository
import com.menasy.merkezisagliksistemi.di.SessionCache

class LogoutUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke() {
        SessionCache.clear()
        authRepository.logout()
    }
}
