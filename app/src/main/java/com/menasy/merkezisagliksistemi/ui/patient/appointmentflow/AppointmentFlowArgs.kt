package com.menasy.merkezisagliksistemi.ui.patient.appointmentflow

import android.os.Bundle

data class AppointmentSearchArgs(
    val startDateMillis: Long,
    val endDateMillis: Long,
    val cityId: String,
    val districtId: String?,
    val branchId: String,
    val hospitalId: String?,
    val doctorId: String?
) {
    fun toBundle(): Bundle {
        return Bundle().apply {
            putLong(KEY_START_DATE_MILLIS, startDateMillis)
            putLong(KEY_END_DATE_MILLIS, endDateMillis)
            putString(KEY_CITY_ID, cityId)
            putString(KEY_DISTRICT_ID, districtId)
            putString(KEY_BRANCH_ID, branchId)
            putString(KEY_HOSPITAL_ID, hospitalId)
            putString(KEY_DOCTOR_ID, doctorId)
        }
    }

    companion object {
        fun fromBundle(bundle: Bundle?): AppointmentSearchArgs? {
            if (bundle == null) return null

            val cityId = bundle.getString(KEY_CITY_ID) ?: return null
            val branchId = bundle.getString(KEY_BRANCH_ID) ?: return null
            val startDateMillis = bundle.getLong(KEY_START_DATE_MILLIS, -1L)
            val endDateMillis = bundle.getLong(KEY_END_DATE_MILLIS, -1L)

            if (startDateMillis <= 0L || endDateMillis <= 0L) return null

            return AppointmentSearchArgs(
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
                cityId = cityId,
                districtId = bundle.getString(KEY_DISTRICT_ID),
                branchId = branchId,
                hospitalId = bundle.getString(KEY_HOSPITAL_ID),
                doctorId = bundle.getString(KEY_DOCTOR_ID)
            )
        }
    }
}

data class DoctorAvailabilityArgs(
    val searchArgs: AppointmentSearchArgs,
    val doctorId: String,
    val doctorName: String,
    val hospitalId: String,
    val hospitalName: String,
    val branchId: String,
    val branchName: String,
    val slotStartHour: Int,
    val slotEndHour: Int,
    val slotDurationMinutes: Int
) {
    fun toBundle(): Bundle {
        return searchArgs.toBundle().apply {
            putString(KEY_SELECTED_DOCTOR_ID, doctorId)
            putString(KEY_SELECTED_DOCTOR_NAME, doctorName)
            putString(KEY_SELECTED_HOSPITAL_ID, hospitalId)
            putString(KEY_SELECTED_HOSPITAL_NAME, hospitalName)
            putString(KEY_SELECTED_BRANCH_ID, branchId)
            putString(KEY_SELECTED_BRANCH_NAME, branchName)
            putInt(KEY_SLOT_START_HOUR, slotStartHour)
            putInt(KEY_SLOT_END_HOUR, slotEndHour)
            putInt(KEY_SLOT_DURATION_MINUTES, slotDurationMinutes)
        }
    }

    companion object {
        fun fromBundle(bundle: Bundle?): DoctorAvailabilityArgs? {
            val searchArgs = AppointmentSearchArgs.fromBundle(bundle) ?: return null
            if (bundle == null) return null

            val doctorId = bundle.getString(KEY_SELECTED_DOCTOR_ID) ?: return null
            val doctorName = bundle.getString(KEY_SELECTED_DOCTOR_NAME) ?: return null
            val hospitalId = bundle.getString(KEY_SELECTED_HOSPITAL_ID) ?: return null
            val hospitalName = bundle.getString(KEY_SELECTED_HOSPITAL_NAME) ?: return null
            val branchId = bundle.getString(KEY_SELECTED_BRANCH_ID) ?: return null
            val branchName = bundle.getString(KEY_SELECTED_BRANCH_NAME) ?: return null

            return DoctorAvailabilityArgs(
                searchArgs = searchArgs,
                doctorId = doctorId,
                doctorName = doctorName,
                hospitalId = hospitalId,
                hospitalName = hospitalName,
                branchId = branchId,
                branchName = branchName,
                slotStartHour = bundle.getInt(KEY_SLOT_START_HOUR, 9),
                slotEndHour = bundle.getInt(KEY_SLOT_END_HOUR, 17),
                slotDurationMinutes = bundle.getInt(KEY_SLOT_DURATION_MINUTES, 20)
            )
        }
    }
}

data class AppointmentConfirmationArgs(
    val doctorId: String,
    val doctorName: String,
    val hospitalId: String,
    val hospitalName: String,
    val branchId: String,
    val branchName: String,
    val dateMillis: Long,
    val timeLabel: String
) {
    fun toBundle(): Bundle {
        return Bundle().apply {
            putString(KEY_SELECTED_DOCTOR_ID, doctorId)
            putString(KEY_SELECTED_DOCTOR_NAME, doctorName)
            putString(KEY_SELECTED_HOSPITAL_ID, hospitalId)
            putString(KEY_SELECTED_HOSPITAL_NAME, hospitalName)
            putString(KEY_SELECTED_BRANCH_ID, branchId)
            putString(KEY_SELECTED_BRANCH_NAME, branchName)
            putLong(KEY_CONFIRM_DATE_MILLIS, dateMillis)
            putString(KEY_CONFIRM_TIME_LABEL, timeLabel)
        }
    }

    companion object {
        fun fromBundle(bundle: Bundle?): AppointmentConfirmationArgs? {
            if (bundle == null) return null

            val doctorId = bundle.getString(KEY_SELECTED_DOCTOR_ID) ?: return null
            val doctorName = bundle.getString(KEY_SELECTED_DOCTOR_NAME) ?: return null
            val hospitalId = bundle.getString(KEY_SELECTED_HOSPITAL_ID) ?: return null
            val hospitalName = bundle.getString(KEY_SELECTED_HOSPITAL_NAME) ?: return null
            val branchId = bundle.getString(KEY_SELECTED_BRANCH_ID) ?: return null
            val branchName = bundle.getString(KEY_SELECTED_BRANCH_NAME) ?: return null
            val dateMillis = bundle.getLong(KEY_CONFIRM_DATE_MILLIS, -1L)
            val timeLabel = bundle.getString(KEY_CONFIRM_TIME_LABEL) ?: return null

            if (dateMillis <= 0L) return null

            return AppointmentConfirmationArgs(
                doctorId = doctorId,
                doctorName = doctorName,
                hospitalId = hospitalId,
                hospitalName = hospitalName,
                branchId = branchId,
                branchName = branchName,
                dateMillis = dateMillis,
                timeLabel = timeLabel
            )
        }
    }
}

private const val KEY_START_DATE_MILLIS = "key_start_date_millis"
private const val KEY_END_DATE_MILLIS = "key_end_date_millis"
private const val KEY_CITY_ID = "key_city_id"
private const val KEY_DISTRICT_ID = "key_district_id"
private const val KEY_BRANCH_ID = "key_branch_id"
private const val KEY_HOSPITAL_ID = "key_hospital_id"
private const val KEY_DOCTOR_ID = "key_doctor_id"

private const val KEY_SELECTED_DOCTOR_ID = "key_selected_doctor_id"
private const val KEY_SELECTED_DOCTOR_NAME = "key_selected_doctor_name"
private const val KEY_SELECTED_HOSPITAL_ID = "key_selected_hospital_id"
private const val KEY_SELECTED_HOSPITAL_NAME = "key_selected_hospital_name"
private const val KEY_SELECTED_BRANCH_ID = "key_selected_branch_id"
private const val KEY_SELECTED_BRANCH_NAME = "key_selected_branch_name"
private const val KEY_SLOT_START_HOUR = "key_slot_start_hour"
private const val KEY_SLOT_END_HOUR = "key_slot_end_hour"
private const val KEY_SLOT_DURATION_MINUTES = "key_slot_duration_minutes"

private const val KEY_CONFIRM_DATE_MILLIS = "key_confirm_date_millis"
private const val KEY_CONFIRM_TIME_LABEL = "key_confirm_time_label"
