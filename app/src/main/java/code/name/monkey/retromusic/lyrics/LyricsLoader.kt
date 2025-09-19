package code.name.monkey.retromusic.lyrics

import code.name.monkey.retromusic.model.Song

object LyricsLoader {

        suspend fun loadLyrics(song: Song, preferSynced: Boolean = true): String? {
            val result = LyricsFetcher.fetchLyrics(
                title = song.title,
                artist = song.albumArtist ?: song.artistName,
                album = song.albumName,
                durationMs = song.duration
            )

            val lyrics = if (preferSynced) {
                    result?.syncedLyrics?.takeIf { it.isNotBlank() } 
                        ?: result?.plainLyrics?.takeIf { it.isNotBlank() }
            } else {
                    result?.plainLyrics?.takeIf { it.isNotBlank() } 
                        ?: result?.syncedLyrics?.takeIf { it.isNotBlank() }
            }
            if (!lyrics.isNullOrBlank()) return lyrics
                
            return null
        }
}
