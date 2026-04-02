package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.menasy.merkezisagliksistemi.data.model.Appointment
import com.menasy.merkezisagliksistemi.data.model.AppointmentStatus
import com.menasy.merkezisagliksistemi.data.model.Medicine
import com.menasy.merkezisagliksistemi.data.model.Prescription
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.AppException
import com.menasy.merkezisagliksistemi.utils.DateTimeUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

class AppointmentDataSource(
    private val firestore: FirebaseFirestore
) {

    private data class AppointmentTransitionContext(
        val ownerDoctorId: String,
        val date: String,
        val time: String
    )

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
                ensureAppointmentCreationDateTimeIsBookable(appointment)

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

    private fun ensureAppointmentCreationDateTimeIsBookable(appointment: Appointment) {
        val appointmentDateTime = DateTimeUtils.parseAppointmentDateTime(
            dateStr = appointment.appointmentDate,
            timeStr = appointment.appointmentTime
        ) ?: throw AppException(AppErrorReason.APPOINTMENT_INFO_MISSING)

        if (!appointmentDateTime.isAfter(DateTimeUtils.currentLocalDateTime())) {
            throw AppException(AppErrorReason.PAST_APPOINTMENT_TIME_NOT_ALLOWED)
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

        return snapshot.documents.mapNotNull(::toAppointment)
    }

    /**
     * Cancels an appointment and removes its lock to free the slot.
     */
    suspend fun cancelAppointment(appointmentId: String, patientId: String): Result<Unit> {
        return try {
            val appointmentRef = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)

            firestore.runTransaction { transaction ->
                val appointmentSnapshot = transaction.get(appointmentRef)
                if (!appointmentSnapshot.exists()) {
                    throw AppException(AppErrorReason.APPOINTMENT_NOT_FOUND)
                }

                val ownerPatientId = appointmentSnapshot.getString(FIELD_PATIENT_ID).orEmpty()
                if (ownerPatientId != patientId) {
                    throw AppException(AppErrorReason.APPOINTMENT_PATIENT_MISMATCH)
                }

                val currentStatus = appointmentSnapshot.getString(FIELD_STATUS).orEmpty()
                if (currentStatus != AppointmentStatus.SCHEDULED.name) {
                    throw AppException(AppErrorReason.APPOINTMENT_CANCELLATION_NOT_ALLOWED)
                }

                val appointmentDate = appointmentSnapshot.getString(FIELD_APPOINTMENT_DATE).orEmpty()
                val appointmentTime = appointmentSnapshot.getString(FIELD_APPOINTMENT_TIME).orEmpty()
                val appointmentMillis = parseAppointmentToMillis(
                    dateStr = appointmentDate,
                    timeStr = appointmentTime
                ) ?: throw AppException(AppErrorReason.APPOINTMENT_INFO_MISSING)

                if (appointmentMillis <= System.currentTimeMillis()) {
                    throw AppException(AppErrorReason.APPOINTMENT_CANCELLATION_NOT_ALLOWED)
                }

                transaction.update(appointmentRef, FIELD_STATUS, AppointmentStatus.CANCELLED.name)

                val ownerDoctorId = appointmentSnapshot.getString(FIELD_DOCTOR_ID).orEmpty()
                if (ownerDoctorId.isNotBlank() && appointmentDate.isNotBlank() && appointmentTime.isNotBlank()) {
                    val lockRef = firestore.collection(LOCKS_COLLECTION).document(
                        buildLockId(ownerDoctorId, appointmentDate, appointmentTime)
                    )
                    transaction.delete(lockRef)
                }
            }.await()

            Result.success(Unit)
        } catch (e: AppException) {
            Result.failure(e)
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

            val appointments = snapshot.documents.mapNotNull(::toAppointment)

            Result.success(appointments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets a single appointment by its ID.
     */
    suspend fun getAppointmentById(appointmentId: String): Result<Appointment> {
        return try {
            val snapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                .document(appointmentId)
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.failure(AppException(AppErrorReason.APPOINTMENT_NOT_FOUND))
            }

            val appointment = toAppointment(snapshot)
                ?: return Result.failure(AppException(AppErrorReason.APPOINTMENT_NOT_FOUND))
            Result.success(appointment)
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
                        ?.mapNotNull(::toAppointment)
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
     * Realtime listener for all appointments that belong to a doctor.
     */
    fun observeDoctorAppointments(doctorId: String): Flow<List<Appointment>> = callbackFlow {
        var registration: ListenerRegistration? = null

        try {
            registration = firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo(FIELD_DOCTOR_ID, doctorId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val appointments = snapshot?.documents
                        ?.mapNotNull(::toAppointment)
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
     * Reads display names from `users` collection in batches.
     */
    suspend fun getUserFullNamesByIds(userIds: Set<String>): Result<Map<String, String>> {
        if (userIds.isEmpty()) return Result.success(emptyMap())

        return try {
            val names = mutableMapOf<String, String>()
            userIds.toList().chunked(MAX_WHERE_IN_BATCH).forEach { chunk ->
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                snapshot.documents.forEach { document ->
                    val fullName = document.getString(FIELD_FULL_NAME)
                    if (!fullName.isNullOrBlank()) {
                        names[document.id] = fullName
                    }
                }
            }
            Result.success(names)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches prescription previews for given appointment IDs.
     * Uses chunked `whereIn` queries to keep reads bounded.
     */
    suspend fun getPrescriptionPreviewsByAppointmentIds(
        appointmentIds: Set<String>
    ): Result<Map<String, Prescription>> {
        if (appointmentIds.isEmpty()) return Result.success(emptyMap())

        return try {
            val prescriptionsByAppointmentId = mutableMapOf<String, Prescription>()

            appointmentIds.toList().chunked(MAX_WHERE_IN_BATCH).forEach { chunk ->
                val snapshotByDocumentId = firestore.collection(PRESCRIPTIONS_COLLECTION)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                snapshotByDocumentId.documents.forEach { document ->
                    val prescription = parsePrescription(document.id, document.data ?: emptyMap())
                        ?: return@forEach
                    prescriptionsByAppointmentId.putLatestByCreatedAt(prescription)
                }

                val unresolvedIds = chunk.toSet() - snapshotByDocumentId.documents.map { it.id }.toSet()
                if (unresolvedIds.isEmpty()) {
                    return@forEach
                }

                val snapshotByField = firestore.collection(PRESCRIPTIONS_COLLECTION)
                    .whereIn(FIELD_APPOINTMENT_ID, unresolvedIds.toList())
                    .get()
                    .await()

                snapshotByField.documents.forEach { document ->
                    val prescription = parsePrescription(document.id, document.data ?: emptyMap())
                        ?: return@forEach
                    prescriptionsByAppointmentId.putLatestByCreatedAt(prescription)
                }
            }

            Result.success(prescriptionsByAppointmentId)
        } catch (e: Exception) {
            Result.failure(e)
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

            val appointments = snapshot.documents.mapNotNull(::toAppointment)

            val now = System.currentTimeMillis()
            val overdueCount = appointments.count { appointment ->
                val appointmentMillis = parseAppointmentToMillis(
                    appointment.appointmentDate,
                    appointment.appointmentTime
                )
                appointmentMillis != null && appointmentMillis < now
            }

            Result.success(overdueCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the total count of completed appointments for a doctor (all time).
     */
    suspend fun getDoctorTotalCompletedCount(doctorId: String): Result<Int> {
        return try {
            val count = firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo(FIELD_DOCTOR_ID, doctorId)
                .whereEqualTo(FIELD_STATUS, AppointmentStatus.COMPLETED.name)
                .get()
                .await()
                .size()

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the count of appointments completed today for a doctor.
     */
    suspend fun getDoctorCompletedTodayCount(doctorId: String, todayDate: String): Result<Int> {
        return try {
            val completedTodayByDate = firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo(FIELD_DOCTOR_ID, doctorId)
                .whereEqualTo(FIELD_STATUS, AppointmentStatus.COMPLETED.name)
                .whereEqualTo(FIELD_COMPLETED_DATE, todayDate)
                .get()
                .await()
                .size()

            val fallbackSnapshot = firestore.collection(APPOINTMENTS_COLLECTION)
                .whereEqualTo(FIELD_DOCTOR_ID, doctorId)
                .whereEqualTo(FIELD_STATUS, AppointmentStatus.COMPLETED.name)
                .whereEqualTo(FIELD_APPOINTMENT_DATE, todayDate)
                .get()
                .await()

            val completedTodayFromLegacyRecords = fallbackSnapshot.documents.count { document ->
                document.getString(FIELD_COMPLETED_DATE).isNullOrBlank()
            }

            Result.success(completedTodayByDate + completedTodayFromLegacyRecords)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates doctor appointment result status from SCHEDULED to COMPLETED or MISSED.
     * Also releases lock document after transition.
     */
    suspend fun completeDoctorAppointmentWithPrescription(
        appointmentId: String,
        doctorId: String,
        prescription: Prescription,
        examinationNote: String = ""
    ): Result<Unit> {
        return try {
            val normalizedMedicines = sanitizeMedicines(prescription.medicines)
            if (normalizedMedicines.isEmpty()) {
                return Result.failure(AppException(AppErrorReason.PRESCRIPTION_MEDICINE_REQUIRED))
            }
            val normalizedExaminationNote = examinationNote.trim()

            val appointmentRef = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)
            val prescriptionRef = firestore.collection(PRESCRIPTIONS_COLLECTION).document(appointmentId)

            firestore.runTransaction { transaction ->
                val appointmentSnapshot = transaction.get(appointmentRef)
                val transition = validateDoctorTransition(
                    appointmentSnapshot = appointmentSnapshot,
                    doctorId = doctorId
                )

                val createdAtMillis = prescription.createdAtMillis
                    .takeIf { it > 0L }
                    ?: System.currentTimeMillis()
                val prescriptionId = prescription.id
                    .takeIf { it.isNotBlank() }
                    ?: appointmentId
                val prescriptionCode = prescription.prescriptionCode
                    .takeIf { it.isNotBlank() }
                    ?: buildPrescriptionCode(
                        appointmentId = appointmentId,
                        createdAtMillis = createdAtMillis
                    )

                val normalizedPrescription = prescription.copy(
                    id = prescriptionId,
                    appointmentId = appointmentId,
                    prescriptionCode = prescriptionCode,
                    createdAtMillis = createdAtMillis,
                    note = prescription.note.trim(),
                    medicines = normalizedMedicines
                )

                transaction.set(prescriptionRef, normalizedPrescription)
                val appointmentUpdates = mutableMapOf<String, Any>(
                    FIELD_STATUS to AppointmentStatus.COMPLETED.name,
                    FIELD_COMPLETED_AT to System.currentTimeMillis(),
                    FIELD_COMPLETED_DATE to LocalDate.now().toString()
                )
                if (normalizedExaminationNote.isNotBlank()) {
                    appointmentUpdates[FIELD_EXAMINATION_NOTE] = normalizedExaminationNote
                }
                transaction.update(appointmentRef, appointmentUpdates)

                if (transition.date.isNotBlank() && transition.time.isNotBlank()) {
                    val lockRef = firestore.collection(LOCKS_COLLECTION).document(
                        buildLockId(transition.ownerDoctorId, transition.date, transition.time)
                    )
                    transaction.delete(lockRef)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates doctor appointment result status from SCHEDULED to COMPLETED or MISSED.
     * Also releases lock document after transition.
     */
    suspend fun updateDoctorAppointmentResultStatus(
        appointmentId: String,
        doctorId: String,
        targetStatus: String,
        examinationNote: String = ""
    ): Result<Unit> {
        return try {
            val allowedStatuses = setOf(
                AppointmentStatus.COMPLETED.name,
                AppointmentStatus.MISSED.name
            )
            if (targetStatus !in allowedStatuses) {
                return Result.failure(AppException(AppErrorReason.APPOINTMENT_STATUS_INVALID_FOR_EXAMINATION))
            }
            val normalizedExaminationNote = examinationNote.trim()

            val appointmentRef = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)

            firestore.runTransaction { transaction ->
                val appointmentSnapshot = transaction.get(appointmentRef)
                val transition = validateDoctorTransition(
                    appointmentSnapshot = appointmentSnapshot,
                    doctorId = doctorId
                )

                val appointmentUpdates = mutableMapOf<String, Any>(
                    FIELD_STATUS to targetStatus
                )
                if (targetStatus == AppointmentStatus.COMPLETED.name) {
                    appointmentUpdates[FIELD_COMPLETED_AT] = System.currentTimeMillis()
                    appointmentUpdates[FIELD_COMPLETED_DATE] = LocalDate.now().toString()
                } else {
                    appointmentUpdates[FIELD_COMPLETED_AT] = FieldValue.delete()
                    appointmentUpdates[FIELD_COMPLETED_DATE] = FieldValue.delete()
                }
                if (normalizedExaminationNote.isNotBlank()) {
                    appointmentUpdates[FIELD_EXAMINATION_NOTE] = normalizedExaminationNote
                }
                transaction.update(appointmentRef, appointmentUpdates)

                if (transition.date.isNotBlank() && transition.time.isNotBlank()) {
                    val lockRef = firestore.collection(LOCKS_COLLECTION).document(
                        buildLockId(transition.ownerDoctorId, transition.date, transition.time)
                    )
                    transaction.delete(lockRef)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateDoctorTransition(
        appointmentSnapshot: DocumentSnapshot,
        doctorId: String
    ): AppointmentTransitionContext {
        if (!appointmentSnapshot.exists()) {
            throw AppException(AppErrorReason.APPOINTMENT_NOT_FOUND)
        }

        val ownerDoctorId = appointmentSnapshot.getString(FIELD_DOCTOR_ID).orEmpty()
        if (ownerDoctorId != doctorId) {
            throw AppException(AppErrorReason.APPOINTMENT_DOCTOR_MISMATCH)
        }

        val currentStatus = appointmentSnapshot.getString(FIELD_STATUS).orEmpty()
        if (currentStatus != AppointmentStatus.SCHEDULED.name) {
            throw AppException(AppErrorReason.APPOINTMENT_STATUS_INVALID_FOR_EXAMINATION)
        }

        val date = appointmentSnapshot.getString(FIELD_APPOINTMENT_DATE).orEmpty()
        val time = appointmentSnapshot.getString(FIELD_APPOINTMENT_TIME).orEmpty()

        val appointmentMillis = parseAppointmentToMillis(date, time)
            ?: throw AppException(AppErrorReason.APPOINTMENT_INFO_MISSING)
        if (appointmentMillis > System.currentTimeMillis()) {
            throw AppException(AppErrorReason.APPOINTMENT_TIME_NOT_REACHED)
        }

        return AppointmentTransitionContext(
            ownerDoctorId = ownerDoctorId,
            date = date,
            time = time
        )
    }

    private fun parseAppointmentToMillis(dateStr: String, timeStr: String): Long? {
        val dateTime = DateTimeUtils.parseAppointmentDateTime(dateStr, timeStr) ?: return null
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun parsePrescription(
        id: String,
        data: Map<String, Any?>
    ): Prescription? {
        val appointmentId = valueAsString(data[FIELD_APPOINTMENT_ID])
            ?.takeIf { it.isNotBlank() }
            ?: id.takeIf { it.isNotBlank() }
            .orEmpty()
        if (appointmentId.isBlank()) return null

        val prescriptionCode = valueAsString(data[FIELD_PRESCRIPTION_CODE])
            ?.takeIf { it.isNotBlank() }
            ?: id

        val note = valueAsString(data[FIELD_NOTE])
            ?: valueAsString(data[FIELD_DOCTOR_NOTE])
            ?: ""

        val medicines = parsePrescriptionMedicines(data[FIELD_MEDICINES])

        return Prescription(
            id = id,
            appointmentId = appointmentId,
            prescriptionCode = prescriptionCode,
            createdAtMillis = parseMillis(data[FIELD_CREATED_AT_MILLIS] ?: data[FIELD_CREATED_AT]),
            note = note,
            medicines = medicines
        )
    }

    private fun toAppointment(document: DocumentSnapshot): Appointment? {
        val appointment = document.toObject(Appointment::class.java) ?: return null
        return if (appointment.id.isBlank()) {
            appointment.copy(id = document.id)
        } else {
            appointment
        }
    }

    private fun MutableMap<String, Prescription>.putLatestByCreatedAt(prescription: Prescription) {
        val existing = this[prescription.appointmentId]
        if (existing == null || prescription.createdAtMillis > existing.createdAtMillis) {
            this[prescription.appointmentId] = prescription
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parsePrescriptionMedicines(value: Any?): List<Medicine> {
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val map = item as? Map<String, Any?> ?: return@mapNotNull null

            val medicineName = valueAsString(map[FIELD_MEDICINE_NAME])
                ?: valueAsString(map[FIELD_NAME])
                ?: ""
            if (medicineName.isBlank()) return@mapNotNull null

            val medicineId = valueAsString(map[FIELD_MEDICINE_ID])
                ?: valueAsString(map[FIELD_ID])
                ?: buildMedicineIdFallback(medicineName)

            Medicine(
                medicineId = medicineId,
                medicineName = medicineName,
                dosage = valueAsString(map[FIELD_DOSAGE]) ?: "",
                frequency = valueAsString(map[FIELD_FREQUENCY]) ?: "",
                usageDescription = valueAsString(map[FIELD_USAGE_DESCRIPTION]) ?: "",
                doctorNote = valueAsString(map[FIELD_DOCTOR_NOTE]) ?: ""
            )
        }
    }

    private fun sanitizeMedicines(medicines: List<Medicine>): List<Medicine> {
        return medicines.mapNotNull { medicine ->
            val medicineName = medicine.medicineName.trim()
            if (medicineName.isBlank()) return@mapNotNull null

            val medicineId = medicine.medicineId
                .takeIf { it.isNotBlank() }
                ?: buildMedicineIdFallback(medicineName)

            medicine.copy(
                medicineId = medicineId,
                medicineName = medicineName,
                dosage = medicine.dosage.trim(),
                frequency = medicine.frequency.trim(),
                usageDescription = medicine.usageDescription.trim(),
                doctorNote = medicine.doctorNote.trim()
            )
        }
    }

    private fun buildPrescriptionCode(appointmentId: String, createdAtMillis: Long): String {
        val suffix = appointmentId.takeLast(6).uppercase()
        val millisPart = (createdAtMillis % 100000L).toString().padStart(5, '0')
        return "RX-$suffix-$millisPart"
    }

    private fun buildMedicineIdFallback(medicineName: String): String {
        val normalized = medicineName
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return normalized.ifBlank { "medicine_${System.currentTimeMillis()}" }
    }

    private fun parseMillis(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            is Timestamp -> value.toDate().time
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    private fun valueAsString(value: Any?): String? {
        return when (value) {
            null -> null
            is String -> value
            else -> value.toString()
        }
    }

    private fun buildLockId(doctorId: String, date: String, time: String): String {
        return "${doctorId}_${date}_${time}"
    }

    private companion object {
        const val APPOINTMENTS_COLLECTION = "appointments"
        const val LOCKS_COLLECTION = "appointmentLocks"
        const val USERS_COLLECTION = "users"
        const val PRESCRIPTIONS_COLLECTION = "prescriptions"

        const val FIELD_DOCTOR_ID = "doctorId"
        const val FIELD_PATIENT_ID = "patientId"
        const val FIELD_APPOINTMENT_DATE = "appointmentDate"
        const val FIELD_APPOINTMENT_TIME = "appointmentTime"
        const val FIELD_STATUS = "status"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_CREATED_AT_MILLIS = "createdAtMillis"
        const val FIELD_DATE = "date"
        const val FIELD_TIME = "time"
        const val FIELD_EXAMINATION_NOTE = "examinationNote"
        const val FIELD_COMPLETED_AT = "completedAt"
        const val FIELD_COMPLETED_DATE = "completedDate"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_APPOINTMENT_ID = "appointmentId"
        const val FIELD_PRESCRIPTION_CODE = "prescriptionCode"
        const val FIELD_NOTE = "note"
        const val FIELD_MEDICINES = "medicines"
        const val FIELD_MEDICINE_ID = "medicineId"
        const val FIELD_MEDICINE_NAME = "medicineName"
        const val FIELD_NAME = "name"
        const val FIELD_ID = "id"
        const val FIELD_DOSAGE = "dosage"
        const val FIELD_FREQUENCY = "frequency"
        const val FIELD_USAGE_DESCRIPTION = "usageDescription"
        const val FIELD_DOCTOR_NOTE = "doctorNote"

        const val MAX_ACTIVE_APPOINTMENTS = 5
        const val MAX_WHERE_IN_BATCH = 10
    }
}
