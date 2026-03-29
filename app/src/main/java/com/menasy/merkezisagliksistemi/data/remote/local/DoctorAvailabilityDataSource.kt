package com.menasy.merkezisagliksistemi.data.remote.local

import java.time.LocalDate

class DoctorAvailabilityDataSource {

    fun getUnavailableSlotLabels(doctorId: String, date: LocalDate): Set<String> {
        // TODO: Replace with real backend lookup (Firestore/API) for reserved slots.
        // Expected format: "HH:mm" labels for the given doctor/date.
        return emptySet()
    }
}
