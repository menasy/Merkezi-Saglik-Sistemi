package com.menasy.merkezisagliksistemi.ui.doctor.examination

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.data.model.Medicine
import com.menasy.merkezisagliksistemi.databinding.ItemMedicineSelectionBinding

class MedicineSelectionAdapter(
    private val onSelectionChanged: (selectedIds: Set<String>) -> Unit
) : ListAdapter<Medicine, MedicineSelectionAdapter.ViewHolder>(DiffCallback()) {

    private val selectedIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicineSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Only update selection state when payload is present
            val medicine = getItem(position)
            holder.updateSelectionState(selectedIds.contains(medicine.medicineId))
        }
    }

    fun getSelectedIds(): Set<String> = selectedIds.toSet()

    fun clearSelections() {
        val previousSelected = selectedIds.toSet()
        selectedIds.clear()
        previousSelected.forEach { id ->
            val position = currentList.indexOfFirst { it.medicineId == id }
            if (position != -1) {
                notifyItemChanged(position, PAYLOAD_SELECTION_CHANGED)
            }
        }
        onSelectionChanged(selectedIds)
    }

    private fun toggleSelection(medicineId: String, position: Int) {
        if (selectedIds.contains(medicineId)) {
            selectedIds.remove(medicineId)
        } else {
            selectedIds.add(medicineId)
        }
        notifyItemChanged(position, PAYLOAD_SELECTION_CHANGED)
        onSelectionChanged(selectedIds)
    }

    inner class ViewHolder(
        private val binding: ItemMedicineSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val defaultNameColor by lazy {
            ContextCompat.getColor(binding.root.context, R.color.text_primary)
        }
        private val selectedNameColor by lazy {
            ContextCompat.getColor(binding.root.context, R.color.primary_dark)
        }
        private val defaultDosageColor by lazy {
            ContextCompat.getColor(binding.root.context, R.color.text_secondary)
        }
        private val selectedDosageColor by lazy {
            ContextCompat.getColor(binding.root.context, R.color.primary)
        }

        init {
            binding.root.setOnClickListener { handleToggleClick() }
            binding.cbMedicine.setOnClickListener { handleToggleClick() }
        }

        fun bind(medicine: Medicine) {
            val isSelected = selectedIds.contains(medicine.medicineId)

            binding.tvMedicineName.text = medicine.medicineName
            updateSelectionState(isSelected)

            val dosageInfo = buildDosageInfo(medicine)
            binding.tvMedicineDosage.text = dosageInfo
            binding.tvMedicineDosage.isVisible = dosageInfo.isNotBlank()
        }

        fun updateSelectionState(isSelected: Boolean) {
            binding.cbMedicine.isChecked = isSelected
            binding.root.isSelected = isSelected
            binding.viewSelectionIndicator.isVisible = isSelected
            binding.tvMedicineName.setTextColor(if (isSelected) selectedNameColor else defaultNameColor)
            binding.tvMedicineDosage.setTextColor(if (isSelected) selectedDosageColor else defaultDosageColor)
        }

        @Suppress("DEPRECATION")
        private fun handleToggleClick() {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val medicine = getItem(position)
                toggleSelection(medicine.medicineId, position)
            }
        }

        private fun buildDosageInfo(medicine: Medicine): String {
            val parts = mutableListOf<String>()
            if (medicine.dosage.isNotBlank()) parts.add(medicine.dosage)
            if (medicine.frequency.isNotBlank()) parts.add(medicine.frequency)
            return parts.joinToString(" • ")
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.medicineId == newItem.medicineId
        }

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION_CHANGED = "selection_changed"
    }
}
