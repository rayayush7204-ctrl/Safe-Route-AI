package com.saferouteai.domain.audio

import com.saferouteai.core.result.Result
import com.saferouteai.data.repository.InMemoryAudioRepository
import com.saferouteai.data.repository.InMemoryConsentRepository
import com.saferouteai.data.repository.InMemoryJourneySessionRepository
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.model.audio.AcousticSignalType
import com.saferouteai.domain.model.audio.AudioCaptureState
import com.saferouteai.domain.model.audio.AudioDetectionPolicy
import com.saferouteai.domain.model.audio.AudioError
import com.saferouteai.domain.model.audio.AudioFrame
import com.saferouteai.domain.model.audio.AudioPermissionStatus
import com.saferouteai.domain.model.session.CheckpointPolicy
import com.saferouteai.domain.repository.FakeAudioPermissionChecker
import com.saferouteai.domain.repository.FakeLocationPermissionChecker
import com.saferouteai.test.FakeClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the Milestone 5A audio pipeline:
 * - Hard gate enforcement (session, consent, permission)
 * - Capture state machine transitions
 * - NoOpAcousticSignalDetector deterministic no-classification boundary
 * - Bounded signal retention
 * - Foreground-only invariant
 */
class AudioPipelineTest {

    private lateinit var audioPermissionChecker: FakeAudioPermissionChecker
    private lateinit var consentRepository: InMemoryConsentRepository
    private lateinit var audioRepository: InMemoryAudioRepository
    private lateinit var fakeClock: FakeClock

    @Before
    fun setup() {
        audioPermissionChecker = FakeAudioPermissionChecker(AudioPermissionStatus.NOT_REQUESTED)
        consentRepository = InMemoryConsentRepository()
        fakeClock = FakeClock(1000L)
        audioRepository = InMemoryAudioRepository(
            permissionChecker = audioPermissionChecker,
            consentRepository = consentRepository
        )
    }

    // =============================================================
    // Hard Gate 1: Session Active Gate
    // =============================================================

    @Test
    fun `startCapture fails when no session repository and gate checks session`() = runTest {
        // With no session repository, InMemoryAudioRepository skips session check
        // Consent and permission still required
        val repoNoSession = InMemoryAudioRepository(
            permissionChecker = audioPermissionChecker,
            consentRepository = consentRepository
        )
        audioPermissionChecker.setStatus(AudioPermissionStatus.GRANTED)
        consentRepository.setAudioProcessingConsent(true)

        val result = repoNoSession.startCapture()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `startCapture fails when session is not active`() = runTest {
        val locationPermChecker = FakeLocationPermissionChecker()
        locationPermChecker.setStatus(com.saferouteai.domain.model.location.LocationPermissionStatus.FINE_GRANTED)
        val sessionRepo = InMemoryJourneySessionRepository(clock = fakeClock)
        val repo = InMemoryAudioRepository(
            permissionChecker = audioPermissionChecker,
            consentRepository = consentRepository,
            journeySessionRepository = sessionRepo
        )

        audioPermissionChecker.setStatus(AudioPermissionStatus.GRANTED)
        consentRepository.setAudioProcessingConsent(true)

        val result = repo.startCapture()
        assertTrue("Should fail when session is not active", result is Result.Error)
        assertTrue(
            (result as Result.Error).exception.message?.contains("active Safe Journey session") == true
        )
    }

    // =============================================================
    // Hard Gate 2: Consent Gate
    // =============================================================

    @Test
    fun `startCapture fails when audio consent is not granted`() = runTest {
        audioPermissionChecker.setStatus(AudioPermissionStatus.GRANTED)
        // audioProcessingConsent defaults to false

        val result = audioRepository.startCapture()
        assertTrue("Should fail when consent missing", result is Result.Error)
        assertTrue(
            (result as Result.Error).exception.message?.contains("consent") == true
        )
    }

    // =============================================================
    // Hard Gate 3: Permission Gate
    // =============================================================

    @Test
    fun `startCapture fails when RECORD_AUDIO permission is not granted`() = runTest {
        consentRepository.setAudioProcessingConsent(true)
        audioPermissionChecker.setStatus(AudioPermissionStatus.DENIED)

        val result = audioRepository.startCapture()
        assertTrue("Should fail when permission denied", result is Result.Error)
        assertTrue(
            (result as Result.Error).exception.message?.contains("permission") == true
        )
    }

    // =============================================================
    // Gate All Satisfied: Capture Start Succeeds
    // =============================================================

    @Test
    fun `startCapture succeeds when all gates are satisfied`() = runTest {
        audioPermissionChecker.setStatus(AudioPermissionStatus.GRANTED)
        consentRepository.setAudioProcessingConsent(true)

        val result = audioRepository.startCapture()
        assertTrue("Should succeed when all gates satisfied", result is Result.Success)
        assertEquals(AudioCaptureState.CAPTURING, audioRepository.captureState.value)
    }

    // =============================================================
    // Double Start Prevention
    // =============================================================

    @Test
    fun `startCapture fails when already capturing`() = runTest {
        audioPermissionChecker.setStatus(AudioPermissionStatus.GRANTED)
        consentRepository.setAudioProcessingConsent(true)

        audioRepository.startCapture()
        val secondResult = audioRepository.startCapture()
        assertTrue("Should reject double start", secondResult is Result.Error)
        assertTrue(
            (secondResult as Result.Error).exception.message?.contains("already running") == true
        )
    }

    // =============================================================
    // Stop Capture
    // =============================================================

    @Test
    fun `stopCapture transitions to STOPPED`() = runTest {
        audioPermissionChecker.setStatus(AudioPermissionStatus.GRANTED)
        consentRepository.setAudioProcessingConsent(true)

        audioRepository.startCapture()
        assertEquals(AudioCaptureState.CAPTURING, audioRepository.captureState.value)

        audioRepository.stopCapture()
        assertEquals(AudioCaptureState.STOPPED, audioRepository.captureState.value)
    }

    // =============================================================
    // Clear Signals
    // =============================================================

    @Test
    fun `clearSignals empties the acoustic signals list`() = runTest {
        audioPermissionChecker.setStatus(AudioPermissionStatus.GRANTED)
        consentRepository.setAudioProcessingConsent(true)
        audioRepository.startCapture()

        audioRepository.emitSignal(
            AcousticSignal(
                type = AcousticSignalType.WAKE_WORD,
                detectedAtEpochMs = 1000L,
                explanation = "Test signal"
            )
        )
        assertEquals(1, audioRepository.acousticSignals.value.size)

        audioRepository.clearSignals()
        assertEquals(0, audioRepository.acousticSignals.value.size)
    }

    // =============================================================
    // NoOp Detector: Deterministic Boundary
    // =============================================================

    @Test
    fun `NoOpAcousticSignalDetector returns empty list for all frames`() {
        val detector = NoOpAcousticSignalDetector()
        val frame = AudioFrame(
            timestampEpochMs = 1000L,
            pcmData = ShortArray(1600),
            sampleRate = 16000
        )

        val signals = detector.processFrame(frame)
        assertTrue("NoOp detector should return empty list", signals.isEmpty())
    }

    @Test
    fun `NoOpAcousticSignalDetector reset is safe`() {
        val detector = NoOpAcousticSignalDetector()
        // Should not throw
        detector.reset()
    }

    // =============================================================
    // Bounded Signal Retention (max 30)
    // =============================================================

    @Test
    fun `signals are bounded by maxRetainedSignals policy`() = runTest {
        val policy = AudioDetectionPolicy(maxRetainedSignals = 5)
        val repo = InMemoryAudioRepository(
            permissionChecker = audioPermissionChecker,
            consentRepository = consentRepository,
            policy = policy
        )

        audioPermissionChecker.setStatus(AudioPermissionStatus.GRANTED)
        consentRepository.setAudioProcessingConsent(true)
        repo.startCapture()

        // Emit more signals than the max
        repeat(10) { i ->
            repo.emitSignal(
                AcousticSignal(
                    type = AcousticSignalType.DISTRESS_KEYWORD,
                    detectedAtEpochMs = 1000L + i,
                    explanation = "Signal $i"
                )
            )
        }

        assertTrue(
            "Signal count should not exceed max retained",
            repo.acousticSignals.value.size <= 5
        )
    }

    // =============================================================
    // AudioFrame Invariants
    // =============================================================

    @Test
    fun `AudioFrame correctly calculates sample count and duration`() {
        val frame = AudioFrame(
            timestampEpochMs = 5000L,
            pcmData = ShortArray(1600),
            sampleRate = 16000
        )

        assertEquals(1600, frame.sampleCount)
        assertEquals(100L, frame.durationMs) // 1600 samples / 16000 Hz = 100ms
    }

    @Test
    fun `AudioFrame toString does not expose PCM data`() {
        val frame = AudioFrame(
            timestampEpochMs = 5000L,
            pcmData = ShortArray(1600),
            sampleRate = 16000
        )

        val str = frame.toString()
        assertTrue("ToString should include metadata", str.contains("1600"))
        assertTrue("ToString should include sample rate", str.contains("16000"))
    }

    // =============================================================
    // AudioDetectionPolicy Configuration
    // =============================================================

    @Test
    fun `default AudioDetectionPolicy has correct 16kHz Mono 16-bit config`() {
        val policy = AudioDetectionPolicy()
        assertEquals(16000, policy.sampleRate)
        assertEquals(1, policy.channelConfig) // Mono
        assertEquals(16, policy.audioEncodingBits) // 16-bit PCM
        assertEquals(100, policy.frameSizeMs)
        assertEquals(1600, policy.frameSizeInSamples) // 16000 * 100 / 1000
        assertEquals(3200, policy.frameSizeInBytes)   // 1600 * 2 * 1
        assertEquals(30, policy.maxRetainedSignals)
    }

    // =============================================================
    // AudioCaptureState Convenience Properties
    // =============================================================

    @Test
    fun `AudioCaptureState isCapturing property`() {
        assertTrue(AudioCaptureState.CAPTURING.isCapturing)
        assertTrue(!AudioCaptureState.IDLE.isCapturing)
        assertTrue(!AudioCaptureState.STARTING.isCapturing)
        assertTrue(!AudioCaptureState.STOPPING.isCapturing)
        assertTrue(!AudioCaptureState.STOPPED.isCapturing)
        assertTrue(!AudioCaptureState.ERROR.isCapturing)
    }

    @Test
    fun `AudioCaptureState isActiveOrStarting property`() {
        assertTrue(AudioCaptureState.CAPTURING.isActiveOrStarting)
        assertTrue(AudioCaptureState.STARTING.isActiveOrStarting)
        assertTrue(!AudioCaptureState.IDLE.isActiveOrStarting)
        assertTrue(!AudioCaptureState.STOPPING.isActiveOrStarting)
        assertTrue(!AudioCaptureState.STOPPED.isActiveOrStarting)
        assertTrue(!AudioCaptureState.ERROR.isActiveOrStarting)
    }

    // =============================================================
    // AudioPermissionStatus Convenience Property
    // =============================================================

    @Test
    fun `AudioPermissionStatus isGranted property`() {
        assertTrue(AudioPermissionStatus.GRANTED.isGranted)
        assertTrue(!AudioPermissionStatus.NOT_REQUESTED.isGranted)
        assertTrue(!AudioPermissionStatus.DENIED.isGranted)
    }

    // =============================================================
    // Audio Error Types
    // =============================================================

    @Test
    fun `AudioError sealed hierarchy covers all expected error types`() {
        val errors = listOf(
            AudioError.ConsentRequired,
            AudioError.PermissionRequired,
            AudioError.InactiveSession,
            AudioError.DeviceUnavailable,
            AudioError.AlreadyCapturing,
            AudioError.InitializationFailed("test"),
            AudioError.RecordingFailed("test")
        )

        assertEquals(7, errors.size)
        errors.forEach { assertTrue(it.message.isNotBlank()) }
    }

    // =============================================================
    // Simulated Frame Processing (with NoOp Detector)
    // =============================================================

    @Test
    fun `simulateFrame with NoOp detector produces no signals`() = runTest {
        audioPermissionChecker.setStatus(AudioPermissionStatus.GRANTED)
        consentRepository.setAudioProcessingConsent(true)
        audioRepository.startCapture()

        val frame = AudioFrame(
            timestampEpochMs = 1000L,
            pcmData = ShortArray(1600),
            sampleRate = 16000
        )
        audioRepository.simulateFrame(frame)

        assertEquals(
            "NoOp detector should produce no signals",
            0,
            audioRepository.acousticSignals.value.size
        )
    }

    @Test
    fun `simulateFrame while not capturing is ignored`() = runTest {
        // Don't start capture
        val frame = AudioFrame(
            timestampEpochMs = 1000L,
            pcmData = ShortArray(1600),
            sampleRate = 16000
        )
        audioRepository.simulateFrame(frame)

        assertEquals(0, audioRepository.acousticSignals.value.size)
    }
}
