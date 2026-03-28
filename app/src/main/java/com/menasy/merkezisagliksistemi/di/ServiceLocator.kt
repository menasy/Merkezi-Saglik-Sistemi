package com.menasy.merkezisagliksistemi.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.remote.firebase.AuthDataSource
import com.menasy.merkezisagliksistemi.data.remote.firebase.CityDataSource
import com.menasy.merkezisagliksistemi.data.remote.firebase.DoctorDataSource
import com.menasy.merkezisagliksistemi.data.remote.firebase.HospitalDataSource
import com.menasy.merkezisagliksistemi.data.remote.firebase.ReferenceDataSource
import com.menasy.merkezisagliksistemi.data.repository.AuthRepository
import com.menasy.merkezisagliksistemi.data.repository.CityRepository
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository
import com.menasy.merkezisagliksistemi.data.repository.HospitalRepository
import com.menasy.merkezisagliksistemi.data.repository.ReferenceDataRepository
import com.menasy.merkezisagliksistemi.domain.usecase.GetBranchesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetCitiesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDistrictsByCityUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetHospitalsByDistrictUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.InitializeReferenceDataUseCase
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

    private val referenceDataSource: ReferenceDataSource by lazy {
        ReferenceDataSource(
            firestore = firestore
        )
    }

    private val cityDataSource: CityDataSource by lazy {
        CityDataSource(
            firestore = firestore
        )
    }

    private val hospitalDataSource: HospitalDataSource by lazy {
        HospitalDataSource(
            firestore = firestore
        )
    }

    private val doctorDataSource: DoctorDataSource by lazy {
        DoctorDataSource(
            firestore = firestore
        )
    }

    private val authRepository: AuthRepository by lazy {
        AuthRepository(authDataSource)
    }

    private val referenceDataRepository: ReferenceDataRepository by lazy {
        ReferenceDataRepository(referenceDataSource)
    }

    private val cityRepository: CityRepository by lazy {
        CityRepository(cityDataSource)
    }

    private val hospitalRepository: HospitalRepository by lazy {
        HospitalRepository(hospitalDataSource)
    }

    private val doctorRepository: DoctorRepository by lazy {
        DoctorRepository(doctorDataSource)
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

    fun provideInitializeReferenceDataUseCase(): InitializeReferenceDataUseCase {
        return InitializeReferenceDataUseCase(referenceDataRepository)
    }

    fun provideGetCitiesUseCase(): GetCitiesUseCase {
        return GetCitiesUseCase(cityRepository)
    }

    fun provideGetDistrictsByCityUseCase(): GetDistrictsByCityUseCase {
        return GetDistrictsByCityUseCase(hospitalRepository)
    }

    fun provideGetHospitalsByDistrictUseCase(): GetHospitalsByDistrictUseCase {
        return GetHospitalsByDistrictUseCase(hospitalRepository)
    }

    fun provideGetBranchesUseCase(): GetBranchesUseCase {
        return GetBranchesUseCase(doctorRepository)
    }

    fun provideGetDoctorsUseCase(): GetDoctorsUseCase {
        return GetDoctorsUseCase(doctorRepository)
    }
}
