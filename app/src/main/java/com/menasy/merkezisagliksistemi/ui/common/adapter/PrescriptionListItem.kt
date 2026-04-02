package com.menasy.merkezisagliksistemi.ui.common.adapter

import com.menasy.merkezisagliksistemi.data.model.Prescription

data class PrescriptionListItem(
    val appointmentId: String,
    val prescriptionCode: String,
    val createdAtMillis: Long,
    val personName: String,
    val hospitalName: String,
    val branchName: String,
    val appointmentDateMillis: Long,
    val appointmentTimeLabel: String,
    val appointmentStatusLabel: String,
    val medicineCount: Int,
    val note: String,
    val prescription: Prescription
)
