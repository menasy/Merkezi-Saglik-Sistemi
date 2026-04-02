package com.menasy.merkezisagliksistemi.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.remote.firebase.AppointmentDataSource
import com.menasy.merkezisagliksistemi.data.remote.firebase.AuthDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.BranchDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.CityDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.DistrictDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.DoctorDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.HospitalDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.MedicineDataSource
import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository
import com.menasy.merkezisagliksistemi.data.repository.AuthRepository
import com.menasy.merkezisagliksistemi.data.repository.BranchRepository
import com.menasy.merkezisagliksistemi.data.repository.CityRepository
import com.menasy.merkezisagliksistemi.data.repository.DistrictRepository
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository
import com.menasy.merkezisagliksistemi.data.repository.HospitalRepository
import com.menasy.merkezisagliksistemi.data.repository.MedicineRepository
import com.menasy.merkezisagliksistemi.domain.usecase.CancelAppointmentUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.CreateAppointmentUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetBranchesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetCitiesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetCurrentUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorExaminationAppointmentUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorHomeSummaryUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDistrictsByCityUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetDoctorsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetHospitalsByDistrictUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetMedicinesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetNearestAvailableDateUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetPrescriptionPreviewsByAppointmentIdsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.GetUserFullNamesByIdsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.LoginUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.LogoutUserUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.ObserveDoctorAppointmentsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.ObserveOccupiedTimesUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.ObservePatientAppointmentsUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.RegisterPatientUseCase
import com.menasy.merkezisagliksistemi.domain.usecase.UpdateDoctorAppointmentResultUseCase
import com.menasy.merkezisagliksistemi.ui.patient.appointmentlist.AppointmentMapper

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

    private val medicineDataSource: MedicineDataSource by lazy {
        MedicineDataSource()
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

    private val medicineRepository: MedicineRepository by lazy {
        MedicineRepository(medicineDataSource)
    }

    fun provideLoginUserUseCase(): LoginUserUseCase {
        return LoginUserUseCase(authRepository, doctorRepository)
    }

    fun provideRegisterPatientUseCase(): RegisterPatientUseCase {
        return RegisterPatientUseCase(authRepository)
    }

    fun provideGetCurrentUserUseCase(): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(authRepository, doctorRepository)
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

    fun provideGetMedicinesUseCase(): GetMedicinesUseCase {
        return GetMedicinesUseCase(medicineRepository)
    }

    fun provideObserveOccupiedTimesUseCase(): ObserveOccupiedTimesUseCase {
        return ObserveOccupiedTimesUseCase(appointmentRepository)
    }

    fun provideGetNearestAvailableDateUseCase(): GetNearestAvailableDateUseCase {
        return GetNearestAvailableDateUseCase(provideObserveOccupiedTimesUseCase())
    }

    fun provideCreateAppointmentUseCase(): CreateAppointmentUseCase {
        return CreateAppointmentUseCase(appointmentRepository)
    }

    fun provideObservePatientAppointmentsUseCase(): ObservePatientAppointmentsUseCase {
        return ObservePatientAppointmentsUseCase(appointmentRepository)
    }

    fun provideObserveDoctorAppointmentsUseCase(): ObserveDoctorAppointmentsUseCase {
        return ObserveDoctorAppointmentsUseCase(appointmentRepository)
    }

    fun provideCancelAppointmentUseCase(): CancelAppointmentUseCase {
        return CancelAppointmentUseCase(appointmentRepository)
    }

    fun provideGetUserFullNamesByIdsUseCase(): GetUserFullNamesByIdsUseCase {
        return GetUserFullNamesByIdsUseCase(appointmentRepository)
    }

    fun provideGetPrescriptionPreviewsByAppointmentIdsUseCase(): GetPrescriptionPreviewsByAppointmentIdsUseCase {
        return GetPrescriptionPreviewsByAppointmentIdsUseCase(appointmentRepository)
    }

    fun provideGetDoctorExaminationAppointmentUseCase(): GetDoctorExaminationAppointmentUseCase {
        return GetDoctorExaminationAppointmentUseCase(
            appointmentRepository = appointmentRepository,
            doctorRepository = doctorRepository,
            hospitalRepository = hospitalRepository,
            branchRepository = branchRepository
        )
    }

    fun provideUpdateDoctorAppointmentResultUseCase(): UpdateDoctorAppointmentResultUseCase {
        return UpdateDoctorAppointmentResultUseCase(appointmentRepository)
    }

    fun provideAppointmentMapper(): AppointmentMapper {
        return AppointmentMapper(
            doctorDataSource = doctorDataSource,
            hospitalDataSource = hospitalDataSource,
            branchDataSource = branchDataSource
        )
    }

    fun provideGetDoctorHomeSummaryUseCase(): GetDoctorHomeSummaryUseCase {
        return GetDoctorHomeSummaryUseCase(
            appointmentRepository = appointmentRepository,
            doctorRepository = doctorRepository,
            branchRepository = branchRepository,
            hospitalRepository = hospitalRepository
        )
    }
}
