package com.menasy.merkezisagliksistemi.ui.patient.appointmentlist

import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.model.AppointmentStatus
import com.menasy.merkezisagliksistemi.data.remote.local.BranchDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.DoctorDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.HospitalDataSource
import com.menasy.merkezisagliksistemi.utils.DateTimeUtils

/**
 * Mapper class to convert Appointment domain model to PatientAppointmentItem UI model.
 * Resolves IDs to names using local seed data sources.
 */
class AppointmentMapper(
    private val doctorDataSource: DoctorDataSource,
    private val hospitalDataSource: HospitalDataSource,
    private val branchDataSource: BranchDataSource
) {

    /**
     * Maps a single Appointment to PatientAppointmentItem.
     * Resolves doctorId, hospitalId, branchId to their display names.
     */
    fun mapToUiModel(appointment: Appointment): PatientAppointmentItem {
        val doctor = doctorDataSource.getDoctorById(appointment.doctorId)
        val hospital = hospitalDataSource.getHospitalById(appointment.hospitalId)
        val branch = branchDataSource.getBranchById(appointment.branchId)

        val isActive = isAppointmentActive(appointment)

        return PatientAppointmentItem(
            id = appointment.id,
            hospitalName = hospital?.name ?: "Bilinmeyen Hastane",
            branchName = branch?.name ?: "Bilinmeyen Bölüm",
            doctorName = doctor?.fullName ?: "Bilinmeyen Doktor",
            dateMillis = DateTimeUtils.dateStringToMillis(appointment.appointmentDate),
            timeLabel = appointment.appointmentTime,
            status = appointment.status,
            isActive = isActive
        )
    }

    /**
     * Maps a list of Appointments to PatientAppointmentItems.
     */
    fun mapToUiModelList(appointments: List<Appointment>): List<PatientAppointmentItem> {
        return appointments.map { mapToUiModel(it) }
    }

    /**
     * Partitions appointments into active and past lists.
     * 
     * Active: SCHEDULED status AND future date/time
     * Past: COMPLETED, CANCELLED, MISSED, or past date/time
     */
    fun partitionAppointments(
        appointments: List<Appointment>
    ): Pair<List<PatientAppointmentItem>, List<PatientAppointmentItem>> {
        val active = mutableListOf<PatientAppointmentItem>()
        val past = mutableListOf<PatientAppointmentItem>()

        appointments.forEach { appointment ->
            val uiModel = mapToUiModel(appointment)
            if (uiModel.isActive) {
                active.add(uiModel)
            } else {
                past.add(uiModel)
            }
        }

        // Sort active by date ascending (soonest first)
        active.sortBy { it.dateMillis }
        
        // Sort past by date descending (most recent first)
        past.sortByDescending { it.dateMillis }

        return Pair(active, past)
    }

    /**
     * Determines if an appointment should be considered active.
     * 
     * An appointment is active if:
     * - Status is SCHEDULED
     * - AND date/time is in the future
     */
    private fun isAppointmentActive(appointment: Appointment): Boolean {
        if (appointment.status != AppointmentStatus.SCHEDULED.name) {
            return false
        }

        return DateTimeUtils.isAppointmentInFuture(
            appointment.appointmentDate,
            appointment.appointmentTime
        )
    }
}
