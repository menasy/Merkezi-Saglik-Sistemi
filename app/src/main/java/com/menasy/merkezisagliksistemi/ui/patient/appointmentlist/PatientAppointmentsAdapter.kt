package com.menasy.merkezisagliksistemi.ui.patient.appointmentlist

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.databinding.ItemPatientAppointmentBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PatientAppointmentItem(
    val id: String,
    val hospitalName: String,
    val branchName: String,
    val doctorName: String,
    val dateMillis: Long,
    val timeLabel: String,
    val status: String,
    val isActive: Boolean
)

class PatientAppointmentsAdapter(
    private val onCancelClick: (PatientAppointmentItem) -> Unit
) : ListAdapter<PatientAppointmentItem, PatientAppointmentsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPatientAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onCancelClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemPatientAppointmentBinding,
        private val onCancelClick: (PatientAppointmentItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PatientAppointmentItem) {
            val context = binding.root.context
            val isPastAppointment = !item.isActive
            val isCancelled = item.status == "CANCELLED"

            // Parse date
            val date = Instant.ofEpochMilli(item.dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            // Day number
            binding.tvAppointmentDay.text = date.dayOfMonth.toString()

            // Month, year and day of week
            val dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy EEEE", Locale.forLanguageTag("tr-TR"))
            binding.tvAppointmentDateLabel.text = dateFormatter.format(date)

            // Time
            binding.tvAppointmentTime.text = item.timeLabel

            // Hospital
            binding.tvHospitalName.text = item.hospitalName.uppercase(Locale.getDefault())

            // Branch
            binding.tvBranchName.text = item.branchName

            // Doctor
            binding.tvDoctorName.text = item.doctorName

            // Status
            val statusText = when {
                isCancelled -> "İptal Edildi"
                item.isActive -> "Aktif Randevu"
                item.status == "COMPLETED" -> "Tamamlandı"
                item.status == "MISSED" -> "Katılmadı"
                else -> "Geçmiş Randevu"
            }
            binding.tvAppointmentStatus.text = statusText

            binding.layoutAppointmentHeader.setBackgroundResource(
                if (isPastAppointment) R.drawable.bg_appointment_card_header_past
                else R.drawable.bg_appointment_card_header
            )
            binding.viewLeftAccent.setBackgroundResource(
                if (isPastAppointment) R.drawable.bg_appointment_card_left_border_past
                else R.drawable.bg_appointment_card_left_border
            )

            binding.cardAppointment.setCardBackgroundColor(
                resolveColor(context, if (isPastAppointment) R.color.background else R.color.surface)
            )
            binding.cardAppointment.strokeColor = resolveColor(
                context,
                if (isPastAppointment) R.color.message_card_stroke else R.color.primary_light
            )
            binding.cardAppointment.alpha = if (isPastAppointment) 0.92f else 1f

            val headerTextColor = resolveColor(
                context,
                if (isPastAppointment) R.color.text_secondary else android.R.color.white
            )
            binding.tvAppointmentDay.setTextColor(headerTextColor)
            binding.tvAppointmentDateLabel.setTextColor(headerTextColor)
            binding.tvAppointmentTime.setTextColor(headerTextColor)
            binding.ivAppointmentClock.imageTintList = ColorStateList.valueOf(headerTextColor)

            val contentTextColor = resolveColor(
                context,
                if (isPastAppointment) R.color.text_secondary else R.color.text_primary
            )
            val secondaryTextColor = resolveColor(context, R.color.text_secondary)
            val iconTintColor = resolveColor(
                context,
                if (isPastAppointment) R.color.text_secondary else R.color.primary
            )

            binding.tvHospitalName.setTextColor(secondaryTextColor)
            binding.tvBranchName.setTextColor(contentTextColor)
            binding.tvDoctorName.setTextColor(contentTextColor)
            binding.ivBranchIcon.imageTintList = ColorStateList.valueOf(iconTintColor)
            binding.ivDoctorIcon.imageTintList = ColorStateList.valueOf(iconTintColor)

            binding.tvAppointmentStatus.setTextColor(
                resolveColor(
                    context,
                    when {
                        isCancelled -> R.color.error
                        isPastAppointment -> R.color.text_secondary
                        else -> R.color.primary
                    }
                )
            )

            // Buttons visibility - only show for active appointments
            binding.layoutButtons.visibility = if (item.isActive) View.VISIBLE else View.GONE

            // Button clicks
            binding.btnCancelAppointment.setOnClickListener {
                onCancelClick(item)
            }
        }

        private fun resolveColor(context: android.content.Context, colorRes: Int): Int {
            return ContextCompat.getColor(context, colorRes)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PatientAppointmentItem>() {
        override fun areItemsTheSame(
            oldItem: PatientAppointmentItem,
            newItem: PatientAppointmentItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: PatientAppointmentItem,
            newItem: PatientAppointmentItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
