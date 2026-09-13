package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.data.audio.AndroidAudioRecordDataSource
import com.saferouteai.domain.audio.AcousticSignalDetector
import com.saferouteai.domain.audio.DeterministicAcousticSignalDetector
import com.saferouteai.domain.audio.NoOpAcousticSignalDetector
import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.model.audio.AudioCaptureState
import com.saferouteai.domain.model.audio.AudioDetectionPolicy
import com.saferouteai.domain.model.audio.AudioError
import com.saferouteai.domain.repository.AudioPermissionChecker
import com.saferouteai.domain.repository.AudioRepository
import com.saferouteai.domain.repository.ConsentRepository
import com.saferouteai.domain.repository.JourneySessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production implementation of [AudioRepository] coordinating Android AudioRecord capture,
 * hard audio readiness gates, and pluggable acoustic signal analysis.
 */
class AndroidAudioRepository(
    private val audioRecordDataSource: AndroidAudioRecordDataSource,
    private val permissionChecker: AudioPermissionChecker,
    private val consentRepository: ConsentRepository,
    private val journeySessionRepository: JourneySessionRepository? = null,
    private val policy: AudioDetectionPolicy = AudioDetectionPolicy(),
    private val detector: AcousticSignalDetector = DeterministicAcousticSignalDetector(policy),
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AudioRepository {

    private val mutex = Mutex()
    private val _captureState = MutableStateFlow(AudioCaptureState.IDLE)
    override val captureState: StateFlow<AudioCaptureState> = _captureState.asStateFlow()

    private val _acousticSignals = MutableStateFlow<List<AcousticSignal>>(emptyList())
    override val acousticSignals: StateFlow<List<AcousticSignal>> = _acousticSignals.asStateFlow()

    private var recordingJob: Job? = null

    init {
        // Automatically terminate audio capture if permission, consent, or session is revoked/ended
        externalScope.launch {
            permissionChecker.permissionStatus.collect { status ->
                if (!status.isGranted && _captureState.value.isCapturing) {
                    stopCapture()
                }
            }
        }

        externalScope.launch {
            consentRepository.getConsent().collect { consent ->
                if (!consent.audioProcessingConsent && _captureState.value.isCapturing) {
                    stopCapture()
                }
            }
        }

        if (journeySessionRepository != null) {
            externalScope.launch {
                journeySessionRepository.sessionStatus.collect { status ->
                    if (!status.isActiveSession && _captureState.value.isCapturing) {
                        stopCapture()
                    }
                }
            }
        }
    }

    override suspend fun startCapture(): Result<Unit> = mutex.withLock {
        if (_captureState.value.isActiveOrStarting) {
            return Result.Error(Exception(AudioError.AlreadyCapturing.message))
        }

        // Hard Gate 1: Active Journey Session required
        val isSessionActive = journeySessionRepository?.sessionStatus?.value?.isActiveSession ?: true
        if (!isSessionActive) {
            return Result.Error(Exception(AudioError.InactiveSession.message))
        }

        // Hard Gate 2: Explicit audio processing consent required
        val consent = consentRepository.getConsent().firstOrNull()
        if (consent == null || !consent.audioProcessingConsent) {
            return Result.Error(Exception(AudioError.ConsentRequired.message))
        }

        // Hard Gate 3: Android RECORD_AUDIO runtime permission required
        if (!permissionChecker.hasAudioPermission()) {
            return Result.Error(Exception(AudioError.PermissionRequired.message))
        }

        // Hard Gate 4: Hardware audio capability verification
        if (!audioRecordDataSource.isSupported()) {
            return Result.Error(Exception(AudioError.DeviceUnavailable.message))
        }

        _captureState.value = AudioCaptureState.STARTING

        val job = externalScope.launch {
            _captureState.value = AudioCaptureState.CAPTURING
            val result = audioRecordDataSource.startRecording { frame ->
                val detected = detector.processFrame(frame)
                if (detected.isNotEmpty()) {
                    val currentList = _acousticSignals.value.toMutableList()
                    currentList.addAll(detected)
                    val trimmed = if (currentList.size > policy.maxRetainedSignals) {
                        currentList.takeLast(policy.maxRetainedSignals)
                    } else {
                        currentList
                    }
                    _acousticSignals.value = trimmed
                }
            }

            if (result is Result.Error) {
                _captureState.value = AudioCaptureState.ERROR
            } else {
                _captureState.value = AudioCaptureState.STOPPED
            }
        }

        recordingJob = job
        Result.Success(Unit)
    }

    override suspend fun stopCapture(): Result<Unit> = mutex.withLock {
        if (!_captureState.value.isActiveOrStarting && _captureState.value != AudioCaptureState.ERROR) {
            return Result.Success(Unit)
        }

        _captureState.value = AudioCaptureState.STOPPING
        audioRecordDataSource.stopRecording()
        recordingJob?.cancel()
        recordingJob = null
        detector.reset()
        _captureState.value = AudioCaptureState.STOPPED
        Result.Success(Unit)
    }

    override suspend fun clearSignals(): Result<Unit> = mutex.withLock {
        _acousticSignals.value = emptyList()
        Result.Success(Unit)
    }
}
