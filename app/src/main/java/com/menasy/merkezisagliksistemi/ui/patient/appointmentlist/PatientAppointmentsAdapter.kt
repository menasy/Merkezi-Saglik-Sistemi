package com.menasy.merkezisagliksistemi.ui.patient.appointmentlist

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.databinding.ItemPatientAppointmentBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AppointmentActionType {
    CANCEL,
    EXAMINE,
    VIEW_PRESCRIPTION
}

enum class AppointmentActionStyle {
    DANGER,
    PRIMARY
}

data class PatientAppointmentItem(
    val id: String,
    val hospitalName: String,
    val branchName: String,
    val doctorName: String,
    val dateMillis: Long,
    val timeLabel: String,
    val status: String,
    val isActive: Boolean,
    val isPastStyle: Boolean = !isActive,
    val statusTextOverride: String? = null,
    val personIconRes: Int = R.drawable.ic_appointment_doctor,
    val actionText: String? = if (isActive) "İptal Et" else null,
    val actionType: AppointmentActionType? = if (isActive) AppointmentActionType.CANCEL else null,
    val actionStyle: AppointmentActionStyle = AppointmentActionStyle.DANGER,
    val examinationNote: String = "",
    val prescription: Prescription? = null
)

class PatientAppointmentsAdapter(
    private val onActionClick: (PatientAppointmentItem) -> Unit
) : ListAdapter<PatientAppointmentItem, PatientAppointmentsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPatientAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onActionClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemPatientAppointmentBinding,
        private val onActionClick: (PatientAppointmentItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PatientAppointmentItem) {
            val context = binding.root.context
            val isPastAppointment = item.isPastStyle
            val isCancelled = item.status == "CANCELLED"

            val date = Instant.ofEpochMilli(item.dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            binding.tvAppointmentDay.text = date.dayOfMonth.toString()
            val dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy EEEE", Locale.forLanguageTag("tr-TR"))
            binding.tvAppointmentDateLabel.text = dateFormatter.format(date)
            binding.tvAppointmentTime.text = item.timeLabel
            binding.tvHospitalName.text = item.hospitalName.uppercase(Locale.getDefault())
            binding.tvBranchName.text = item.branchName
            binding.tvDoctorName.text = item.doctorName
            binding.ivDoctorIcon.setImageResource(item.personIconRes)

            val statusText = item.statusTextOverride ?: when {
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
                        item.status == "COMPLETED" -> R.color.success
                        item.status == "MISSED" -> R.color.error
                        isPastAppointment -> R.color.text_secondary
                        else -> R.color.primary
                    }
                )
            )

            if (item.actionText.isNullOrBlank()) {
                binding.layoutButtons.visibility = View.GONE
                binding.btnCancelAppointment.setOnClickListener(null)
            } else {
                binding.layoutButtons.visibility = View.VISIBLE
                binding.btnCancelAppointment.text = item.actionText
                applyActionStyle(context, item.actionStyle)
                binding.btnCancelAppointment.setOnClickListener {
                    onActionClick(item)
                }
            }
        }

        private fun applyActionStyle(context: Context, style: AppointmentActionStyle) {
            when (style) {
                AppointmentActionStyle.DANGER -> {
                    val dangerColor = resolveColor(context, R.color.button_danger_background_tint)
                    binding.btnCancelAppointment.backgroundTintList = ColorStateList.valueOf(dangerColor)
                }
                AppointmentActionStyle.PRIMARY -> {
                    val primaryColor = resolveColor(context, R.color.primary)
                    binding.btnCancelAppointment.backgroundTintList = ColorStateList.valueOf(primaryColor)
                }
            }
            binding.btnCancelAppointment.setTextColor(resolveColor(context, android.R.color.white))
            binding.btnCancelAppointment.strokeWidth = 0
        }

        private fun resolveColor(context: Context, colorRes: Int): Int {
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
