package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.repository.AuthRepository

class RegisterPatientUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        fullName: String,
        email: String,
        password: String,
        tcNo: String,
        birthDate: String,
        gender: String
    ): Result<Unit> {
        return authRepository.registerPatient(
            fullName = fullName,
            email = email,
            password = password,
            tcNo = tcNo,
            birthDate = birthDate,
            gender = gender
        )
    }
}