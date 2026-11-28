package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaLibraryBrowseIdsTest {

    @Test
    fun artistIdsRoundTripUnicodeAndReservedCharacters() {
        val names = listOf(
            "Artist One",
            "A/B + C",
            "Исполнитель №1",
            "Björk",
            "🎵 Live"
        )

        names.forEach { name ->
            assertEquals(name, MediaLibraryBrowseIds.artistName(MediaLibraryBrowseIds.artistId(name)))
        }
    }

    @Test
    fun differentArtistNamesProduceDifferentIds() {
        assertNotEquals(
            MediaLibraryBrowseIds.artistId("A/B"),
            MediaLibraryBrowseIds.artistId("A B")
        )
        assertNotEquals(
            MediaLibraryBrowseIds.artistId("Artist"),
            MediaLibraryBrowseIds.ALL_TRACKS
        )
    }

    @Test
    fun rejectsNonArtistAndMalformedIds() {
        assertNull(MediaLibraryBrowseIds.artistName(MediaLibraryBrowseIds.ROOT))
        assertNull(MediaLibraryBrowseIds.artistName(MediaLibraryBrowseIds.ARTISTS))
        assertNull(MediaLibraryBrowseIds.artistName("elantra_local_music:artist:"))
        assertNull(MediaLibraryBrowseIds.artistName("elantra_local_music:artist:%ZZ"))
    }
}
