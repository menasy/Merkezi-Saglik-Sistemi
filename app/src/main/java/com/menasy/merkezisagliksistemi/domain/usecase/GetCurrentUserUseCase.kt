package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.repository.AuthRepository
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository,
    private val doctorRepository: DoctorRepository
) {
    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }

    suspend fun getCurrentUserRole(): Result<String> {
        return authRepository.getCurrentUserRole()
    }

    suspend fun getCurrentUserFullName(): Result<String> {
        return authRepository.getCurrentUserFullName()
    }

    /**
     * Doktor kullanıcısı için business doctor ID döndürür.
     * Firebase UID ile local seed'den doktor profili bulunur.
     *
     * @param userId Firebase Auth UID
     * @return Doctor.id veya null (profil bulunamazsa)
     */
    fun getDoctorIdByUserId(userId: String): String? {
        return doctorRepository.getDoctorByUserId(userId)?.id
    }

    /**
     * Firebase UID için seed'de doktor profili var mı kontrol eder.
     * Login yetkisi olmasa bile profile bakar.
     */
    fun hasDoctorProfileByUserId(userId: String): Boolean {
        return doctorRepository.hasDoctorProfileByUserId(userId)
    }
}
