/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package code.name.monkey.retromusic.service

import android.content.Context
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import android.content.SharedPreferences
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.extensions.uri
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.service.playback.Playback.PlaybackCallbacks
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.PreferenceUtil.isGapLessPlayback
import code.name.monkey.retromusic.util.logE
import code.name.monkey.retromusic.util.Taglib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.media.audiofx.LoudnessEnhancer
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import code.name.monkey.retromusic.extensions.showToast
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.min
import kotlin.math.max

/**
 * @author Andrew Neal, Karim Abou Zeid (kabouzeid)
 */
class MultiPlayer(context: Context) : LocalPlayback(context) {
    private var mCurrentMediaPlayer = MediaPlayer()
    private var mNextMediaPlayer: MediaPlayer? = null
    override var callbacks: PlaybackCallbacks? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    // Call this after MediaPlayer is initialized and audioSessionId is available
    private fun initAudioEffects() {
        releaseAudioEffects()

        val sessionId = mCurrentMediaPlayer.audioSessionId
        equalizer = Equalizer(0, sessionId).apply { enabled = true }
        bassBoost = BassBoost(0, sessionId).apply { enabled = true }
        virtualizer = Virtualizer(0, sessionId).apply { enabled = true }

        registerPrefListener()
    }

    private fun openAudioEffectSession() {
        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
        context.sendBroadcast(intent)
    }
    
    private fun closeAudioEffectSession() {
        val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        }
        context.sendBroadcast(intent)
    }

    private fun applyEqualizerPreferences() {
        val prefs = context.getSharedPreferences("equalizer_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("equalizer_enabled", false)

        if (!isEnabled) {
            setEqualizerEnabled(false)
            setBassBoostStrength(0)
            setVirtualizerStrength(0)
            setAmplifierStrength(0)
            return
        }
        
        setEqualizerEnabled(isEnabled)
        for (i in 0 until (equalizer?.numberOfBands ?: 0)) {
            val level = prefs.getFloat("band_$i", 0f)
            setEqualizerBandLevel(i.toShort(), (level * 100).toInt().toShort())
        }

        val virtualizerStrength = (prefs.getFloat("virtualizer_strength", 0f) * 10).toInt().toShort()
        setVirtualizerStrength(virtualizerStrength)
        
        val bassBoostStrength = (prefs.getFloat("bass_boost_strength", 0f) * 10).toInt().toShort()
        setBassBoostStrength(bassBoostStrength)

        val amplifierStrength = (prefs.getFloat("amplifier_strength", 0f) * 10).toInt().toShort()
        setAmplifierStrength(amplifierStrength)
    }

    private fun applyReplayGain(song: Song) {
        if (PreferenceUtil.enableReplayGain == false) return
        if (song == Song.emptySong) return
        val preferAlbumGain = PreferenceUtil.preferAlbumGain
        
        scope.launch {
            val tags =  Taglib.getAllTags(context, song)

            fun parse(value: String?, default: Float = 0f): Float {
                return value?.replace("dB", "", ignoreCase = true)
                    ?.replace("[^\\d+-.]".toRegex(), "")
                    ?.toFloatOrNull() ?: default
            }
            val trackGain = parse(tags["REPLAYGAIN_TRACK_GAIN"]?.firstOrNull())
            val albumGain = parse(tags["REPLAYGAIN_ALBUM_GAIN"]?.firstOrNull())
            val trackPeak = parse(tags["REPLAYGAIN_TRACK_PEAK"]?.firstOrNull(), 1f)
            val albumPeak = parse(tags["REPLAYGAIN_ALBUM_PEAK"]?.firstOrNull(), 1f)
        
            val adjustDB = if (preferAlbumGain == true) {
                if ( albumGain != 0f)  albumGain else trackGain
            } else {
                if (trackGain != 0f) trackGain else albumGain
            }

            val peak = if (preferAlbumGain == true) {
                if (albumPeak != 1f) albumPeak else trackPeak
            } else {
                if (trackPeak != 1f) trackPeak else albumPeak
            }
        
            val safeDB = min(adjustDB, -20f * log10(peak))
        
            val gain = 10.0f.pow(safeDB / 20f).coerceIn(0f, 1f)
            
            scope.launch(Dispatchers.Main) {
                setVolume(gain)
            }
        }
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when {
            (key?.startsWith("band_") == true || key == "equalizer_enabled") -> {
                applyEqualizerPreferences()
            }
            key == "virtualizer_strength" -> {
                val value = sharedPreferences.getFloat(key, 0f)
                setVirtualizerStrength((value * 10).toInt().toShort())
            }
            key == "bass_boost_strength" -> {
                val value = sharedPreferences.getFloat(key, 0f)
                setBassBoostStrength((value * 10).toInt().toShort())
            }
            key == "amplifier_strength" -> {
                val value = sharedPreferences.getFloat(key, 0f)
                setAmplifierStrength((value * 10).toInt().toShort())
            }
        }
    }

    private fun registerPrefListener() {
        val prefs = context.getSharedPreferences("equalizer_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun unregisterPrefListener() {
        val prefs = context.getSharedPreferences("equalizer_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun releaseAudioEffects() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    override fun release() {
        stop()
        scope.cancel()
        closeAudioEffectSession() 
        mCurrentMediaPlayer.release()
        mNextMediaPlayer?.release()
        releaseAudioEffects()
        unregisterPrefListener()
    }

    fun getEqualizerMinBandLevel(): Short {
        equalizer?.let { eq ->
            val minLevel = eq.bandLevelRange[0]
            return minLevel
        }
        return 0
    }
    
    fun getEqualizerMaxBandLevel(): Short {
        equalizer?.let { eq ->
            val maxLevel = eq.bandLevelRange[1]
            return maxLevel
        }
        return 0
    }

    // Setters for effects
    fun setEqualizerBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
        } catch (_: Exception) {}
    }

    fun setBassBoostStrength(strength: Short) {
        try {
            bassBoost?.setStrength(strength)
        } catch (_: Exception) {}
    }

    fun setVirtualizerStrength(strength: Short) {
        try {
            virtualizer?.setStrength(strength)
        } catch (_: Exception) {}
    }

    fun setAmplifierStrength(strength: Short) {
        if (loudnessEnhancer == null) {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
        }
        loudnessEnhancer?.enabled = true
        val gain = strength.toInt()
        loudnessEnhancer?.setTargetGain(gain)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
    }

    /**
     * @return True if the player is ready to go, false otherwise
     */
    override var isInitialized = false
        private set

    init {
        mCurrentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
    }

    /**
     * @param song The song object you want to play
     * @return True if the `player` has been prepared and is ready to play, false otherwise
     */
    override fun setDataSource(
        song: Song,
        force: Boolean,
        completion: (success: Boolean) -> Unit,
    ) {
        isInitialized = false
        setDataSourceImpl(mCurrentMediaPlayer, song.uri.toString()) { success ->
            isInitialized = success
            if (isInitialized) {
                setNextDataSource(null)
                initAudioEffects()
                openAudioEffectSession()
                applyEqualizerPreferences()
                applyReplayGain(song)
            }
            completion(isInitialized)
        }
    }

    /**
     * Set the MediaPlayer to start when this MediaPlayer finishes playback.
     *
     * @param path The path of the file, or the http/rtsp URL of the stream you want to play
     */
    override fun setNextDataSource(path: String?) {
        try {
            mCurrentMediaPlayer.setNextMediaPlayer(null)
        } catch (e: IllegalArgumentException) {
            Log.i(TAG, "Next media player is current one, continuing")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Media player not initialized!")
            return
        }
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer?.release()
            mNextMediaPlayer = null
        }
        if (path == null) {
            return
        }
        if (isGapLessPlayback) {
            mNextMediaPlayer = MediaPlayer()
            mNextMediaPlayer?.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            mNextMediaPlayer?.audioSessionId = audioSessionId
            setDataSourceImpl(mNextMediaPlayer!!, path) { success ->
                if (success) {
                    try {
                        mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "setNextDataSource: setNextMediaPlayer()", e)
                        if (mNextMediaPlayer != null) {
                            mNextMediaPlayer?.release()
                            mNextMediaPlayer = null
                        }
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "setNextDataSource: setNextMediaPlayer()", e)
                        if (mNextMediaPlayer != null) {
                            mNextMediaPlayer?.release()
                            mNextMediaPlayer = null
                        }
                    }
                } else {
                    if (mNextMediaPlayer != null) {
                        mNextMediaPlayer?.release()
                        mNextMediaPlayer = null
                    }
                }
            }
        }
    }

    /**
     * Starts or resumes playback.
     */
    override fun start(): Boolean {
        super.start()
        return try {
            mCurrentMediaPlayer.start()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Resets the MediaPlayer to its uninitialized state.
     */
    override fun stop() {
        super.stop()
        mCurrentMediaPlayer.reset()
        isInitialized = false
    }

    /**
     * Releases resources associated with this MediaPlayer object.
     */
     
    /**
     * Pauses playback. Call start() to resume.
     */
    override fun pause(): Boolean {
        super.pause()
        return try {
            mCurrentMediaPlayer.pause()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Checks whether the MultiPlayer is playing.
     */
    override val isPlaying: Boolean
        get() = isInitialized && mCurrentMediaPlayer.isPlaying

    /**
     * Gets the duration of the file.
     *
     * @return The duration in milliseconds
     */
    override fun duration(): Int {
        return if (!this.isInitialized) {
            -1
        } else try {
            mCurrentMediaPlayer.duration
        } catch (e: IllegalStateException) {
            -1
        }
    }

    /**
     * Gets the current playback position.
     *
     * @return The current position in milliseconds
     */
    override fun position(): Int {
        return if (!this.isInitialized) {
            -1
        } else try {
            mCurrentMediaPlayer.currentPosition
        } catch (e: IllegalStateException) {
            -1
        }
    }

    /**
     * Gets the current playback position.
     *
     * @param whereto The offset in milliseconds from the start to seek to
     * @return The offset in milliseconds from the start to seek to
     */
    override fun seek(whereto: Int, force: Boolean): Int {
        return try {
            mCurrentMediaPlayer.seekTo(whereto)
            whereto
        } catch (e: IllegalStateException) {
            -1
        }
    }

    override fun setVolume(vol: Float): Boolean {
        return try {
            mCurrentMediaPlayer.setVolume(vol, vol)
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Sets the audio session ID.
     *
     * @param sessionId The audio session ID
     */
    override fun setAudioSessionId(sessionId: Int): Boolean {
        return try {
            mCurrentMediaPlayer.audioSessionId = sessionId
            true
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Returns the audio session ID.
     *
     * @return The current audio session ID.
     */
    override val audioSessionId: Int
        get() = mCurrentMediaPlayer.audioSessionId

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        isInitialized = false
        mCurrentMediaPlayer.release()
        mCurrentMediaPlayer = MediaPlayer()
        mCurrentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        logE(what.toString() + extra)
        return true
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
            isInitialized = false
            mCurrentMediaPlayer.release()
            mCurrentMediaPlayer = mNextMediaPlayer!!
            isInitialized = true
            mNextMediaPlayer = null
            callbacks?.onTrackWentToNext()
        } else {
            callbacks?.onTrackEnded()
        }
    }

    override fun setCrossFadeDuration(duration: Int) {}

    override fun setPlaybackSpeedPitch(speed: Float, pitch: Float) {
        mCurrentMediaPlayer.setPlaybackSpeedPitch(speed, pitch)
        mNextMediaPlayer?.setPlaybackSpeedPitch(speed, pitch)
    }

    companion object {
        val TAG: String = MultiPlayer::class.java.simpleName
    }
}
