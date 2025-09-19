/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package code.name.monkey.retromusic.repository

import android.content.Context
import android.content.ContentResolver
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.Audio.Playlists.*
import android.provider.MediaStore.Audio.PlaylistsColumns
import code.name.monkey.retromusic.Constants
import code.name.monkey.retromusic.db.RetroDatabase
import code.name.monkey.retromusic.db.SongMetadataEntity
import code.name.monkey.retromusic.db.SongMetadataDao
import code.name.monkey.retromusic.extensions.getInt
import code.name.monkey.retromusic.extensions.getLong
import code.name.monkey.retromusic.extensions.getString
import code.name.monkey.retromusic.extensions.getStringOrNull
import code.name.monkey.retromusic.model.Playlist
import code.name.monkey.retromusic.model.PlaylistSong
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.PreferenceUtil
import kotlinx.coroutines.runBlocking

/**
 * Created by hemanths on 16/08/17.
 */
interface PlaylistRepository {
    fun playlist(cursor: Cursor?): Playlist

    fun searchPlaylist(query: String): List<Playlist>

    fun playlist(playlistName: String): Playlist

    fun playlists(): List<Playlist>

    fun playlists(cursor: Cursor?): List<Playlist>

    fun favoritePlaylist(playlistName: String): List<Playlist>

    fun deletePlaylist(playlistId: Long)

    fun playlist(playlistId: Long): Playlist

    fun playlistSongs(playlistId: Long): List<Song>
}
@Suppress("Deprecation")
class RealPlaylistRepository(
    private val context: Context
) : PlaylistRepository {

    private val contentResolver = context.contentResolver

    private val metadataDao: SongMetadataDao = RetroDatabase.getInstance(context).songMetadataDao()

    private var metadataMap: Map<Long, SongMetadataEntity>? = null

    override fun playlist(cursor: Cursor?): Playlist {
        return cursor.use {
            if (cursor?.moveToFirst() == true) {
                getPlaylistFromCursorImpl(cursor)
            } else {
                Playlist.empty
            }
        }
    }

    override fun playlist(playlistName: String): Playlist {
        return playlist(makePlaylistCursor(PlaylistsColumns.NAME + "=?", arrayOf(playlistName)))
    }

    override fun playlist(playlistId: Long): Playlist {
        return playlist(
            makePlaylistCursor(
                BaseColumns._ID + "=?",
                arrayOf(playlistId.toString())
            )
        )
    }

    override fun searchPlaylist(query: String): List<Playlist> {
        return playlists(makePlaylistCursor(PlaylistsColumns.NAME + "=?", arrayOf(query)))
    }

    override fun playlists(): List<Playlist> {
        return playlists(makePlaylistCursor(null, null))
    }

    override fun playlists(cursor: Cursor?): List<Playlist> {
        val playlists = mutableListOf<Playlist>()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                playlists.add(getPlaylistFromCursorImpl(cursor))
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return playlists
    }

    override fun favoritePlaylist(playlistName: String): List<Playlist> {
        return playlists(
            makePlaylistCursor(
                PlaylistsColumns.NAME + "=?",
                arrayOf(playlistName)
            )
        )
    }

    override fun deletePlaylist(playlistId: Long) {
        val localUri = EXTERNAL_CONTENT_URI
        val localStringBuilder = StringBuilder()
        localStringBuilder.append("_id IN (")
        localStringBuilder.append(playlistId)
        localStringBuilder.append(")")
        contentResolver.delete(localUri, localStringBuilder.toString(), null)
    }

    private fun getPlaylistFromCursorImpl(
        cursor: Cursor
    ): Playlist {
        val id = cursor.getLong(0)
        val name = cursor.getString(1)
        return if (name != null) {
            Playlist(id, name)
        } else {
            Playlist.empty
        }
    }

    override fun playlistSongs(playlistId: Long): List<Song> {
        val songs = arrayListOf<Song>()
        if (playlistId == -1L) return songs
        val cursor = makePlaylistSongCursor(playlistId)

        if (cursor != null && cursor.moveToFirst()) {
            do {
                songs.add(getPlaylistSongFromCursorImpl(cursor, playlistId))
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return songs
    }

    private suspend fun getMetadata(id: Long): SongMetadataEntity? {
        if (metadataMap == null) {
            metadataMap = metadataDao.getAllMetadata().associateBy { it.id }
        }
        return metadataMap?.get(id)
    }

    private fun getPlaylistSongFromCursorImpl(cursor: Cursor, playlistId: Long): PlaylistSong {
        val id = cursor.getLong(Members.AUDIO_ID)

        val base = PlaylistSong(
            id = id,
            title = cursor.getString(AudioColumns.TITLE),
            trackNumber = cursor.getInt(AudioColumns.TRACK),
            year = cursor.getStringOrNull(AudioColumns.YEAR),
            duration = cursor.getLong(AudioColumns.DURATION),
            data = cursor.getString(Constants.DATA),
            dateModified = cursor.getLong(AudioColumns.DATE_MODIFIED),
            albumId = cursor.getLong(AudioColumns.ALBUM_ID),
            albumName = cursor.getString(AudioColumns.ALBUM),
            artistId = cursor.getLong(AudioColumns.ARTIST_ID),
            artistName = cursor.getString(AudioColumns.ARTIST),
            playlistId = playlistId,
            idInPlayList = cursor.getLong(Members._ID),
            composer = cursor.getStringOrNull(AudioColumns.COMPOSER),
            albumArtist = cursor.getStringOrNull("album_artist")
            )

        if (PreferenceUtil.fixYear) {
            val meta = runBlocking { getMetadata(id) }
            if (meta != null) {
                return PlaylistSong(
                    id = base.id,
                    title = meta.title ?: base.title,
                    trackNumber  = meta.trackNumber ?: base.trackNumber,
                    year         = meta.year ?: base.year,
                    duration     = meta.duration ?: base.duration,
                    data         = meta.data ?: base.data,
                    dateModified = meta.dateModified ?: base.dateModified,
                    albumId      = meta.albumId ?: base.albumId,
                    albumName    = meta.albumName ?: base.albumName,
                    artistId     = meta.artistId ?: base.artistId,
                    artistName   = meta.artistName ?: base.artistName,
                    playlistId = base.playlistId,
                    idInPlayList = base.idInPlayList,
                    composer     = meta.composer ?: base.composer,
                    albumArtist  = meta.albumArtist ?: base.albumArtist,
                    artistIds  = meta.artistIds ?: base.artistIds,
                    artistNames  = meta.artistNames ?: base.artistNames
                )
            }
        }
        return base
    }

    private fun makePlaylistCursor(
        selection: String?,
        values: Array<String>?
    ): Cursor? {
        return contentResolver.query(
            EXTERNAL_CONTENT_URI,
            arrayOf(
                BaseColumns._ID, /* 0 */
                PlaylistsColumns.NAME /* 1 */
            ),
            selection,
            values,
            DEFAULT_SORT_ORDER
        )
    }

    private fun makePlaylistSongCursor(playlistId: Long): Cursor? {
        return contentResolver.query(
            Members.getContentUri("external", playlistId),
            arrayOf(
                Members.AUDIO_ID, // 0
                AudioColumns.TITLE, // 1
                AudioColumns.TRACK, // 2
                AudioColumns.YEAR, // 3
                AudioColumns.DURATION, // 4
                Constants.DATA, // 5
                AudioColumns.DATE_MODIFIED, // 6
                AudioColumns.ALBUM_ID, // 7
                AudioColumns.ALBUM, // 8
                AudioColumns.ARTIST_ID, // 9
                AudioColumns.ARTIST, // 10
                Members._ID,//11
                AudioColumns.COMPOSER,//12
                "album_artist"//13
            ), Constants.IS_MUSIC, null, Members.DEFAULT_SORT_ORDER
        )
    }
}
