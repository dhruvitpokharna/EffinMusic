package code.name.monkey.retromusic.adapter.artist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.ItemArtistAlbumsBinding
import code.name.monkey.retromusic.databinding.ItemArtistBiographyBinding
import code.name.monkey.retromusic.databinding.ItemArtistHeaderBinding
import code.name.monkey.retromusic.databinding.ItemArtistSongsBinding
import code.name.monkey.retromusic.databinding.ItemArtistStatsBinding
import code.name.monkey.retromusic.fragments.artists.ArtistItem
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.artistImageOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.SingleColorTarget
import code.name.monkey.retromusic.interfaces.IAlbumClickListener
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.adapter.album.HorizontalAlbumAdapter
import code.name.monkey.retromusic.adapter.song.SimpleSongAdapter
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.extensions.*
import com.bumptech.glide.Glide


class ArtistDetailsAdapter(
    private var items: List<ArtistItem>,
    private var albumClickListener: IAlbumClickListener,
    private val onAlbumSortClicked: (View) -> Unit,
    private val onSongSortClicked: (View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ALBUMS = 1
        private const val TYPE_SONGS = 2
        private const val TYPE_BIOGRAPHY = 3
        private const val TYPE_STATS = 4
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ArtistItem.Header -> TYPE_HEADER
        is ArtistItem.Albums -> TYPE_ALBUMS
        is ArtistItem.Songs -> TYPE_SONGS
        is ArtistItem.Biography -> TYPE_BIOGRAPHY
        is ArtistItem.Stats -> TYPE_STATS
    }

    fun swapDataSet(newItems: List<ArtistItem>) {
        items = newItems
        notifyDataSetChanged()
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
            TYPE_SONGS -> SongsViewHolder(
                ItemArtistSongsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
            is ArtistItem.Songs -> (holder as SongsViewHolder).bind(item)
            is ArtistItem.Biography -> (holder as BiographyViewHolder).bind(item)
            is ArtistItem.Stats -> (holder as StatsViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(private val binding: ItemArtistHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.Header) {
            binding.artistTitle.text = item.artist.name
            binding.artistSubtitle.text = String.format(
                "%s • %s",
                MusicUtil.getArtistInfoString(binding.artistSubtitle.context, item.artist),
                MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(item.artist.songs))
            )

            loadArtistImage(item.artist)

            binding.playAction.setOnClickListener {
                MusicPlayerRemote.openQueue(item.artist.sortedSongs, 0, true)
            }
            binding.shuffleAction.setOnClickListener {
                MusicPlayerRemote.openAndShuffleQueue(item.artist.songs, true)
            }
        }

        private fun loadArtistImage(artist: Artist) {
            val glideRequest = Glide.with(binding.image.context)
                .asBitmapPalette()
                .artistImageOptions(artist)
                .error(R.drawable.ic_artist)
                .placeholder(R.drawable.ic_artist)

            binding.image?.let { imageView ->
                glideRequest.load(RetroGlideExtension.getArtistModel(artist))
                    .dontAnimate()
                    .into(object : SingleColorTarget(binding.image) {
                        override fun onColorReady(color: Int) {
                            binding.shuffleAction.applyColor(color)
                            binding.playAction.applyOutlineColor(color)
                        }
                    })
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
            binding.albumRecyclerView.itemAnimator = DefaultItemAnimator()
            binding.albumSortOrder.setOnClickListener {
                onAlbumSortClicked(it)
            }
        }
    }

    class SongsViewHolder(
        private val binding: ItemArtistSongsBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.Songs) {
            val adapter = SimpleSongAdapter(
                binding.root.context as FragmentActivity,
                ArrayList(item.songs), 
                R.layout.item_song
            )
            binding.songRecyclerView.layoutManager =
                LinearLayoutManager(binding.root.context)
            binding.songRecyclerView.adapter = adapter
            binding.songRecyclerView.itemAnimator = DefaultItemAnimator()  
            binding.songSortOrder.setOnClickListener {
                onSongSortClicked(it)
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
