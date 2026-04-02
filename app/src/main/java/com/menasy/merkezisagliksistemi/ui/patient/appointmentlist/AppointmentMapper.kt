package com.menasy.merkezisagliksistemi.ui.patient.appointmentlist

import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.model.AppointmentStatus
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.data.remote.local.BranchDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.DoctorDataSource
import com.menasy.merkezisagliksistemi.data.remote.local.HospitalDataSource
import com.menasy.merkezisagliksistemi.ui.common.adapter.PrescriptionListItem
import com.menasy.merkezisagliksistemi.utils.DateTimeUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class DoctorAppointmentsSections(
    val pendingAppointments: List<PatientAppointmentItem>,
    val upcomingAppointments: List<PatientAppointmentItem>,
    val pastAppointments: List<PatientAppointmentItem>
)

/**
 * Shared appointment mapper used by both patient and doctor appointment screens.
 * Resolves local seed-based names and applies role specific card/action rules.
 */
class AppointmentMapper(
    private val doctorDataSource: DoctorDataSource,
    private val hospitalDataSource: HospitalDataSource,
    private val branchDataSource: BranchDataSource
) {

    /**
     * Maps a single Appointment to patient list UI model.
     */
    fun mapToUiModel(appointment: Appointment): PatientAppointmentItem {
        val doctor = doctorDataSource.getDoctorById(appointment.doctorId)
        val isActive = isAppointmentActive(appointment)

        return PatientAppointmentItem(
            id = appointment.id,
            hospitalName = resolveHospitalName(appointment.hospitalId),
            branchName = resolveBranchName(appointment.branchId),
            doctorName = doctor?.fullName ?: "Bilinmeyen Doktor",
            dateMillis = DateTimeUtils.dateStringToMillis(appointment.appointmentDate),
            timeLabel = appointment.appointmentTime,
            status = appointment.status,
            isActive = isActive
        )
    }

    fun mapToUiModelList(appointments: List<Appointment>): List<PatientAppointmentItem> {
        return appointments.map(::mapToUiModel)
    }

    /**
     * Patient partitions:
     * - Active = SCHEDULED + future
     * - Past = all remaining
     */
    fun partitionAppointments(
        appointments: List<Appointment>,
        prescriptionByAppointmentId: Map<String, Prescription> = emptyMap()
    ): Pair<List<PatientAppointmentItem>, List<PatientAppointmentItem>> {
        val active = mutableListOf<PatientAppointmentItem>()
        val past = mutableListOf<PatientAppointmentItem>()

        appointments.forEach { appointment ->
            val uiModel = mapToUiModel(appointment)
            if (uiModel.isActive) {
                active.add(uiModel)
            } else {
                val appointmentDateTime = DateTimeUtils.parseAppointmentDateTime(
                    dateStr = appointment.appointmentDate,
                    timeStr = appointment.appointmentTime
                )
                val appointmentDateTimeMillis = appointmentDateTime
                    ?.atZone(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli()
                    ?: DateTimeUtils.dateStringToMillis(appointment.appointmentDate)

                val prescription = prescriptionByAppointmentId[appointment.id]?.let { value ->
                    if (value.createdAtMillis > 0L) {
                        value
                    } else {
                        val normalizedCreatedAtMillis = appointment.completedAt.takeIf { it > 0L }
                            ?: appointmentDateTimeMillis
                        value.copy(createdAtMillis = normalizedCreatedAtMillis)
                    }
                }
                val examinationNote = appointment.examinationNote.trim()

                past.add(
                    uiModel.copy(
                        isPastStyle = true,
                        actionText = if (prescription != null) "Reçetemi Görüntüle" else null,
                        actionType = if (prescription != null) {
                            AppointmentActionType.VIEW_PRESCRIPTION
                        } else {
                            null
                        },
                        actionStyle = AppointmentActionStyle.PRIMARY,
                        examinationNote = examinationNote,
                        prescription = prescription
                    )
                )
            }
        }

        active.sortBy { it.dateMillis }
        past.sortByDescending { it.dateMillis }

        return Pair(active, past)
    }

    /**
     * Doctor partitions:
     * - Pending: SCHEDULED and datetime <= now
     * - Upcoming: SCHEDULED and datetime > now and date = selectedDate
     * - Past: COMPLETED / MISSED / CANCELLED
     */
    fun partitionDoctorAppointments(
        appointments: List<Appointment>,
        selectedDate: LocalDate,
        patientNamesById: Map<String, String>,
        prescriptionByAppointmentId: Map<String, Prescription>
    ): DoctorAppointmentsSections {
        val now = LocalDateTime.now()
        val pending = mutableListOf<Pair<LocalDateTime, PatientAppointmentItem>>()
        val upcoming = mutableListOf<Pair<LocalDateTime, PatientAppointmentItem>>()
        val past = mutableListOf<Pair<LocalDateTime, PatientAppointmentItem>>()

        appointments.forEach { appointment ->
            val dateTime = DateTimeUtils.parseAppointmentDateTime(
                dateStr = appointment.appointmentDate,
                timeStr = appointment.appointmentTime
            ) ?: return@forEach
            val dateTimeMillis = dateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val patientName = patientNamesById[appointment.patientId]
                ?: "Bilinmeyen Hasta"

            val commonItem = PatientAppointmentItem(
                id = appointment.id,
                hospitalName = resolveHospitalName(appointment.hospitalId),
                branchName = resolveBranchName(appointment.branchId),
                doctorName = patientName,
                dateMillis = DateTimeUtils.dateStringToMillis(appointment.appointmentDate),
                timeLabel = appointment.appointmentTime,
                status = appointment.status,
                isActive = appointment.status == AppointmentStatus.SCHEDULED.name,
                personIconRes = R.drawable.ic_account_person_24
            )

            when {
                appointment.status == AppointmentStatus.SCHEDULED.name && !dateTime.isAfter(now) -> {
                    pending.add(
                        dateTime to commonItem.copy(
                            isPastStyle = false,
                            statusTextOverride = "Bekleyen Muayene",
                            actionText = "Muayene Et",
                            actionType = AppointmentActionType.EXAMINE,
                            actionStyle = AppointmentActionStyle.PRIMARY
                        )
                    )
                }

                appointment.status == AppointmentStatus.SCHEDULED.name &&
                    dateTime.isAfter(now) &&
                    dateTime.toLocalDate() == selectedDate -> {
                    upcoming.add(
                        dateTime to commonItem.copy(
                            isPastStyle = false,
                            statusTextOverride = "Planlandı",
                            actionText = null,
                            actionType = null
                        )
                    )
                }

                appointment.status == AppointmentStatus.COMPLETED.name ||
                    appointment.status == AppointmentStatus.MISSED.name ||
                    appointment.status == AppointmentStatus.CANCELLED.name -> {
                    val prescription = prescriptionByAppointmentId[appointment.id]?.let { value ->
                        if (value.createdAtMillis > 0L) {
                            value
                        } else {
                            value.copy(createdAtMillis = dateTimeMillis)
                        }
                    }
                    val examinationNote = appointment.examinationNote.trim()
                    past.add(
                        dateTime to commonItem.copy(
                            isPastStyle = true,
                            statusTextOverride = resolveAppointmentStatusLabel(appointment.status),
                            actionText = if (prescription != null) "Reçeteyi Görüntüle" else null,
                            actionType = if (prescription != null) {
                                AppointmentActionType.VIEW_PRESCRIPTION
                            } else {
                                null
                            },
                            actionStyle = AppointmentActionStyle.PRIMARY,
                            examinationNote = examinationNote,
                            prescription = prescription
                        )
                    )
                }
            }
        }

        val pendingSorted = pending.sortedBy { it.first }.map { it.second }
        val upcomingSorted = upcoming.sortedBy { it.first }.map { it.second }
        val pastSorted = past.sortedByDescending { it.first }.map { it.second }

        return DoctorAppointmentsSections(
            pendingAppointments = pendingSorted,
            upcomingAppointments = upcomingSorted,
            pastAppointments = pastSorted
        )
    }

    fun mapDoctorPrescriptionItems(
        appointments: List<Appointment>,
        patientNamesById: Map<String, String>,
        prescriptionByAppointmentId: Map<String, Prescription>
    ): List<PrescriptionListItem> {
        return appointments.mapNotNull { appointment ->
            val prescription = prescriptionByAppointmentId[appointment.id] ?: return@mapNotNull null
            val patientName = patientNamesById[appointment.patientId] ?: "Bilinmeyen Hasta"
            mapToPrescriptionItem(
                appointment = appointment,
                prescription = prescription,
                personName = patientName
            )
        }.sortedByDescending { it.createdAtMillis }
    }

    fun mapPatientPrescriptionItems(
        appointments: List<Appointment>,
        prescriptionByAppointmentId: Map<String, Prescription>
    ): List<PrescriptionListItem> {
        return appointments.mapNotNull { appointment ->
            val prescription = prescriptionByAppointmentId[appointment.id] ?: return@mapNotNull null
            val doctorName = doctorDataSource.getDoctorById(appointment.doctorId)?.fullName
                ?: "Bilinmeyen Doktor"
            mapToPrescriptionItem(
                appointment = appointment,
                prescription = prescription,
                personName = doctorName
            )
        }.sortedByDescending { it.createdAtMillis }
    }

    private fun mapToPrescriptionItem(
        appointment: Appointment,
        prescription: Prescription,
        personName: String
    ): PrescriptionListItem {
        val appointmentDateTime = DateTimeUtils.parseAppointmentDateTime(
            dateStr = appointment.appointmentDate,
            timeStr = appointment.appointmentTime
        )
        val appointmentDateMillis = appointmentDateTime
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
            ?: DateTimeUtils.dateStringToMillis(appointment.appointmentDate)

        val normalizedCreatedAtMillis = prescription.createdAtMillis.takeIf { it > 0L }
            ?: appointment.completedAt.takeIf { it > 0L }
            ?: appointmentDateMillis

        val normalizedPrescription = if (prescription.createdAtMillis > 0L) {
            prescription
        } else {
            prescription.copy(createdAtMillis = normalizedCreatedAtMillis)
        }

        return PrescriptionListItem(
            appointmentId = appointment.id,
            prescriptionCode = normalizedPrescription.prescriptionCode.ifBlank { "-" },
            createdAtMillis = normalizedCreatedAtMillis,
            personName = personName,
            hospitalName = resolveHospitalName(appointment.hospitalId),
            branchName = resolveBranchName(appointment.branchId),
            appointmentDateMillis = appointmentDateMillis,
            appointmentTimeLabel = appointment.appointmentTime,
            appointmentStatusLabel = resolveAppointmentStatusLabel(appointment.status),
            medicineCount = normalizedPrescription.medicines.size,
            examinationNote = appointment.examinationNote.trim(),
            note = normalizedPrescription.note.trim(),
            prescription = normalizedPrescription
        )
    }

    private fun resolveHospitalName(hospitalId: String): String {
        return hospitalDataSource.getHospitalById(hospitalId)?.name ?: "Bilinmeyen Hastane"
    }

    private fun resolveBranchName(branchId: String): String {
        return branchDataSource.getBranchById(branchId)?.name ?: "Bilinmeyen Bölüm"
    }

    private fun isAppointmentActive(appointment: Appointment): Boolean {
        if (appointment.status != AppointmentStatus.SCHEDULED.name) {
            return false
        }

        return DateTimeUtils.isAppointmentInFuture(
            appointment.appointmentDate,
            appointment.appointmentTime
        )
    }

    private fun resolveAppointmentStatusLabel(status: String): String {
        return when (status) {
            AppointmentStatus.COMPLETED.name -> "Tamamlandı"
            AppointmentStatus.MISSED.name -> "Katılmadı"
            AppointmentStatus.CANCELLED.name -> "İptal Edildi"
            AppointmentStatus.SCHEDULED.name -> "Planlandı"
            else -> "Geçmiş Randevu"
        }
    }
}
