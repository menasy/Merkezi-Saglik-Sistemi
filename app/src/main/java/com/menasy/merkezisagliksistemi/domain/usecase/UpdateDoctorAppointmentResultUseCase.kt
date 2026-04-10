package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.AppointmentStatus
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.AppException

class UpdateDoctorAppointmentResultUseCase(
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(
        appointmentId: String,
        doctorId: String,
        targetStatus: AppointmentStatus,
        prescription: Prescription? = null,
        examinationNote: String = ""
    ): Result<Unit> {
        if (targetStatus != AppointmentStatus.COMPLETED &&
            targetStatus != AppointmentStatus.MISSED
        ) {
            return Result.failure(AppException(AppErrorReason.APPOINTMENT_STATUS_INVALID_FOR_EXAMINATION))
        }

        if (targetStatus == AppointmentStatus.MISSED && prescription != null) {
            return Result.failure(AppException(AppErrorReason.APPOINTMENT_STATUS_INVALID_FOR_EXAMINATION))
        }

        if (targetStatus == AppointmentStatus.COMPLETED && prescription != null) {
            if (prescription.medicines.isEmpty()) {
                return Result.failure(AppException(AppErrorReason.PRESCRIPTION_MEDICINE_REQUIRED))
            }
            return appointmentRepository.completeDoctorAppointmentWithPrescription(
                appointmentId = appointmentId,
                doctorId = doctorId,
                prescription = prescription,
                examinationNote = examinationNote
            )
        }

        return appointmentRepository.updateDoctorAppointmentResultStatus(
            appointmentId = appointmentId,
            doctorId = doctorId,
            targetStatus = targetStatus.name,
            examinationNote = examinationNote
        )
    }
}
