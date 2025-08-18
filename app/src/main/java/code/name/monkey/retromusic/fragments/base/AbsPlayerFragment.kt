/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.fragments.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.EXTRA_ALBUM_ID
import code.name.monkey.retromusic.EXTRA_ARTIST_ID
import code.name.monkey.retromusic.EXTRA_ARTIST_NAME
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.MainActivity
import code.name.monkey.retromusic.activities.tageditor.AbsTagEditorActivity
import code.name.monkey.retromusic.activities.tageditor.SongTagEditorActivity
import code.name.monkey.retromusic.db.PlaylistEntity
import code.name.monkey.retromusic.db.toSongEntity
import code.name.monkey.retromusic.dialogs.*
import code.name.monkey.retromusic.extensions.*
import code.name.monkey.retromusic.fragments.LibraryViewModel
import code.name.monkey.retromusic.fragments.NowPlayingScreen
import code.name.monkey.retromusic.fragments.ReloadType
import code.name.monkey.retromusic.fragments.player.PlayerAlbumCoverFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.interfaces.IPaletteColorHolder
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.repository.RealRepository
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.util.NavigationUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.PreferenceUtil.HIDE_ALL_ACTION_BUTTONS
import code.name.monkey.retromusic.util.PreferenceUtil.SHOW_FAVORITE_BUTTON
import code.name.monkey.retromusic.util.PreferenceUtil.SHOW_LYRICS_BUTTON
import code.name.monkey.retromusic.util.PreferenceUtil.SHOW_NOW_PLAYING_QUEUE_BUTTON
import code.name.monkey.retromusic.util.PreferenceUtil.SHOW_OPTIONS_MENU
import code.name.monkey.retromusic.util.PreferenceUtil.SHOW_SLEEP_TIMER_BUTTON
import code.name.monkey.retromusic.util.RingtoneManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.math.abs
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.TextView

abstract class AbsPlayerFragment(@LayoutRes layout: Int) : AbsMusicServiceFragment(layout),
    Toolbar.OnMenuItemClickListener, IPaletteColorHolder, PlayerAlbumCoverFragment.Callbacks,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var sharedPreferences: SharedPreferences

    val libraryViewModel: LibraryViewModel by activityViewModel()

    val mainActivity: MainActivity
        get() = activity as MainActivity

    private var playerAlbumCoverFragment: PlayerAlbumCoverFragment? = null

    override fun onMenuItemClick(
        item: MenuItem,
    ): Boolean {
        val song = MusicPlayerRemote.currentSong
        when (item.itemId) {
            R.id.action_playback_speed -> {
                PlaybackSpeedDialog.newInstance().show(childFragmentManager, "PLAYBACK_SETTINGS")
                return true
            }

            R.id.action_toggle_lyrics -> {
                PreferenceUtil.showLyrics = !PreferenceUtil.showLyrics
                showLyricsIcon(item)
                if (PreferenceUtil.isScreenOnEnabled ||
                        (PreferenceUtil.showLyrics && PreferenceUtil.lyricsScreenOn)
                   ) {
                    mainActivity.keepScreenOn(true)
                } else {
                    mainActivity.keepScreenOn(false)
                }
                return true
            }



            R.id.action_toggle_favorite -> {
                toggleFavorite(song)
                return true
            }

            R.id.action_share -> {
                SongShareDialog.create(song).show(childFragmentManager, "SHARE_SONG")
                return true
            }

            R.id.action_go_to_drive_mode -> {
                NavigationUtil.gotoDriveMode(requireActivity())
                return true
            }

            R.id.action_delete_from_device -> {
                DeleteSongsDialog.create(song).show(childFragmentManager, "DELETE_SONGS")
                return true
            }

            R.id.action_add_to_playlist -> {
                lifecycleScope.launch(IO) {
                    val playlists = get<RealRepository>().fetchPlaylists()
                    withContext(Main) {
                        AddToPlaylistDialog.create(playlists, song)
                            .show(childFragmentManager, "ADD_PLAYLIST")
                    }
                }
                return true
            }

            R.id.action_clear_playing_queue -> {
                MusicPlayerRemote.clearQueue()
                return true
            }

            R.id.action_save_playing_queue -> {
                CreatePlaylistDialog.create(ArrayList(MusicPlayerRemote.playingQueue))
                    .show(childFragmentManager, "ADD_TO_PLAYLIST")
                return true
            }

            R.id.action_tag_editor -> {
                val intent = Intent(activity, SongTagEditorActivity::class.java)
                intent.putExtra(AbsTagEditorActivity.EXTRA_ID, song.id)
                startActivity(intent)
                return true
            }

            R.id.action_details -> {
                SongDetailDialog.create(song).show(childFragmentManager, "SONG_DETAIL")
                return true
            }

            R.id.action_go_to_album -> {
                //Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
                mainActivity.setBottomNavVisibility(false)
                mainActivity.collapsePanel()
                requireActivity().findNavController(R.id.fragment_container).navigate(
                    R.id.albumDetailsFragment,
                    bundleOf(EXTRA_ALBUM_ID to song.albumId)
                )
                return true
            }

            R.id.action_go_to_artist -> {
                goToArtist(requireActivity(), MusicPlayerRemote.currentSong.artistName, MusicPlayerRemote.currentSong.artistId)
                return true
            }

            R.id.now_playing -> {
                requireActivity().findNavController(R.id.fragment_container).navigate(
                    R.id.playing_queue_fragment,
                    null,
                    navOptions { launchSingleTop = true }
                )
                mainActivity.collapsePanel()
                return true
            }

            R.id.action_show_lyrics -> {
                goToLyrics(requireActivity())
                return true
            }

            R.id.action_equalizer -> {
                mainActivity.setBottomNavVisibility(false)
                mainActivity.collapsePanel()
                NavigationUtil.openEqualizer(requireActivity())
                return true
            }

            R.id.action_sleep_timer -> {
                SleepTimerDialog().show(parentFragmentManager, "SLEEP_TIMER")
                return true
            }

            R.id.action_set_as_ringtone -> {
                requireContext().run {
                    if (RingtoneManager.requiresDialog(this)) {
                        RingtoneManager.showDialog(this)
                    } else {
                        RingtoneManager.setRingtone(this, song)
                    }
                }

                return true
            }

            R.id.action_go_to_genre -> {
                val retriever = MediaMetadataRetriever()
                val trackUri =
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        song.id
                    )
                retriever.setDataSource(activity, trackUri)
                var genre: String? =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                if (genre == null) {
                    genre = "Not Specified"
                }
                showToast(genre)
                return true
            }

            R.id.action_settings -> {
                mainActivity.setBottomNavVisibility(false)
                mainActivity.collapsePanel()
                requireActivity().findNavController(R.id.fragment_container).navigate(
                    R.id.settings_fragment,
                    null,
                    navOptions { launchSingleTop = true }
                )
                return true
            }
        }
        return false
    }

    private fun showLyricsIcon(item: MenuItem) {
        val icon =
            if (PreferenceUtil.showLyrics) R.drawable.ic_lyrics else R.drawable.ic_lyrics_outline
        val drawable = requireContext().getTintedDrawable(
            icon,
            toolbarIconColor()
        )
        item.isChecked = PreferenceUtil.showLyrics
        item.icon = drawable
    }

    abstract fun playerToolbar(): Toolbar?

    abstract fun onShow()

    abstract fun onHide(): Unit // Added Unit explicitly for clarity, might not be necessary

    abstract fun toolbarIconColor(): Int

    override fun onServiceConnected() {
        updateIsFavorite()
    }

    override fun onPlayingMetaChanged() {
        updateIsFavorite()
    }

    override fun onFavoriteStateChanged() {
        updateIsFavorite(animate = true)
    }

    protected open fun toggleFavorite(song: Song) {
        lifecycleScope.launch(IO) {
            val playlist: PlaylistEntity = libraryViewModel.favoritePlaylist()
            val songEntity = song.toSongEntity(playlist.playListId)
            val isFavorite = libraryViewModel.isSongFavorite(song.id)
            if (isFavorite) {
                libraryViewModel.removeSongFromPlaylist(songEntity)
            } else {
                libraryViewModel.insertSongs(listOf(song.toSongEntity(playlist.playListId)))
            }
            libraryViewModel.forceReload(ReloadType.Playlists)
            LocalBroadcastManager.getInstance(requireContext())
                .sendBroadcast(Intent(MusicService.FAVORITE_STATE_CHANGED))
        }
    }

    fun setupTitleAndArtistClicks(
        titleView: TextView,
        artistView: TextView,
        individualArtists: List<String>
    ) {
        titleView.setOnClickListener {
            if (PreferenceUtil.tapOnTitle) {
                goToAlbum(requireActivity())
            }
        }

        artistView.setOnClickListener {
            if (PreferenceUtil.tapOnArtist) {
                if (individualArtists.size > 1) {
                    val displayList = mutableListOf<String>()
                    displayList.add("Album Artist")
                    displayList.add(individualArtists[0])
                    displayList.add("Contributing Artist")
                    displayList.add(individualArtists[1])
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.select_artist)
                        .setItems(displayList.toTypedArray()) { _, which ->
                            val selectedArtistName = displayList[which]
                            if (selectedArtistName == "Album Artist" || selectedArtistName == "Contributing Artist") return@setItems
                            lifecycleScope.launch(Dispatchers.IO) {
                                val albumArtists = libraryViewModel.albumArtists.value
                                val contributingArtists = libraryViewModel.contributingArtists.value
                                if (which == 1) {
                                    val selectedArtist = albumArtists?.find {
                                        it.name.equals(selectedArtistName, ignoreCase = true)
                                    }
                                }
                                if (which == 3) {
                                    val selectedArtist = contributingArtists?.find {
                                        it.name.equals(selectedArtistName, ignoreCase = true)
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    if (selectedArtist != null) {
                                        if (which == 1) {
                                            goToAlbumArtist(requireActivity(), selectedArtist.name)
                                        }
                                        if (which == 3) {
                                            goToArtist(requireActivity(), selectedArtist.name, selectedArtist.id)
                                        }
                                    } else {
                                        context?.showToast("Artist not found: $selectedArtistName")
                                    }
                                }
                            }
                        }
                        .show()
                } else {
                    val song = MusicPlayerRemote.currentSong
                    val artistName = song.artistName
                    val artistId = song.artistId

                    lifecycleScope.launch(Dispatchers.IO) {
                        val allArtists = libraryViewModel.artists.value
                        val artist = allArtists?.find {
                            it.name.equals(artistName, ignoreCase = true)
                        }
                        withContext(Dispatchers.Main) {
                            if (artist != null) {
                                goToArtist(requireActivity(), artist.name, artist.id)
                            } else {
                                context?.showToast("Artist not found: $artistName")
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateIsFavorite(animate: Boolean = false) {
        lifecycleScope.launch(IO) {
            val isFavorite: Boolean =
                libraryViewModel.isSongFavorite(MusicPlayerRemote.currentSong.id)
            withContext(Main) {
                val icon = if (animate && VersionUtils.hasMarshmallow()) {
                    if (isFavorite) R.drawable.avd_favorite else R.drawable.avd_unfavorite
                } else {
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                }
                val drawable = requireContext().getTintedDrawable(
                    icon,
                    toolbarIconColor()
                )
                if (playerToolbar() != null) {
                    playerToolbar()?.menu?.findItem(R.id.action_toggle_favorite)?.apply {
                        setIcon(drawable)
                        title =
                            if (isFavorite) getString(R.string.action_remove_from_favorites)
                            else getString(R.string.action_add_to_favorites)
                        getIcon().also {
                            if (it is AnimatedVectorDrawable) {
                                it.start()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PreferenceUtil.circlePlayButton) {
            requireContext().theme.applyStyle(R.style.CircleFABOverlay, true)
        } else {
            requireContext().theme.applyStyle(R.style.RoundedFABOverlay, true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (PreferenceUtil.isFullScreenMode &&
            view.findViewById<View>(R.id.status_bar) != null
        ) {
            view.findViewById<View>(R.id.status_bar).isVisible = false
        }
        playerAlbumCoverFragment = whichFragment(R.id.playerAlbumCoverFragment)
        playerAlbumCoverFragment?.setCallbacks(this)

        if (VersionUtils.hasMarshmallow())
            view.findViewById<RelativeLayout>(R.id.statusBarShadow)?.hide()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updateMenuVisibility()
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            HIDE_ALL_ACTION_BUTTONS,
            SHOW_SLEEP_TIMER_BUTTON,
            SHOW_LYRICS_BUTTON,
            SHOW_FAVORITE_BUTTON,
            SHOW_NOW_PLAYING_QUEUE_BUTTON,
            SHOW_OPTIONS_MENU -> updateMenuVisibility()
        }
    }

    private fun updateMenuVisibility() {
        val toolbar = playerToolbar()
        val menu = toolbar?.menu
        if (menu == null) return

        val hideAll = PreferenceUtil.hideAllActionButtons
        val showSleepTimer = PreferenceUtil.showSleepTimerButton
        val showLyricsPref = PreferenceUtil.showLyricsButton
        val showFavorite = PreferenceUtil.showFavoriteButton
        val showQueueButton = PreferenceUtil.showNowPlayingQueueButton
        val showOptionsMenuPref = PreferenceUtil.showOptionsMenu

        // Control visibility of individual action buttons based on preferences and hideAllActionButtons
        menu.findItem(R.id.action_sleep_timer)?.isVisible = !hideAll && showSleepTimer
        menu.findItem(R.id.action_toggle_favorite)?.isVisible = !hideAll && showFavorite
        menu.findItem(R.id.now_playing)?.isVisible = !hideAll && showQueueButton

        // Control lyrics menu item visibility by preference and player style
        val nps = PreferenceUtil.nowPlayingScreen
        if (nps == NowPlayingScreen.Circle || nps == NowPlayingScreen.Peek || nps == NowPlayingScreen.Tiny) {
             menu.findItem(R.id.action_toggle_lyrics)?.isVisible = false
        } else {
             menu.findItem(R.id.action_toggle_lyrics)?.isVisible = !hideAll && showLyricsPref
        }

        // Control visibility of the overall options menu (3 dots icon)
        // Iterate through all menu items. If showOptionsMenuPref is false, hide items that are not explicitly controlled
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            when (item.itemId) {
                R.id.action_sleep_timer,
                R.id.action_toggle_lyrics,
                R.id.action_toggle_favorite,
                R.id.now_playing -> {
                    // These are already handled above, do nothing here
                }
                else -> {
                    // Hide other menu items if showOptionsMenuPref is false
                    item.isVisible = showOptionsMenuPref
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        addSwipeDetector()
    }

    fun addSwipeDetector() {
        view?.setOnTouchListener(
            if (PreferenceUtil.swipeAnywhereToChangeSong) {
                SwipeDetector(
                    requireContext(),
                    playerAlbumCoverFragment?.viewPager,
                    requireView()
                )
            } else null
        )
    }

    class SwipeDetector(val context: Context, val viewPager: ViewPager?, val view: View) :
        View.OnTouchListener {
        private var flingPlayBackController: GestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float,
                ): Boolean {
                    return when {
                        abs(distanceX) > abs(distanceY) -> {
                            // Disallow Intercept Touch Event so that parent(BottomSheet) doesn't consume the events
                            view.parent.requestDisallowInterceptTouchEvent(true)
                            true
                        }

                        else -> {
                            false
                        }
                    }
                }
            })

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            viewPager?.dispatchTouchEvent(event)
            return flingPlayBackController.onTouchEvent(event)
        }
    }

    companion object {
        val TAG: String = AbsPlayerFragment::class.java.simpleName
        const val VISIBILITY_ANIM_DURATION: Long = 300
    }
}

fun goToAlbumArtist(activity: Activity, artistName: String) {
    if (activity !is MainActivity) return
    activity.apply {
        currentFragment(R.id.fragment_container)?.exitTransition = 
        setBottomNavVisibility(false)
        if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
        }
        findNavController(R.id.fragment_container).navigate(
            R.id.albumArtistDetailsFragment,
            bundleOf(EXTRA_ARTIST_NAME to artistName)
        )
    }
}

fun goToArtist(activity: Activity, artistName: String, artistId: Long) {
    if (activity !is MainActivity) return
    activity.apply {

        // Remove exit transition of current fragment so
        // it doesn't exit with a weird transition
        currentFragment(R.id.fragment_container)?.exitTransition = null

        //Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
        setBottomNavVisibility(false)
        if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
        }

        findNavController(R.id.fragment_container).navigate(
            R.id.artistDetailsFragment,
            bundleOf(EXTRA_ARTIST_ID to artistId)
        )
    }
}

fun goToAlbum(activity: Activity) {
    if (activity !is MainActivity) return
    val song = MusicPlayerRemote.currentSong
    activity.apply {
        currentFragment(R.id.fragment_container)?.exitTransition = null

        //Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
        setBottomNavVisibility(false)
        if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
        }

        findNavController(R.id.fragment_container).navigate(
            R.id.albumDetailsFragment,
            bundleOf(EXTRA_ALBUM_ID to song.albumId)
        )
    }
}

fun goToLyrics(activity: Activity) {
    if (activity !is MainActivity) return
    activity.apply {
        //Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
        setBottomNavVisibility(false)
        if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
        }

        findNavController(R.id.fragment_container).navigate(
            R.id.lyrics_fragment,
            null,
            navOptions { launchSingleTop = true }
        )
    }
}
