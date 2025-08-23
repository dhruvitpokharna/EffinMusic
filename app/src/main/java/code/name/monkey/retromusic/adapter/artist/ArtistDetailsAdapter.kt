package code.name.monkey.retromusic.adapter.artist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.ItemArtistAlbumsBinding
import code.name.monkey.retromusic.databinding.ItemArtistBiographyBinding
import code.name.monkey.retromusic.databinding.ItemArtistHeaderBinding
import code.name.monkey.retromusic.databinding.ItemArtistSongBinding
import code.name.monkey.retromusic.databinding.ItemArtistStatsBinding
import code.name.monkey.retromusic.fragments.artists.ArtistItem
import code.name.monkey.retromusic.interfaces.IAlbumClickListener
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.adapter.album.HorizontalAlbumAdapter
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song

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

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ArtistItem.Header -> TYPE_HEADER
        is ArtistItem.Albums -> TYPE_ALBUMS
        is ArtistItem.SongItem -> TYPE_SONG
        is ArtistItem.Biography -> TYPE_BIOGRAPHY
        is ArtistItem.Stats -> TYPE_STATS
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemArtistHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            TYPE_ALBUMS -> AlbumsViewHolder(
                ItemArtistAlbumsBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                albumClickListener
            )
            TYPE_SONG -> SongViewHolder(
                ItemArtistSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            TYPE_BIOGRAPHY -> BiographyViewHolder(
                ItemArtistBiographyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            TYPE_STATS -> StatsViewHolder(
                ItemArtistStatsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type")
        }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ArtistItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ArtistItem.Albums -> (holder as AlbumsViewHolder).bind(item)
            is ArtistItem.SongItem -> (holder as SongViewHolder).bind(item)
            is ArtistItem.Biography -> (holder as BiographyViewHolder).bind(item)
            is ArtistItem.Stats -> (holder as StatsViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(private val binding: ItemArtistHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.Header) {
            binding.artistTitle.text = item.artist.name
            binding.artistSubtitle.text = "${item.artist.songCount} songs • ${item.artist.albumCount} albums"

            binding.playButton.setOnClickListener {
                MusicPlayerRemote.openQueue(item.artist.sortedSongs, 0, true)
            }
            binding.shuffleButton.setOnClickListener {
                MusicPlayerRemote.openAndShuffleQueue(item.artist.songs, true)
            }
        }
    }

    class AlbumsViewHolder(
        private val binding: ItemArtistAlbumsBinding,
        private val listener: IAlbumClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.Albums) {
            val adapter = HorizontalAlbumAdapter(
                binding.root.context as FragmentActivity,
                item.albums,
                listener,
                showCovers = true
            )
            binding.albumRecyclerView.layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.albumRecyclerView.adapter = adapter
        }
    }

    class SongViewHolder(private val binding: ItemArtistSongBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.SongItem) {
            binding.songTitle.text = item.song.title
            binding.songDuration.text = MusicUtil.getReadableDurationString(item.song.duration)
            binding.root.setOnClickListener {
                MusicPlayerRemote.openQueue(listOf(item.song), 0, true)
            }
        }
    }

    class BiographyViewHolder(private val binding: ItemArtistBiographyBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.Biography) {
            binding.biographyText.text = item.text
        }
    }

    class StatsViewHolder(private val binding: ItemArtistStatsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.Stats) {
            binding.listeners.text = item.listeners
            binding.scrobbles.text = item.scrobbles
        }
    }
}
