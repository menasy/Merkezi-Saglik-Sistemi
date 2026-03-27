package com.menasy.merkezisagliksistemi.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.remote.firebase.AuthDataSource
import com.menasy.merkezisagliksistemi.data.repository.AuthRepository
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.LoginUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.LogoutUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.RegisterPatientUseCase

object ServiceLocator {

    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val authDataSource: AuthDataSource by lazy {
        AuthDataSource(
            auth = firebaseAuth,
            firestore = firestore
        )
    }

    private val authRepository: AuthRepository by lazy {
        AuthRepository(authDataSource)
    }

    fun provideLoginUserUseCase(): LoginUserUseCase {
        return LoginUserUseCase(authRepository)
    }

    fun provideRegisterPatientUseCase(): RegisterPatientUseCase {
        return RegisterPatientUseCase(authRepository)
    }

    fun provideGetCurrentUserUseCase(): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(authRepository)
    }

    fun provideLogoutUserUseCase(): LogoutUserUseCase {
        return LogoutUserUseCase(authRepository)
    }
}
