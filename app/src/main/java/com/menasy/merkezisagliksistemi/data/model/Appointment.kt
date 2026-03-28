package com.menasy.merkezisagliksistemi.data.model

data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val hospitalId: String = "",
    val branchId: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val status: String = AppointmentStatus.SCHEDULED.name,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    MISSED
}