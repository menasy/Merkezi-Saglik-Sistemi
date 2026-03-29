package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

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

    init {
        setHasStableIds(true)
    }

    private enum class SlotVisualState {
        AVAILABLE,
        SELECTED,
        OCCUPIED
    }

    data class SelectionPayload(
        val hourSelectionChanged: Boolean,
        val slotSelectionChanged: Boolean
    )

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

    override fun onBindViewHolder(
        holder: DaySlotsViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val selectionPayload = payloads.lastOrNull { it is SelectionPayload } as? SelectionPayload
        if (selectionPayload == null) {
            holder.bind(getItem(position))
            return
        }
        holder.bindSelection(getItem(position), selectionPayload)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).dateMillis
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

        fun bindSelection(item: DayAvailabilityUiModel, payload: SelectionPayload) {
            if (payload.hourSelectionChanged) {
                bindHourChips(item)
                bindSlotChips(item)
                return
            }

            if (payload.slotSelectionChanged) {
                updateSlotSelectionOnly(item)
            }
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
                val visualState = resolveSlotVisualState(
                    isAvailable = slot.isAvailable,
                    isSelected = item.selectedSlotLabel == slot.timeLabel
                )
                val slotChip = createSlotChip().apply {
                    text = slot.timeLabel
                    applyVisualState(visualState)

                    setOnClickListener {
                        if (visualState == SlotVisualState.OCCUPIED) return@setOnClickListener
                        val currentPosition = this@DaySlotsViewHolder.adapterPosition
                        if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                        onSlotSelected(currentPosition, slot.timeLabel)
                    }
                }
                binding.chipGroupSlots.addView(slotChip)
            }
        }

        private fun updateSlotSelectionOnly(item: DayAvailabilityUiModel) {
            val selectedHourIndex = item.selectedHourIndex ?: run {
                bindSlotChips(item)
                return
            }
            val selectedHour = item.hourBlocks.getOrNull(selectedHourIndex) ?: run {
                bindSlotChips(item)
                return
            }

            if (binding.chipGroupSlots.childCount != selectedHour.slots.size) {
                bindSlotChips(item)
                return
            }

            binding.tvSlotLabel.visibility = View.VISIBLE
            binding.chipGroupSlots.visibility = View.VISIBLE

            selectedHour.slots.forEachIndexed { index, slot ->
                val chip = binding.chipGroupSlots.getChildAt(index) as? Chip ?: run {
                    bindSlotChips(item)
                    return
                }
                chip.text = slot.timeLabel
                val visualState = resolveSlotVisualState(
                    isAvailable = slot.isAvailable,
                    isSelected = item.selectedSlotLabel == slot.timeLabel
                )
                chip.applyVisualState(visualState)
            }
        }

        private fun createHourChip(): Chip {
            return Chip(binding.root.context, null, R.style.Widget_MerkeziSaglik_Chip_Hour)
        }

        private fun createSlotChip(): Chip {
            return Chip(binding.root.context, null, R.style.Widget_MerkeziSaglik_Chip_Slot)
        }

        private fun resolveSlotVisualState(
            isAvailable: Boolean,
            isSelected: Boolean
        ): SlotVisualState {
            return when {
                !isAvailable -> SlotVisualState.OCCUPIED
                isSelected -> SlotVisualState.SELECTED
                else -> SlotVisualState.AVAILABLE
            }
        }

        private fun Chip.applyVisualState(state: SlotVisualState) {
            when (state) {
                SlotVisualState.AVAILABLE -> {
                    isEnabled = true
                    isClickable = true
                    isCheckable = true
                    isChecked = false
                    alpha = 1f
                }

                SlotVisualState.SELECTED -> {
                    isEnabled = true
                    isClickable = true
                    isCheckable = true
                    isChecked = true
                    alpha = 1f
                }

                SlotVisualState.OCCUPIED -> {
                    isEnabled = false
                    isClickable = false
                    isCheckable = false
                    isChecked = false
                    alpha = OCCUPIED_SLOT_ALPHA
                }
            }
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

        override fun getChangePayload(
            oldItem: DayAvailabilityUiModel,
            newItem: DayAvailabilityUiModel
        ): Any? {
            val hasStructuralChange =
                oldItem.dayTitle != newItem.dayTitle ||
                    oldItem.daySubtitle != newItem.daySubtitle ||
                    oldItem.hourBlocks != newItem.hourBlocks

            if (hasStructuralChange) return null

            val hourSelectionChanged = oldItem.selectedHourIndex != newItem.selectedHourIndex
            val slotSelectionChanged = oldItem.selectedSlotLabel != newItem.selectedSlotLabel

            return if (hourSelectionChanged || slotSelectionChanged) {
                SelectionPayload(
                    hourSelectionChanged = hourSelectionChanged,
                    slotSelectionChanged = slotSelectionChanged
                )
            } else {
                null
            }
        }
    }

    private companion object {
        const val OCCUPIED_SLOT_ALPHA = 0.58f
    }
}
