package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.menasy.merkezisagliksistemi.databinding.ItemAppointmentResultBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class AppointmentResultsAdapter(
    private val onCreateAppointmentClick: (AppointmentResultUiModel) -> Unit
) : ListAdapter<AppointmentResultUiModel, AppointmentResultsAdapter.AppointmentResultViewHolder>(
    AppointmentResultDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentResultViewHolder {
        val binding = ItemAppointmentResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppointmentResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppointmentResultViewHolder(
        private val binding: ItemAppointmentResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppointmentResultUiModel) {
            // Patient name (using doctor name as patient identifier for now)
            binding.tvPatientName.text = extractPatientName(item.doctorName)
            
            // Doctor info
            binding.tvDoctorName.text = item.doctorName
            binding.tvBranchName.text = item.branchName
            binding.tvHospitalName.text = item.hospitalName.uppercase(Locale.forLanguageTag("tr-TR"))
            
            // Date formatting for short display
            binding.tvAppointmentDate.text = formatShortDate(item.appointmentDateMillis)
            
            // Days left with "Gün Var" format
            binding.tvDaysLeft.text = formatDaysLeft(item.daysLeftText)

            // Make entire card clickable
            binding.root.setOnClickListener {
                onCreateAppointmentClick(item)
            }
        }
        
        private fun extractPatientName(doctorName: String): String {
            // Extract surname part from doctor name for patient display
            val parts = doctorName.split(" ")
            return if (parts.size >= 2) {
                parts.takeLast(2).joinToString(" ").uppercase(Locale.forLanguageTag("tr-TR"))
            } else {
                doctorName.uppercase(Locale.forLanguageTag("tr-TR"))
            }
        }
        
        private fun formatShortDate(millis: Long): String {
            val date = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy EEEE", Locale.forLanguageTag("tr-TR"))
            return formatter.format(date)
        }
        
        private fun formatDaysLeft(daysLeftText: String): String {
            // Convert "X gün kaldı" to "X Gün Var"
            val number = daysLeftText.filter { it.isDigit() }
            return if (number.isNotEmpty()) {
                "$number Gün Var"
            } else {
                daysLeftText
            }
        }
    }

    private class AppointmentResultDiffCallback : DiffUtil.ItemCallback<AppointmentResultUiModel>() {
        override fun areItemsTheSame(
            oldItem: AppointmentResultUiModel,
            newItem: AppointmentResultUiModel
        ): Boolean {
            return oldItem.doctorId == newItem.doctorId
        }

        override fun areContentsTheSame(
            oldItem: AppointmentResultUiModel,
            newItem: AppointmentResultUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
