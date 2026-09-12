package com.saferouteai.domain.model

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileValidationTest {

    @Test
    fun validProfile_returnsNullValidationError() {
        val profile = UserProfile(
            displayName = "Jane Doe",
            phoneNumber = "+1234567890",
            email = "jane@example.com"
        )
        assertNull(profile.validate())
    }

    @Test
    fun profileWithEmptyOptionalFields_isValid() {
        val profile = UserProfile(
            displayName = "",
            phoneNumber = "",
            email = ""
        )
        assertNull(profile.validate())
    }

    @Test
    fun blankId_failsValidation() {
        val profile = UserProfile(id = "")
        assertNotNull(profile.validate())
    }

    @Test
    fun invalidEmail_failsValidation() {
        val profile = UserProfile(email = "invalid-email-string")
        val error = profile.validate()
        assertNotNull(error)
    }
}
