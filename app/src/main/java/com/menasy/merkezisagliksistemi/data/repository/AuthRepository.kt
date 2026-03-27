package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.remote.firebase.AuthDataSource
import com.menasy.merkezisagliksistemi.data.model.LoginResult

class AuthRepository(
    private val authDataSource: AuthDataSource
) {

    suspend fun registerPatient(
        fullName: String,
        email: String,
        password: String,
        tcNo: String,
        birthDate: String,
        gender: String
    ): Result<Unit> {
        return authDataSource.registerPatient(
            fullName = fullName,
            email = email,
            password = password,
            tcNo = tcNo,
            birthDate = birthDate,
            gender = gender
        )
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<LoginResult> {
        return authDataSource.login(email, password)
    }

    fun getCurrentUserId(): String? {
        return authDataSource.getCurrentUserId()
    }

    suspend fun getCurrentUserRole(): Result<String> {
        return authDataSource.getCurrentUserRole()
    }

    fun logout() {
        authDataSource.logout()
    }
}
