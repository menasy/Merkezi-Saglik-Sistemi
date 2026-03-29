package com.menasy.merkezisagliksistemi.ui.patient.appointment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.ItemDoctorDaySlotsBinding

class DoctorDaySlotsAdapter(
    private val onHourSelected: (Int, Int) -> Unit,
    private val onSlotSelected: (Int, String) -> Unit
) : ListAdapter<DayAvailabilityUiModel, DoctorDaySlotsAdapter.DaySlotsViewHolder>(DaySlotsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DaySlotsViewHolder {
        val binding = ItemDoctorDaySlotsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DaySlotsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DaySlotsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DaySlotsViewHolder(
        private val binding: ItemDoctorDaySlotsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DayAvailabilityUiModel) {
            binding.tvDayTitle.text = item.dayTitle
            binding.tvDaySubtitle.text = item.daySubtitle
            bindHourChips(item)
            bindSlotChips(item)
        }

        private fun bindHourChips(item: DayAvailabilityUiModel) {
            binding.chipGroupHours.removeAllViews()

            item.hourBlocks.forEachIndexed { hourIndex, hourBlock ->
                val chip = createHourChip().apply {
                    text = hourBlock.hourLabel
                    isEnabled = hourBlock.isEnabled
                    isChecked = item.selectedHourIndex == hourIndex

                    setOnClickListener {
                        if (!hourBlock.isEnabled) return@setOnClickListener
                        val currentPosition = this@DaySlotsViewHolder.adapterPosition
                        if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                        onHourSelected(currentPosition, hourIndex)
                    }
                }
                binding.chipGroupHours.addView(chip)
            }
        }

        private fun bindSlotChips(item: DayAvailabilityUiModel) {
            val selectedHour = item.selectedHourIndex?.let { hourIndex ->
                item.hourBlocks.getOrNull(hourIndex)
            }

            if (selectedHour == null) {
                binding.tvSlotLabel.visibility = View.GONE
                binding.chipGroupSlots.visibility = View.GONE
                binding.chipGroupSlots.removeAllViews()
                return
            }

            binding.tvSlotLabel.visibility = View.VISIBLE
            binding.chipGroupSlots.visibility = View.VISIBLE
            binding.chipGroupSlots.removeAllViews()

            selectedHour.slots.forEach { slot ->
                val slotChip = createSlotChip().apply {
                    text = slot.timeLabel
                    isEnabled = slot.isAvailable
                    isChecked = item.selectedSlotLabel == slot.timeLabel

                    setOnClickListener {
                        if (!slot.isAvailable) return@setOnClickListener
                        val currentPosition = this@DaySlotsViewHolder.adapterPosition
                        if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                        onSlotSelected(currentPosition, slot.timeLabel)
                    }
                }
                binding.chipGroupSlots.addView(slotChip)
            }
        }

        private fun createHourChip(): Chip {
            return Chip(binding.root.context, null, R.style.    Widget_MerkeziSaglik_Chip_Hour)
        }

        private fun createSlotChip(): Chip {
            return Chip(binding.root.context, null, R.style.Widget_MerkeziSaglik_Chip_Slot)
        }
    }

    private class DaySlotsDiffCallback : DiffUtil.ItemCallback<DayAvailabilityUiModel>() {
        override fun areItemsTheSame(
            oldItem: DayAvailabilityUiModel,
            newItem: DayAvailabilityUiModel
        ): Boolean {
            return oldItem.dateMillis == newItem.dateMillis
        }

        override fun areContentsTheSame(
            oldItem: DayAvailabilityUiModel,
            newItem: DayAvailabilityUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
