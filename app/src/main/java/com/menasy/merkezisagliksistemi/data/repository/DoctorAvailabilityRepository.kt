package com.menasy.merkezisagliksistemi.data.repository

import com.menasy.merkezisagliksistemi.data.remote.local.DoctorAvailabilityDataSource
import java.time.LocalDate

class DoctorAvailabilityRepository(
    private val doctorAvailabilityDataSource: DoctorAvailabilityDataSource
) {
    fun getUnavailableSlotLabels(doctorId: String, date: LocalDate): Set<String> {
        return doctorAvailabilityDataSource.getUnavailableSlotLabels(
            doctorId = doctorId,
            date = date
        )
    }
}
