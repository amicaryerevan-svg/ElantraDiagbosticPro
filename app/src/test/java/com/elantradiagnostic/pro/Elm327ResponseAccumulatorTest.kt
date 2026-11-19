package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Elm327ResponseAccumulatorTest {

    @Test
    fun waitsForPromptAcrossMultipleReads() {
        val accumulator = Elm327ResponseAccumulator()

        val firstChunk = "41 0C 1A ".toByteArray()
        assertFalse(accumulator.append(firstChunk, firstChunk.size))
        assertFalse(accumulator.isComplete)

        val secondChunk = "F8\r>".toByteArray()
        assertTrue(accumulator.append(secondChunk, secondChunk.size))

        assertTrue(accumulator.isComplete)
        assertEquals("41 0C 1A F8", accumulator.content())
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
