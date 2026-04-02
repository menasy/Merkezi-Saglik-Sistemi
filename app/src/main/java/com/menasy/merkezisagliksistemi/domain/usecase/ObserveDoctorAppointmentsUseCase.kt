package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow

class ObserveDoctorAppointmentsUseCase(
    private val appointmentRepository: AppointmentRepository
) {
    operator fun invoke(doctorId: String): Flow<List<Appointment>> {
        return appointmentRepository.observeDoctorAppointments(doctorId)
    }
}
