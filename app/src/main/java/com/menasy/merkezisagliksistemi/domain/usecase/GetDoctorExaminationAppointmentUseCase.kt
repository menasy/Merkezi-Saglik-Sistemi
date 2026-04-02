package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.AppointmentStatus
import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository
import com.menasy.merkezisagliksistemi.data.repository.BranchRepository
import com.menasy.merkezisagliksistemi.data.repository.DoctorRepository
import com.menasy.merkezisagliksistemi.data.repository.HospitalRepository
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.AppException
import com.menasy.merkezisagliksistemi.utils.DateTimeUtils
import java.time.LocalDateTime

data class DoctorExaminationAppointment(
    val appointmentId: String,
    val appointmentDate: String,
    val appointmentTime: String,
    val patientName: String,
    val doctorName: String,
    val hospitalName: String,
    val branchName: String
)

class GetDoctorExaminationAppointmentUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val doctorRepository: DoctorRepository,
    private val hospitalRepository: HospitalRepository,
    private val branchRepository: BranchRepository
) {

    suspend operator fun invoke(
        appointmentId: String,
        doctorId: String,
        prefetchedPatientName: String? = null
    ): Result<DoctorExaminationAppointment> {
        return try {
            val appointment = appointmentRepository.getAppointmentById(appointmentId)
                .getOrElse { throwable -> return Result.failure(throwable) }

            if (appointment.doctorId != doctorId) {
                return Result.failure(AppException(AppErrorReason.APPOINTMENT_DOCTOR_MISMATCH))
            }

            if (appointment.status != AppointmentStatus.SCHEDULED.name) {
                return Result.failure(AppException(AppErrorReason.APPOINTMENT_STATUS_INVALID_FOR_EXAMINATION))
            }

            val appointmentDateTime = DateTimeUtils.parseAppointmentDateTime(
                dateStr = appointment.appointmentDate,
                timeStr = appointment.appointmentTime
            ) ?: return Result.failure(AppException(AppErrorReason.APPOINTMENT_INFO_MISSING))
            if (appointmentDateTime.isAfter(LocalDateTime.now())) {
                return Result.failure(AppException(AppErrorReason.APPOINTMENT_TIME_NOT_REACHED))
            }

            val patientName = prefetchedPatientName
                ?.takeIf { it.isNotBlank() }
                ?: appointmentRepository.getUserFullNamesByIds(setOf(appointment.patientId))
                    .getOrNull()
                    ?.get(appointment.patientId)
                ?: "Bilinmeyen Hasta"

            val doctorName = doctorRepository.getDoctorById(appointment.doctorId)?.fullName
                ?: "Bilinmeyen Doktor"
            val hospitalName = hospitalRepository.getHospitalById(appointment.hospitalId)?.name
                ?: "Bilinmeyen Hastane"
            val branchName = branchRepository.getBranchById(appointment.branchId)?.name
                ?: "Bilinmeyen Bölüm"

            Result.success(
                DoctorExaminationAppointment(
                    appointmentId = appointment.id,
                    appointmentDate = appointment.appointmentDate,
                    appointmentTime = appointment.appointmentTime,
                    patientName = patientName,
                    doctorName = doctorName,
                    hospitalName = hospitalName,
                    branchName = branchName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
