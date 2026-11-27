package com.elantradiagnostic.pro

/** Resolves legacy mediaId playback requests only to items currently in our local library. */
internal object MediaLibraryItemResolver {
    fun resolveIds(requestedIds: List<String>, availableIds: Set<String>): List<String> =
        requestedIds.filter { it in availableIds }
}
