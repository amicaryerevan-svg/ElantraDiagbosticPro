package com.elantradiagnostic.pro

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.Locale

/**
 * Exposes the phone's locally stored music to Android Auto.
 *
 * This service deliberately does not extract or stream YouTube content.
 * Android Auto renders its own driver-optimized media interface.
 */
class ElantraMediaService : MediaLibraryService() {

    private var librarySession: MediaLibrarySession? = null

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            // Legacy MediaBrowser clients (including Android Auto integrations) can
            // request playback by mediaId without carrying the original content URI.
            // Resolve those IDs against the current MediaStore library before ExoPlayer
            // receives the playlist. Removed files and unknown IDs are not playable.
            val availableItems = queryLocalAudio().associateBy { it.mediaId }
            val resolvedIds = MediaLibraryItemResolver.resolveIds(
                mediaItems.map { it.mediaId },
                availableItems.keys
            )
            val resolvedItems = resolvedIds.mapNotNull(availableItems::get)
            return Futures.immediateFuture(resolvedItems)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem(), params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children: List<MediaItem> = when (parentId) {
                MediaLibraryBrowseIds.ROOT -> listOf(
                    categoryItem(
                        MediaLibraryBrowseIds.ALL_TRACKS,
                        "Все композиции",
                        "Вся музыка, сохранённая на телефоне"
                    ),
                    categoryItem(
                        MediaLibraryBrowseIds.ARTISTS,
                        "Исполнители",
                        "Музыка по исполнителям"
                    )
                )

                MediaLibraryBrowseIds.ALL_TRACKS -> queryLocalAudio()

                MediaLibraryBrowseIds.ARTISTS -> artistFolders(queryLocalAudio())

                else -> {
                    val artist = MediaLibraryBrowseIds.artistName(parentId)
                    if (artist == null) {
                        emptyList()
                    } else {
                        queryLocalAudio().filter { trackArtist(it).equals(artist, ignoreCase = true) }
                    }
                }
            }

            // Media3 supplies pageSize > 0. Keep defensive bounds for unusual legacy clients.
            val safePage = page.coerceAtLeast(0).toLong()
            val safePageSize = pageSize.coerceAtLeast(1).toLong()
            val start = (safePage * safePageSize)
                .coerceAtMost(children.size.toLong())
                .toInt()
            val end = (start.toLong() + safePageSize)
                .coerceAtMost(children.size.toLong())
                .toInt()
            val pageItems = ImmutableList.copyOf(children.subList(start, end))
            return Futures.immediateFuture(LibraryResult.ofItemList(pageItems, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = when (mediaId) {
                MediaLibraryBrowseIds.ROOT -> rootItem()
                MediaLibraryBrowseIds.ALL_TRACKS -> categoryItem(
                    MediaLibraryBrowseIds.ALL_TRACKS,
                    "Все композиции",
                    "Вся музыка, сохранённая на телефоне"
                )
                MediaLibraryBrowseIds.ARTISTS -> categoryItem(
                    MediaLibraryBrowseIds.ARTISTS,
                    "Исполнители",
                    "Музыка по исполнителям"
                )
                else -> {
                    val tracks = queryLocalAudio()
                    val artist = MediaLibraryBrowseIds.artistName(mediaId)
                    if (artist != null) {
                        if (tracks.any { trackArtist(it).equals(artist, ignoreCase = true) }) {
                            artistFolderItem(artist)
                        } else {
                            null
                        }
                    } else {
                        tracks.firstOrNull { it.mediaId == mediaId }
                    }
                }
            }

            return if (item != null) {
                Futures.immediateFuture(LibraryResult.ofItem(item, null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        librarySession = MediaLibrarySession.Builder(this, player, libraryCallback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return librarySession
    }

    override fun onDestroy() {
        librarySession?.run {
            player.release()
            release()
        }
        librarySession = null
        super.onDestroy()
    }

    private fun rootItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(MediaLibraryBrowseIds.ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Elantra Diagnostic Pro")
                    .setSubtitle("Музыка на телефоне")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private fun categoryItem(mediaId: String, title: String, subtitle: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private fun artistFolderItem(artist: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(MediaLibraryBrowseIds.artistId(artist))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(artist)
                    .setArtist(artist)
                    .setSubtitle("Композиции исполнителя")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private fun artistFolders(tracks: List<MediaItem>): List<MediaItem> {
        return tracks
            .map(::trackArtist)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .map(::artistFolderItem)
    }

    private fun trackArtist(item: MediaItem): String =
        item.mediaMetadata.artist
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: UNKNOWN_ARTIST

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun queryLocalAudio(): List<MediaItem> {
        if (!hasAudioPermission()) return emptyList()

        return try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.MIME_TYPE
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val result = mutableListOf<MediaItem>()

            contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Без названия"
                    val artist = cursor.getString(artistColumn) ?: UNKNOWN_ARTIST
                    val mimeType = cursor.getString(mimeColumn)
                    val uri: Uri = ContentUris.withAppendedId(collection, id)

                    val metadata = MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()

                    val builder = MediaItem.Builder()
                        .setMediaId(uri.toString())
                        .setUri(uri)
                        .setMediaMetadata(metadata)

                    if (!mimeType.isNullOrBlank()) builder.setMimeType(mimeType)
                    result += builder.build()
                }
            }
            result
        } catch (error: SecurityException) {
            // Permission can be revoked after the initial check, while Android Auto is browsing.
            Log.w(TAG, "Audio permission is unavailable while reading MediaStore", error)
            emptyList()
        } catch (error: RuntimeException) {
            // A transient MediaStore/provider failure must not crash the car media service.
            Log.w(TAG, "Unable to read the local audio library", error)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "ElantraMediaService"
        private const val UNKNOWN_ARTIST = "Неизвестный исполнитель"
    }
}
