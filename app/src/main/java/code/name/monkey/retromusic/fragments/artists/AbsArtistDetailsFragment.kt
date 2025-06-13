package code.name.monkey.retromusic.fragments.artists

import android.graphics.Color
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.EXTRA_ALBUM_ID
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.album.HorizontalAlbumAdapter
import code.name.monkey.retromusic.adapter.song.SimpleSongAdapter
import code.name.monkey.retromusic.databinding.FragmentArtistDetailsBinding
import code.name.monkey.retromusic.dialogs.AddToPlaylistDialog
import code.name.monkey.retromusic.extensions.*
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.artistImageOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.SingleColorTarget
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.interfaces.IAlbumClickListener
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.network.Result
import code.name.monkey.retromusic.network.model.LastFmArtist
import code.name.monkey.retromusic.repository.RealRepository
import code.name.monkey.retromusic.util.*
import com.bumptech.glide.Glide
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import java.util.*
import android.content.SharedPreferences

abstract class AbsArtistDetailsFragment : AbsMainActivityFragment(R.layout.fragment_artist_details),
    IAlbumClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private var _binding: FragmentArtistDetailsBinding? = null
    private val binding get() = _binding!!

    abstract val detailsViewModel: ArtistDetailsViewModel
    abstract val artistId: Long?
    abstract val artistName: String?
    private lateinit var artist: Artist
    private lateinit var songAdapter: SimpleSongAdapter
    private lateinit var albumAdapter: HorizontalAlbumAdapter
    private var forceDownload: Boolean = false
    private var lang: String? = null
    private var biography: Spanned? = null

    private val savedSongSortOrder: String
        get() = PreferenceUtil.artistDetailSongSortOrder
    private val savedAlbumSortOrder: String
        get() = PreferenceUtil.artistAlbumSortOrder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(surfaceColor())
        }
        PreferenceUtil.registerOnSharedPreferenceChangedListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity.addMusicServiceEventListener(detailsViewModel)
        mainActivity.setSupportActionBar(binding.toolbar)
        binding.toolbar.title = null
        binding.artistCoverContainer.transitionName = (artistId ?: artistName).toString()
        postponeEnterTransition()
        detailsViewModel.getArtist().observe(viewLifecycleOwner) {
            view.doOnPreDraw {
                startPostponedEnterTransition()
            }
            showArtist(it)
        }
        setupRecyclerView()

        binding.fragmentArtistContent.playAction.apply {
            setOnClickListener { MusicPlayerRemote.openQueue(artist.sortedSongs, 0, true) }
        }
        binding.fragmentArtistContent.shuffleAction.apply {
            setOnClickListener { MusicPlayerRemote.openAndShuffleQueue(artist.songs, true) }
        }

        binding.fragmentArtistContent.biographyText.setOnClickListener {
            if (binding.fragmentArtistContent.biographyText.maxLines == 4) {
                binding.fragmentArtistContent.biographyText.maxLines = Integer.MAX_VALUE
            } else {
                binding.fragmentArtistContent.biographyText.maxLines = 4
            }
        }
        setupSongSortButton()
        setupAlbumSortButton()
        binding.appBarLayout?.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceUtil.unregisterOnSharedPreferenceChangedListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferenceUtil.OFFLINE_MODE -> {
                if (::artist.isInitialized) {
                    // Clear the current artist image to force a reload
                    Glide.with(this).clear(binding.image)
                    showArtist(artist)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        albumAdapter = HorizontalAlbumAdapter(requireActivity(), ArrayList(), this, true)
        binding.fragmentArtistContent.albumRecyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            layoutManager = GridLayoutManager(this.context, 1, GridLayoutManager.HORIZONTAL, false)
            adapter = albumAdapter
        }
        songAdapter = SimpleSongAdapter(requireActivity(), ArrayList(), R.layout.item_song)
        binding.fragmentArtistContent.recyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            layoutManager = LinearLayoutManager(this.context)
            adapter = songAdapter
        }
    }

    private fun showArtist(artist: Artist) {
        this.artist = artist
        loadArtistImage(artist)
        if ((!PreferenceUtil.isOfflineMode) && PreferenceUtil.isAllowedToDownloadMetadata(requireContext())) {
            loadBiography(artist.name)
            binding.fragmentArtistContent.biographyText.isVisible = true
            binding.fragmentArtistContent.biographyTitle.isVisible = true
            binding.fragmentArtistContent.listeners.isVisible = true
            binding.fragmentArtistContent.listenersLabel.isVisible = true
            binding.fragmentArtistContent.scrobbles.isVisible = true
            binding.fragmentArtistContent.scrobblesLabel.isVisible = true

        } else {
            binding.fragmentArtistContent.biographyText.isVisible = false
            binding.fragmentArtistContent.biographyTitle.isVisible = false
             binding.fragmentArtistContent.listeners.isVisible = false
             binding.fragmentArtistContent.listenersLabel.isVisible = false
             binding.fragmentArtistContent.scrobbles.isVisible = false
             binding.fragmentArtistContent.scrobblesLabel.isVisible = false
        }
        binding.artistTitle.text = artist.name
        binding.text.text = String.format(
            "%s • %s",
            MusicUtil.getArtistInfoString(requireContext(), artist),
            MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(artist.songs))
        )
        val songText = resources.getQuantityString(
            R.plurals.albumSongs, artist.songCount, artist.songCount
        )
        val albumText = resources.getQuantityString(
            R.plurals.albums, artist.songCount, artist.songCount
        )
        binding.fragmentArtistContent.songTitle.text = songText
        binding.fragmentArtistContent.albumTitle.text = albumText
        songAdapter.swapDataSet(artist.sortedSongs)
        albumAdapter.swapDataSet(artist.albums)
    }

    private fun loadBiography(
        name: String,
        lang: String? = Locale.getDefault().language,
    ) {
        biography = null
        this.lang = lang
        detailsViewModel.getArtistInfo(name, lang, null).observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> logD("Loading")
                is Result.Error -> logE("Error")
                is Result.Success -> artistInfo(result.data)
            }
        }
    }

    private fun artistInfo(lastFmArtist: LastFmArtist?) {
        if (lastFmArtist != null && lastFmArtist.artist != null && lastFmArtist.artist.bio != null) {
            val bioContent = lastFmArtist.artist.bio.content
            if (bioContent != null && bioContent.trim { it <= ' ' }.isNotEmpty()) {
                binding.fragmentArtistContent.run {
                    biographyText.isVisible = true
                    biographyTitle.isVisible = true
                    biography = bioContent.parseAsHtml()
                    biographyText.text = biography
                    if (lastFmArtist.artist.stats.listeners.isNotEmpty()) {
                        listeners.show()
                        listenersLabel.show()
                        scrobbles.show()
                        scrobblesLabel.show()
                        listeners.text =
                            RetroUtil.formatValue(lastFmArtist.artist.stats.listeners.toFloat())
                        scrobbles.text =
                            RetroUtil.formatValue(lastFmArtist.artist.stats.playcount.toFloat())
                    }
                }
            }
        }

        // If the "lang" parameter is set and no biography is given, retry with default language
        if (biography == null && lang != null) {
            loadBiography(artist.name, null)
        }
    }


    private fun loadArtistImage(artist: Artist) {
        val glideRequest = Glide.with(requireContext())
            .asBitmapPalette()
            .artistImageOptions(artist)
            .error(R.drawable.ic_artist) // Use existing ic_artist as a default error image
            .placeholder(R.drawable.ic_artist) // Use existing ic_artist as a placeholder image

        if PreferenceUtil.isOfflineMode {
            glideRequest.load(RetroGlideExtension.getArtistModel(artist))
                .dontAnimate()
                .skipMemoryCache(false) // Allow caching of missing images/placeholders
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC) // Use default caching strategy
        } else {
            glideRequest.load(RetroGlideExtension.getArtistModel(artist))
                .dontAnimate()
        }
        glideRequest.into(object : SingleColorTarget(binding.image) {
            override fun onColorReady(color: Int) {
                setColors(color)
            }
        })
    }

    private fun setColors(color: Int) {
        if (_binding != null) {
            binding.fragmentArtistContent.shuffleAction.applyColor(color)
            binding.fragmentArtistContent.playAction.applyOutlineColor(color)
        }
    }

    override fun onAlbumClick(albumId: Long, view: View) {
        findNavController().navigate(
            R.id.albumDetailsFragment,
            bundleOf(EXTRA_ALBUM_ID to albumId),
            null,
            FragmentNavigatorExtras(
                view to albumId.toString()
            )
        )
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return handleSortOrderMenuItem(item)
    }

    private fun handleSortOrderMenuItem(item: MenuItem): Boolean {
        val songs = artist.songs
        when (item.itemId) {
            android.R.id.home -> findNavController().navigateUp()
            R.id.action_play_next -> {
                MusicPlayerRemote.playNext(songs)
                return true
            }

            R.id.action_add_to_current_playing -> {
                MusicPlayerRemote.enqueue(songs)
                return true
            }

            R.id.action_add_to_playlist -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val playlists = get<RealRepository>().fetchPlaylists()
                    withContext(Dispatchers.Main) {
                        AddToPlaylistDialog.create(playlists, songs)
                            .show(childFragmentManager, "ADD_PLAYLIST")
                    }
                }
                return true
            }

            R.id.action_set_artist_image -> {
                selectImageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                return true
            }

            R.id.action_reset_artist_image -> {
                showToast(resources.getString(R.string.updating))
                lifecycleScope.launch {
                    CustomArtistImageUtil.getInstance(requireContext())
                        .resetCustomArtistImage(artist)
                }
                forceDownload = true
                return true
            }
        }
        return true
    }

    private fun setupSongSortButton() {
        binding.fragmentArtistContent.songSortOrder.setOnClickListener {
            PopupMenu(requireContext(), binding.fragmentArtistContent.songSortOrder).apply {
                inflate(R.menu.menu_artist_song_sort_order)
                setUpSortOrderMenu(menu)
                setOnMenuItemClickListener { item ->
                    val sortOrder = when (item.itemId) {
                        R.id.action_sort_order_title -> SortOrder.ArtistSongSortOrder.SONG_A_Z
                        R.id.action_sort_order_title_desc -> SortOrder.ArtistSongSortOrder.SONG_Z_A
                        R.id.action_sort_order_album -> SortOrder.ArtistSongSortOrder.SONG_ALBUM
                        R.id.action_sort_order_year -> SortOrder.ArtistSongSortOrder.SONG_YEAR
                        R.id.action_sort_order_song_duration -> SortOrder.ArtistSongSortOrder.SONG_DURATION
                        else -> {
                            throw IllegalArgumentException("invalid ${item.title}")
                        }
                    }
                    item.isChecked = true
                    setSaveSortOrder(sortOrder)
                    return@setOnMenuItemClickListener true
                }
                show()
            }
        }
    }

    private fun setSaveSortOrder(sortOrder: String) {
        PreferenceUtil.artistDetailSongSortOrder = sortOrder
        songAdapter.swapDataSet(artist.sortedSongs)
    }

    private fun setupAlbumSortButton() {
        binding.fragmentArtistContent.albumSortOrder.setOnClickListener {
            PopupMenu(requireContext(), binding.fragmentArtistContent.albumSortOrder).apply {
                inflate(R.menu.menu_artist_album_sort_order)
                setUpAlbumSortOrderMenu(menu)
                setOnMenuItemClickListener { item ->
                    val sortOrder = when (item.itemId) {
                        R.id.action_sort_order_title -> SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z
                        R.id.action_sort_order_title_desc -> SortOrder.ArtistAlbumSortOrder.ALBUM_Z_A
                        R.id.action_sort_order_year -> SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR_ASC
                        R.id.action_sort_order_year_desc -> SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR
                        else -> {
                            throw IllegalArgumentException("invalid ${item.title}")
                        }
                    }
                    item.isChecked = true
                    setSaveAlbumSortOrder(sortOrder)
                    return@setOnMenuItemClickListener true
                }
                show()
            }
        }
    }

    private fun setSaveAlbumSortOrder(sortOrder: String) {
        PreferenceUtil.artistAlbumSortOrder = sortOrder
        albumAdapter.swapDataSet(artist.sortedAlbums)
    }

    private fun setUpAlbumSortOrderMenu(sortOrder: Menu) {
        when (savedAlbumSortOrder) {
            SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z -> sortOrder.findItem(R.id.action_sort_order_title).isChecked =
                true

            SortOrder.ArtistAlbumSortOrder.ALBUM_Z_A -> sortOrder.findItem(R.id.action_sort_order_title_desc).isChecked =
                true

            SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR_ASC -> sortOrder.findItem(R.id.action_sort_order_year).isChecked =
                true

            SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR -> sortOrder.findItem(R.id.action_sort_order_year_desc).isChecked =
                true

            else -> {
                throw IllegalArgumentException("invalid $savedAlbumSortOrder")
            }
        }
    }

    private fun setUpSortOrderMenu(sortOrder: Menu) {
        when (savedSongSortOrder) {
            SortOrder.ArtistSongSortOrder.SONG_A_Z -> sortOrder.findItem(R.id.action_sort_order_title).isChecked =
                true

            SortOrder.ArtistSongSortOrder.SONG_Z_A -> sortOrder.findItem(R.id.action_sort_order_title_desc).isChecked =
                true

            SortOrder.ArtistSongSortOrder.SONG_ALBUM -> sortOrder.findItem(R.id.action_sort_order_album).isChecked =
                true

            SortOrder.ArtistSongSortOrder.SONG_YEAR -> sortOrder.findItem(R.id.action_sort_order_year).isChecked =
                true

            SortOrder.ArtistSongSortOrder.SONG_DURATION -> sortOrder.findItem(R.id.action_sort_order_song_duration).isChecked =
                true

            else -> {
                throw IllegalArgumentException("invalid $savedSongSortOrder")
            }
        }
    }

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            lifecycleScope.launch {
                if (uri != null) {
                    CustomArtistImageUtil.getInstance(requireContext())
                        .setCustomArtistImage(artist, uri)
                }
            }
        }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_artist_detail, menu)
    }
}
