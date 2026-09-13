package com.saferouteai.domain.model

/**
 * Domain model representing explicit user consent.
 *
 * Requirements:
 * - locationSharingConsent defaults to false
 * - trustedContactSharingConsent defaults to false
 * - Consent is never inferred from adding a trusted contact or any implicit action
 * - Explicit opt-in only
 */
data class UserConsent(
    val locationSharingConsent: Boolean = false,
    val trustedContactSharingConsent: Boolean = false,
    val audioProcessingConsent: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
