package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.model.AppointmentStatus
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.AppException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AppointmentDataSource(
    private val firestore: FirebaseFirestore
) {

    /**
     * Realtime listener for occupied time slots of a specific doctor on a specific date.
     * Only returns slots with SCHEDULED status.
     */
    fun observeOccupiedSlots(doctorId: String, date: String): Flow<Set<String>> = callbackFlow {
        var registration: ListenerRegistration? = null

        try {
            registration = firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo(FIELD_DOCTOR_ID, doctorId)
                .whereEqualTo(FIELD_APPOINTMENT_DATE, date)
                .whereEqualTo(FIELD_STATUS, AppointmentStatus.SCHEDULED.name)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val occupiedSlots = snapshot?.documents
                        ?.mapNotNull { it.getString(FIELD_APPOINTMENT_TIME) }
                        ?.toSet()
                        ?: emptySet()

                    trySend(occupiedSlots)
                }
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * Creates an appointment using Firestore transaction to prevent double-booking.
     * 
     * Transaction flow:
     * 1. Validate business rules (active appointment limits)
     * 2. Check if lock exists for the slot
     * 3. If lock exists → abort with SLOT_ALREADY_TAKEN error
     * 4. If no lock → create lock + appointment atomically
     *
     * Business Rules:
     * - A patient cannot have more than one active appointment with the same doctor
     * - A patient cannot have more than 5 active appointments in total
     *
     * @return Result containing the appointment ID on success
     */
    suspend fun createAppointmentWithLock(appointment: Appointment): Result<String> {
        return try {
            // Fetch patient's active appointments before transaction
            val activeAppointments = getPatientActiveAppointments(appointment.patientId)

            // Business Rule 1: Check if patient already has an active appointment with this doctor
            val hasActiveAppointmentWithDoctor = activeAppointments.any { 
                it.doctorId == appointment.doctorId 
            }
            if (hasActiveAppointmentWithDoctor) {
                return Result.failure(AppException(AppErrorReason.ACTIVE_APPOINTMENT_EXISTS_FOR_DOCTOR))
            }

            // Business Rule 2: Check if patient has reached the maximum active appointments limit
            if (activeAppointments.size >= MAX_ACTIVE_APPOINTMENTS) {
                return Result.failure(AppException(AppErrorReason.MAX_ACTIVE_APPOINTMENTS_REACHED))
            }

            val lockId = buildLockId(
                doctorId = appointment.doctorId,
                date = appointment.appointmentDate,
                time = appointment.appointmentTime
            )

            val appointmentId = appointment.id.ifEmpty {
                firestore.collection(APPOINTMENTS_COLLECTION).document().id
            }

            val lockRef = firestore.collection(LOCKS_COLLECTION).document(lockId)
            val appointmentRef = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)

            firestore.runTransaction { transaction ->
                val lockSnapshot = transaction.get(lockRef)

                if (lockSnapshot.exists()) {
                    throw AppException(AppErrorReason.SLOT_ALREADY_TAKEN)
                }

                val lockData = mapOf(
                    FIELD_DOCTOR_ID to appointment.doctorId,
                    FIELD_DATE to appointment.appointmentDate,
                    FIELD_TIME to appointment.appointmentTime,
                    FIELD_CREATED_AT to System.currentTimeMillis()
                )
                transaction.set(lockRef, lockData)

                val appointmentData = appointment.copy(id = appointmentId)
                transaction.set(appointmentRef, appointmentData)

                appointmentId
            }.await()

            Result.success(appointmentId)
        } catch (e: AppException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets all active (SCHEDULED) appointments for a patient.
     */
    private suspend fun getPatientActiveAppointments(patientId: String): List<Appointment> {
        val snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
            .whereEqualTo(FIELD_PATIENT_ID, patientId)
            .whereEqualTo(FIELD_STATUS, AppointmentStatus.SCHEDULED.name)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Appointment::class.java)
        }
    }

    /**
     * Cancels an appointment and removes its lock to free the slot.
     */
    suspend fun cancelAppointment(appointmentId: String): Result<Unit> {
        return try {
            val appointmentRef = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)
            val appointmentSnapshot = appointmentRef.get().await()

            if (!appointmentSnapshot.exists()) {
                return Result.failure(AppException(AppErrorReason.APPOINTMENT_NOT_FOUND))
            }

            val doctorId = appointmentSnapshot.getString(FIELD_DOCTOR_ID) ?: ""
            val date = appointmentSnapshot.getString(FIELD_APPOINTMENT_DATE) ?: ""
            val time = appointmentSnapshot.getString(FIELD_APPOINTMENT_TIME) ?: ""

            val lockId = buildLockId(doctorId, date, time)
            val lockRef = firestore.collection(LOCKS_COLLECTION).document(lockId)

            firestore.runTransaction { transaction ->
                transaction.update(appointmentRef, FIELD_STATUS, AppointmentStatus.CANCELLED.name)
                transaction.delete(lockRef)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets all appointments for a specific patient.
     */
    suspend fun getPatientAppointments(patientId: String): Result<List<Appointment>> {
        return try {
            val snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo(FIELD_PATIENT_ID, patientId)
                .get()
                .await()

            val appointments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Appointment::class.java)
            }

            Result.success(appointments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Realtime listener for patient appointments.
     */
    fun observePatientAppointments(patientId: String): Flow<List<Appointment>> = callbackFlow {
        var registration: ListenerRegistration? = null

        try {
            registration = firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo(FIELD_PATIENT_ID, patientId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val appointments = snapshot?.documents
                        ?.mapNotNull { it.toObject(Appointment::class.java) }
                        ?: emptyList()

                    trySend(appointments)
                }
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * Gets the count of overdue appointments for a doctor.
     * Overdue = SCHEDULED status + appointment datetime is in the past.
     */
    suspend fun getDoctorOverdueAppointmentCount(doctorId: String): Result<Int> {
        return try {
            val snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo(FIELD_DOCTOR_ID, doctorId)
                .whereEqualTo(FIELD_STATUS, AppointmentStatus.SCHEDULED.name)
                .get()
                .await()

            val appointments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Appointment::class.java)
            }

            val now = System.currentTimeMillis()
            val overdueCount = appointments.count { appointment ->
                val appointmentMillis = parseAppointmentToMillis(
                    appointment.appointmentDate,
                    appointment.appointmentTime
                )
                appointmentMillis < now
            }

            Result.success(overdueCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the count of all scheduled appointments for a doctor.
     * This includes both future and past appointments with SCHEDULED status.
     */
    suspend fun getDoctorScheduledAppointmentCount(doctorId: String): Result<Int> {
        return try {
            val snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo(FIELD_DOCTOR_ID, doctorId)
                .whereEqualTo(FIELD_STATUS, AppointmentStatus.SCHEDULED.name)
                .get()
                .await()

            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the count of appointments completed today for a doctor.
     */
    suspend fun getDoctorCompletedTodayCount(doctorId: String, todayDate: String): Result<Int> {
        return try {
            val snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo(FIELD_DOCTOR_ID, doctorId)
                .whereEqualTo(FIELD_STATUS, AppointmentStatus.COMPLETED.name)
                .whereEqualTo(FIELD_APPOINTMENT_DATE, todayDate)
                .get()
                .await()

            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseAppointmentToMillis(dateStr: String, timeStr: String): Long {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val dateTime = java.time.LocalDateTime.parse("$dateStr $timeStr", formatter)
            dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }

    private fun buildLockId(doctorId: String, date: String, time: String): String {
        return "${doctorId}_${date}_${time}"
    }

    private companion object {
        const val APPOINTMENTS_COLLECTION = "appointments"
        const val LOCKS_COLLECTION = "appointmentLocks"

        const val FIELD_DOCTOR_ID = "doctorId"
        const val FIELD_PATIENT_ID = "patientId"
        const val FIELD_APPOINTMENT_DATE = "appointmentDate"
        const val FIELD_APPOINTMENT_TIME = "appointmentTime"
        const val FIELD_STATUS = "status"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_DATE = "date"
        const val FIELD_TIME = "time"

        const val MAX_ACTIVE_APPOINTMENTS = 5
    }
}
