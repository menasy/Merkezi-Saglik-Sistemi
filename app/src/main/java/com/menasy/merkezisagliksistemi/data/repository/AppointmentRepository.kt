package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.model.Prescription
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
    suspend fun cancelAppointment(appointmentId: String, patientId: String): Result<Unit> {
        return appointmentDataSource.cancelAppointment(
            appointmentId = appointmentId,
            patientId = patientId
        )
    }

    /**
     * Gets all appointments for a specific patient.
     */
    suspend fun getPatientAppointments(patientId: String): Result<List<Appointment>> {
        return appointmentDataSource.getPatientAppointments(patientId)
    }

    /**
     * Gets a single appointment by appointment ID.
     */
    suspend fun getAppointmentById(appointmentId: String): Result<Appointment> {
        return appointmentDataSource.getAppointmentById(appointmentId)
    }

    /**
     * Observes all appointments for a specific patient in realtime.
     */
    fun observePatientAppointments(patientId: String): Flow<List<Appointment>> {
        return appointmentDataSource.observePatientAppointments(patientId)
    }

    /**
     * Observes all appointments for a specific doctor in realtime.
     */
    fun observeDoctorAppointments(doctorId: String): Flow<List<Appointment>> {
        return appointmentDataSource.observeDoctorAppointments(doctorId)
    }

    /**
     * Resolves user display names by user IDs from Firestore `users` collection.
     */
    suspend fun getUserFullNamesByIds(userIds: Set<String>): Result<Map<String, String>> {
        return appointmentDataSource.getUserFullNamesByIds(userIds)
    }

    /**
     * Fetches prescription previews for appointment IDs.
     */
    suspend fun getPrescriptionPreviewsByAppointmentIds(
        appointmentIds: Set<String>
    ): Result<Map<String, Prescription>> {
        return appointmentDataSource.getPrescriptionPreviewsByAppointmentIds(appointmentIds)
    }

    /**
     * Gets the count of overdue appointments for a doctor.
     * Overdue = SCHEDULED status + appointment datetime is in the past.
     */
    suspend fun getDoctorOverdueAppointmentCount(doctorId: String): Result<Int> {
        return appointmentDataSource.getDoctorOverdueAppointmentCount(doctorId)
    }

    /**
     * Gets the count of appointments completed today for a doctor.
     */
    suspend fun getDoctorCompletedTodayCount(doctorId: String, todayDate: String): Result<Int> {
        return appointmentDataSource.getDoctorCompletedTodayCount(doctorId, todayDate)
    }

    /**
     * Updates doctor-owned appointment result from SCHEDULED to COMPLETED or MISSED.
     */
    suspend fun updateDoctorAppointmentResultStatus(
        appointmentId: String,
        doctorId: String,
        targetStatus: String,
        examinationNote: String = ""
    ): Result<Unit> {
        return appointmentDataSource.updateDoctorAppointmentResultStatus(
            appointmentId = appointmentId,
            doctorId = doctorId,
            targetStatus = targetStatus,
            examinationNote = examinationNote
        )
    }

    /**
     * Completes a doctor-owned appointment by creating prescription and updating status atomically.
     */
    suspend fun completeDoctorAppointmentWithPrescription(
        appointmentId: String,
        doctorId: String,
        prescription: Prescription,
        examinationNote: String = ""
    ): Result<Unit> {
        return appointmentDataSource.completeDoctorAppointmentWithPrescription(
            appointmentId = appointmentId,
            doctorId = doctorId,
            prescription = prescription,
            examinationNote = examinationNote
        )
    }
}
