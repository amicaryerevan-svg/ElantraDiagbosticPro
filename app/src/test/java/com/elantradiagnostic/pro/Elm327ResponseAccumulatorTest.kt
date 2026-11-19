package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Elm327ResponseAccumulatorTest {

    @Test
    fun waitsForPromptAcrossMultipleReads() {
        val accumulator = Elm327ResponseAccumulator()

        assertFalse(accumulator.append("41 0C 1A".toByteArray(), 8))
        assertFalse(accumulator.isComplete)
        assertTrue(accumulator.append("F8\r>".toByteArray(), 5))

        assertTrue(accumulator.isComplete)
        assertEquals("41 0C 1AF8", accumulator.content())
    }

    @Test
    fun returnsCompleteResponseWhenPromptIsInSameChunk() {
        val accumulator = Elm327ResponseAccumulator()
        val bytes = "ELM327 v1.5\r>".toByteArray()

        assertTrue(accumulator.append(bytes, bytes.size))
        assertEquals("ELM327 v1.5", accumulator.content())
    }

    @Test
    fun doesNotTreatPartialDataAsComplete() {
        val accumulator = Elm327ResponseAccumulator()
        val bytes = "NO DATA".toByteArray()

        assertFalse(accumulator.append(bytes, bytes.size))
        assertFalse(accumulator.isComplete)
        assertEquals("NO DATA", accumulator.content())
    }

    @Test
    fun ignoresBytesAfterThePrompt() {
        val accumulator = Elm327ResponseAccumulator()
        val bytes = "43 01 33>STALE".toByteArray()

        assertTrue(accumulator.append(bytes, bytes.size))
        assertEquals("43 01 33", accumulator.content())
    }
}
