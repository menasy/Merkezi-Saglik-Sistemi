package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository

class CancelAppointmentUseCase(
    private val appointmentRepository: AppointmentRepository
) {

    /**
     * Cancels an appointment and frees the associated time slot.
     * 
     * This operation:
     * - Updates appointment status to CANCELLED
     * - Removes the slot lock to allow re-booking
     *
     * @param appointmentId The ID of the appointment to cancel
     * @param patientId The active patient ID that owns the appointment
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(appointmentId: String, patientId: String): Result<Unit> {
        return appointmentRepository.cancelAppointment(
            appointmentId = appointmentId,
            patientId = patientId
        )
    }
}
