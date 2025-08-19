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
package code.name.monkey.retromusic.adapter.artist

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.base.AbsMultiSelectAdapter
import code.name.monkey.retromusic.adapter.base.MediaEntryViewHolder
import code.name.monkey.retromusic.extensions.hide
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.artistImageOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.RetroMusicColoredTarget
import code.name.monkey.retromusic.helper.menu.SongsMenuHelper
import code.name.monkey.retromusic.interfaces.IAlbumArtistClickListener
import code.name.monkey.retromusic.interfaces.IArtistClickListener
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import coil.load
import com.bumptech.glide.Glide
import me.zhanghai.android.fastscroll.PopupTextProvider
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.bumptech.glide.request.RequestOptions

class ArtistAdapter(
    override val activity: FragmentActivity,
    var dataSet: List<Artist>,
    var itemLayoutRes: Int,
    val IArtistClickListener: IArtistClickListener,
    val IAlbumArtistClickListener: IAlbumArtistClickListener? = null,
    private val showCovers: Boolean
) : AbsMultiSelectAdapter<ArtistAdapter.ViewHolder, Artist>(activity, R.menu.menu_media_selection),
    PopupTextProvider {

    var albumArtistsOnly = false

    init {
        this.setHasStableIds(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapDataSet(dataSet: List<Artist>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
        albumArtistsOnly = PreferenceUtil.albumArtistsOnly
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            try {
                LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
            } catch (e: Resources.NotFoundException) {
                LayoutInflater.from(activity).inflate(R.layout.item_grid_circle, parent, false)
            }
        return createViewHolder(view)
    }

    private fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = dataSet[position]
        val isChecked = isChecked(artist)
        holder.itemView.isActivated = isChecked
        holder.title?.text = artist.name
        holder.text?.hide()
        val transitionName =
            if (albumArtistsOnly) artist.name else artist.id.toString()
        if (holder.imageContainer != null) {
            holder.imageContainer?.transitionName = transitionName
        } else {
            holder.image?.transitionName = transitionName
        }

        if (showCovers) {
            holder.image?.isVisible = true
            holder.imageText?.isVisible = false // Hide imageText if showing cover
            loadArtistImage(artist, holder)
        } else {
            holder.image?.isVisible = false
            holder.imageText?.isVisible = true // Show imageText if not showing cover
            holder.imageText?.text = (position + 1).toString() // Display song number
        }
    }

    private fun setColors(processor: MediaNotificationProcessor, holder: ViewHolder) {
        holder.mask?.backgroundTintList = ColorStateList.valueOf(processor.primaryTextColor)
        if (holder.paletteColorContainer != null) {
            holder.paletteColorContainer?.setBackgroundColor(processor.backgroundColor)
            holder.title?.setTextColor(processor.primaryTextColor)
        }
        holder.imageContainerCard?.setCardBackgroundColor(processor.backgroundColor)
    }

    private fun loadArtistImage(artist: Artist, holder: ViewHolder) {
        if (holder.image == null) {
            return
        }
        
        Glide.with(activity)
            .asBitmapPalette()
            .artistImageOptions(artist)
            .load(RetroGlideExtension.getArtistModel(artist))
            .transition(RetroGlideExtension.getDefaultTransition())
            .into(object : RetroMusicColoredTarget(holder.image!!) {
                override fun onColorReady(colors: MediaNotificationProcessor) {
                    setColors(colors, holder)
                }
            })
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): Artist {
        return dataSet[position]
    }

    override fun getName(model: Artist): String {
        return model.name
    }

    override fun onMultipleItemAction(
        menuItem: MenuItem,
        selection: List<Artist>
    ) {
        SongsMenuHelper.handleMenuClick(activity, getSongList(selection), menuItem.itemId)
    }

    private fun getSongList(artists: List<Artist>): List<Song> {
        val songs = ArrayList<Song>()
        for (artist in artists) {
            songs.addAll(artist.songs) // maybe async in future?
        }
        return songs
    }

    override fun getPopupText(position: Int): String {
        return if (position in 0 until dataSet.size) {
            getSectionName(position)
        } else {
            ""
        }
    }

    private fun getSectionName(position: Int): String {
        return if (position in 0 until dataSet.size) {
            MusicUtil.getSectionName(dataSet[position].name)
        } else {
            ""
        }
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {

        init {
            menu?.isVisible = false
        }

        override fun onClick(v: View?) {
            super.onClick(v)
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                val artist = dataSet[layoutPosition]
                if (showCovers) {
                    image?.let {
                        if (albumArtistsOnly && IAlbumArtistClickListener != null) {
                            IAlbumArtistClickListener.onAlbumArtist(artist.name, imageContainer ?: it)
                        } else {
                            IArtistClickListener.onArtist(artist.id, imageContainer ?: it)
                        }
                    }
                } else {
                    if (albumArtistsOnly && IAlbumArtistClickListener != null) {
                        IAlbumArtistClickListener.onAlbumArtist(artist.name, itemView)
                    } else {
                        IArtistClickListener.onArtist(artist.id, itemView)
                    }
                }
            }
        }

        override fun onLongClick(v: View?): Boolean {
            return toggleChecked(layoutPosition)
        }
    }
}
