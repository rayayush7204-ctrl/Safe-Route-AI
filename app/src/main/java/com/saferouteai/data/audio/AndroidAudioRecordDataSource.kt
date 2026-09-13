package com.saferouteai.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.audio.AudioDetectionPolicy
import com.saferouteai.domain.model.audio.AudioError
import com.saferouteai.domain.model.audio.AudioFrame
import com.saferouteai.domain.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Platform audio data source wrapping Android [AudioRecord].
 *
 * Requirements:
 * - 16 kHz Mono 16-bit PCM capture
 * - Bounded buffer / frame size (100ms chunks = 1600 samples)
 * - Purely in-memory; no file storage, no network calls
 * - Clean lifecycle startup and teardown
 * - Zero sample logging
 */
class AndroidAudioRecordDataSource(
    private val clock: Clock,
    private val policy: AudioDetectionPolicy = AudioDetectionPolicy(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val isRecording = AtomicBoolean(false)
    private var activeAudioRecord: AudioRecord? = null

    val sampleRate: Int = policy.sampleRate
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    val frameSizeInSamples: Int = policy.frameSizeInSamples

    fun isCapturing(): Boolean = isRecording.get()

    /**
     * Verifies that the device audio hardware supports the configured sample rate and format.
     */
    fun isSupported(): Boolean {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        return minBufferSize != AudioRecord.ERROR && minBufferSize != AudioRecord.ERROR_BAD_VALUE
    }

    /**
     * Starts the AudioRecord capture loop. Blocks the calling coroutine on [ioDispatcher]
     * until [stopRecording] is invoked or the coroutine is cancelled.
     *
     * @param onFrameAvailable Callback receiving in-memory [AudioFrame] objects.
     */
    @SuppressLint("MissingPermission")
    suspend fun startRecording(
        onFrameAvailable: (AudioFrame) -> Unit
    ): Result<Unit> = withContext(ioDispatcher) {
        if (!isRecording.compareAndSet(false, true)) {
            return@withContext Result.Error(Exception(AudioError.AlreadyCapturing.message))
        }

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            isRecording.set(false)
            return@withContext Result.Error(Exception(AudioError.InitializationFailed("Invalid min buffer size").message))
        }

        val bufferSize = maxOf(minBufferSize, frameSizeInSamples * 2 * 2)
        val audioRecord: AudioRecord
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            activeAudioRecord = audioRecord
        } catch (e: Exception) {
            isRecording.set(false)
            return@withContext Result.Error(Exception(AudioError.InitializationFailed(e.message ?: "Unknown error").message))
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            activeAudioRecord = null
            isRecording.set(false)
            return@withContext Result.Error(Exception(AudioError.DeviceUnavailable.message))
        }

        try {
            audioRecord.startRecording()
        } catch (e: Exception) {
            audioRecord.release()
            activeAudioRecord = null
            isRecording.set(false)
            return@withContext Result.Error(Exception(AudioError.RecordingFailed(e.message ?: "Failed to start AudioRecord").message))
        }

        val buffer = ShortArray(frameSizeInSamples)

        try {
            while (isActive && isRecording.get()) {
                val readCount = audioRecord.read(buffer, 0, frameSizeInSamples)
                if (readCount > 0) {
                    val frameData = buffer.copyOf(readCount)
                    val frame = AudioFrame(
                        timestampEpochMs = clock.nowEpochMs(),
                        pcmData = frameData,
                        sampleRate = sampleRate
                    )
                    onFrameAvailable(frame)
                } else if (readCount < 0) {
                    // Encountered platform read error
                    break
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception(AudioError.RecordingFailed(e.message ?: "Audio read loop failed").message))
        } finally {
            try {
                if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
            } catch (ignored: Exception) {
            } finally {
                audioRecord.release()
                activeAudioRecord = null
                isRecording.set(false)
            }
        }
    }

    /**
     * Signals the capture loop to stop and releases hardware resources.
     */
    fun stopRecording() {
        isRecording.set(false)
        try {
            activeAudioRecord?.let { record ->
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            }
        } catch (ignored: Exception) {
        }
    }
}
