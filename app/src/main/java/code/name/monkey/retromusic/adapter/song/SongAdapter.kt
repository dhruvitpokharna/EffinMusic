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

import android.content.res.ColorStateList
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import code.name.monkey.retromusic.EXTRA_ALBUM_ID
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.base.AbsMultiSelectAdapter
import code.name.monkey.retromusic.adapter.base.MediaEntryViewHolder
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.glide.RetroMusicColoredTarget
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.helper.menu.SongMenuHelper
import code.name.monkey.retromusic.helper.menu.SongsMenuHelper
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import android.text.TextUtils
import code.name.monkey.retromusic.util.RetroUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import coil.load
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import code.name.monkey.retromusic.glide.palette.BitmapPaletteWrapper
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy

/**
 * Created by hemanths on 13/08/17.
 */

open class SongAdapter(
    override val activity: FragmentActivity,
    var dataSet: MutableList<Song>,
    protected var itemLayoutRes: Int,
    showSectionName: Boolean = true
) : AbsMultiSelectAdapter<SongAdapter.ViewHolder, Song>(
    activity,
    R.menu.menu_media_selection
), PopupTextProvider {

    private var showSectionName = true

    init {
        this.showSectionName = showSectionName
        this.setHasStableIds(true)
    }

    open fun swapDataSet(dataSet: List<Song>) {
        this.dataSet = ArrayList(dataSet)
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            try {
                LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
            } catch (e: Resources.NotFoundException) {
                LayoutInflater.from(activity).inflate(R.layout.item_list, parent, false)
            }
        return createViewHolder(view)
    }

    protected open fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = dataSet[position]
        val isChecked = isChecked(song)
        holder.itemView.isActivated = isChecked
        holder.menu?.isGone = isChecked
        holder.title?.text = getSongTitle(song)
        // Apply text size preference
        holder.title?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, PreferenceUtil.songTextSize.toFloat())
        holder.text?.text = getSongText(song)
        holder.text?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, PreferenceUtil.artistTextSize.toFloat())
        holder.text2?.text = String.format("%s • %s", getTrackNumber(song).toString(), getSongText2(song))

        if (PreferenceUtil.showCoversInSongsTab) {
            holder.image?.isVisible = true
            holder.imageText?.isVisible = false
            loadAlbumCover(song, holder)
        } else {
            holder.image?.isVisible = false
            holder.imageText?.isVisible = true
        }

        val landscape = RetroUtil.isLandscape
        if (!PreferenceUtil.showSongMenuGrid || (PreferenceUtil.songGridSize > 2 && !landscape) || (PreferenceUtil.songGridSizeLand > 5 && landscape)) {
            holder.menu?.isVisible = false
        }

        // Apply marquee effect based on preference
        if (PreferenceUtil.enableSongTitleMarquee) {
            holder.title?.ellipsize = TextUtils.TruncateAt.MARQUEE
            holder.title?.isSelected = true // This is crucial for marquee to work
            holder.title?.isSingleLine = true
        } else {
            holder.title?.ellipsize = TextUtils.TruncateAt.END
            holder.title?.isSelected = false
            holder.title?.isSingleLine = true
        }
    }

    private fun setColors(color: MediaNotificationProcessor, holder: ViewHolder) {
        if (holder.paletteColorContainer != null) {
            holder.title?.setTextColor(color.primaryTextColor)
            holder.text?.setTextColor(color.secondaryTextColor)
            holder.paletteColorContainer?.setBackgroundColor(color.backgroundColor)
            holder.menu?.imageTintList = ColorStateList.valueOf(color.primaryTextColor)
        }
        holder.mask?.backgroundTintList = ColorStateList.valueOf(color.primaryTextColor)
    }

    protected open fun loadAlbumCover(song: Song, holder: ViewHolder) {
        val imageView = holder.image ?: return

        val overrideSize = when (PreferenceUtil.songGridSize) {
            2 -> 500
            3 -> 300
            4 -> 250
            else -> 200
        }

        val model = RetroGlideExtension.getSongModel(song)

        if (PreferenceUtil.isIgnoreMediaStoreArtwork) {
            Glide.with(imageView)
                .asBitmap()
                .songCoverOptions(song)
                .load(model)
                .into(imageView)
            return
        }

        imageView.load(model) {
            size(overrideSize, overrideSize)
            crossfade(true)
            allowHardware(true)
            placeholder(null)
            error(null)
            memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            diskCachePolicy(coil.request.CachePolicy.ENABLED)
        }
    }

    private fun getSongTitle(song: Song): String {
        return song.title
    }

    private fun getSongText(song: Song): String {
        return listOfNotNull(song.albumArtist, song.artistName)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
    }

    private fun getSongText2(song: Song): String {
        return song.albumName
    }

    private fun getTrackNumber(song: Song): Int {
        return song.trackNumber
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): Song? {
        return dataSet[position]
    }

    override fun getName(model: Song): String {
        return model.title
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        SongsMenuHelper.handleMenuClick(activity, selection, menuItem.itemId)
    }

    override fun getPopupText(position: Int): String {
        val sectionName: String? = when (PreferenceUtil.songSortOrder) {
            SortOrder.SongSortOrder.SONG_DEFAULT -> return MusicUtil.getSectionName(
                dataSet[position].title,
                true
            )

            SortOrder.SongSortOrder.SONG_A_Z, SortOrder.SongSortOrder.SONG_Z_A -> dataSet[position].title
            SortOrder.SongSortOrder.SONG_ALBUM -> dataSet[position].albumName
            SortOrder.SongSortOrder.SONG_ARTIST -> dataSet[position].artistName
            SortOrder.SongSortOrder.SONG_YEAR -> return MusicUtil.getYearString(dataSet[position].year)
            SortOrder.SongSortOrder.SONG_DATE -> return MusicUtil.getDateModifiedString(MusicUtil.getDateAdded(activity, dataSet[position].id))
            SortOrder.SongSortOrder.COMPOSER -> dataSet[position].composer
            SortOrder.SongSortOrder.SONG_ALBUM_ARTIST -> dataSet[position].albumArtist
            SortOrder.SongSortOrder.SONG_DATE_MODIFIED -> return MusicUtil.getDateModifiedString(dataSet[position].dateModified)
            else -> {
                return ""
            }
        }
        return MusicUtil.getSectionName(sectionName)
    }

    open inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        protected open var songMenuRes = SongMenuHelper.MENU_RES
        protected open val song: Song
            get() = dataSet[layoutPosition]

        init {
            menu?.setOnClickListener(object : SongMenuHelper.OnClickSongMenu(activity) {
                override val song: Song
                    get() = this@ViewHolder.song

                override val menuRes: Int
                    get() = songMenuRes

                override fun onMenuItemClick(item: MenuItem): Boolean {
                    return onSongMenuItemClick(item) || super.onMenuItemClick(item)
                }
            })
        }

        protected open fun onSongMenuItemClick(item: MenuItem): Boolean {
            if (image != null && image!!.isVisible) {
                when (item.itemId) {
                    R.id.action_go_to_album -> {
                        activity.findNavController(R.id.fragment_container)
                            .navigate(
                                R.id.albumDetailsFragment,
                                bundleOf(EXTRA_ALBUM_ID to song.albumId)
                            )
                        return true
                    }
                }
            }
            return false
        }

        override fun onClick(v: View?) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                MusicPlayerRemote.openQueueKeepShuffleMode(dataSet, layoutPosition, true)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            println("Long click")
            return toggleChecked(layoutPosition)
        }
    }

    companion object {
        val TAG: String = SongAdapter::class.java.simpleName
    }
}
