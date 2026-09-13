package com.saferouteai.domain.usecase.audio

import com.saferouteai.core.result.Result
import com.saferouteai.domain.repository.AudioRepository

class StartAudioCaptureUseCase(
    private val audioRepository: AudioRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return audioRepository.startCapture()
    }
}
