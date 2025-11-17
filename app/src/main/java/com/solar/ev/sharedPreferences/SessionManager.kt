package com.solar.ev.sharedPreferences // Or your actual package

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.semantics.role
import com.solar.ev.model.User
import kotlinx.serialization.builtins.UByteArraySerializer

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("EvableSolar", Context.MODE_PRIVATE)

    companion object {
        const val USER_TOKEN = "user_token"
        const val USER_NAME = "user_name"
        const val USER_EMAIL = "user_email" // Already exists
        const val USER_ROLE = "user_role"
        const val USER_ID = "user_id"
        const val USER_PROFILE_PHOTO_URL = "user_photo"
        const val USER_EMAIL_VERIFIED_AT = "user_email_verified_at"
    }

    // Token Management
    fun saveAuthToken(token: String) {
        prefs.edit().putString(USER_TOKEN, token).apply()
    }

    fun getUserToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    // User Data Management
    fun saveUserId(id: String) {
        prefs.edit().putString(USER_ID, id).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(USER_ID, null)
    }

    fun saveUserName(name: String) {
        prefs.edit().putString(USER_NAME, name).apply()
    }

    fun getUserName(): String? {
        return prefs.getString(USER_NAME, null)
    }

    fun saveUserEmail(email: String) { // Existing method
        prefs.edit().putString(USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? { // Existing method
        return prefs.getString(USER_EMAIL, null)
    }

    fun saveUserRole(role: String) {
        prefs.edit().putString(USER_ROLE, role).apply()
    }

    fun getUserRole(): String? {
        return prefs.getString(USER_ROLE, null)
    }

    fun getProfilePhotoUrl() : String? {
        return prefs.getString(USER_PROFILE_PHOTO_URL, null)

    }
    fun getUserEmailVerifiedAt() : String? {
        return prefs.getString(USER_EMAIL_VERIFIED_AT, null)
    }
    fun clearSession() {
        val editor = prefs.edit()
        editor.remove(USER_TOKEN)
        editor.remove(USER_ID)
        editor.remove(USER_NAME)
        editor.remove(USER_EMAIL)
        editor.remove(USER_ROLE)
        editor.remove(USER_PROFILE_PHOTO_URL)
        editor.remove(USER_EMAIL_VERIFIED_AT)
        // Remove other keys as needed
        editor.apply()
    }

    // Inside SessionManager class:
    fun isLoggedIn(): Boolean {
        return getUserToken() != null // It checks if a user token exists
    }

    fun saveUserData(user: User) {
        val editor = prefs.edit()
        editor.putString(USER_ID, user.id)
        user.name?.let { editor.putString(USER_NAME, it) }
        user.email?.let { editor.putString(USER_EMAIL, it) }
        user.role?.let { editor.putString(USER_ROLE, it) }
        // Save other fields as needed, e.g.:
        user.profile_photo?.let { editor.putString(USER_PROFILE_PHOTO_URL, it) }

        user.email_verified_at?.let { editor.putString(USER_EMAIL_VERIFIED_AT, it.toString()) }
        editor.apply()
    }
}