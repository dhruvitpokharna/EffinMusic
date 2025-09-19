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
package code.name.monkey.retromusic.fragments.other

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentMiniPlayerBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.show
import code.name.monkey.retromusic.extensions.textColorPrimary
import code.name.monkey.retromusic.extensions.textColorSecondary
import code.name.monkey.retromusic.fragments.base.AbsMusicServiceFragment
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.helper.PlayPauseButtonOnClickHandler
import android.content.SharedPreferences
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.PreferenceUtil.MINI_PLAYER_TIME
import code.name.monkey.retromusic.util.PreferenceUtil.MINI_PLAYER_TIME_DISABLED
import code.name.monkey.retromusic.util.PreferenceUtil.MINI_PLAYER_TIME_REMAINING
import code.name.monkey.retromusic.util.PreferenceUtil.MINI_PLAYER_TIME_TOTAL
import code.name.monkey.retromusic.util.PreferenceUtil.MINI_PLAYER_TIME_ELAPSED
import android.graphics.drawable.Drawable
import android.net.Uri
import code.name.monkey.retromusic.util.RetroUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import code.name.monkey.retromusic.MINI_PLAYER_SCROLLING
import kotlin.math.abs

open class MiniPlayerFragment : AbsMusicServiceFragment(R.layout.fragment_mini_player),
    MusicProgressViewUpdateHelper.Callback, View.OnClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.actionNext -> MusicPlayerRemote.playNextSong()
            R.id.actionPrevious -> MusicPlayerRemote.back()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMiniPlayerBinding.bind(view)
        view.setOnTouchListener(FlingPlayBackController(requireContext()))
        setUpMiniPlayer()
        setUpButtons()
    }

    fun setUpButtons() {
        if (RetroUtil.isTablet) {
            binding.actionNext.show()
            binding.actionPrevious.show()
        } else {
            binding.actionNext.isVisible = PreferenceUtil.isExtraControls
            binding.actionPrevious.isVisible = PreferenceUtil.isExtraControls
        }
        binding.actionNext.setOnClickListener(this)
        binding.actionPrevious.setOnClickListener(this)
    }

    private fun setUpMiniPlayer() {
        setUpPlayPauseButton()
        binding.progressBar.accentColor()
    }

    private fun setUpPlayPauseButton() {
        binding.miniPlayerPlayPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
    }

    private fun updateSongTitle() {

        val song = MusicPlayerRemote.currentSong

        val builder = SpannableStringBuilder()

        val title = song.title.toSpannable()
        title.setSpan(ForegroundColorSpan(textColorPrimary()), 0, title.length, 0)

        val allArtists = if (!PreferenceUtil.fixYear) {
            listOfNotNull(song.albumArtist, song.artistName)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        } else {
            song.artistNames?.split(",")?.map { it.trim() } ?: emptyList()
        }

        val text = allArtists.joinToString(", ").toSpannable()
        text.setSpan(ForegroundColorSpan(textColorSecondary()), 0, text.length, 0)

        builder.append(title).append(" • ").append(text)

        binding.miniPlayerTitle.isSelected = PreferenceUtil.miniPlayerScrolling
        binding.miniPlayerTitle.text = builder

//        binding.title.isSelected = true
//        binding.title.text = song.title
//        binding.text.isSelected = true
//        binding.text.text = song.artistName
    }

    private fun updateSongCover() {
        val song = MusicPlayerRemote.currentSong
        val primaryRequest = Glide.with(requireContext())
            .load(RetroGlideExtension.getSongModel(song))
            .transition(RetroGlideExtension.getDefaultTransition())
            .songCoverOptions(song)

        val customArtworkUri = PreferenceUtil.customFallbackArtworkUri
        if (!customArtworkUri.isNullOrEmpty()) {
            val fallbackRequest: RequestBuilder<Drawable> = Glide.with(requireContext())
                .load(Uri.parse(customArtworkUri))
                .transition(RetroGlideExtension.getDefaultTransition())
                .songCoverOptions(song)

            primaryRequest.error(fallbackRequest)
        }

        primaryRequest.into(binding.image)
    }

    override fun onServiceConnected() {
        updateSongTitle()
        updateSongCover()
        updatePlayPauseDrawableState()
    }

    override fun onPlayingMetaChanged() {
        updateSongTitle()
        updateSongCover()
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        binding.progressBar.max = total
        binding.progressBar.progress = progress

        // Update time display based on preference, prioritizing progress helper for remaining time
        when (PreferenceUtil.miniPlayerTime) {
            MINI_PLAYER_TIME_REMAINING -> {
                val remaining = total - progress
                binding.miniPlayerTime.text = RetroUtil.getFormattedDuration(remaining.toLong())
                binding.miniPlayerTime.isVisible = true
            }
            MINI_PLAYER_TIME_ELAPSED -> {
                binding.miniPlayerTime.text = RetroUtil.getFormattedDuration(progress.toLong())
                binding.miniPlayerTime.isVisible = true
            }
            MINI_PLAYER_TIME_DISABLED -> {
                binding.miniPlayerTime.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
        PreferenceUtil.registerOnSharedPreferenceChangedListener(this)
        updateTime() // Ensure time is updated when fragment resumes based on current preference
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
        PreferenceUtil.unregisterOnSharedPreferenceChangedListener(this)
    }

    protected fun updatePlayPauseDrawableState() {
        if (MusicPlayerRemote.isPlaying) {
            binding.miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    class FlingPlayBackController(context: Context) : View.OnTouchListener {

        private var flingPlayBackController = GestureDetector(context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (abs(velocityX) > abs(velocityY)) {
                        if (velocityX < 0) {
                            MusicPlayerRemote.playNextSong()
                            return true
                        } else if (velocityX > 0) {
                            MusicPlayerRemote.playPreviousSong()
                            return true
                        }
                    }
                    return false
                }
            })

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return flingPlayBackController.onTouchEvent(event)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == MINI_PLAYER_SCROLLING) {
            updateSongTitle()
        }
        if (key == MINI_PLAYER_TIME) {
            // When preference changes, update the time display based on the new preference
            updateTime()
            // The onUpdateProgressViews will handle time updates if needed
        }
    }

    private fun updateTime() {
        // This function is mainly used for initial setup or when preference changes
        // During playback, onUpdateProgressViews provides accurate progress updates
        val currentSong = MusicPlayerRemote.currentSong
        val time = when (PreferenceUtil.miniPlayerTime) {
            MINI_PLAYER_TIME_TOTAL -> {
                currentSong.duration
            }
            MINI_PLAYER_TIME_REMAINING -> {
                 // Fallback calculation if onUpdateProgressViews hasn't happened yet
                 currentSong.duration - MusicPlayerRemote.position
            }
             MINI_PLAYER_TIME_ELAPSED -> {
                 // Fallback calculation if onUpdateProgressViews hasn't happened yet
                 MusicPlayerRemote.position
            }
            else -> {
                0
            }
        }
        val timeVisible = PreferenceUtil.miniPlayerTime != MINI_PLAYER_TIME_DISABLED
        
        binding.miniPlayerTime.visibility = if (timeVisible) View.VISIBLE else View.GONE
        
        val timeLayoutParams = binding.miniPlayerTime.layoutParams
        timeLayoutParams.width = if (timeVisible)
            resources.getDimensionPixelSize(R.dimen.fixed_time_width)
        else
            0
        binding.miniPlayerTime.layoutParams = timeLayoutParams
        
        if (timeVisible) {
            binding.miniPlayerTime.text = RetroUtil.getFormattedDuration(time.toLong())
        }
    }
}
