package com.menasy.merkezisagliksistemi.ui.doctor.examination

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.data.model.Medicine
import com.menasy.merkezisagliksistemi.databinding.ItemDoctorSelectedMedicineBinding

class SelectedMedicinesAdapter(
    private val onEditMedicineNote: (Medicine) -> Unit,
    private val onRemoveMedicine: (Medicine) -> Unit
) : ListAdapter<Medicine, SelectedMedicinesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDoctorSelectedMedicineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(
            binding = binding,
            onEditMedicineNote = onEditMedicineNote,
            onRemoveMedicine = onRemoveMedicine
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemDoctorSelectedMedicineBinding,
        private val onEditMedicineNote: (Medicine) -> Unit,
        private val onRemoveMedicine: (Medicine) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Medicine) {
            binding.tvMedicineName.text = item.medicineName
            binding.tvMedicineDosage.text = item.dosage.ifBlank { "-" }
            binding.tvMedicineFrequency.text = item.frequency.ifBlank { "-" }
            binding.tvMedicineUsage.text = item.usageDescription.ifBlank { "-" }

            val hasNote = item.doctorNote.isNotBlank()
            binding.layoutDoctorNote.isVisible = hasNote
            if (hasNote) {
                binding.layoutDoctorNote.text = binding.root.context.getString(
                    R.string.doctor_examination_medicine_note,
                    item.doctorNote
                )
            }

            binding.btnEditMedicineNote.setOnClickListener {
                onEditMedicineNote(item)
            }
            binding.btnRemoveMedicine.setOnClickListener {
                onRemoveMedicine(item)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.medicineId == newItem.medicineId
        }

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }
}
