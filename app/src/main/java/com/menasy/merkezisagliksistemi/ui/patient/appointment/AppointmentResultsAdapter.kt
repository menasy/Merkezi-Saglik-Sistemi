package com.menasy.merkezisagliksistemi.ui.patient.appointment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.menasy.merkezisagliksistemi.databinding.ItemAppointmentResultBinding

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
            binding.tvDoctorName.text = item.doctorName
            binding.tvBranchName.text = item.branchName
            binding.tvHospitalName.text = item.hospitalName
            binding.tvAppointmentDate.text = item.appointmentDateLabel
            binding.tvDaysLeft.text = item.daysLeftText

            binding.btnCreateAppointment.setOnClickListener {
                onCreateAppointmentClick(item)
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
