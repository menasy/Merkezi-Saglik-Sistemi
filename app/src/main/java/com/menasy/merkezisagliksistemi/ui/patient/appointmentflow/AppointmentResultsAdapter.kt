package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.menasy.merkezisagliksistemi.databinding.ItemAppointmentResultBinding
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
            binding.tvPatientName.text = extractPatientName(item.doctorName)
            binding.tvDoctorName.text = item.doctorName
            binding.tvBranchName.text = item.branchName
            binding.tvHospitalName.text = item.hospitalName.uppercase(Locale.forLanguageTag("tr-TR"))
            binding.tvAppointmentDate.text = item.nearestAvailableDateLabel
            binding.tvDaysLeft.text = item.nearestAvailableRelativeText
            binding.root.setOnClickListener {
                onCreateAppointmentClick(item)
            }
        }

        private fun extractPatientName(doctorName: String): String {
            val parts = doctorName.split(" ")
            return if (parts.size >= 2) {
                parts.takeLast(2).joinToString(" ").uppercase(Locale.forLanguageTag("tr-TR"))
            } else {
                doctorName.uppercase(Locale.forLanguageTag("tr-TR"))
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
