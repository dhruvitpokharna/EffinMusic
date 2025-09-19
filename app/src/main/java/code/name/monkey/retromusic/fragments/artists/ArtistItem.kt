package code.name.monkey.retromusic.fragments.artists

import android.text.Spanned
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Song

sealed class ArtistItem {
    data class Header(val artist: Artist) : ArtistItem()
    data class Albums(val albums: List<Album>) : ArtistItem()
    data class SongsHeader(val title: String) : ArtistItem()
    data class SongItem(val song: Song) : ArtistItem()
    data class Biography(val text: Spanned) : ArtistItem()
    data class Stats(val listeners: String, val scrobbles: String) : ArtistItem()
}
