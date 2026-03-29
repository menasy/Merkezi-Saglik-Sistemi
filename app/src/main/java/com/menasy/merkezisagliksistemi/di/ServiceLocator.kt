package com.menasy.merkezisagliksistemi.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.remote.firebase.AppointmentDataSource
import com.menasy.merkezisagliksistemi.data.remote.firebase.AuthDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.BranchDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.CityDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.DoctorAvailabilityDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.DistrictDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.DoctorDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.HospitalDataSource
import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository
import com.menasy.merkezisagliksistemi.data.repository.AuthRepository
import com.menasy.merkezisagliksistemi.data.repository.BranchRepository
import com.menasy.merkezisagliksistemi.data.repository.CityRepository
import com.menasy.merkezisagliksistemi.data.repository.DoctorAvailabilityRepository
import com.menasy.merkezisagliksistemi.data.repository.DistrictRepository
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository
import com.menasy.merkezisagliksistemi.data.repository.HospitalRepository
import com.menasy.merkezisagliksistemi.domain.usecase.CancelAppointmentUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.CreateAppointmentUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetBranchesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetCitiesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorUnavailableSlotsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDistrictsByCityUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetHospitalsByDistrictUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.LoginUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.LogoutUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.ObserveOccupiedTimesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.ObservePatientAppointmentsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.RegisterPatientUseCase
import com.menasy.merkezisagliksistemi.ui.patient.appointments.AppointmentMapper

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

    private val appointmentDataSource: AppointmentDataSource by lazy {
        AppointmentDataSource(firestore = firestore)
    }

    private val cityDataSource: CityDataSource by lazy {
        CityDataSource()
    }

    private val districtDataSource: DistrictDataSource by lazy {
        DistrictDataSource()
    }

    private val branchDataSource: BranchDataSource by lazy {
        BranchDataSource()
    }

    private val hospitalDataSource: HospitalDataSource by lazy {
        HospitalDataSource()
    }

    private val doctorDataSource: DoctorDataSource by lazy {
        DoctorDataSource()
    }

    private val doctorAvailabilityDataSource: DoctorAvailabilityDataSource by lazy {
        DoctorAvailabilityDataSource()
    }

    private val authRepository: AuthRepository by lazy {
        AuthRepository(authDataSource)
    }

    private val appointmentRepository: AppointmentRepository by lazy {
        AppointmentRepository(appointmentDataSource)
    }

    private val cityRepository: CityRepository by lazy {
        CityRepository(cityDataSource)
    }

    private val districtRepository: DistrictRepository by lazy {
        DistrictRepository(districtDataSource)
    }

    private val branchRepository: BranchRepository by lazy {
        BranchRepository(branchDataSource)
    }

    private val hospitalRepository: HospitalRepository by lazy {
        HospitalRepository(hospitalDataSource)
    }

    private val doctorRepository: DoctorRepository by lazy {
        DoctorRepository(doctorDataSource)
    }

    private val doctorAvailabilityRepository: DoctorAvailabilityRepository by lazy {
        DoctorAvailabilityRepository(doctorAvailabilityDataSource)
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

    fun provideGetCitiesUseCase(): GetCitiesUseCase {
        return GetCitiesUseCase(cityRepository)
    }

    fun provideGetDistrictsByCityUseCase(): GetDistrictsByCityUseCase {
        return GetDistrictsByCityUseCase(districtRepository)
    }

    fun provideGetHospitalsByDistrictUseCase(): GetHospitalsByDistrictUseCase {
        return GetHospitalsByDistrictUseCase(hospitalRepository)
    }

    fun provideGetBranchesUseCase(): GetBranchesUseCase {
        return GetBranchesUseCase(branchRepository)
    }

    fun provideGetDoctorsUseCase(): GetDoctorsUseCase {
        return GetDoctorsUseCase(doctorRepository)
    }

    fun provideGetDoctorUnavailableSlotsUseCase(): GetDoctorUnavailableSlotsUseCase {
        return GetDoctorUnavailableSlotsUseCase(doctorAvailabilityRepository)
    }

    fun provideObserveOccupiedTimesUseCase(): ObserveOccupiedTimesUseCase {
        return ObserveOccupiedTimesUseCase(appointmentRepository)
    }

    fun provideCreateAppointmentUseCase(): CreateAppointmentUseCase {
        return CreateAppointmentUseCase(appointmentRepository)
    }

    fun provideObservePatientAppointmentsUseCase(): ObservePatientAppointmentsUseCase {
        return ObservePatientAppointmentsUseCase(appointmentRepository)
    }

    fun provideCancelAppointmentUseCase(): CancelAppointmentUseCase {
        return CancelAppointmentUseCase(appointmentRepository)
    }

    fun provideAppointmentMapper(): AppointmentMapper {
        return AppointmentMapper(
            doctorDataSource = doctorDataSource,
            hospitalDataSource = hospitalDataSource,
            branchDataSource = branchDataSource
        )
    }
}
