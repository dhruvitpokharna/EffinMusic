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
package code.name.monkey.retromusic.adapter.album

import android.app.AlertDialog
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import code.name.monkey.retromusic.fragments.lyrics.LyricsFragment
import androidx.lifecycle.lifecycleScope
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.MainActivity
import code.name.monkey.retromusic.db.PlaylistEntity
import code.name.monkey.retromusic.db.toSongEntity
import code.name.monkey.retromusic.extensions.currentFragment
import code.name.monkey.retromusic.fragments.AlbumCoverStyle
import code.name.monkey.retromusic.fragments.LibraryViewModel
import code.name.monkey.retromusic.fragments.NowPlayingScreen.*
import code.name.monkey.retromusic.fragments.base.goToLyrics
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.glide.RetroMusicColoredTarget
import code.name.monkey.retromusic.misc.CustomFragmentStatePagerAdapter
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.lyrics.LyricsLoader
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import android.net.Uri
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import code.name.monkey.retromusic.glide.palette.BitmapPaletteWrapper
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AlbumCoverPagerAdapter(
    fragmentManager: FragmentManager,
    private val dataSet: List<Song>
) : CustomFragmentStatePagerAdapter(fragmentManager) {

    private var currentColorReceiver: AlbumCoverFragment.ColorReceiver? = null
    private var currentColorReceiverPosition = -1

    val libraryViewModel: LibraryViewModel by activityViewModel()

    override fun getItem(position: Int): Fragment {
        return AlbumCoverFragment.newInstance(dataSet[position])
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val o = super.instantiateItem(container, position)
        if (currentColorReceiver != null && currentColorReceiverPosition == position) {
            receiveColor(currentColorReceiver!!, currentColorReceiverPosition)
        }
        return o
    }

    /**
     * Only the latest passed [AlbumCoverFragment.ColorReceiver] is guaranteed to receive a
     * response
     */
    fun receiveColor(colorReceiver: AlbumCoverFragment.ColorReceiver, position: Int) {

        if (getFragment(position) is AlbumCoverFragment) {
            val fragment = getFragment(position) as AlbumCoverFragment
            currentColorReceiver = null
            currentColorReceiverPosition = -1
            fragment.receiveColor(colorReceiver, position)
        } else {
            currentColorReceiver = colorReceiver
            currentColorReceiverPosition = position
        }
    }

    class AlbumCoverFragment : Fragment() {

        private var isColorReady: Boolean = false
        private lateinit var color: MediaNotificationProcessor
        private lateinit var song: Song
        private var colorReceiver: ColorReceiver? = null
        private var request: Int = 0
        private val mainActivity get() = activity as MainActivity

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            if (arguments != null) {
                song = BundleCompat.getParcelable(requireArguments(), SONG_ARG, Song::class.java)!!
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(getLayoutWithPlayerTheme(), container, false)
            val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (mainActivity.getBottomSheetBehavior().state == STATE_EXPANDED) {
                        when (PreferenceUtil.artworkClickAction) {
                            0 -> showLyricsDialog()
                            1 -> { /* Do nothing */ }
                            2 -> {
                                if (MusicPlayerRemote.isPlaying) {
                                    MusicPlayerRemote.pauseSong()
                                } else {
                                    MusicPlayerRemote.resumePlaying()
                                }
                            }
                        }
                    }
                    return true
                }
                
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val song = MusicPlayerRemote.currentSong
                    val playlist: PlaylistEntity = libraryViewModel.favoritePlaylist()
                    if (!libraryViewModel.isSongFavorite(song.id)) {
                        libraryViewModel.insertSongs(listOf(song.toSongEntity(playlist.playListId)))
                        Toast.makeText(requireContext(), "Added to Favorites", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Already in Favorites", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
            })

            view.setOnTouchListener { _, motionEvent ->
                gestureDetector.onTouchEvent(motionEvent)
                true
            }
            return view
        }
        
        private fun showLyricsDialog(lyrics: String? = null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val data = lyrics ?: MusicUtil.getLyrics(song)
                withContext(Dispatchers.Main) {
                    val dialog = MaterialAlertDialogBuilder(
                        requireContext(),
                        com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog_Alert
                    )
                        .setTitle(song.title)
                        .setMessage(if (data.isNullOrEmpty()) "No lyrics found" else data)
                        .setNeutralButton(R.string.fetch_lyrics_online, null)
                        .setPositiveButton(R.string.synced_lyrics, null)
                        .create()

                        dialog.setOnShowListener {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                                dialog.dismiss()
                                goToLyrics(requireActivity())
                            }
                        
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                            lifecycleScope.launch {
                                val fetchedLyrics = LyricsLoader.loadLyrics(song, preferSynced = false)
                                if (!fetchedLyrics.isNullOrBlank()) {
                                    dialog.dismiss()
                                    showLyricsDialog(fetchedLyrics)
                                } else {
                                    Toast.makeText(requireContext(), R.string.no_lyrics_found, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    dialog.show()
                }
            }
        }

        private fun getLayoutWithPlayerTheme(): Int {
            return when (PreferenceUtil.nowPlayingScreen) {
                Card, Fit, Tiny, Classic, Gradient, Full -> R.layout.fragment_album_full_cover
                Peek -> R.layout.fragment_peek_album_cover
                else -> {
                    if (PreferenceUtil.isCarouselEffect) {
                        R.layout.fragment_album_carousel_cover
                    } else {
                        when (PreferenceUtil.albumCoverStyle) {
                            AlbumCoverStyle.Normal -> R.layout.fragment_album_cover
                            AlbumCoverStyle.Flat -> R.layout.fragment_album_flat_cover
                            AlbumCoverStyle.Circle -> R.layout.fragment_album_circle_cover
                            AlbumCoverStyle.Card -> R.layout.fragment_album_card_cover
                            AlbumCoverStyle.Full -> R.layout.fragment_album_full_cover
                            AlbumCoverStyle.FullCard -> R.layout.fragment_album_full_card_cover
                        }
                    }
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            loadAlbumCover(albumCover = view.findViewById(R.id.player_image))
        }

        override fun onDestroyView() {
            super.onDestroyView()
            colorReceiver = null
        }

        private fun loadAlbumCover(albumCover: ImageView) {
            val primaryRequest = Glide.with(this)
                .asBitmapPalette()
                .songCoverOptions(song)
                .load(RetroGlideExtension.getSongModel(song))
                .dontAnimate()

            val customArtworkUri = PreferenceUtil.customFallbackArtworkUri
            if (!customArtworkUri.isNullOrEmpty()) {
                val fallbackRequest: RequestBuilder<BitmapPaletteWrapper> = Glide.with(this)
                    .asBitmapPalette()
                    .songCoverOptions(song)
                    .load(Uri.parse(customArtworkUri))
                    .dontAnimate()

                primaryRequest.error(fallbackRequest)
            }

            primaryRequest.into(object : RetroMusicColoredTarget(albumCover) {
                override fun onColorReady(colors: MediaNotificationProcessor) {
                    setColor(colors)
                }
            })
        }

        private fun setColor(color: MediaNotificationProcessor) {
            this.color = color
            isColorReady = true
            if (colorReceiver != null) {
                colorReceiver!!.onColorReady(color, request)
                colorReceiver = null
            }
        }

        internal fun receiveColor(colorReceiver: ColorReceiver, request: Int) {
            if (isColorReady) {
                colorReceiver.onColorReady(color, request)
            } else {
                this.colorReceiver = colorReceiver
                this.request = request
            }
        }

        interface ColorReceiver {
            fun onColorReady(color: MediaNotificationProcessor, request: Int)
        }

        companion object {

            private const val SONG_ARG = "song"

            fun newInstance(song: Song): AlbumCoverFragment {
                val frag = AlbumCoverFragment()
                frag.arguments = bundleOf(SONG_ARG to song)
                return frag
            }
        }
    }

    companion object {
        val TAG: String = AlbumCoverPagerAdapter::class.java.simpleName
    }
}
