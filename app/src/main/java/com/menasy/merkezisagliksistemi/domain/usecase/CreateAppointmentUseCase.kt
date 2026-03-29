package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository

class CreateAppointmentUseCase(
    private val appointmentRepository: AppointmentRepository
) {

    /**
     * Creates a new appointment with transaction-based slot locking.
     * 
     * This ensures that:
     * - No two appointments can be created for the same doctor/date/time
     * - If two users try to book the same slot simultaneously, only one succeeds
     * 
     * @param appointment The appointment details to create
     * @return Result containing the created appointment ID on success,
     *         or an error (SLOT_ALREADY_TAKEN if slot was taken by another user)
     */
    suspend operator fun invoke(appointment: Appointment): Result<String> {
        return appointmentRepository.createAppointment(appointment)
    }
}
