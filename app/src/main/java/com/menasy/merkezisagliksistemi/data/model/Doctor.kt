package com.menasy.merkezisagliksistemi.data.model

data class Doctor(
    val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val branch: String = "",
    val hospitalId: String = "",
    val roomInfo: String = "",
    val workingDays: List<String> = emptyList(),
    val slotStartHour: Int = 9,
    val slotEndHour: Int = 17,
    val slotDurationMinutes: Int = 20
)
