package com.saferouteai.domain.usecase

import com.saferouteai.data.repository.InMemoryConsentRepository
import com.saferouteai.domain.model.UserConsent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConsentUseCasesTest {

    private lateinit var repository: InMemoryConsentRepository
    private lateinit var getConsentUseCase: GetConsentUseCase
    private lateinit var updateConsentUseCase: UpdateConsentUseCase

    @Before
    fun setUp() {
        repository = InMemoryConsentRepository()
        getConsentUseCase = GetConsentUseCase(repository)
        updateConsentUseCase = UpdateConsentUseCase(repository)
    }

    @Test
    fun initialConsent_isStrictlyFalse() = runTest {
        val consent = getConsentUseCase().first()
        assertFalse(consent.locationSharingConsent)
        assertFalse(consent.trustedContactSharingConsent)
    }

    @Test
    fun updateLocationConsent_modifiesOnlyLocation() = runTest {
        updateConsentUseCase.setLocationSharingConsent(true)

        val consent = getConsentUseCase().first()
        assertTrue(consent.locationSharingConsent)
        assertFalse(consent.trustedContactSharingConsent)
    }

    @Test
    fun updateContactConsent_modifiesOnlyContact() = runTest {
        updateConsentUseCase.setTrustedContactSharingConsent(true)

        val consent = getConsentUseCase().first()
        assertFalse(consent.locationSharingConsent)
        assertTrue(consent.trustedContactSharingConsent)
    }

    @Test
    fun revokeConsent_disablesImmediately() = runTest {
        updateConsentUseCase.setLocationSharingConsent(true)
        updateConsentUseCase.setTrustedContactSharingConsent(true)

        var consent = getConsentUseCase().first()
        assertTrue(consent.locationSharingConsent)
        assertTrue(consent.trustedContactSharingConsent)

        updateConsentUseCase.setLocationSharingConsent(false)
        consent = getConsentUseCase().first()
        assertFalse(consent.locationSharingConsent)
        assertTrue(consent.trustedContactSharingConsent)
    }
}
