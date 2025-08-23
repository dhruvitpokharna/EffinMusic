package code.name.monkey.retromusic.adapter.artist

import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.album.HorizontalAlbumAdapter
import code.name.monkey.retromusic.extensions.inflate
import code.name.monkey.retromusic.interfaces.IAlbumClickListener
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Song
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import androidx.appcompat.widget.AppCompatImageView

sealed class ArtistItem {
    data class Header(val artist: Artist) : ArtistItem()
    data class Albums(val albums: List<Album>) : ArtistItem()
    data class SongItem(val song: Song) : ArtistItem()
    data class Biography(val text: Spanned) : ArtistItem()
    data class Stats(val listeners: String, val scrobbles: String) : ArtistItem()
}

class ArtistDetailsAdapter(
    private val items: List<ArtistItem>,
    private val albumClickListener: IAlbumClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ALBUMS = 1
        private const val TYPE_SONG = 2
        private const val TYPE_BIOGRAPHY = 3
        private const val TYPE_STATS = 4
    }

    override fun getItemViewType(position: Int): Int = when(items[position]) {
        is ArtistItem.Header -> TYPE_HEADER
        is ArtistItem.Albums -> TYPE_ALBUMS
        is ArtistItem.SongItem -> TYPE_SONG
        is ArtistItem.Biography -> TYPE_BIOGRAPHY
        is ArtistItem.Stats -> TYPE_STATS
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when(viewType) {
            TYPE_HEADER -> HeaderViewHolder(parent.inflate(R.layout.item_artist_header))
            TYPE_ALBUMS -> AlbumsViewHolder(parent.inflate(R.layout.item_artist_albums), albumClickListener)
            TYPE_SONG -> SongViewHolder(parent.inflate(R.layout.item_song))
            TYPE_BIOGRAPHY -> BiographyViewHolder(parent.inflate(R.layout.item_artist_biography))
            TYPE_STATS -> StatsViewHolder(parent.inflate(R.layout.item_artist_stats))
            else -> throw IllegalArgumentException("Invalid view type")
        }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(val item = items[position]) {
            is ArtistItem.Header -> (holder as HeaderViewHolder).bind(item.artist)
            is ArtistItem.Albums -> (holder as AlbumsViewHolder).bind(item.albums)
            is ArtistItem.SongItem -> (holder as SongViewHolder).bind(item.song)
            is ArtistItem.Biography -> (holder as BiographyViewHolder).bind(item.text)
            is ArtistItem.Stats -> (holder as StatsViewHolder).bind(item.listeners, item.scrobbles)
        }
    }

    // ---------------- ViewHolders ----------------

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val artistImage: AppCompatImageView = view.findViewById(R.id.image)
        private val artistName: MaterialTextView = view.findViewById(R.id.artistTitle)
        private val artistInfo: MaterialTextView = view.findViewById(R.id.text)

        fun bind(artist: Artist) {
            artistName.text = artist.name
            artistInfo.text = "${artist.songCount} songs • ${artist.albumCount} albums"
            // Load image via Glide as before
        }
    }

    class AlbumsViewHolder(view: View, private val listener: IAlbumClickListener) : RecyclerView.ViewHolder(view) {
        private val recycler: RecyclerView = view.findViewById(R.id.albumRecyclerView)
        private val adapter = HorizontalAlbumAdapter(view.context, emptyList(), listener, true)

        init { recycler.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
               recycler.adapter = adapter
        }

        fun bind(albums: List<Album>) { adapter.swapDataSet(albums) }
    }

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val songTitle: MaterialTextView = view.findViewById(R.id.songTitle)
        fun bind(song: Song) { songTitle.text = song.title }
    }

    class BiographyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val bioText: MaterialTextView = view.findViewById(R.id.biographyText)
        fun bind(text: Spanned) { bioText.text = text }
    }

    class StatsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val listeners: MaterialTextView = view.findViewById(R.id.listeners)
        private val scrobbles: MaterialTextView = view.findViewById(R.id.scrobbles)
        fun bind(listenersValue: String, scrobblesValue: String) {
            listeners.text = listenersValue
            scrobbles.text = scrobblesValue
        }
    }
}
