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

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.base.AbsMultiSelectAdapter
import code.name.monkey.retromusic.adapter.base.MediaEntryViewHolder
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.albumCoverOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.RetroMusicColoredTarget
import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.helper.menu.SongsMenuHelper
import code.name.monkey.retromusic.interfaces.IAlbumClickListener
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import coil.load
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import me.zhanghai.android.fastscroll.PopupTextProvider
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy

open class AlbumAdapter(
    override val activity: FragmentActivity,
    var dataSet: List<Album>,
    var itemLayoutRes: Int,
    val listener: IAlbumClickListener?,
    private val showCovers: Boolean
) : AbsMultiSelectAdapter<AlbumAdapter.ViewHolder, Album>(
    activity,
    R.menu.menu_media_selection
), PopupTextProvider {

    init {
        this.setHasStableIds(true)
    }

    fun swapDataSet(dataSet: List<Album>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view, viewType)
    }

    protected open fun createViewHolder(view: View, viewType: Int): ViewHolder {
        return ViewHolder(view)
    }

    private fun getAlbumTitle(album: Album): String {
        return album.title
    }

    protected open fun getAlbumText(album: Album): String? {
        return album.albumArtist.let {
            if (it.isNullOrEmpty()) {
                album.artistName
            } else {
                it
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = dataSet[position]
        val isChecked = isChecked(album)
        holder.itemView.isActivated = isChecked
        holder.title?.text = getAlbumTitle(album)
        holder.text?.text = getAlbumText(album)
        // Check if imageContainer exists so we can have a smooth transition without
        // CardView clipping, if it doesn't exist in current layout set transition name to image instead.
        if (holder.imageContainer != null) {
            holder.imageContainer?.transitionName = album.id.toString()
        } else {
            holder.image?.transitionName = album.id.toString()
        }

        if (showCovers) {
            holder.image?.isVisible = true
            holder.imageText?.isVisible = false // Hide imageText if showing cover
            loadAlbumCover(album, holder)
        } else {
            holder.image?.isVisible = false
            holder.imageText?.isVisible = true // Show imageText if not showing cover
            holder.imageText?.text = (position + 1).toString() // Display song number
        }
    }

    protected open fun setColors(color: MediaNotificationProcessor, holder: ViewHolder) {
        if (holder.paletteColorContainer != null) {
            holder.title?.setTextColor(color.primaryTextColor)
            holder.text?.setTextColor(color.secondaryTextColor)
            holder.paletteColorContainer?.setBackgroundColor(color.backgroundColor)
        }
        holder.mask?.backgroundTintList = ColorStateList.valueOf(color.primaryTextColor)
        holder.imageContainerCard?.setCardBackgroundColor(color.backgroundColor)
    }

    protected open fun loadAlbumCover(album: Album, holder: ViewHolder) {
        val imageView = holder.image ?: return

        val overrideSize = when (PreferenceUtil.albumGridSize) {
            2 -> 500
            3 -> 300
            4 -> 250
            else -> 200
        }
        
        val song = album.safeGetFirstSong()

        val model = RetroGlideExtension.getSongModel(song)

        if (PreferenceUtil.isIgnoreMediaStoreArtwork) {
            val requestOptions = RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .skipMemoryCache(false)
                .override(overrideSize, overrideSize)
            Glide.with(imageView)
                .asBitmap()
                .albumCoverOptions(song)
                .load(model)
                .apply(requestOptions)
                .into(imageView)
            return
        }
        
        imageView.load(model) {
            size(overrideSize, overrideSize)
            crossfade(true)
            allowHardware(true)
            placeholder(R.drawable.default_album_art)
            memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            diskCachePolicy(coil.request.CachePolicy.ENABLED)
            error(R.drawable.default_album_art)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun getIdentifier(position: Int): Album? {
        return dataSet[position]
    }

    override fun getName(model: Album): String {
        return model.title
    }

    override fun onMultipleItemAction(
        menuItem: MenuItem,
        selection: List<Album>
    ) {
        SongsMenuHelper.handleMenuClick(activity, getSongList(selection), menuItem.itemId)
    }

    private fun getSongList(albums: List<Album>): List<Song> {
        val songs = ArrayList<Song>()
        for (album in albums) {
            songs.addAll(album.songs)
        }
        return songs
    }

    override fun getPopupText(position: Int): String {
        return getSectionName(position)
    }

    private fun getSectionName(position: Int): String {
        var sectionName: String? = null
        when (PreferenceUtil.albumSortOrder) {
            SortOrder.AlbumSortOrder.ALBUM_A_Z, SortOrder.AlbumSortOrder.ALBUM_Z_A -> sectionName =
                dataSet[position].title

            SortOrder.AlbumSortOrder.ALBUM_ARTIST -> sectionName = dataSet[position].albumArtist
            SortOrder.AlbumSortOrder.ALBUM_YEAR -> return MusicUtil.getYearString(
                dataSet[position].year
            )
        }
        return MusicUtil.getSectionName(sectionName)
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
                if (showCovers) {
                    image?.let {
                        listener?.onAlbumClick(dataSet[layoutPosition].id, imageContainer ?: it)
                    }
                } else {
                    listener?.onAlbumClick(dataSet[layoutPosition].id, itemView)
                }
            }
        }

        override fun onLongClick(v: View?): Boolean {
            return toggleChecked(layoutPosition)
        }
    }

    companion object {
        val TAG: String = AlbumAdapter::class.java.simpleName
    }
}
