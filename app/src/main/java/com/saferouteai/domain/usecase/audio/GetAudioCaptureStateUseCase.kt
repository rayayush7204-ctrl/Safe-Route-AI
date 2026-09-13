package com.saferouteai.domain.usecase.audio

import com.saferouteai.domain.model.audio.AudioCaptureState
import com.saferouteai.domain.repository.AudioRepository
import kotlinx.coroutines.flow.StateFlow

class GetAudioCaptureStateUseCase(
    private val audioRepository: AudioRepository
) {
    operator fun invoke(): StateFlow<AudioCaptureState> {
        return audioRepository.captureState
    }
}
