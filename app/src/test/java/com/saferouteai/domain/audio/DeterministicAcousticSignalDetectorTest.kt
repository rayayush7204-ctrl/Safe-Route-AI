package com.saferouteai.domain.audio

import com.saferouteai.domain.model.audio.AcousticSignalType
import com.saferouteai.domain.model.audio.AudioDetectionPolicy
import com.saferouteai.domain.model.audio.AudioFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class DeterministicAcousticSignalDetectorTest {

    private lateinit var policy: AudioDetectionPolicy
    private lateinit var detector: DeterministicAcousticSignalDetector

    @Before
    fun setUp() {
        policy = AudioDetectionPolicy()
        detector = DeterministicAcousticSignalDetector(policy)
    }

    // Helper to generate a sine wave audio frame
    private fun createSineFrame(
        frequencyHz: Double,
        amplitude: Short,
        timestampEpochMs: Long = 1000L,
        sampleCount: Int = 1600,
        sampleRate: Int = 16000
    ): AudioFrame {
        val pcm = ShortArray(sampleCount)
        val angularFreq = 2.0 * PI * frequencyHz / sampleRate
        for (i in 0 until sampleCount) {
            pcm[i] = (amplitude * sin(angularFreq * i)).toInt().toShort()
        }
        return AudioFrame(timestampEpochMs, pcm, sampleRate)
    }

    // Helper to generate a quiet/silence frame
    private fun createSilenceFrame(timestampEpochMs: Long = 1000L): AudioFrame {
        return AudioFrame(timestampEpochMs, ShortArray(1600), 16000)
    }

    // Helper to generate low ambient noise
    private fun createAmbientNoiseFrame(amplitude: Short = 150, timestampEpochMs: Long = 1000L): AudioFrame {
        val pcm = ShortArray(1600) { i ->
            if (i % 2 == 0) amplitude else (-amplitude).toShort()
        }
        return AudioFrame(timestampEpochMs, pcm, 16000)
    }

    // =============================================================
    // 1. Silence & Ambient Noise
    // =============================================================

    @Test
    fun `silence produces no signal`() {
        val frame = createSilenceFrame()
        val signals = detector.processFrame(frame)
        assertTrue("Silence must produce no acoustic signals", signals.isEmpty())
    }

    @Test
    fun `low ambient noise produces no signal`() {
        repeat(10) { i ->
            val frame = createAmbientNoiseFrame(amplitude = 200, timestampEpochMs = 1000L + (i * 100))
            val signals = detector.processFrame(frame)
            assertTrue("Low ambient noise must produce no signals", signals.isEmpty())
        }
    }

    // =============================================================
    // 2. Normal Conversational Voice Rejection
    // =============================================================

    @Test
    fun `normal speaking low-energy frames produce no signal`() {
        // Normal voice at 250 Hz with moderate amplitude (~3500 peak, ~2470 RMS)
        repeat(15) { i ->
            val frame = createSineFrame(
                frequencyHz = 250.0,
                amplitude = 3500,
                timestampEpochMs = 1000L + (i * 100)
            )
            val signals = detector.processFrame(frame)
            assertTrue("Normal conversational speech must not trigger alerts", signals.isEmpty())
        }
    }

    // =============================================================
    // 3. Isolated Low-Amplitude Click
    // =============================================================

    @Test
    fun `isolated low amplitude click produces no signal`() {
        val pcm = ShortArray(1600)
        pcm[800] = 5000 // Below impact peak threshold (18000)
        val frame = AudioFrame(1000L, pcm, 16000)
        val signals = detector.processFrame(frame)
        assertTrue(signals.isEmpty())
    }

    // =============================================================
    // 4. Single Sample Pop (Low RMS) Rejection
    // =============================================================

    @Test
    fun `single sample pop with high peak but negligible RMS is rejected`() {
        val pcm = ShortArray(1600)
        pcm[500] = 28000 // High peak, but RMS is ~700, below impact RMS threshold 5000
        val frame = AudioFrame(1000L, pcm, 16000)
        val signals = detector.processFrame(frame)
        assertTrue("Single sample pop must not trigger SUDDEN_LOUD_IMPACT", signals.isEmpty())
    }

    // =============================================================
    // 5. Sudden Loud Impact (SUDDEN_LOUD_IMPACT)
    // =============================================================

    @Test
    fun `sudden high-energy transition triggers SUDDEN_LOUD_IMPACT`() {
        // Feed 5 quiet baseline frames
        repeat(5) { i ->
            detector.processFrame(createAmbientNoiseFrame(amplitude = 100, timestampEpochMs = 1000L + (i * 100)))
        }

        // Feed sudden loud impulse (high peak 25000, high RMS > 5000)
        val impactFrame = createSineFrame(
            frequencyHz = 500.0,
            amplitude = 25000,
            timestampEpochMs = 1600L
        )
        val signals = detector.processFrame(impactFrame)

        assertEquals("Should detect exactly 1 signal", 1, signals.size)
        val signal = signals.first()
        assertEquals(AcousticSignalType.SUDDEN_LOUD_IMPACT, signal.type)
        assertEquals("Sudden elevated acoustic impulse detected locally.", signal.explanation)
        assertNotNull(signal.confidence)
        assertTrue("Confidence must be a valid heuristic strength", signal.confidence!! in 0.5f..0.95f)
        assertEquals(1600L, signal.detectedAtEpochMs)
    }

    @Test
    fun `impact rejected if immediately preceding frame was already loud`() {
        // Feed 5 quiet frames
        repeat(5) { i ->
            detector.processFrame(createAmbientNoiseFrame(amplitude = 100, timestampEpochMs = 1000L + (i * 100)))
        }

        // First loud frame
        val frame1 = createSineFrame(500.0, 25000, 1600L)
        val signals1 = detector.processFrame(frame1)
        assertEquals(1, signals1.size)
        assertEquals(AcousticSignalType.SUDDEN_LOUD_IMPACT, signals1[0].type)

        // Second consecutive loud frame (not an impulse onset)
        val frame2 = createSineFrame(500.0, 25000, 1700L)
        val signals2 = detector.processFrame(frame2)
        val impactSignals = signals2.filter { it.type == AcousticSignalType.SUDDEN_LOUD_IMPACT }
        assertTrue("Second consecutive loud frame must not trigger another sudden impact", impactSignals.isEmpty())
    }

    // =============================================================
    // 6. Sustained Vocal Energy (SCREAM_OR_SHOUT)
    // =============================================================

    @Test
    fun `sustained high-energy pattern across 3 consecutive frames triggers SCREAM_OR_SHOUT`() {
        // 800 Hz sine wave, peak 18000, RMS ~12700, ZCR = 2 * 800 / 16000 = 0.10 (within 0.08..0.55)
        val frame1 = createSineFrame(800.0, 18000, 1000L)
        val frame2 = createSineFrame(800.0, 18000, 1100L)
        val frame3 = createSineFrame(800.0, 18000, 1200L)

        val sig1 = detector.processFrame(frame1)
        assertTrue("Frame 1 alone should not trigger scream (needs 3 sustained)", sig1.none { it.type == AcousticSignalType.SCREAM_OR_SHOUT })

        val sig2 = detector.processFrame(frame2)
        assertTrue("Frame 2 alone should not trigger scream (needs 3 sustained)", sig2.none { it.type == AcousticSignalType.SCREAM_OR_SHOUT })

        val sig3 = detector.processFrame(frame3)
        val screamSignals = sig3.filter { it.type == AcousticSignalType.SCREAM_OR_SHOUT }
        assertEquals("Frame 3 should trigger SCREAM_OR_SHOUT", 1, screamSignals.size)

        val scream = screamSignals.first()
        assertEquals(AcousticSignalType.SCREAM_OR_SHOUT, scream.type)
        assertEquals("Sustained elevated vocal-like acoustic energy detected locally.", scream.explanation)
        assertNotNull(scream.confidence)
        assertTrue(scream.confidence!! in 0.6f..0.95f)
        assertEquals(300L, scream.durationMs) // 3 frames * 100ms
    }

    @Test
    fun `scream episode is debounced during continuous sustained loud sound`() {
        val frames = (1..6).map { i ->
            createSineFrame(800.0, 18000, 1000L + (i * 100))
        }

        var screamCount = 0
        frames.forEach { frame ->
            val sigs = detector.processFrame(frame)
            screamCount += sigs.count { it.type == AcousticSignalType.SCREAM_OR_SHOUT }
        }

        assertEquals("Scream should fire exactly once per continuous episode", 1, screamCount)
    }

    @Test
    fun `high-energy signal with invalid ZCR (high-frequency hiss) rejects scream`() {
        // High frequency alternating samples (ZCR = 1.0 > screamZcrMax 0.55)
        repeat(5) { i ->
            val pcm = ShortArray(1600) { idx -> if (idx % 2 == 0) 18000 else (-18000).toShort() }
            val frame = AudioFrame(1000L + (i * 100), pcm, 16000)
            val signals = detector.processFrame(frame)
            assertTrue("High frequency hiss must not trigger scream", signals.none { it.type == AcousticSignalType.SCREAM_OR_SHOUT })
        }
    }

    // =============================================================
    // 7. Persistent Commotion (PERSISTENT_DISTRESS_COMMOTION)
    // =============================================================

    @Test
    fun `repeated elevated frames over sliding window trigger PERSISTENT_DISTRESS_COMMOTION`() {
        // Feed 10 elevated frames out of 25 (exceeds commotionMinElevatedFrames = 8)
        var commotionDetected = false
        repeat(25) { i ->
            val frame = if (i % 2 == 0) {
                // Elevated frame: RMS > 3500
                createSineFrame(400.0, 8000, 1000L + (i * 100))
            } else {
                createSilenceFrame(1000L + (i * 100))
            }
            val sigs = detector.processFrame(frame)
            if (sigs.any { it.type == AcousticSignalType.PERSISTENT_DISTRESS_COMMOTION }) {
                commotionDetected = true
                val commotion = sigs.first { it.type == AcousticSignalType.PERSISTENT_DISTRESS_COMMOTION }
                assertEquals("Repeated elevated acoustic activity detected within the recent window.", commotion.explanation)
                assertNotNull(commotion.confidence)
            }
        }

        assertTrue("Commotion must be detected when elevated frames exceed threshold", commotionDetected)
    }

    // =============================================================
    // 8. Feature Extraction Accuracy
    // =============================================================

    @Test
    fun `feature extractor correctly computes RMS, peak, ZCR, and energy`() {
        // Pure 1000 Hz sine wave at 16000 Hz sample rate, amplitude 10000
        val frame = createSineFrame(1000.0, 10000, 5000L, 1600, 16000)
        val features = AcousticFeatureExtractor.extract(frame)

        // Peak should be very close to 10000
        assertTrue("Peak amplitude should be near 10000", features.peakAmplitude in 9900..10000)

        // RMS of sine wave is Amplitude / sqrt(2) = 10000 / 1.4142 = ~7071
        assertTrue("RMS should be ~7071", features.rms in 7000.0..7150.0)

        // Energy = rms^2
        assertEquals(features.rms * features.rms, features.energy, 0.1)

        // 1000 Hz in 16000 Hz has 2 zero crossings per period (16 samples/period) -> ZCR = 1/8 = 0.125
        assertTrue("ZCR should be ~0.125", features.zeroCrossingRate in 0.11..0.14)

        assertEquals(100L, features.durationMs)
        assertEquals(1600, features.sampleCount)
    }

    @Test
    fun `empty audio frame produces safe zeroed features without crash`() {
        val emptyFrame = AudioFrame(1000L, ShortArray(0), 16000)
        val features = AcousticFeatureExtractor.extract(emptyFrame)
        assertEquals(0.0, features.rms, 0.001)
        assertEquals(0, features.peakAmplitude.toInt())
        assertEquals(0.0, features.zeroCrossingRate, 0.001)

        val signals = detector.processFrame(emptyFrame)
        assertTrue(signals.isEmpty())
    }

    // =============================================================
    // 9. Bounded Feature Window & Reset
    // =============================================================

    @Test
    fun `bounded feature window caps at maxCapacity`() {
        val window = BoundedFeatureWindow(maxCapacity = 10)
        repeat(25) { i ->
            window.add(
                AcousticFeatures(
                    timestampEpochMs = 1000L + i,
                    sampleCount = 1600,
                    sampleRate = 16000,
                    rms = i * 100.0,
                    peakAmplitude = (i * 100).toShort(),
                    zeroCrossingRate = 0.1,
                    highFrequencyRatio = 0.1,
                    energy = 1000.0,
                    durationMs = 100L
                )
            )
        }

        assertEquals(10, window.size)
        // Verify oldest entries dropped, last entry is i = 24
        assertEquals(2400.0, window.getAll().last().rms, 0.1)
    }

    @Test
    fun `detector reset clears state and prevents stale triggers`() {
        // Feed 2 loud frames towards a scream
        detector.processFrame(createSineFrame(800.0, 18000, 1000L))
        detector.processFrame(createSineFrame(800.0, 18000, 1100L))

        // Reset detector (e.g. capture stopped)
        detector.reset()

        // Next frame alone must not trigger scream because history was reset
        val sigs = detector.processFrame(createSineFrame(800.0, 18000, 2000L))
        assertTrue(sigs.none { it.type == AcousticSignalType.SCREAM_OR_SHOUT })
    }

    @Test
    fun `NoOpAcousticSignalDetector returns empty list`() {
        val noOp = NoOpAcousticSignalDetector()
        val frame = createSineFrame(1000.0, 25000)
        assertTrue(noOp.processFrame(frame).isEmpty())
        noOp.reset()
    }
}
