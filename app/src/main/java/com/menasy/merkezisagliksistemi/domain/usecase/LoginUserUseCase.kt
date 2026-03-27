package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.LoginResult
import com.menasy.merkezisagliksistemi.data.repository.AuthRepository

class LoginUserUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<LoginResult> {
        return authRepository.login(email, password)
    }
}