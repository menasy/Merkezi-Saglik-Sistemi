package com.menasy.merkezisagliksistemi.data.model

data class Prescription(
    val id: String = "",
    val appointmentId: String = "",
    val prescriptionCode: String = "",
    val createdAtMillis: Long = 0L,
    val note: String = "",
    val medicines: List<Medicine> = emptyList()
)
