package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow

class ObservePatientAppointmentsUseCase(
    private val appointmentRepository: AppointmentRepository
) {

    /**
     * Observes all appointments for a specific patient in realtime.
     * 
     * This uses Firestore snapshot listener to provide live updates
     * whenever appointments are added, modified, or deleted.
     *
     * @param patientId The ID of the patient whose appointments to observe
     * @return Flow emitting list of appointments whenever changes occur
     */
    operator fun invoke(patientId: String): Flow<List<Appointment>> {
        return appointmentRepository.observePatientAppointments(patientId)
    }
}
