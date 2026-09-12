package com.saferouteai.domain.model

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TrustedContactValidationTest {

    @Test
    fun validContactWithPhoneAndEmail_isValid() {
        val contact = TrustedContact(
            name = "Sarah Connor",
            relationship = ContactRelationship.PARENT,
            phoneNumber = "+1987654321",
            email = "sarah@example.com"
        )
        assertNull(contact.validate())
    }

    @Test
    fun validContactWithPhoneOnly_isValid() {
        val contact = TrustedContact(
            name = "John Connor",
            phoneNumber = "+1987654321"
        )
        assertNull(contact.validate())
    }

    @Test
    fun validContactWithEmailOnly_isValid() {
        val contact = TrustedContact(
            name = "Kyle Reese",
            email = "kyle@example.com"
        )
        assertNull(contact.validate())
    }

    @Test
    fun blankName_failsValidation() {
        val contact = TrustedContact(
            name = "   ",
            phoneNumber = "+1987654321"
        )
        assertNotNull(contact.validate())
    }

    @Test
    fun missingBothPhoneAndEmail_failsValidation() {
        val contact = TrustedContact(
            name = "John Doe",
            phoneNumber = "",
            email = ""
        )
        assertNotNull(contact.validate())
    }

    @Test
    fun invalidEmail_failsValidation() {
        val contact = TrustedContact(
            name = "John Doe",
            email = "not-an-email"
        )
        assertNotNull(contact.validate())
    }

    @Test
    fun blankId_failsValidation() {
        val contact = TrustedContact(
            id = "",
            name = "Valid Name",
            phoneNumber = "+111222333"
        )
        assertNotNull(contact.validate())
    }
}
