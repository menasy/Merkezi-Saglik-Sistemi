package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.LoginResult
import com.menasy.merkezisagliksistemi.data.repository.AuthRepository
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository
import com.menasy.merkezisagliksistemi.di.SessionCache
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.AppException

class LoginUserUseCase(
    private val authRepository: AuthRepository,
    private val doctorRepository: DoctorRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<LoginResult> {
        val result = authRepository.login(email, password)

        return result.fold(
            onSuccess = { loginResult ->
                handleLoginSuccess(loginResult)
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
    }

    private fun handleLoginSuccess(loginResult: LoginResult): Result<LoginResult> {
        return when (loginResult.role) {
            "doctor" -> handleDoctorLogin(loginResult)
            "patient" -> handlePatientLogin(loginResult)
            else -> Result.success(loginResult).also {
                SessionCache.populate(
                    userId = loginResult.uid,
                    role = loginResult.role,
                    fullName = loginResult.fullName
                )
            }
        }
    }

    private fun handleDoctorLogin(loginResult: LoginResult): Result<LoginResult> {
        // Firebase UID ile local seed'den doktor profilini bul
        val doctor = doctorRepository.getDoctorByUserId(loginResult.uid)
            ?: return Result.failure(AppException(AppErrorReason.DOCTOR_PROFILE_NOT_FOUND))

        // Session cache'i doktor bilgileriyle doldur
        SessionCache.populateDoctor(
            userId = loginResult.uid,
            role = loginResult.role,
            fullName = loginResult.fullName,
            doctorId = doctor.id
        )

        // LoginResult'a doctorId ekle
        return Result.success(
            loginResult.copy(doctorId = doctor.id)
        )
    }

    private fun handlePatientLogin(loginResult: LoginResult): Result<LoginResult> {
        SessionCache.populate(
            userId = loginResult.uid,
            role = loginResult.role,
            fullName = loginResult.fullName
        )
        return Result.success(loginResult)
    }
}
