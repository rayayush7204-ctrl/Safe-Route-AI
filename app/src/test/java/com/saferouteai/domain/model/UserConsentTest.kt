package com.saferouteai.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserConsentTest {

    @Test
    fun defaultConsent_isStrictlyFalse() {
        val consent = UserConsent()
        assertFalse("Location sharing consent must default to false", consent.locationSharingConsent)
        assertFalse("Trusted contact sharing consent must default to false", consent.trustedContactSharingConsent)
    }

    @Test
    fun explicitEnableLocation_doesNotAffectContactConsent() {
        val initial = UserConsent()
        val updated = initial.copy(locationSharingConsent = true)
        assertTrue(updated.locationSharingConsent)
        assertFalse("Trusted contact consent must not be automatically enabled", updated.trustedContactSharingConsent)
    }

    @Test
    fun explicitEnableContacts_doesNotAffectLocationConsent() {
        val initial = UserConsent()
        val updated = initial.copy(trustedContactSharingConsent = true)
        assertTrue(updated.trustedContactSharingConsent)
        assertFalse("Location consent must not be automatically enabled", updated.locationSharingConsent)
    }
}
