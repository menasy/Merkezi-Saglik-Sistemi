package com.menasy.merkezisagliksistemi.ui.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.ItemPrescriptionRecordBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrescriptionRecordsAdapter(
    private val onItemClick: (PrescriptionListItem) -> Unit
) : ListAdapter<PrescriptionListItem, PrescriptionRecordsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPrescriptionRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemPrescriptionRecordBinding,
        private val onItemClick: (PrescriptionListItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PrescriptionListItem) {
            val context = binding.root.context

            binding.tvPersonName.text = item.personName
            binding.tvHospitalBranch.text = context.getString(
                R.string.prescription_records_hospital_branch_format,
                item.hospitalName,
                item.branchName
            )
            binding.tvAppointmentDateTime.text = context.getString(
                R.string.prescription_records_appointment_format,
                formatDate(item.appointmentDateMillis),
                item.appointmentTimeLabel
            )
            binding.tvPrescriptionCodeValue.text = item.prescriptionCode
            binding.tvCreatedAtValue.text = context.getString(
                R.string.prescription_records_created_at_format,
                formatDateTime(item.createdAtMillis)
            )
            binding.tvMedicineCountValue.text = context.getString(
                R.string.prescription_records_medicine_count_format,
                item.medicineCount
            )
            binding.tvStatusValue.text = item.appointmentStatusLabel

            // Status badge background based on status
            val statusBgRes = when {
                item.appointmentStatusLabel.contains("Tamamlandı", ignoreCase = true) ->
                    R.drawable.bg_status_badge_completed
                item.appointmentStatusLabel.contains("Bekliyor", ignoreCase = true) ->
                    R.drawable.bg_status_badge_pending
                else -> R.drawable.bg_status_badge_completed
            }
            binding.tvStatusValue.setBackgroundResource(statusBgRes)

            val statusTextColor = when {
                item.appointmentStatusLabel.contains("Tamamlandı", ignoreCase = true) ->
                    R.color.status_completed_text
                item.appointmentStatusLabel.contains("Bekliyor", ignoreCase = true) ->
                    R.color.status_pending_text
                else -> R.color.status_completed_text
            }
            binding.tvStatusValue.setTextColor(context.getColor(statusTextColor))

            // Show note only if not empty
            val hasNote = item.note.isNotBlank()
            binding.tvNoteValue.isVisible = hasNote
            if (hasNote) {
                binding.tvNoteValue.text = item.note
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun formatDate(millis: Long): String {
            if (millis <= 0L) return "-"
            return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DATE_FORMATTER)
        }

        private fun formatDateTime(millis: Long): String {
            if (millis <= 0L) return "-"
            return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_TIME_FORMATTER)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PrescriptionListItem>() {
        override fun areItemsTheSame(
            oldItem: PrescriptionListItem,
            newItem: PrescriptionListItem
        ): Boolean {
            return oldItem.appointmentId == newItem.appointmentId &&
                oldItem.prescriptionCode == newItem.prescriptionCode
        }

        override fun areContentsTheSame(
            oldItem: PrescriptionListItem,
            newItem: PrescriptionListItem
        ): Boolean {
            return oldItem == newItem
        }
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))
        val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
    }
}
