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
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(appointmentId: String): Result<Unit> {
        return appointmentRepository.cancelAppointment(appointmentId)
    }
}
