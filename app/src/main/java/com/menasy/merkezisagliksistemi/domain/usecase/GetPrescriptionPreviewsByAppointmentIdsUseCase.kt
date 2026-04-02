package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.data.repository.AppointmentRepository

class GetPrescriptionPreviewsByAppointmentIdsUseCase(
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(appointmentIds: Set<String>): Result<Map<String, Prescription>> {
        return appointmentRepository.getPrescriptionPreviewsByAppointmentIds(appointmentIds)
    }
}
