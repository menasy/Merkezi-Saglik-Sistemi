package com.menasy.merkezisagliksistemi.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.menasy.merkezisagliksistemi.data.model.Patient
import com.menasy.merkezisagliksistemi.data.model.User
import com.menasy.merkezisagliksistemi.data.model.LoginResult
import com.menasy.merkezisagliksistemi.ui.common.error.AppErrorReason
import com.menasy.merkezisagliksistemi.ui.common.error.AppException
import kotlinx.coroutines.tasks.await

class AuthDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun registerPatient(
        fullName: String,
        email: String,
        password: String,
        tcNo: String,
        birthDate: String,
        gender: String
    ): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(AppException(AppErrorReason.USER_UID_MISSING))

            val user = User(
                id = uid,
                fullName = fullName,
                email = email,
                role = "patient",
                createdAt = System.currentTimeMillis()
            )

            val patient = Patient(
                userId = uid,
                tcNo = tcNo,
                birthDate = birthDate,
                gender = gender
            )

            firestore.collection("users")
                .document(uid)
                .set(user)
                .await()

            firestore.collection("patients")
                .document(uid)
                .set(patient)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<LoginResult> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(AppException(AppErrorReason.USER_NOT_FOUND))

            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.failure(AppException(AppErrorReason.USER_RECORD_NOT_FOUND))
            }

            val role = snapshot.getString("role")
                ?: return Result.failure(AppException(AppErrorReason.USER_ROLE_NOT_FOUND))

            val fullName = snapshot.getString("fullName") ?: ""

            Result.success(LoginResult(uid = uid, role = role, fullName = fullName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getCurrentUserRole(): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(AppException(AppErrorReason.NO_ACTIVE_SESSION))

            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val role = snapshot.getString("role")
                ?: return Result.failure(AppException(AppErrorReason.USER_ROLE_NOT_FOUND))

            Result.success(role)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserFullName(): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(AppException(AppErrorReason.NO_ACTIVE_SESSION))

            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.failure(AppException(AppErrorReason.USER_RECORD_NOT_FOUND))
            }

            val fullName = snapshot.getString("fullName")
                ?: return Result.failure(AppException(AppErrorReason.USER_FULL_NAME_NOT_FOUND))

            Result.success(fullName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}
