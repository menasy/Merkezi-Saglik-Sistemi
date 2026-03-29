package com.menasy.merkezisagliksistemi.domain.usecase

import com.menasy.merkezisagliksistemi.data.repository.DoctorAvailabilityRepository
import java.time.LocalDate

class GetDoctorUnavailableSlotsUseCase(
    private val doctorAvailabilityRepository: DoctorAvailabilityRepository
) {
    operator fun invoke(doctorId: String, date: LocalDate): Set<String> {
        return doctorAvailabilityRepository.getUnavailableSlotLabels(
            doctorId = doctorId,
            date = date
        )
    }
}
