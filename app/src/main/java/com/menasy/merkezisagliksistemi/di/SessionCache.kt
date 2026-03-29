package com.menasy.merkezisagliksistemi.di

/**
 * In-memory cache for current user session data.
 * Used to avoid redundant Firestore reads during app lifecycle.
 *
 * Populate on login, check on splash, clear on logout.
 */
object SessionCache {

    private var _userId: String? = null
    private var _role: String? = null
    private var _fullName: String? = null
    private var _doctorId: String? = null

    val userId: String? get() = _userId
    val role: String? get() = _role
    val fullName: String? get() = _fullName

    /**
     * Doktor kullanıcıları için business doctor ID.
     * Randevu ve reçete sorgularında kullanılır.
     * Hasta kullanıcıları için null döner.
     */
    val doctorId: String? get() = _doctorId

    val isPopulated: Boolean
        get() = _userId != null && _role != null

    /**
     * Hasta kullanıcısı için session bilgilerini doldurur.
     */
    fun populate(userId: String, role: String, fullName: String) {
        _userId = userId
        _role = role
        _fullName = fullName
        _doctorId = null
    }

    /**
     * Doktor kullanıcısı için session bilgilerini doldurur.
     *
     * @param userId Firebase Auth UID
     * @param role Kullanıcı rolü ("doctor")
     * @param fullName Doktor tam adı
     * @param doctorId Business doctor ID (Doctor.id)
     */
    fun populateDoctor(userId: String, role: String, fullName: String, doctorId: String) {
        _userId = userId
        _role = role
        _fullName = fullName
        _doctorId = doctorId
    }

    fun clear() {
        _userId = null
        _role = null
        _fullName = null
        _doctorId = null
    }
}
