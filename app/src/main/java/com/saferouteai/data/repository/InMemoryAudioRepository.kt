package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.audio.AcousticSignalDetector
import com.saferouteai.domain.audio.NoOpAcousticSignalDetector
import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.model.audio.AudioCaptureState
import com.saferouteai.domain.model.audio.AudioDetectionPolicy
import com.saferouteai.domain.model.audio.AudioError
import com.saferouteai.domain.model.audio.AudioFrame
import com.saferouteai.domain.repository.AudioPermissionChecker
import com.saferouteai.domain.repository.AudioRepository
import com.saferouteai.domain.repository.ConsentRepository
import com.saferouteai.domain.repository.JourneySessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pure JVM in-memory test double for [AudioRepository].
 * Enforces identical audio readiness gates without touching Android AudioRecord hardware.
 */
class InMemoryAudioRepository(
    private val permissionChecker: AudioPermissionChecker? = null,
    private val consentRepository: ConsentRepository? = null,
    private val journeySessionRepository: JourneySessionRepository? = null,
    private val detector: AcousticSignalDetector = NoOpAcousticSignalDetector(),
    private val policy: AudioDetectionPolicy = AudioDetectionPolicy()
) : AudioRepository {

    private val mutex = Mutex()
    private val _captureState = MutableStateFlow(AudioCaptureState.IDLE)
    override val captureState: StateFlow<AudioCaptureState> = _captureState.asStateFlow()

    private val _acousticSignals = MutableStateFlow<List<AcousticSignal>>(emptyList())
    override val acousticSignals: StateFlow<List<AcousticSignal>> = _acousticSignals.asStateFlow()

    override suspend fun startCapture(): Result<Unit> = mutex.withLock {
        if (_captureState.value.isActiveOrStarting) {
            return Result.Error(Exception(AudioError.AlreadyCapturing.message))
        }

        // Gate 1: Session active
        if (journeySessionRepository != null && !journeySessionRepository.sessionStatus.value.isActiveSession) {
            return Result.Error(Exception(AudioError.InactiveSession.message))
        }

        // Gate 2: Consent
        val consent = consentRepository?.getConsent()?.firstOrNull()
        if (consent != null && !consent.audioProcessingConsent) {
            return Result.Error(Exception(AudioError.ConsentRequired.message))
        }

        // Gate 3: Permission
        if (permissionChecker != null && !permissionChecker.hasAudioPermission()) {
            return Result.Error(Exception(AudioError.PermissionRequired.message))
        }

        _captureState.value = AudioCaptureState.CAPTURING
        Result.Success(Unit)
    }

    override suspend fun stopCapture(): Result<Unit> = mutex.withLock {
        _captureState.value = AudioCaptureState.STOPPED
        detector.reset()
        Result.Success(Unit)
    }

    override suspend fun clearSignals(): Result<Unit> = mutex.withLock {
        _acousticSignals.value = emptyList()
        Result.Success(Unit)
    }

    /**
     * Test helper: simulates an incoming in-memory audio frame through the detector.
     */
    suspend fun simulateFrame(frame: AudioFrame) = mutex.withLock {
        if (!_captureState.value.isCapturing) return@withLock
        val detected = detector.processFrame(frame)
        if (detected.isNotEmpty()) {
            val list = _acousticSignals.value.toMutableList()
            list.addAll(detected)
            val trimmed = if (list.size > policy.maxRetainedSignals) {
                list.takeLast(policy.maxRetainedSignals)
            } else {
                list
            }
            _acousticSignals.value = trimmed
        }
    }

    /**
     * Test helper: directly emits an observational signal into the repository stream.
     */
    suspend fun emitSignal(signal: AcousticSignal) = mutex.withLock {
        val list = _acousticSignals.value.toMutableList()
        list.add(signal)
        val trimmed = if (list.size > policy.maxRetainedSignals) {
            list.takeLast(policy.maxRetainedSignals)
        } else {
            list
        }
        _acousticSignals.value = trimmed
    }
}
