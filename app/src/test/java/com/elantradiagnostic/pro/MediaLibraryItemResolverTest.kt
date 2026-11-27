package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaLibraryItemResolverTest {

    @Test
    fun resolvesLegacyMediaIdsInRequestedOrder() {
        assertEquals(
            listOf("content://media/audio/2", "content://media/audio/1"),
            MediaLibraryItemResolver.resolveIds(
                listOf("content://media/audio/2", "content://media/audio/1"),
                setOf("content://media/audio/1", "content://media/audio/2")
            )
        )
    }

    @Test
    fun omitsItemsNoLongerPresentInTheLocalLibrary() {
        assertEquals(
            listOf("track-1"),
            MediaLibraryItemResolver.resolveIds(
                listOf("track-1", "removed-track"),
                setOf("track-1")
            )
        )
    }

    @Test
    fun preservesDuplicatesAndReturnsEmptyForUnknownIds() {
        assertEquals(
            listOf("track-1", "track-1"),
            MediaLibraryItemResolver.resolveIds(
                listOf("track-1", "missing", "track-1"),
                setOf("track-1")
            )
        )
        assertEquals(
            emptyList<String>(),
            MediaLibraryItemResolver.resolveIds(listOf("missing"), emptySet())
        )
    }
}
