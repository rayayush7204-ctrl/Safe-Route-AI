package com.saferouteai.domain.audio

import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.model.audio.AcousticSignalType
import com.saferouteai.domain.model.audio.AudioDetectionPolicy
import com.saferouteai.domain.model.audio.AudioFrame
import kotlin.math.min

/**
 * Pure Kotlin, deterministic on-device acoustic signal detector.
 *
 * Evaluates conservative heuristic acoustic features extracted from volatile 100ms PCM16 frames.
 *
 * Invariants:
 * - Deterministic, side-effect free heuristic evaluation.
 * - Operates entirely on-device with zero network, cloud, or ML dependencies.
 * - Confidence represents heuristic detector strength, NOT probability of danger.
 * - Factual observational signals only (SIGNAL != DANGER, SIGNAL != EMERGENCY).
 * - Retains only derived numeric feature metrics in a bounded window; zero raw audio persistence.
 */
class DeterministicAcousticSignalDetector(
    private val policy: AudioDetectionPolicy = AudioDetectionPolicy()
) : AcousticSignalDetector {

    private val featureWindow = BoundedFeatureWindow(maxCapacity = policy.maxFeatureHistoryFrames)

    // Episode tracking to prevent duplicate firing during continuous sustained events
    private var screamEpisodeActive = false
    private var commotionCooldownRemaining = 0

    @Synchronized
    override fun processFrame(frame: AudioFrame): List<AcousticSignal> {
        if (frame.sampleCount == 0) return emptyList()

        val features = AcousticFeatureExtractor.extract(frame)

        // Decrement commotion cooldown
        if (commotionCooldownRemaining > 0) {
            commotionCooldownRemaining--
        }

        val signals = mutableListOf<AcousticSignal>()

        // 1. Evaluate Sudden Loud Impact
        evaluateSuddenLoudImpact(features)?.let { signals.add(it) }

        // 2. Evaluate Scream / Shout (Sustained Vocal Energy)
        evaluateScreamOrShout(features)?.let { signals.add(it) }

        // 3. Evaluate Persistent Distress Commotion
        evaluatePersistentCommotion(features)?.let { signals.add(it) }

        // Store current feature into the bounded window
        featureWindow.add(features)

        return signals
    }

    /**
     * Evaluates whether the current frame represents a sudden loud acoustic impulse.
     *
     * Heuristic criteria:
     * - Peak amplitude exceeds [policy.impactPeakThreshold]
     * - RMS exceeds [policy.impactRmsThreshold]
     * - Significant energy jump over the pre-event baseline (at least [policy.impactEnergyDeltaRatio]x)
     * - Preceding frame was not already loud (onset requirement)
     */
    private fun evaluateSuddenLoudImpact(features: AcousticFeatures): AcousticSignal? {
        val peakExceeded = features.peakAmplitude >= policy.impactPeakThreshold
        val rmsExceeded = features.rms >= policy.impactRmsThreshold
        if (!peakExceeded || !rmsExceeded) return null

        val baselineRms = featureWindow.getBaselineRms(skipLastN = 0, baselineWindow = 10)
        val energyDeltaRatio = if (baselineRms > 0.0) features.rms / baselineRms else 1.0

        if (energyDeltaRatio < policy.impactEnergyDeltaRatio) return null

        // Check that the immediately preceding frame was not already loud
        val lastFeature = featureWindow.getRecent(1).firstOrNull()
        if (lastFeature != null && lastFeature.rms >= policy.impactRmsThreshold) {
            return null
        }

        // Heuristic detector strength (confidence): 0.50 to 0.95 based on energy delta and peak amplitude
        val peakStrength = (features.peakAmplitude.toDouble() / Short.MAX_VALUE).coerceIn(0.0, 1.0)
        val deltaStrength = ((energyDeltaRatio - policy.impactEnergyDeltaRatio) / 10.0).coerceIn(0.0, 1.0)
        val confidence = (0.50f + (0.30f * peakStrength.toFloat()) + (0.15f * deltaStrength.toFloat())).coerceIn(0.50f, 0.95f)

        return AcousticSignal(
            type = AcousticSignalType.SUDDEN_LOUD_IMPACT,
            detectedAtEpochMs = features.timestampEpochMs,
            durationMs = features.durationMs,
            explanation = "Sudden elevated acoustic impulse detected locally.",
            confidence = confidence,
            metadata = mapOf(
                "rms" to String.format("%.1f", features.rms),
                "peak" to features.peakAmplitude.toString(),
                "baselineRms" to String.format("%.1f", baselineRms),
                "deltaRatio" to String.format("%.2f", energyDeltaRatio)
            )
        )
    }

    /**
     * Evaluates whether recent frames represent a sustained elevated vocalization pattern.
     *
     * Heuristic criteria:
     * - Sustained elevated RMS and peak across at least [policy.screamMinSustainedFrames] consecutive frames
     * - Zero-Crossing Rate (ZCR) falls within vocalization range [policy.screamZcrMin, policy.screamZcrMax]
     * - Debounced per continuous episode so it emits once per sustained vocalization burst
     */
    private fun evaluateScreamOrShout(features: AcousticFeatures): AcousticSignal? {
        val currentIsLoud = features.rms >= policy.screamRmsThreshold &&
                features.peakAmplitude >= policy.screamPeakThreshold
        val zcrValid = features.zeroCrossingRate in policy.screamZcrMin..policy.screamZcrMax

        if (!currentIsLoud || !zcrValid) {
            // Loud episode ended
            screamEpisodeActive = false
            return null
        }

        // Check previous (screamMinSustainedFrames - 1) consecutive frames
        val requiredPrior = policy.screamMinSustainedFrames - 1
        if (requiredPrior > 0) {
            val recent = featureWindow.getRecent(requiredPrior)
            if (recent.size < requiredPrior) {
                return null
            }
            val priorAllLoud = recent.all {
                it.rms >= policy.screamRmsThreshold &&
                        it.peakAmplitude >= policy.screamPeakThreshold &&
                        it.zeroCrossingRate in policy.screamZcrMin..policy.screamZcrMax
            }
            if (!priorAllLoud) return null
        }

        // Debounce: fire once per sustained episode
        if (screamEpisodeActive) {
            return null
        }
        screamEpisodeActive = true

        val sustainedDurationMs = features.durationMs * policy.screamMinSustainedFrames
        val confidence = min(
            0.90f,
            0.60f + (0.20f * (features.rms / Short.MAX_VALUE).toFloat()) +
                    (0.10f * (features.peakAmplitude.toFloat() / Short.MAX_VALUE))
        )

        return AcousticSignal(
            type = AcousticSignalType.SCREAM_OR_SHOUT,
            detectedAtEpochMs = features.timestampEpochMs,
            durationMs = sustainedDurationMs,
            explanation = "Sustained elevated vocal-like acoustic energy detected locally.",
            confidence = confidence,
            metadata = mapOf(
                "rms" to String.format("%.1f", features.rms),
                "peak" to features.peakAmplitude.toString(),
                "zcr" to String.format("%.3f", features.zeroCrossingRate),
                "sustainedFrames" to policy.screamMinSustainedFrames.toString()
            )
        )
    }

    /**
     * Evaluates whether recent window history indicates repeated elevated acoustic activity.
     *
     * Heuristic criteria:
     * - At least [policy.commotionMinElevatedFrames] in the recent window of [policy.commotionWindowFrames]
     *   have RMS exceeding [policy.commotionFrameRmsThreshold]
     * - Guarded by [policy.commotionCooldownFrames] to prevent frame-by-frame spamming
     */
    private fun evaluatePersistentCommotion(features: AcousticFeatures): AcousticSignal? {
        if (commotionCooldownRemaining > 0) return null

        val windowSlice = featureWindow.getRecent(policy.commotionWindowFrames - 1)
        var elevatedCount = if (features.rms >= policy.commotionFrameRmsThreshold) 1 else 0

        for (item in windowSlice) {
            if (item.rms >= policy.commotionFrameRmsThreshold) {
                elevatedCount++
            }
        }

        if (elevatedCount < policy.commotionMinElevatedFrames) return null

        commotionCooldownRemaining = policy.commotionCooldownFrames

        val densityRatio = elevatedCount.toFloat() / policy.commotionWindowFrames
        val confidence = (0.55f + (0.35f * densityRatio)).coerceIn(0.55f, 0.90f)

        return AcousticSignal(
            type = AcousticSignalType.PERSISTENT_DISTRESS_COMMOTION,
            detectedAtEpochMs = features.timestampEpochMs,
            durationMs = policy.commotionWindowFrames * features.durationMs,
            explanation = "Repeated elevated acoustic activity detected within the recent window.",
            confidence = confidence,
            metadata = mapOf(
                "elevatedFrames" to elevatedCount.toString(),
                "windowFrames" to policy.commotionWindowFrames.toString(),
                "frameRmsThreshold" to policy.commotionFrameRmsThreshold.toString()
            )
        )
    }

    @Synchronized
    override fun reset() {
        featureWindow.clear()
        screamEpisodeActive = false
        commotionCooldownRemaining = 0
    }
}
