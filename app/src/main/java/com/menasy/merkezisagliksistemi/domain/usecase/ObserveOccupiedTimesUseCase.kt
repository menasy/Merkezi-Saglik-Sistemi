package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow

class ObserveOccupiedTimesUseCase(
    private val appointmentRepository: AppointmentRepository
) {

    /**
     * Observes occupied time slots for a specific doctor on a specific date.
     * 
     * @param doctorId The doctor's unique identifier
     * @param date The date in yyyy-MM-dd format
     * @return Flow emitting sets of occupied time labels (e.g., "09:00", "09:20")
     */
    operator fun invoke(doctorId: String, date: String): Flow<Set<String>> {
        return appointmentRepository.observeOccupiedSlots(doctorId, date)
    }
}
