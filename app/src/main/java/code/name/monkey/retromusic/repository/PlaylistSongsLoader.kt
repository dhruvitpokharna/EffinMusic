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
import android.database.Cursor
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.Audio.Playlists.Members
import code.name.monkey.retromusic.Constants
import code.name.monkey.retromusic.Constants.IS_MUSIC
import code.name.monkey.retromusic.db.RetroDatabase
import code.name.monkey.retromusic.db.SongMetadataEntity
import code.name.monkey.retromusic.db.SongMetadataDao
import code.name.monkey.retromusic.extensions.getInt
import code.name.monkey.retromusic.extensions.getLong
import code.name.monkey.retromusic.extensions.getString
import code.name.monkey.retromusic.extensions.getStringOrNull
import code.name.monkey.retromusic.model.PlaylistSong
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.PreferenceUtil
import kotlinx.coroutines.runBlocking

/**
 * Created by hemanths on 16/08/17.
 */
@Suppress("Deprecation")
object PlaylistSongsLoader {

    private lateinit var metadataDao: SongMetadataDao
    
    private fun getMetadataDao(context: Context): SongMetadataDao {
        if (!::metadataDao.isInitialized) {
            metadataDao = RetroDatabase.getInstance(context).songMetadataDao()
        }
        return metadataDao
    }

    private var metadataMap: Map<Long, SongMetadataEntity>? = null

    @JvmStatic
    fun getPlaylistSongList(context: Context, playlistId: Long): List<Song> {
        val songs = mutableListOf<Song>()
        val cursor =
            makePlaylistSongCursor(
                context,
                playlistId
            )

        if (cursor != null && cursor.moveToFirst()) {
            do {
                songs.add(
                    getPlaylistSongFromCursorImpl(
                        cursor,
                        playlistId,
                        context
                    )
                )
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

    // TODO duplicated in [PlaylistRepository.kt]
    private fun getPlaylistSongFromCursorImpl(cursor: Cursor, playlistId: Long, context: Context): PlaylistSong {
        val dao = getMetadataDao(context)
        
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

    private fun makePlaylistSongCursor(context: Context, playlistId: Long): Cursor? {
        try {
            return context.contentResolver.query(
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
                ), IS_MUSIC, null, Members.DEFAULT_SORT_ORDER
            )
        } catch (e: SecurityException) {
            return null
        }
    }
}
