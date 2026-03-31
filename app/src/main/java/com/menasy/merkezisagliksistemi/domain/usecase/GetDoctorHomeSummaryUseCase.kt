package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Doctor
import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository
import com.menasy.merkezisagliksistemi.data.repository.BranchRepository
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository
import com.menasy.merkezisagliksistemi.data.repository.HospitalRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Use case for fetching doctor home screen summary data.
 *
 * Returns:
 * - Doctor profile information (name, branch)
 * - All scheduled appointment count (both past and future)
 * - Today's completed examination count
 */
class GetDoctorHomeSummaryUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val doctorRepository: DoctorRepository,
    private val branchRepository: BranchRepository,
    private val hospitalRepository: HospitalRepository
) {

    /**
     * Fetches summary data for the doctor home screen.
     *
     * @param doctorId Business doctor ID (Doctor.id)
     * @return Result containing DoctorHomeSummary on success
     */
    suspend operator fun invoke(doctorId: String): Result<DoctorHomeSummary> {
        return try {
            val doctor = doctorRepository.getDoctorById(doctorId)
                ?: return Result.failure(IllegalStateException("Doktor profili bulunamadı"))

            val branchName = branchRepository.getBranchById(doctor.branchId)?.name ?: ""
            val hospitalName = hospitalRepository.getHospitalById(doctor.hospitalId)?.name ?: ""

            val todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val scheduledCountResult = appointmentRepository.getDoctorScheduledAppointmentCount(doctorId)
            val scheduledCount = scheduledCountResult.getOrDefault(0)

            val completedTodayResult = appointmentRepository.getDoctorCompletedTodayCount(doctorId, todayDate)
            val completedTodayCount = completedTodayResult.getOrDefault(0)

            Result.success(
                DoctorHomeSummary(
                    doctorFullName = doctor.fullName,
                    branchName = branchName,
                    hospitalName = hospitalName,
                    pendingAppointmentCount = scheduledCount,
                    completedTodayCount = completedTodayCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Data class representing the doctor home screen summary.
 */
data class DoctorHomeSummary(
    val doctorFullName: String,
    val branchName: String,
    val hospitalName: String,
    val pendingAppointmentCount: Int,
    val completedTodayCount: Int
)
