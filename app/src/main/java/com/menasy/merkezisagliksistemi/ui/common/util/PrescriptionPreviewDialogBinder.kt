package com.menasy.merkezisagliksistemi.ui.common.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.databinding.DialogPrescriptionPreviewBinding
import com.menasy.merkezisagliksistemi.databinding.ItemMedicinePreviewBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object PrescriptionPreviewDialogBinder {

    fun bind(
        context: Context,
        inflater: LayoutInflater,
        dialogBinding: DialogPrescriptionPreviewBinding,
        personNameLabelRes: Int,
        personName: String,
        prescription: Prescription,
        examinationNote: String
    ) {
        dialogBinding.tvPersonNameLabel.setText(personNameLabelRes)
        dialogBinding.tvPrescriptionCodeValue.text = prescription.prescriptionCode.ifBlank { "-" }
        dialogBinding.tvPatientNameValue.text = personName

        dialogBinding.tvCreatedAtValue.text = if (prescription.createdAtMillis > 0L) {
            Instant.ofEpochMilli(prescription.createdAtMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(PREVIEW_DATE_TIME_FORMATTER)
        } else {
            context.getString(R.string.doctor_appointments_unknown_date)
        }

        dialogBinding.tvMedicinesTitle.text = context.getString(
            R.string.doctor_appointments_medicines_label_with_count,
            prescription.medicines.size
        )

        bindMedicines(
            context = context,
            inflater = inflater,
            dialogBinding = dialogBinding,
            prescription = prescription
        )

        val emptyFallback = context.getString(R.string.doctor_appointments_note_empty_fallback)
        dialogBinding.tvExaminationNoteValue.text = examinationNote.trim().ifBlank { emptyFallback }
        dialogBinding.tvPrescriptionNoteValue.text = prescription.note.trim().ifBlank { emptyFallback }
    }

    private fun bindMedicines(
        context: Context,
        inflater: LayoutInflater,
        dialogBinding: DialogPrescriptionPreviewBinding,
        prescription: Prescription
    ) {
        dialogBinding.layoutMedicines.removeAllViews()
        if (prescription.medicines.isEmpty()) {
            dialogBinding.tvMedicinesValue.visibility = View.VISIBLE
            dialogBinding.tvMedicinesValue.text = context.getString(R.string.doctor_appointments_no_medicines)
            dialogBinding.tvMedicinesValue.setTextColor(
                ContextCompat.getColor(context, R.color.text_secondary)
            )
            dialogBinding.tvMedicinesValue.textSize = 14f

            val verticalPadding = context.resources.getDimensionPixelSize(R.dimen.spacing_md)
            dialogBinding.tvMedicinesValue.setPadding(0, verticalPadding, 0, verticalPadding)
            return
        }

        dialogBinding.tvMedicinesValue.visibility = View.GONE
        prescription.medicines.forEach { medicine ->
            val medicineBinding = ItemMedicinePreviewBinding.inflate(
                inflater,
                dialogBinding.layoutMedicines,
                false
            )
            medicineBinding.tvMedicineName.text = medicine.medicineName
            medicineBinding.tvMedicineDosage.text = medicine.dosage.ifBlank { "-" }
            medicineBinding.tvMedicineFrequency.text = medicine.frequency.ifBlank { "-" }
            medicineBinding.tvMedicineUsage.text = medicine.usageDescription.ifBlank { "-" }

            if (medicine.doctorNote.isNotBlank()) {
                medicineBinding.layoutMedicineNote.visibility = View.VISIBLE
                medicineBinding.tvMedicineNote.text = medicine.doctorNote
            } else {
                medicineBinding.layoutMedicineNote.visibility = View.GONE
            }

            dialogBinding.layoutMedicines.addView(medicineBinding.root)
        }
    }

    private val PREVIEW_DATE_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
}
