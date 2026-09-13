package com.saferouteai.domain.usecase.audio

import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.repository.AudioRepository
import kotlinx.coroutines.flow.StateFlow

class GetAcousticSignalsUseCase(
    private val audioRepository: AudioRepository
) {
    operator fun invoke(): StateFlow<List<AcousticSignal>> {
        return audioRepository.acousticSignals
    }
}
