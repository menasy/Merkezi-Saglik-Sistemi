package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.remote.firebase.AppointmentDataSource
import kotlinx.coroutines.flow.Flow

class AppointmentRepository(
    private val appointmentDataSource: AppointmentDataSource
) {

    /**
     * Observes occupied time slots for a specific doctor on a specific date.
     * Returns a Flow that emits updated slot sets in realtime.
     */
    fun observeOccupiedSlots(doctorId: String, date: String): Flow<Set<String>> {
        return appointmentDataSource.observeOccupiedSlots(doctorId, date)
    }

    /**
     * Creates an appointment using transaction-based locking.
     * Ensures no double-booking can occur.
     */
    suspend fun createAppointment(appointment: Appointment): Result<String> {
        return appointmentDataSource.createAppointmentWithLock(appointment)
    }

    /**
     * Cancels an appointment and frees the associated time slot.
     */
    suspend fun cancelAppointment(appointmentId: String): Result<Unit> {
        return appointmentDataSource.cancelAppointment(appointmentId)
    }

    /**
     * Gets all appointments for a specific patient.
     */
    suspend fun getPatientAppointments(patientId: String): Result<List<Appointment>> {
        return appointmentDataSource.getPatientAppointments(patientId)
    }

    /**
     * Observes all appointments for a specific patient in realtime.
     */
    fun observePatientAppointments(patientId: String): Flow<List<Appointment>> {
        return appointmentDataSource.observePatientAppointments(patientId)
    }
}
