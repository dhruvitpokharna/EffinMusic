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

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.whichFragment
import code.name.monkey.retromusic.fragments.MusicSeekSkipTouchListener
import code.name.monkey.retromusic.fragments.other.VolumeFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.PreferenceUtil.TIME_DISPLAY_MODE_REMAINING
import code.name.monkey.retromusic.util.PreferenceUtil.TIME_DISPLAY_MODE_TOGGLE
import code.name.monkey.retromusic.util.PreferenceUtil.TIME_DISPLAY_MODE_TOTAL
import code.name.monkey.retromusic.util.PreferenceUtil.TIME_DISPLAY_MODE
import code.name.monkey.retromusic.util.PreferenceUtil.IS_SQUIGGLY
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import code.name.monkey.retromusic.views.SquigglyProgress
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.android.material.slider.Slider
import code.name.monkey.retromusic.SWAP_SHUFFLE_REPEAT_BUTTONS


/**
 * Created by hemanths on 24/09/17.
 */

abstract class AbsPlayerControlsFragment(@LayoutRes layout: Int) : AbsMusicServiceFragment(layout),
    MusicProgressViewUpdateHelper.Callback, SharedPreferences.OnSharedPreferenceChangeListener {

    protected abstract fun show()

    protected abstract fun hide()

    abstract fun setColor(color: MediaNotificationProcessor)

    var lastPlaybackControlsColor: Int = 0

    var lastDisabledPlaybackControlsColor: Int = 0

    private var isSeeking = false

    open val progressSlider: Slider? = null
        
    protected lateinit var squiggly: SquigglyProgress

    open val seekBar: SeekBar? = null

    abstract val shuffleButton: ImageButton

    abstract val repeatButton: ImageButton

    open val nextButton: ImageButton? = null

    open val previousButton: ImageButton? = null

    open val songTotalTime: TextView? = null

    open val songCurrentProgress: TextView? = null

    private var progressAnimator: ObjectAnimator? = null

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        val safeTotal = total.coerceAtLeast(0)
        val safeProgress = progress.coerceIn(0, safeTotal)

        if (seekBar == null) {
            progressSlider?.let { slider ->
                // Always define a valid range
                slider.valueFrom = 0f
                slider.valueTo = safeTotal.toFloat()

                // Clamp progress within range
                slider.value = safeProgress.toFloat()
            }
        } else {
            seekBar?.apply {
                max = safeTotal
                if (isSeeking) {
                    this.progress = safeProgress
                } else {
                    progressAnimator = ObjectAnimator.ofInt(this, "progress", safeProgress).apply {
                        duration = SLIDER_ANIMATION_TIME
                        interpolator = LinearInterpolator()
                        start()
                    }
                }
            }
        }
        val timeDisplayMode = PreferenceUtil.timeDisplayMode
        when (timeDisplayMode) {
            TIME_DISPLAY_MODE_TOTAL -> {
                songTotalTime?.text = MusicUtil.getReadableDurationString(total.toLong())
                songTotalTime?.setOnClickListener(null) 
            }
            TIME_DISPLAY_MODE_REMAINING -> {
                songTotalTime?.text = MusicUtil.getReadableDurationString((total - progress).toLong())
                songTotalTime?.setOnClickListener(null) 
            }
            TIME_DISPLAY_MODE_TOGGLE -> {
                if (PreferenceUtil.isShowingRemainingTime) {
                    songTotalTime?.text = MusicUtil.getReadableDurationString((total - progress).toLong())
                } else {
                    songTotalTime?.text = MusicUtil.getReadableDurationString(total.toLong())
                }
                songTotalTime?.setOnClickListener {
                    PreferenceUtil.isShowingRemainingTime = !PreferenceUtil.isShowingRemainingTime
                    onUpdateProgressViews(progress, total) 
                }
            }
        }
        songCurrentProgress?.text = MusicUtil.getReadableDurationString(progress.toLong())
    }

    private fun setUpProgressSlider() {
        progressSlider?.addOnChangeListener(Slider.OnChangeListener { _, value, fromUser ->
            onProgressChange(value.toInt(), fromUser)
        })
        progressSlider?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                onStartTrackingTouch()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                onStopTrackingTouch(slider.value.toInt())
            }
        })

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onProgressChange(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                onStartTrackingTouch()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                onStopTrackingTouch(seekBar?.progress ?: 0)
            }
        })
    }

    private fun onProgressChange(value: Int, fromUser: Boolean) {
        if (fromUser) {
            onUpdateProgressViews(value, MusicPlayerRemote.songDurationMillis)
        }
    }

    private fun onStartTrackingTouch() {
        isSeeking = true
        progressViewUpdateHelper.stop()
        progressAnimator?.cancel()
    }

    private fun onStopTrackingTouch(value: Int) {
        isSeeking = false
        MusicPlayerRemote.seekTo(value)
        progressViewUpdateHelper.start()
    }

    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
        if (PreferenceUtil.circlePlayButton) {
            requireContext().theme.applyStyle(R.style.CircleFABOverlay, true)
        } else {
            requireContext().theme.applyStyle(R.style.RoundedFABOverlay, true)
        }
    }

    fun View.showBounceAnimation() {
        clearAnimation()
        scaleX = 0.9f
        scaleY = 0.9f
        isVisible = true
        pivotX = (width / 2).toFloat()
        pivotY = (height / 2).toFloat()

        animate().setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .scaleX(1.1f)
            .scaleY(1.1f)
            .withEndAction {
                animate().setDuration(200)
                    .setInterpolator(AccelerateInterpolator())
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .start()
            }
            .start()
    }

    private fun setUpSquiggly() {
        seekBar?.let {
            val useSquiggly = MusicPlayerRemote.isPlaying && PreferenceUtil.isSquiggly
            squiggly = SquigglyProgress().apply {
                strokeWidth = 9f
                lineAmplitude = 9f
                waveLength = 80f
                phaseSpeed = 60f
                animate = useSquiggly
            }
            it.progressDrawable = squiggly
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideVolumeIfAvailable()
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this)
        if (seekBar != null) {
            setUpSquiggly()
        }
        setUpProgressSlider()
        setUpPrevNext()
        setUpShuffleButton()
        setUpRepeatButton()
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == SWAP_SHUFFLE_REPEAT_BUTTONS) {
            applyButtonSwapLogic()
        }

        if (key == TIME_DISPLAY_MODE) {
            val progress = MusicPlayerRemote.position
            val total = MusicPlayerRemote.songDurationMillis
            onUpdateProgressViews(progress, total)
        }

        if (key == IS_SQUIGGLY) {
            squiggly.animate = (MusicPlayerRemote.isPlaying && PreferenceUtil.isSquiggly)
        }
    }

    private fun applyButtonSwapLogic() {
        setUpShuffleButton()
        updateShuffleState()
        setUpRepeatButton()
        updateRepeatState()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpPrevNext() {
        nextButton?.setOnTouchListener(MusicSeekSkipTouchListener(requireActivity(), true))
        previousButton?.setOnTouchListener(MusicSeekSkipTouchListener(requireActivity(), false))
    }

    private fun setUpShuffleButton() {
        getShuffle().setOnClickListener { MusicPlayerRemote.toggleShuffleMode() }
        getShuffle().setImageResource(R.drawable.ic_shuffle)
    }

    private fun setUpRepeatButton() {
        getRepeat().setOnClickListener { MusicPlayerRemote.cycleRepeatMode() }
        getRepeat().setImageResource(R.drawable.ic_repeat)
    }

    fun updatePrevNextColor() {
        nextButton?.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        previousButton?.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
    }

    fun updateShuffleState() {
        getShuffle().setColorFilter(
            when (MusicPlayerRemote.shuffleMode) {
                MusicService.SHUFFLE_MODE_SHUFFLE -> lastPlaybackControlsColor
                else -> lastDisabledPlaybackControlsColor
            }, PorterDuff.Mode.SRC_IN
        )
    }

    private fun getRepeat(): ImageButton =
        if (!PreferenceUtil.swapShuffleRepeatButtons) repeatButton else shuffleButton

    private fun getShuffle(): ImageButton =
        if (!PreferenceUtil.swapShuffleRepeatButtons) shuffleButton else repeatButton

    fun updateRepeatState() {
        when (MusicPlayerRemote.repeatMode) {
            MusicService.REPEAT_MODE_NONE -> {
                getRepeat().setImageResource(R.drawable.ic_repeat)
                getRepeat().setColorFilter(
                    lastDisabledPlaybackControlsColor,
                    PorterDuff.Mode.SRC_IN
                )
            }
            MusicService.REPEAT_MODE_ALL -> {
                getRepeat().setImageResource(R.drawable.ic_repeat)
                getRepeat().setColorFilter(
                    lastPlaybackControlsColor,
                    PorterDuff.Mode.SRC_IN
                )
            }
            MusicService.REPEAT_MODE_THIS -> {
                getRepeat().setImageResource(R.drawable.ic_repeat_one)
                getRepeat().setColorFilter(
                    lastPlaybackControlsColor,
                    PorterDuff.Mode.SRC_IN
                )
            }
        }
    }

    protected var volumeFragment: VolumeFragment? = null

    private fun hideVolumeIfAvailable() {
        if (PreferenceUtil.isVolumeVisibilityMode) {
            childFragmentManager.commit {
                replace<VolumeFragment>(R.id.volumeFragmentContainer)
            }
            childFragmentManager.executePendingTransactions()
        }
        volumeFragment = whichFragment(R.id.volumeFragmentContainer)
    }

    override fun onResume() {
        super.onResume()
        if (seekBar != null) {
            setUpSquiggly()
            val max = seekBar?.max?.coerceAtLeast(1) ?: 1
            val progress = seekBar?.progress ?: 0
            val level = ((progress * 10000f) / max).toInt()
            squiggly?.level = level
        }
        progressViewUpdateHelper.start()
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    override fun onStop() {
        super.onStop()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    companion object {
        const val SLIDER_ANIMATION_TIME: Long = 400
    }
}
