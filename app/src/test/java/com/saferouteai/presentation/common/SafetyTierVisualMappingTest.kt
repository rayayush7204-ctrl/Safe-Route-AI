package com.saferouteai.presentation.common

import com.saferouteai.domain.model.risk.SafetyTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyTierVisualMappingTest {

    @Test
    fun normalTier_mapsToCorrectLabelAndConservativeNarrative() {
        val label = SafetyTierVisualMapping.getLabel(SafetyTier.NORMAL)
        val narrative = SafetyTierVisualMapping.getNarrative(SafetyTier.NORMAL)

        assertEquals("NORMAL", label)
        assertEquals("No unusual signals detected.", narrative)

        // Ensure non-alarmist language rules
        assertFalse(narrative.contains("safe", ignoreCase = true))
        assertFalse(narrative.contains("danger", ignoreCase = true))
        assertFalse(narrative.contains("emergency", ignoreCase = true))
    }

    @Test
    fun elevatedTier_mapsToCorrectLabelAndConservativeNarrative() {
        val label = SafetyTierVisualMapping.getLabel(SafetyTier.ELEVATED)
        val narrative = SafetyTierVisualMapping.getNarrative(SafetyTier.ELEVATED)

        assertEquals("ELEVATED", label)
        assertEquals("A few unusual patterns were detected.", narrative)

        // Ensure non-alarmist language rules
        assertFalse(narrative.contains("danger", ignoreCase = true))
        assertFalse(narrative.contains("unsafe", ignoreCase = true))
        assertFalse(narrative.contains("emergency", ignoreCase = true))
    }

    @Test
    fun highTier_mapsToCorrectLabelAndConservativeNarrative() {
        val label = SafetyTierVisualMapping.getLabel(SafetyTier.HIGH)
        val narrative = SafetyTierVisualMapping.getNarrative(SafetyTier.HIGH)

        assertEquals("HIGH", label)
        assertEquals("Multiple unusual signals were detected.", narrative)

        // Ensure non-alarmist language rules
        assertFalse(narrative.contains("Danger confirmed", ignoreCase = true))
        assertFalse(narrative.contains("You are unsafe", ignoreCase = true))
        assertFalse(narrative.contains("Emergency detected", ignoreCase = true))
        assertFalse(narrative.contains("SOS", ignoreCase = true))
    }

    @Test
    fun allTiers_haveNonEmptyDistinctLabelsAndNarratives() {
        val labels = SafetyTier.entries.map { SafetyTierVisualMapping.getLabel(it) }
        val narratives = SafetyTier.entries.map { SafetyTierVisualMapping.getNarrative(it) }

        assertEquals(SafetyTier.entries.size, labels.toSet().size)
        assertEquals(SafetyTier.entries.size, narratives.toSet().size)

        labels.forEach { assertTrue(it.isNotBlank()) }
        narratives.forEach { assertTrue(it.isNotBlank()) }
    }
}
