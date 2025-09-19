package code.name.monkey.retromusic.adapter.artist

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.base.AbsMultiSelectAdapter
import code.name.monkey.retromusic.databinding.ItemArtistAlbumsBinding
import code.name.monkey.retromusic.databinding.ItemArtistBiographyBinding
import code.name.monkey.retromusic.databinding.ItemArtistHeaderBinding
import code.name.monkey.retromusic.databinding.ItemArtistSongsHeaderBinding
import code.name.monkey.retromusic.databinding.ItemArtistSongBinding
import code.name.monkey.retromusic.databinding.ItemArtistStatsBinding
import code.name.monkey.retromusic.fragments.artists.ArtistItem
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.artistImageOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.SingleColorTarget
import code.name.monkey.retromusic.helper.menu.SongMenuHelper
import code.name.monkey.retromusic.helper.menu.SongsMenuHelper
import code.name.monkey.retromusic.interfaces.IAlbumClickListener
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import android.util.TypedValue
import code.name.monkey.retromusic.adapter.album.HorizontalAlbumAdapter
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.extensions.*
import coil.load
import com.bumptech.glide.Glide


class ArtistDetailsAdapter(
    override val activity: FragmentActivity,
    private var items: List<ArtistItem>,
    private var albumClickListener: IAlbumClickListener,
    private val onAlbumSortClicked: (View) -> Unit,
    private val onSongSortClicked: (View) -> Unit,
    private val transitionName: String
) : AbsMultiSelectAdapter<RecyclerView.ViewHolder, Song> (
    activity,
    R.menu.menu_media_selection
) {

    init {
        setHasStableIds(true) 
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ALBUMS = 1
        private const val TYPE_SONGS_HEADER = 2
        private const val TYPE_SONG = 3
        private const val TYPE_BIOGRAPHY = 4
        private const val TYPE_STATS = 5
    }

    override fun getItemId(position: Int): Long {
        return when (val item = items[position]) {
            is ArtistItem.SongItem -> item.song.id 
            else -> RecyclerView.NO_ID
        }
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ArtistItem.Header -> TYPE_HEADER
        is ArtistItem.Albums -> TYPE_ALBUMS
        is ArtistItem.SongsHeader -> TYPE_SONGS_HEADER
        is ArtistItem.SongItem -> TYPE_SONG
        is ArtistItem.Biography -> TYPE_BIOGRAPHY
        is ArtistItem.Stats -> TYPE_STATS
    }

    fun swapDataSet(newItems: List<ArtistItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getIdentifier(position: Int): Song? {
        val item = items.getOrNull(position)
        return if (item is ArtistItem.SongItem) item.song else null
    }

    override fun getName(model: Song): String {
        return model.title
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        SongsMenuHelper.handleMenuClick(activity, selection, menuItem.itemId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemArtistHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false), 
                transitionName
            )
            TYPE_ALBUMS -> AlbumsViewHolder(
                ItemArtistAlbumsBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                albumClickListener, onAlbumSortClicked
            )
            TYPE_SONGS_HEADER -> SongsHeaderViewHolder(
                ItemArtistSongsHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onSongSortClicked
            )
            TYPE_SONG -> SongViewHolder(
                ItemArtistSongBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                activity
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
            is ArtistItem.SongsHeader -> (holder as SongsHeaderViewHolder).bind(item)
            is ArtistItem.SongItem -> (holder as SongViewHolder).bind(item)
            is ArtistItem.Biography -> (holder as BiographyViewHolder).bind(item)
            is ArtistItem.Stats -> (holder as StatsViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(
        private val binding: ItemArtistHeaderBinding,
        private val transitionName: String) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.Header) {
            binding.artistCoverContainer.transitionName = transitionName
            binding.artistTitle.text = item.artist.name 
            binding.artistSubtitle.text = String.format(
                "%s • %s",
                MusicUtil.getArtistInfoString(binding.artistSubtitle.context, item.artist),
                MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(item.artist.songs))
            )

            if (!PreferenceUtil.showSongOnly) {
                loadArtistImage(item.artist)
            } else {
                binding.artistCoverContainer.isVisible = false
            }

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
        private val listener: IAlbumClickListener,
        private val onAlbumSortClicked: (View) -> Unit
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
            binding.albumRecyclerView.itemAnimator = null
            binding.albumRecyclerView.setHasFixedSize(true)
            binding.albumSortOrder.setOnClickListener {
                onAlbumSortClicked(it)
            }
        }
    }

    class SongsHeaderViewHolder(
        private val binding: ItemArtistSongsHeaderBinding,
        private val onSongSortClicked: (View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.SongsHeader) {
            binding.songSortOrder.setOnClickListener { onSongSortClicked(it) }
        }
    }

    inner class SongViewHolder(
        private val binding: ItemArtistSongBinding,
        private val activity: FragmentActivity,
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val songMenuRes = SongMenuHelper.MENU_RES
        private lateinit var song: Song
        
        fun bind(item: ArtistItem.SongItem) {
            song = item.song
            binding.songTitle.text = song.title
            val songTextSize = PreferenceUtil.songTextSize.toFloat()
            binding.songTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, songTextSize)
            
            if (PreferenceUtil.showArtistInSongs) {
                val fixedTrackNumber = MusicUtil.getFixedTrackNumber(song.trackNumber)
                binding.songDuration.text = String.format("%s | %s", fixedTrackNumber, MusicUtil.getReadableDurationString(song.duration))
                val allArtists = if (!PreferenceUtil.fixYear) {
                    listOfNotNull(song.albumArtist, song.artistName)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                } else {
                    song.artistNames?.split(",")?.map { it.trim() } ?: emptyList()
                }
                binding.artist.text = allArtists.joinToString(", ")
                val artistTextSize = PreferenceUtil.artistTextSize.toFloat()
                binding.artist.setTextSize(TypedValue.COMPLEX_UNIT_SP, artistTextSize)
            } else {
                binding.artist.isVisible = false
            }

            loadSongImage(song)
            updateSelectionState()
            
            binding.root.setOnClickListener {
                if (isInQuickSelectMode) {
                    toggleChecked(layoutPosition)
                    updateSelectionState()
                } else {
                    val songs = items.filterIsInstance<ArtistItem.SongItem>().map { it.song }
                    val index = songs.indexOf(song)
                    MusicPlayerRemote.openQueueKeepShuffleMode(songs, index, true)
                }
            }
            binding.root.setOnLongClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    toggleChecked(pos)
                    updateSelectionState()
                    true
                } else false
            }
            binding.menu.setOnClickListener(object : SongMenuHelper.OnClickSongMenu(activity) {
                override val song: Song
                    get() = this@SongViewHolder.song

                override val menuRes: Int
                    get() = this@SongViewHolder.songMenuRes

                override fun onMenuItemClick(item: MenuItem): Boolean {
                    return super.onMenuItemClick(item)
                }
            })
        }

        private fun updateSelectionState() {
            val checked = isChecked(song)
            binding.root.alpha = if (checked) 0.5f else 1f
            binding.root.isActivated = checked
            binding.menu.isVisible = !checked
        }

        private fun loadSongImage(song: Song) {
            val model = RetroGlideExtension.getSongModel(song)
            
            binding.image.load(model) {
                size(250, 250)
                crossfade(true)
                allowHardware(true)
                placeholder(R.drawable.default_audio_art)
                error(R.drawable.default_audio_art)
                memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                diskCachePolicy(coil.request.CachePolicy.ENABLED)
            }
        }
    }

    class BiographyViewHolder(private val binding: ItemArtistBiographyBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.Biography) { 
            if ((!PreferenceUtil.showSongOnly) || (!PreferenceUtil.isOfflineMode)) {
                binding.biographyTitle.isVisible = true
                binding.biographyText.isVisible = true
                binding.biographyText.text = item.text
            }
        }
    }

    class StatsViewHolder(private val binding: ItemArtistStatsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArtistItem.Stats) {
            val showStats = !(PreferenceUtil.showSongOnly || PreferenceUtil.isOfflineMode)
            binding.listenersLabel.isVisible = showStats
            binding.scrobblesLabel.isVisible = showStats
            binding.listeners.isVisible = showStats
            binding.scrobbles.isVisible = showStats
            if (showStats) {
                binding.listeners.text = item.listeners
                binding.scrobbles.text = item.scrobbles
            } 
        }
    }
}
