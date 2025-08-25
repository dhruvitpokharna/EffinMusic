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
package code.name.monkey.retromusic.adapter.song

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import android.util.TypedValue
import com.google.android.material.textview.MaterialTextView
import androidx.core.view.isVisible

class SimpleSongAdapter(
    context: FragmentActivity,
    songs: ArrayList<Song>,
    layoutRes: Int
) : SongAdapter(context, songs, layoutRes) {

    override fun swapDataSet(dataSet: List<Song>) {
        this.dataSet = dataSet.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val fixedTrackNumber = MusicUtil.getFixedTrackNumber(dataSet[position].trackNumber)

        holder.imageText?.text = if (fixedTrackNumber > 0) fixedTrackNumber.toString() else "-"
        holder.time?.text = String.format("%s | %s", fixedTrackNumber, MusicUtil.getReadableDurationString(dataSet[position].duration))

        val songTextSize = PreferenceUtil.songTextSize.toFloat()
        holder.title?.setTextSize(TypedValue.COMPLEX_UNIT_SP, songTextSize)

        if (PreferenceUtil.showArtistInSongs) {
            holder.artist?.text = dataSet[position].allArtists
            holder.artist?.isVisible = true
            val artistTextSize = PreferenceUtil.artistTextSize.toFloat()
            holder.artist?.setTextSize(TypedValue.COMPLEX_UNIT_SP, artistTextSize) // Slightly smaller for artist
        } else {
            holder.artist?.isVisible = false
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}
