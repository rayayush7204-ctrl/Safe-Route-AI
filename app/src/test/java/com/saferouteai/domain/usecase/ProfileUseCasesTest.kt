package com.saferouteai.domain.usecase

import com.saferouteai.data.repository.InMemoryUserProfileRepository
import com.saferouteai.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileUseCasesTest {

    private lateinit var repository: InMemoryUserProfileRepository
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase
    private lateinit var saveUserProfileUseCase: SaveUserProfileUseCase

    @Before
    fun setUp() {
        repository = InMemoryUserProfileRepository()
        getUserProfileUseCase = GetUserProfileUseCase(repository)
        saveUserProfileUseCase = SaveUserProfileUseCase(repository)
    }

    @Test
    fun initialProfile_isNull() = runTest {
        val profile = getUserProfileUseCase().first()
        assertNull(profile)
    }

    @Test
    fun saveValidProfile_succeedsAndUpdatesStream() = runTest {
        val validProfile = UserProfile(
            displayName = "Bruce Wayne",
            phoneNumber = "+123456789",
            email = "bruce@wayne.com"
        )

        val result = saveUserProfileUseCase(validProfile)
        assertTrue(result.isSuccess)

        val saved = getUserProfileUseCase().first()
        assertNotNull(saved)
        assertEquals("Bruce Wayne", saved?.displayName)
        assertEquals("+123456789", saved?.phoneNumber)
        assertEquals("bruce@wayne.com", saved?.email)
    }

    @Test
    fun saveInvalidProfile_failsWithValidationError() = runTest {
        val invalidProfile = UserProfile(
            displayName = "Bruce",
            email = "not-an-email"
        )

        val result = saveUserProfileUseCase(invalidProfile)
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
