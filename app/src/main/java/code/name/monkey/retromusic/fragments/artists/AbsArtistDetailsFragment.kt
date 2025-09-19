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
import code.name.monkey.retromusic.adapter.artist.ArtistDetailsAdapter
import code.name.monkey.retromusic.databinding.FragmentArtistDetailsBinding
import code.name.monkey.retromusic.dialogs.AddToPlaylistDialog
import code.name.monkey.retromusic.extensions.*
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.artistImageOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.SingleColorTarget
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.interfaces.IAlbumClickListener
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.network.Result
import code.name.monkey.retromusic.helper.SortOrder
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
import androidx.core.view.isGone
import code.name.monkey.retromusic.extensions.surfaceColor
import android.graphics.drawable.ColorDrawable
import android.widget.Toast



abstract class AbsArtistDetailsFragment : AbsMainActivityFragment(R.layout.fragment_artist_details),
    IAlbumClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private var _binding: FragmentArtistDetailsBinding? = null
    private val binding get() = _binding!!

    abstract val detailsViewModel: ArtistDetailsViewModel
    abstract val artistId: Long?
    abstract val artistName: String?

    private lateinit var adapter: ArtistDetailsAdapter

    private var lastFm: LastFmArtist? = null

    private lateinit var artist: Artist
    private var biography: Spanned? = null
    private var lang: String? = null
    private var forceDownload: Boolean = false

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
        setupRecyclerView()
        mainActivity.addMusicServiceEventListener(detailsViewModel)
        mainActivity.setSupportActionBar(binding.toolbar)
        binding.toolbar.title = null

        postponeEnterTransition()
        
        detailsViewModel.getArtist().observe(viewLifecycleOwner) { 
            view.doOnPreDraw {
                startPostponedEnterTransition()
            }
            showArtist(it) 
        }
        binding.appBarLayout?.background = ColorDrawable(surfaceColor())
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
        if (key == PreferenceUtil.OFFLINE_MODE && ::artist.isInitialized) {
            binding.image?.let { Glide.with(this).clear(it) }
            showArtist(artist)
        }
    }

    private fun setupRecyclerView() {
        adapter = ArtistDetailsAdapter(
            requireActivity(),
            emptyList(),
            this,
            { view -> showAlbumSortPopup(view) },
            { view -> showSongSortPopup(view) },
            (artistId ?: artistName).toString()
        )
        binding.recyclerView?.apply{
            itemAnimator = DefaultItemAnimator()
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AbsArtistDetailsFragment.adapter
        }
    }

    private fun updateRecyclerView() {
        val layoutManager = binding.recyclerView?.layoutManager as? LinearLayoutManager
        val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val offset = binding.recyclerView?.getChildAt(0)?.top ?: 0

        adapter.swapDataSet(buildArtistItems())
        layoutManager?.scrollToPositionWithOffset(firstVisible, offset)
    }

    private fun buildArtistItems(): List<ArtistItem> {
        val listeners = lastFm?.artist?.stats?.listeners?.let { RetroUtil.formatValue(it.toFloat()) } ?: ""
        val scrobbles = lastFm?.artist?.stats?.playcount?.let { RetroUtil.formatValue(it.toFloat()) } ?: ""
        
        return mutableListOf<ArtistItem>().apply {
            add(ArtistItem.Header(artist))
            add(ArtistItem.Albums(artist.sortedAlbums))
            add(ArtistItem.SongsHeader("Songs"))
            artist.sortedSongs.forEach { add(ArtistItem.SongItem(it)) }
            biography?.let { add(ArtistItem.Biography(it)) }
            add(ArtistItem.Stats(listeners, scrobbles))
        }
    }

    private fun showArtist(artist: Artist) {
        this.artist = artist
        if (!PreferenceUtil.showSongOnly) {
            if (!PreferenceUtil.isOfflineMode && PreferenceUtil.isAllowedToDownloadMetadata(requireContext())) {
                loadBiography(artist.name)
            }
        }

        binding.artistTitle?.text = artist.name
        binding.text?.text = String.format(
            "%s • %s",
            MusicUtil.getArtistInfoString(requireContext(), artist),
            MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(artist.songs))
        )
        setupRecyclerView()
        updateRecyclerView()
    }

    private fun loadBiography(name: String, lang: String? = Locale.getDefault().language) {
        biography = null
        this.lang = lang
        detailsViewModel.getArtistInfo(name, lang, null).observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> artistInfo(result.data)
                else -> {}
            }
        }
    }

    private fun artistInfo(lastFmArtist: LastFmArtist?) {
        lastFm = lastFmArtist
        val bioContent = lastFmArtist?.artist?.bio?.content
        if (!bioContent.isNullOrBlank()) {
            biography = bioContent.parseAsHtml()
        }

        if (biography == null && lang != null) {
            loadBiography(artist.name, null)
        } else {
            updateRecyclerView()
        }
    }

    override fun onAlbumClick(albumId: Long, view: View) {
        findNavController().navigate(
            R.id.albumDetailsFragment,
            bundleOf(EXTRA_ALBUM_ID to albumId),
            null,
            FragmentNavigatorExtras(view to albumId.toString())
        )
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        val songs = artist.songs
        when (item.itemId) {
            android.R.id.home -> findNavController().navigateUp()
            R.id.action_play_next -> MusicPlayerRemote.playNext(songs)
            R.id.action_add_to_current_playing -> MusicPlayerRemote.enqueue(songs)
            R.id.action_add_to_playlist -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val playlists = get<RealRepository>().fetchPlaylists()
                    withContext(Dispatchers.Main) {
                        AddToPlaylistDialog.create(playlists, songs)
                            .show(childFragmentManager, "ADD_PLAYLIST")
                    }
                }
            }
            R.id.action_set_artist_image -> selectImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            R.id.action_reset_artist_image -> {
                showToast(resources.getString(R.string.updating))
                lifecycleScope.launch {
                    CustomArtistImageUtil.getInstance(requireContext())
                        .resetCustomArtistImage(artist)
                }
                forceDownload = true
            }
        }
        return true
    }

    private fun showSongSortPopup(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
            popup.inflate(R.menu.menu_artist_song_sort_order)
            setUpSortOrderMenu(popup.menu)
            popup.setOnMenuItemClickListener { item ->
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
                true
            }
            popup.show()
    }

    private fun setSaveSortOrder(sortOrder: String) {
        PreferenceUtil.artistDetailSongSortOrder = sortOrder
        updateRecyclerView()
    }

    private fun showAlbumSortPopup(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
            popup.inflate(R.menu.menu_artist_album_sort_order)
            setUpAlbumSortOrderMenu(popup.menu)
            popup.setOnMenuItemClickListener { item ->
                val sortOrder = when (item.itemId) {
                    R.id.action_sort_order_title -> SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z
                    R.id.action_sort_order_title_desc -> SortOrder.ArtistAlbumSortOrder.ALBUM_Z_A
                    R.id.action_sort_order_year -> SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR_ASC
                    R.id.action_sort_order_year_desc -> SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR
                    else -> throw IllegalArgumentException("invalid ${item.title}")
                }
                item.isChecked = true
                setSaveAlbumSortOrder(sortOrder)
                true
            }
            popup.show()
    }

    private fun setSaveAlbumSortOrder(sortOrder: String) {
        PreferenceUtil.artistAlbumSortOrder = sortOrder
        updateRecyclerView()
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
                uri?.let { CustomArtistImageUtil.getInstance(requireContext()).setCustomArtistImage(artist, it) }
            }
        }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_artist_detail, menu)
    }
}
