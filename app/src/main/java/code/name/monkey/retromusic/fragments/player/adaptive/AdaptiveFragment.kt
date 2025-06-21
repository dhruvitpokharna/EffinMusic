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
package code.name.monkey.retromusic.fragments.player.adaptive

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentAdaptivePlayerBinding
import code.name.monkey.retromusic.extensions.*
import code.name.monkey.retromusic.fragments.base.AbsPlayerFragment
import code.name.monkey.retromusic.fragments.base.goToAlbum
import code.name.monkey.retromusic.fragments.base.goToArtist
import code.name.monkey.retromusic.fragments.player.PlayerAlbumCoverFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import android.text.TextUtils
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class AdaptiveFragment : AbsPlayerFragment(R.layout.fragment_adaptive_player) {

    private var _binding: FragmentAdaptivePlayerBinding? = null
    private val binding get() = _binding!!

    private var individualArtists: List<String> = emptyList()

    private var lastColor: Int = 0
    private lateinit var playbackControlsFragment: AdaptivePlaybackControlsFragment

    override fun playerToolbar(): Toolbar {
        return binding.playerToolbar
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdaptivePlayerBinding.bind(view)
        setUpSubFragments()
        setUpPlayerToolbar()
        binding.playbackControlsFragment.drawAboveSystemBars()
    }

    private fun setUpSubFragments() {
        playbackControlsFragment =
            whichFragment(R.id.playbackControlsFragment) as AdaptivePlaybackControlsFragment
        val playerAlbumCoverFragment =
            whichFragment(R.id.playerAlbumCoverFragment) as PlayerAlbumCoverFragment
        playerAlbumCoverFragment.apply {
            removeSlideEffect()
            setCallbacks(this@AdaptiveFragment)
        }
    }

    private fun setUpPlayerToolbar() {
        binding.playerToolbar.apply {
            inflateMenu(R.menu.menu_player)
            setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
            ToolbarContentTintHelper.colorizeToolbar(this, surfaceColor(), requireActivity())
            setTitleTextColor(textColorPrimary())
            setSubtitleTextColor(textColorSecondary())
            setOnMenuItemClickListener(this@AdaptiveFragment)
        }
    }

    private fun applyMarqueeToToolbarTitle() {
        _binding?.playerToolbar?.postDelayed({
            val binding = _binding ?: return@postDelayed
            for (i in 0 until binding.playerToolbar.childCount) {
                val view = binding.playerToolbar.getChildAt(i)
                when {
                    view is TextView && view.text == binding.playerToolbar.title -> {
                        view.apply {
                            ellipsize = TextUtils.TruncateAt.MARQUEE
                            isSingleLine = true
                            marqueeRepeatLimit = -1
                            isSelected = true
                            setHorizontallyScrolling(true)
                            isFocusable = true
                            isFocusableInTouchMode = true
                            requestFocus()
                            setOnClickListener {
                                if (PreferenceUtil.tapOnTitle) {
                                    goToAlbum(requireActivity())
                                }
                            }
                        }
                    }

                    view is TextView && view.text == binding.playerToolbar.subtitle -> {
                        view.setOnClickListener {
                            if (PreferenceUtil.tapOnArtist) {
                                if (individualArtists.size > 1) {
                                    MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.select_artist)
                                        .setItems(individualArtists.toTypedArray()) { _, which ->
                                            val selectedArtistName = individualArtists[which]
                                            lifecycleScope.launch {
                                                val allArtists = withContext(Dispatchers.IO) {
                                                    libraryViewModel.artists.value
                                                }
                                                val selectedArtist = allArtists?.find {
                                                    it.name.equals(selectedArtistName, ignoreCase = true)
                                                }
                                                if (selectedArtist != null) {
                                                    goToArtist(
                                                        requireActivity(),
                                                        selectedArtist.name,
                                                        selectedArtist.id
                                                    )
                                                } else {
                                                    context?.showToast("Artist not found: $selectedArtistName")
                                                }
                                            }
                                        }
                                        .show()
                                } else {
                                    val song = MusicPlayerRemote.currentSong
                                    val artistName = song.artistName
                                    lifecycleScope.launch {
                                        val allArtists = withContext(Dispatchers.IO) {
                                            libraryViewModel.artists.value
                                        }
                                        val artist = allArtists?.find {
                                            it.name.equals(artistName, ignoreCase = true)
                                        }
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
            }
        }, 300)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateIsFavorite()
        updateSong()
    }

    override fun onPlayingMetaChanged() {
        updateIsFavorite()
        updateSong()
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong
        binding.playerToolbar.title = song.title

        val artistName = song.artistName?.trim()
        val delimiters = PreferenceUtil.artistDelimiters
        
        val allArtists: List<String> = (song.allArtists?.split(",") ?: emptyList<String>())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            
        individualArtists = if (delimiters.isBlank()) {
            allArtists
        } else {
            val splitNames = allArtists
                .flatMap { artist ->
                    artist.split(*(
                            delimiters.split(",")
                            .map { it.trim() }
                            .map { if (it.isEmpty()) "," else it }
                            .distinct()
                            .toTypedArray()
                    )).map { it.trim() }
                }
                .filter { it.isNotEmpty() }
                .distinct()
            (allArtists + splitNames)
                .filter { it.isNotEmpty() }
                .distinct()
        }
        
        // Always display the full artist name string
        binding.playerToolbar.subtitle = song.allArtists
        applyMarqueeToToolbarTitle()
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        if (song.id == MusicPlayerRemote.currentSong.id) {
            updateIsFavorite()
        }
    }

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.currentSong)
    }

    override fun onColorChanged(color: MediaNotificationProcessor) {
        playbackControlsFragment.setColor(color)
        lastColor = color.primaryTextColor
        libraryViewModel.updateColor(color.primaryTextColor)
        ToolbarContentTintHelper.colorizeToolbar(
            binding.playerToolbar,
            colorControlNormal(),
            requireActivity()
        )
    }

    override fun onShow() {
    }

    override fun onHide() {
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun toolbarIconColor(): Int {
        return colorControlNormal()
    }

    override val paletteColor: Int
        get() = lastColor
}
