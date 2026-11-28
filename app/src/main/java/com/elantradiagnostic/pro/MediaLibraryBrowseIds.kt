package com.elantradiagnostic.pro

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Stable IDs for the hierarchical local-audio tree exposed to Android Auto.
 * Artist names are URL-encoded so punctuation, spaces and non-Latin names cannot
 * collide with the reserved folder IDs.
 */
internal object MediaLibraryBrowseIds {
    const val ROOT = "elantra_local_music"
    const val ALL_TRACKS = "$ROOT:all_tracks"
    const val ARTISTS = "$ROOT:artists"

    private const val ARTIST_PREFIX = "$ROOT:artist:"

    fun artistId(name: String): String {
        require(name.isNotBlank()) { "Artist name must not be blank" }
        return ARTIST_PREFIX + URLEncoder.encode(name, "UTF-8")
    }

    fun artistName(mediaId: String): String? {
        if (!mediaId.startsWith(ARTIST_PREFIX)) return null
        val encodedName = mediaId.removePrefix(ARTIST_PREFIX)
        if (encodedName.isBlank()) return null

        return try {
            URLDecoder.decode(encodedName, "UTF-8").takeIf { it.isNotBlank() }
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
