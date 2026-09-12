package com.saferouteai.domain.model

import java.util.UUID

/**
 * Domain entity representing a designated trusted contact.
 *
 * @property id Unique identifier.
 * @property name Display name of contact (must not be blank).
 * @property relationship Relationship to user.
 * @property phoneNumber Phone number for emergency SMS/voice.
 * @property email Email address for emergency alerts.
 * @property isEnabled Whether the contact is active in safety dispatches.
 * @property createdAt Epoch timestamp of creation.
 * @property updatedAt Epoch timestamp of last update.
 */
data class TrustedContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relationship: ContactRelationship = ContactRelationship.OTHER,
    val phoneNumber: String = "",
    val email: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Domain validation rules:
     * - ID must be non-blank
     * - Name cannot be blank
     * - Must have at least one usable contact method (phone or email)
     */
    fun validate(): String? {
        if (id.isBlank()) {
            return "Contact ID cannot be blank."
        }
        if (name.trim().isBlank()) {
            return "Contact name cannot be blank."
        }
        if (phoneNumber.trim().isBlank() && email.trim().isBlank()) {
            return "Contact must have at least one usable contact method (phone number or email)."
        }
        if (email.isNotBlank() && !email.contains("@")) {
            return "Invalid email format."
        }
        return null
    }
}
