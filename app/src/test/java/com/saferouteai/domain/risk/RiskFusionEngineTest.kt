package com.saferouteai.domain.risk

import com.saferouteai.domain.model.anomaly.AnomalySeverity
import com.saferouteai.domain.model.anomaly.AnomalySignal
import com.saferouteai.domain.model.anomaly.AnomalyType
import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.model.audio.AcousticSignalType
import com.saferouteai.domain.model.risk.RiskFactorType
import com.saferouteai.domain.model.risk.RiskFusionPolicy
import com.saferouteai.domain.model.risk.SafetyTier
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RiskFusionEngineTest {

    private lateinit var policy: RiskFusionPolicy
    private lateinit var engine: RiskFusionEngine

    private val sessionId1 = "session-111"
    private val sessionId2 = "session-222"
    private val baseTime = 1_000_000L

    private fun createActiveSession(
        id: String = sessionId1,
        startedAt: Long = baseTime - 60_000L
    ): JourneySession = JourneySession(
        sessionId = id,
        status = SessionStatus.ACTIVE,
        startedAtEpochMs = startedAt
    )

    private fun createInactiveSession(
        id: String = sessionId1,
        status: SessionStatus = SessionStatus.COMPLETED
    ): JourneySession = JourneySession(
        sessionId = id,
        status = status,
        startedAtEpochMs = baseTime - 60_000L,
        endedAtEpochMs = baseTime - 10_000L
    )

    @Before
    fun setUp() {
        policy = RiskFusionPolicy()
        engine = RiskFusionEngine(policy)
    }

    @Test
    fun `1 - No signals yields NORMAL with zero score`() {
        val session = createActiveSession()
        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = emptyList(),
            acousticSignals = emptyList(),
            currentTimeEpochMs = baseTime
        )

        assertEquals(SafetyTier.NORMAL, assessment.tier)
        assertEquals(0, assessment.score)
        assertTrue(assessment.factors.isEmpty())
        assertEquals(baseTime, assessment.assessedAtEpochMs)
    }

    @Test
    fun `2 - One low-strength signal yields NORMAL`() {
        val session = createActiveSession()
        // Duration anomaly has default weight 10, which is <= normalMaxScore (29)
        val anomaly = AnomalySignal(
            sessionId = sessionId1,
            type = AnomalyType.DURATION_ANOMALY,
            severity = AnomalySeverity.LOW,
            timestampEpochMs = baseTime - 5000L,
            explanation = "Duration slightly elevated."
        )

        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = listOf(anomaly),
            acousticSignals = emptyList(),
            currentTimeEpochMs = baseTime
        )

        assertEquals(SafetyTier.NORMAL, assessment.tier)
        assertEquals(10, assessment.score)
        assertEquals(1, assessment.factors.size)
        assertEquals(RiskFactorType.DURATION_ANOMALY, assessment.factors[0].type)
    }

    @Test
    fun `3 - Location anomaly only yields expected policy tier`() {
        val session = createActiveSession()
        // Route deviation (20) + Unusual movement (20) = 40 -> ELEVATED (30..59)
        val anomalies = listOf(
            AnomalySignal(
                sessionId = sessionId1,
                type = AnomalyType.ROUTE_DEVIATION,
                severity = AnomalySeverity.HIGH,
                timestampEpochMs = baseTime - 10_000L,
                explanation = "Route deviation detected."
            ),
            AnomalySignal(
                sessionId = sessionId1,
                type = AnomalyType.UNUSUAL_MOVEMENT,
                severity = AnomalySeverity.HIGH,
                timestampEpochMs = baseTime - 5_000L,
                explanation = "Repeated direction reversals."
            )
        )

        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = anomalies,
            acousticSignals = emptyList(),
            currentTimeEpochMs = baseTime
        )

        assertEquals(SafetyTier.ELEVATED, assessment.tier)
        assertEquals(40, assessment.score)
        assertEquals(2, assessment.factors.size)
        // No cross-source bonus since only location evidence is present
        assertFalse(assessment.factors.any { it.type == RiskFactorType.OTHER_CONTEXT })
    }

    @Test
    fun `4 - Acoustic signal only yields expected policy tier`() {
        val session = createActiveSession()
        // Sudden loud impact (15) + Scream/shout (25) = 40 -> ELEVATED (30..59)
        val acousticSignals = listOf(
            AcousticSignal(
                sessionId = sessionId1,
                type = AcousticSignalType.SUDDEN_LOUD_IMPACT,
                detectedAtEpochMs = baseTime - 10_000L,
                explanation = "Sudden elevated acoustic impulse detected locally."
            ),
            AcousticSignal(
                sessionId = sessionId1,
                type = AcousticSignalType.SCREAM_OR_SHOUT,
                detectedAtEpochMs = baseTime - 5_000L,
                explanation = "Sustained elevated vocal-like acoustic energy detected locally."
            )
        )

        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = emptyList(),
            acousticSignals = acousticSignals,
            currentTimeEpochMs = baseTime
        )

        assertEquals(SafetyTier.ELEVATED, assessment.tier)
        assertEquals(40, assessment.score)
        assertEquals(2, assessment.factors.size)
        assertFalse(assessment.factors.any { it.type == RiskFactorType.OTHER_CONTEXT })
    }

    @Test
    fun `5 - Location plus acoustic independent signals yields stronger tier with synergy bonus`() {
        val session = createActiveSession()
        // Route deviation (20) + Scream/shout (25) + Cross-source synergy (15) = 60 -> HIGH (60+)
        val locationAnomaly = AnomalySignal(
            sessionId = sessionId1,
            type = AnomalyType.ROUTE_DEVIATION,
            severity = AnomalySeverity.HIGH,
            timestampEpochMs = baseTime - 10_000L,
            explanation = "Vehicle deviated from corridor."
        )
        val acousticSignal = AcousticSignal(
            sessionId = sessionId1,
            type = AcousticSignalType.SCREAM_OR_SHOUT,
            detectedAtEpochMs = baseTime - 5_000L,
            explanation = "Sustained vocalization detected."
        )

        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = listOf(locationAnomaly),
            acousticSignals = listOf(acousticSignal),
            currentTimeEpochMs = baseTime
        )

        assertEquals(SafetyTier.HIGH, assessment.tier)
        assertEquals(60, assessment.score)
        assertEquals(3, assessment.factors.size)
        val synergyFactor = assessment.factors.find { it.type == RiskFactorType.OTHER_CONTEXT }
        org.junit.Assert.assertNotNull("Synergy factor must be present for cross-modal evidence", synergyFactor)
        assertEquals(policy.crossSourceBonusWeight, synergyFactor!!.contribution)
    }

    @Test
    fun `6 - HIGH threshold boundary`() {
        val session = createActiveSession()
        // Score 59 -> ELEVATED, Score 60 -> HIGH
        val customPolicy = RiskFusionPolicy(normalMaxScore = 29, elevatedMaxScore = 59)
        val customEngine = RiskFusionEngine(customPolicy)

        // Prolonged stop (15) + Sudden impact (15) + Route deviation (20) = 50 + 15 (bonus) = 65 -> HIGH
        val assessment = customEngine.evaluate(
            session = session,
            locationAnomalies = listOf(
                AnomalySignal(sessionId = sessionId1, type = AnomalyType.PROLONGED_STOP, severity = AnomalySeverity.MEDIUM, timestampEpochMs = baseTime, explanation = "Stop"),
                AnomalySignal(sessionId = sessionId1, type = AnomalyType.ROUTE_DEVIATION, severity = AnomalySeverity.HIGH, timestampEpochMs = baseTime, explanation = "Deviation")
            ),
            acousticSignals = listOf(
                AcousticSignal(sessionId = sessionId1, type = AcousticSignalType.SUDDEN_LOUD_IMPACT, detectedAtEpochMs = baseTime, explanation = "Impact")
            ),
            currentTimeEpochMs = baseTime
        )

        assertEquals(65, assessment.score)
        assertEquals(SafetyTier.HIGH, assessment.tier)
    }

    @Test
    fun `7 - NORMAL-ELEVATED boundary`() {
        val session = createActiveSession()
        // Score exactly 29 -> NORMAL
        val policy29 = RiskFusionPolicy(prolongedStopWeight = 29)
        val engine29 = RiskFusionEngine(policy29)
        val assessment29 = engine29.evaluate(
            session = session,
            locationAnomalies = listOf(
                AnomalySignal(sessionId = sessionId1, type = AnomalyType.PROLONGED_STOP, severity = AnomalySeverity.LOW, timestampEpochMs = baseTime, explanation = "Stop")
            ),
            acousticSignals = emptyList(),
            currentTimeEpochMs = baseTime
        )
        assertEquals(29, assessment29.score)
        assertEquals(SafetyTier.NORMAL, assessment29.tier)

        // Score 30 -> ELEVATED
        val policy30 = RiskFusionPolicy(prolongedStopWeight = 30)
        val engine30 = RiskFusionEngine(policy30)
        val assessment30 = engine30.evaluate(
            session = session,
            locationAnomalies = listOf(
                AnomalySignal(sessionId = sessionId1, type = AnomalyType.PROLONGED_STOP, severity = AnomalySeverity.LOW, timestampEpochMs = baseTime, explanation = "Stop")
            ),
            acousticSignals = emptyList(),
            currentTimeEpochMs = baseTime
        )
        assertEquals(30, assessment30.score)
        assertEquals(SafetyTier.ELEVATED, assessment30.tier)
    }

    @Test
    fun `8 - Duplicate signals do not artificially inflate score`() {
        val session = createActiveSession()
        // 10 repeated PROLONGED_STOP signals must only yield 1 factor (+15)
        val tenStops = (1..10).map { i ->
            AnomalySignal(
                id = "stop-$i",
                sessionId = sessionId1,
                type = AnomalyType.PROLONGED_STOP,
                severity = AnomalySeverity.MEDIUM,
                timestampEpochMs = baseTime - (i * 1000L),
                explanation = "Stop observation $i"
            )
        }

        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = tenStops,
            acousticSignals = emptyList(),
            currentTimeEpochMs = baseTime
        )

        assertEquals(1, assessment.factors.size)
        assertEquals(15, assessment.score)
        assertEquals(SafetyTier.NORMAL, assessment.tier)
    }

    @Test
    fun `9 - Repeated same-source signals obey aggregation policy`() {
        val session = createActiveSession()
        // 5 repeated loud impacts
        val fiveImpacts = (1..5).map { i ->
            AcousticSignal(
                id = "impact-$i",
                sessionId = sessionId1,
                type = AcousticSignalType.SUDDEN_LOUD_IMPACT,
                detectedAtEpochMs = baseTime - (i * 1000L),
                explanation = "Impact $i"
            )
        }

        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = emptyList(),
            acousticSignals = fiveImpacts,
            currentTimeEpochMs = baseTime
        )

        assertEquals(1, assessment.factors.size)
        assertEquals(15, assessment.score)
    }

    @Test
    fun `10 - Expired signals no longer contribute`() {
        val session = createActiveSession(startedAt = baseTime - 600_000L)
        // Location signal validity is 5 minutes (300_000 ms)
        val expiredAnomaly = AnomalySignal(
            sessionId = sessionId1,
            type = AnomalyType.ROUTE_DEVIATION,
            severity = AnomalySeverity.HIGH,
            timestampEpochMs = baseTime - 350_000L, // 5m 50s ago
            explanation = "Old deviation"
        )
        // Acoustic signal validity is 3 minutes (180_000 ms)
        val expiredAcoustic = AcousticSignal(
            sessionId = sessionId1,
            type = AcousticSignalType.SCREAM_OR_SHOUT,
            detectedAtEpochMs = baseTime - 200_000L, // 3m 20s ago
            explanation = "Old scream"
        )

        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = listOf(expiredAnomaly),
            acousticSignals = listOf(expiredAcoustic),
            currentTimeEpochMs = baseTime
        )

        assertEquals(0, assessment.score)
        assertEquals(SafetyTier.NORMAL, assessment.tier)
        assertTrue(assessment.factors.isEmpty())
    }

    @Test
    fun `11 - Fresh signal contributes again alongside expired signal`() {
        val session = createActiveSession(startedAt = baseTime - 600_000L)
        val expiredAnomaly = AnomalySignal(
            sessionId = sessionId1,
            type = AnomalyType.ROUTE_DEVIATION,
            severity = AnomalySeverity.HIGH,
            timestampEpochMs = baseTime - 400_000L, // expired
            explanation = "Old deviation"
        )
        val freshAnomaly = AnomalySignal(
            sessionId = sessionId1,
            type = AnomalyType.PROLONGED_STOP,
            severity = AnomalySeverity.MEDIUM,
            timestampEpochMs = baseTime - 10_000L, // fresh (10s ago)
            explanation = "Active stop"
        )

        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = listOf(expiredAnomaly, freshAnomaly),
            acousticSignals = emptyList(),
            currentTimeEpochMs = baseTime
        )

        assertEquals(15, assessment.score)
        assertEquals(1, assessment.factors.size)
        assertEquals(RiskFactorType.PROLONGED_STOP, assessment.factors[0].type)
    }

    @Test
    fun `12 - Inactive session returns NORMAL and empty factors`() {
        val completedSession = createInactiveSession(status = SessionStatus.COMPLETED)
        val signals = listOf(
            AnomalySignal(
                sessionId = sessionId1,
                type = AnomalyType.ROUTE_DEVIATION,
                severity = AnomalySeverity.HIGH,
                timestampEpochMs = baseTime - 1000L,
                explanation = "Deviation"
            )
        )

        val assessment = engine.evaluate(
            session = completedSession,
            locationAnomalies = signals,
            acousticSignals = emptyList(),
            currentTimeEpochMs = baseTime
        )

        assertEquals(SafetyTier.NORMAL, assessment.tier)
        assertEquals(0, assessment.score)
        assertTrue(assessment.factors.isEmpty())
    }

    @Test
    fun `13 - Completed journey leaves no stale risk`() {
        val nullSession: JourneySession? = null
        val assessment = engine.evaluate(
            session = nullSession,
            locationAnomalies = listOf(
                AnomalySignal(sessionId = sessionId1, type = AnomalyType.ROUTE_DEVIATION, severity = AnomalySeverity.HIGH, timestampEpochMs = baseTime, explanation = "Old")
            ),
            acousticSignals = emptyList(),
            currentTimeEpochMs = baseTime
        )

        assertEquals(SafetyTier.NORMAL, assessment.tier)
        assertEquals(0, assessment.score)
        assertTrue(assessment.factors.isEmpty())
    }

    @Test
    fun `14 - New journey starts clean and rejects signals from previous journey session`() {
        val session2 = createActiveSession(id = sessionId2, startedAt = baseTime - 10_000L)
        // Signals tagged with sessionId1 must be rejected
        val staleLocation = AnomalySignal(
            sessionId = sessionId1,
            type = AnomalyType.ROUTE_DEVIATION,
            severity = AnomalySeverity.HIGH,
            timestampEpochMs = baseTime - 5000L,
            explanation = "Session 1 deviation"
        )
        val staleAcoustic = AcousticSignal(
            sessionId = sessionId1,
            type = AcousticSignalType.SCREAM_OR_SHOUT,
            detectedAtEpochMs = baseTime - 5000L,
            explanation = "Session 1 shout"
        )

        val assessment = engine.evaluate(
            session = session2,
            locationAnomalies = listOf(staleLocation),
            acousticSignals = listOf(staleAcoustic),
            currentTimeEpochMs = baseTime
        )

        assertEquals(0, assessment.score)
        assertEquals(SafetyTier.NORMAL, assessment.tier)
        assertTrue(assessment.factors.isEmpty())
    }

    @Test
    fun `15 - All factor explanations are present and non-alarmist`() {
        val session = createActiveSession()
        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = listOf(
                AnomalySignal(sessionId = sessionId1, type = AnomalyType.ROUTE_DEVIATION, severity = AnomalySeverity.HIGH, timestampEpochMs = baseTime, explanation = "Corridor deviation observed.")
            ),
            acousticSignals = listOf(
                AcousticSignal(sessionId = sessionId1, type = AcousticSignalType.SUDDEN_LOUD_IMPACT, detectedAtEpochMs = baseTime, explanation = "Sudden elevated acoustic impulse detected locally.")
            ),
            currentTimeEpochMs = baseTime
        )

        assertTrue(assessment.factors.isNotEmpty())
        assessment.factors.forEach { factor ->
            assertTrue("Explanation must not be blank", factor.explanation.isNotBlank())
            assertFalse("Explanation must not use alarmist danger language", factor.explanation.contains("danger", ignoreCase = true))
            assertFalse("Explanation must not use alarmist emergency language", factor.explanation.contains("emergency", ignoreCase = true))
        }
    }

    @Test
    fun `16 - Risk score remains bounded between 0 and 100`() {
        val session = createActiveSession()
        // Super massive factors that would exceed 100 if uncapped
        val massivePolicy = RiskFusionPolicy(
            prolongedStopWeight = 50,
            routeDeviationWeight = 50,
            screamOrShoutWeight = 50,
            crossSourceBonusWeight = 50
        )
        val customEngine = RiskFusionEngine(massivePolicy)

        val assessment = customEngine.evaluate(
            session = session,
            locationAnomalies = listOf(
                AnomalySignal(sessionId = sessionId1, type = AnomalyType.PROLONGED_STOP, severity = AnomalySeverity.HIGH, timestampEpochMs = baseTime, explanation = "Stop"),
                AnomalySignal(sessionId = sessionId1, type = AnomalyType.ROUTE_DEVIATION, severity = AnomalySeverity.HIGH, timestampEpochMs = baseTime, explanation = "Dev")
            ),
            acousticSignals = listOf(
                AcousticSignal(sessionId = sessionId1, type = AcousticSignalType.SCREAM_OR_SHOUT, detectedAtEpochMs = baseTime, explanation = "Shout")
            ),
            currentTimeEpochMs = baseTime
        )

        assertEquals(100, assessment.score) // Coerced to 100
        assertEquals(SafetyTier.HIGH, assessment.tier)
    }

    @Test
    fun `17 - Session isolation verifies acoustic signals tagged with wrong session are excluded`() {
        val session = createActiveSession(id = sessionId1)
        val validSignal = AcousticSignal(
            sessionId = sessionId1,
            type = AcousticSignalType.SUDDEN_LOUD_IMPACT,
            detectedAtEpochMs = baseTime,
            explanation = "Valid impact"
        )
        val invalidSignal = AcousticSignal(
            sessionId = "other-session",
            type = AcousticSignalType.SCREAM_OR_SHOUT,
            detectedAtEpochMs = baseTime,
            explanation = "Other session shout"
        )

        val assessment = engine.evaluate(
            session = session,
            locationAnomalies = emptyList(),
            acousticSignals = listOf(validSignal, invalidSignal),
            currentTimeEpochMs = baseTime
        )

        assertEquals(1, assessment.factors.size)
        assertEquals(RiskFactorType.SUDDEN_LOUD_IMPACT, assessment.factors[0].type)
        assertEquals(15, assessment.score)
    }
}
