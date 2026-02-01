package com.faceanalyzer.app

import com.faceanalyzer.app.analyzer.*
import org.junit.Assert.*
import org.junit.Test

class FaceAnalysisTest {

    @Test
    fun `health level is excellent for scores above 85`() {
        val score = 90
        val healthLevel = score.toHealthLevel()
        assertEquals(HealthLevel.EXCELLENT, healthLevel)
    }

    @Test
    fun `health level is good for scores between 70 and 85`() {
        val score = 75
        val healthLevel = score.toHealthLevel()
        assertEquals(HealthLevel.GOOD, healthLevel)
    }

    @Test
    fun `health level is fair for scores between 50 and 70`() {
        val score = 60
        val healthLevel = score.toHealthLevel()
        assertEquals(HealthLevel.FAIR, healthLevel)
    }

    @Test
    fun `health level is poor for scores below 50`() {
        val score = 40
        val healthLevel = score.toHealthLevel()
        assertEquals(HealthLevel.POOR, healthLevel)
    }

    @Test
    fun `emotion enum has correct display names`() {
        assertEquals("Happy", Emotion.HAPPY.displayName)
        assertEquals("Sad", Emotion.SAD.displayName)
        assertEquals("Neutral", Emotion.NEUTRAL.displayName)
    }

    @Test
    fun `face condition has valid default values`() {
        val condition = FaceCondition()
        assertEquals(0, condition.overallScore)
        assertTrue(condition.suggestions.isEmpty())
    }

    @Test
    fun `face analysis result defaults to no face detected`() {
        val result = FaceAnalysisResult()
        assertFalse(result.faceDetected)
        assertNull(result.boundingBox)
    }
}
