package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.LoginResult
import com.menasy.merkezisagliksistemi.data.repository.AuthRepository
import com.menasy.merkezisagliksistemi.di.SessionCache

class LoginUserUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<LoginResult> {
        val result = authRepository.login(email, password)
        result.onSuccess { loginResult ->
            SessionCache.populate(
                userId = loginResult.uid,
                role = loginResult.role,
                fullName = loginResult.fullName
            )
        }
        return result
    }
}
