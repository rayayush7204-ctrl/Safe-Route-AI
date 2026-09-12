package com.saferouteai.domain.model

/**
 * Domain model representing the local user profile.
 *
 * Designed for local identity without requiring real cloud authentication.
 * Fields are optional and do not require all to be populated.
 */
data class UserProfile(
    val id: String = DEFAULT_USER_ID,
    val displayName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_USER_ID = "local_primary_user"
    }

    /**
     * Domain validation for user profile.
     * Returns an error message if invalid, or null if valid.
     */
    fun validate(): String? {
        if (id.isBlank()) {
            return "User profile ID cannot be blank."
        }
        if (email.isNotBlank() && !email.contains("@")) {
            return "Email address is invalid."
        }
        return null
    }
}
