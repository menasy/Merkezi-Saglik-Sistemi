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

    val userId: String? get() = _userId
    val role: String? get() = _role
    val fullName: String? get() = _fullName

    val isPopulated: Boolean
        get() = _userId != null && _role != null

    fun populate(userId: String, role: String, fullName: String) {
        _userId = userId
        _role = role
        _fullName = fullName
    }

    fun clear() {
        _userId = null
        _role = null
        _fullName = null
    }
}
