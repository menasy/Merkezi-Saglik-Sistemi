package com.menasy.merkezisagliksistemi.data.model

/**
 * Login işlemi sonucu.
 *
 * @property uid Firebase Auth UID
 * @property role Kullanıcı rolü ("patient" veya "doctor")
 * @property fullName Kullanıcı tam adı
 * @property doctorId Doktor kullanıcıları için business doctor ID.
 *                    Hasta kullanıcıları için null.
 */
data class LoginResult(
    val uid: String,
    val role: String,
    val fullName: String,
    val doctorId: String? = null
)
