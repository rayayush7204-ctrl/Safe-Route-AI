package com.saferouteai.domain.audio

import com.saferouteai.domain.model.audio.AudioFrame
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure Kotlin acoustic feature extractor.
 *
 * Computes numeric temporal and spectral-proxy metrics from volatile 16-bit PCM audio samples.
 * Does NOT retain raw PCM data after extraction.
 */
object AcousticFeatureExtractor {

    /**
     * Extracts [AcousticFeatures] from a volatile [AudioFrame].
     */
    fun extract(frame: AudioFrame): AcousticFeatures {
        return extract(
            pcmData = frame.pcmData,
            sampleRate = frame.sampleRate,
            timestampEpochMs = frame.timestampEpochMs
        )
    }

    /**
     * Extracts [AcousticFeatures] from raw 16-bit PCM samples.
     */
    fun extract(
        pcmData: ShortArray,
        sampleRate: Int,
        timestampEpochMs: Long
    ): AcousticFeatures {
        val count = pcmData.size
        if (count == 0 || sampleRate <= 0) {
            return AcousticFeatures(
                timestampEpochMs = timestampEpochMs,
                sampleCount = 0,
                sampleRate = sampleRate,
                rms = 0.0,
                peakAmplitude = 0,
                zeroCrossingRate = 0.0,
                highFrequencyRatio = 0.0,
                energy = 0.0,
                durationMs = 0L
            )
        }

        var sumSq = 0.0
        var maxPeak = 0
        var zeroCrossings = 0
        var diffSumSq = 0.0

        var prevSample = pcmData[0].toInt()
        val firstSample = prevSample
        val firstAbs = abs(firstSample)
        if (firstAbs > maxPeak) maxPeak = firstAbs
        sumSq += firstSample.toDouble() * firstSample.toDouble()

        for (i in 1 until count) {
            val sample = pcmData[i].toInt()
            val sampleAbs = abs(sample)
            if (sampleAbs > maxPeak) {
                maxPeak = sampleAbs
            }

            sumSq += sample.toDouble() * sample.toDouble()

            // Zero-crossing check: transitions across zero
            if ((sample >= 0 && prevSample < 0) || (sample < 0 && prevSample >= 0)) {
                zeroCrossings++
            }

            val diff = sample - prevSample
            diffSumSq += diff.toDouble() * diff.toDouble()

            prevSample = sample
        }

        val rms = sqrt(sumSq / count)
        val energy = rms * rms
        val zcr = if (count > 1) zeroCrossings.toDouble() / (count - 1) else 0.0
        val highFreqRatio = if (sumSq > 0.0) {
            (diffSumSq / (4.0 * sumSq)).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val durationMs = (count * 1000L) / sampleRate

        return AcousticFeatures(
            timestampEpochMs = timestampEpochMs,
            sampleCount = count,
            sampleRate = sampleRate,
            rms = rms,
            peakAmplitude = maxPeak.coerceAtMost(Short.MAX_VALUE.toInt()).toShort(),
            zeroCrossingRate = zcr,
            highFrequencyRatio = highFreqRatio,
            energy = energy,
            durationMs = durationMs
        )
    }
}
