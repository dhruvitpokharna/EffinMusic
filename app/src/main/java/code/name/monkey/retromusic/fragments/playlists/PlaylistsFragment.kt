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
package code.name.monkey.retromusic.fragments.playlists

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.MenuCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import code.name.monkey.retromusic.EXTRA_PLAYLIST_ID
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.playlist.PlaylistAdapter
import code.name.monkey.retromusic.db.PlaylistEntity
import code.name.monkey.retromusic.db.PlaylistWithSongs
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.db.toSongEntity
import code.name.monkey.retromusic.extensions.setUpMediaRouteButton
import code.name.monkey.retromusic.fragments.ReloadType
import code.name.monkey.retromusic.fragments.base.AbsRecyclerViewCustomGridSizeFragment
import code.name.monkey.retromusic.helper.SortOrder.PlaylistSortOrder
import code.name.monkey.retromusic.interfaces.IPlaylistClickListener
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.PreferenceUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.util.RetroUtil
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class PlaylistsFragment :
    AbsRecyclerViewCustomGridSizeFragment<PlaylistAdapter, GridLayoutManager>(),
    IPlaylistClickListener {

    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getPlaylists().observe(viewLifecycleOwner) {
            if (it.isNotEmpty())
                adapter?.swapDataSet(it)
            else
                adapter?.swapDataSet(listOf())
        }
        if (PreferenceUtil.playlistSortOrder == PlaylistSortOrder.PLAYLIST_CUSTOM) {
            setAndSaveSortOrder(PlaylistSortOrder.PLAYLIST_CUSTOM)
        }
        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0) {

            override fun isLongPressDragEnabled(): Boolean = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                adapter?.swapItems(fromPosition, toPosition)
                adapter?.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe action
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                updatePlaylistPositionsInDb()
                setAndSaveSortOrder(PlaylistSortOrder.PLAYLIST_CUSTOM)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override val titleRes: Int
        get() = R.string.playlists

    override val emptyMessage: Int
        get() = R.string.no_playlists

    override val isShuffleVisible: Boolean
        get() = false

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireContext(), getGridSize())
    }

    override fun createAdapter(): PlaylistAdapter {
        val dataSet = if (adapter == null) mutableListOf() else adapter!!.dataSet
        return PlaylistAdapter(
            requireActivity(),
            dataSet,
            itemLayoutRes(),
            this
        ) { viewHolder -> 
            itemTouchHelper.startDrag(viewHolder)
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        val gridSizeItem: MenuItem = menu.findItem(R.id.action_grid_size)
        if (RetroUtil.isLandscape) {
            gridSizeItem.setTitle(R.string.action_grid_size_land)
        }
        setupGridSizeMenu(gridSizeItem.subMenu!!)
        menu.removeItem(R.id.action_layout_type)
        menu.add(0, R.id.action_add_to_playlist, 0, R.string.new_playlist_title)
        menu.add(0, R.id.action_import_playlist, 0, R.string.import_playlist)
        menu.findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        setUpSortOrderMenu(menu.findItem(R.id.action_sort_order).subMenu!!)
        MenuCompat.setGroupDividerEnabled(menu, true)
        //Setting up cast button
        requireContext().setUpMediaRouteButton(menu)
    }

    private val importPlaylistLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uris = mutableListOf<Uri>()
                result.data?.data?.let { uri ->
                    uris.add(uri)
                }
                result.data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                }
                importPlaylists(uris)
            }
        }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (handleGridSizeMenuItem(item)) {
            return true
        }
        if (handleSortOrderMenuItem(item)) {
            return true
        }
        when (item.itemId) {
            R.id.action_import_playlist -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*" // Allow all file types for now, can be refined later
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                importPlaylistLauncher.launch(intent)
                return true
            }
        }
        return super.onMenuItemSelected(item)
    }

    private fun setupGridSizeMenu(gridSizeMenu: SubMenu) {
        when (getGridSize()) {
            1 -> gridSizeMenu.findItem(R.id.action_grid_size_1).isChecked = true
            2 -> gridSizeMenu.findItem(R.id.action_grid_size_2).isChecked = true
            3 -> gridSizeMenu.findItem(R.id.action_grid_size_3).isChecked = true
            4 -> gridSizeMenu.findItem(R.id.action_grid_size_4).isChecked = true
            5 -> gridSizeMenu.findItem(R.id.action_grid_size_5).isChecked = true
            6 -> gridSizeMenu.findItem(R.id.action_grid_size_6).isChecked = true
            7 -> gridSizeMenu.findItem(R.id.action_grid_size_7).isChecked = true
            8 -> gridSizeMenu.findItem(R.id.action_grid_size_8).isChecked = true
        }
        val gridSize = if (RetroUtil.isLandscape) 4 else 3
        if (gridSize < 8) {
            gridSizeMenu.findItem(R.id.action_grid_size_8).isVisible = false
        }
        if (gridSize < 7) {
            gridSizeMenu.findItem(R.id.action_grid_size_7).isVisible = false
        }
        if (gridSize < 6) {
            gridSizeMenu.findItem(R.id.action_grid_size_6).isVisible = false
        }
        if (gridSize < 5) {
            gridSizeMenu.findItem(R.id.action_grid_size_5).isVisible = false
        }
        if (gridSize < 4) {
            gridSizeMenu.findItem(R.id.action_grid_size_4).isVisible = false
        }
        if (gridSize < 3) {
            gridSizeMenu.findItem(R.id.action_grid_size_3).isVisible = false
        }
    }

    private fun setUpSortOrderMenu(subMenu: SubMenu) {
        val order: String? = getSortOrder()
        subMenu.clear()
        createId(
            subMenu,
            R.id.action_song_sort_order_asc,
            R.string.sort_order_a_z,
            order == PlaylistSortOrder.PLAYLIST_A_Z
        )
        createId(
            subMenu,
            R.id.action_song_sort_order_desc,
            R.string.sort_order_z_a,
            order == PlaylistSortOrder.PLAYLIST_Z_A
        )
        createId(
            subMenu,
            R.id.action_playlist_sort_order,
            R.string.sort_order_num_songs,
            order == PlaylistSortOrder.PLAYLIST_SONG_COUNT
        )
        createId(
            subMenu,
            R.id.action_playlist_sort_order_desc,
            R.string.sort_order_num_songs_desc,
            order == PlaylistSortOrder.PLAYLIST_SONG_COUNT_DESC
        )
        createId(
            subMenu,
            R.id.action_playlist_sort_order_custom,
            R.string.sort_order_custom,
            order == PlaylistSortOrder.PLAYLIST_CUSTOM
        )
        subMenu.setGroupCheckable(0, true, true)
    }

    private fun handleSortOrderMenuItem(item: MenuItem): Boolean {
        val sortOrder: String = when (item.itemId) {
            R.id.action_song_sort_order_asc -> PlaylistSortOrder.PLAYLIST_A_Z
            R.id.action_song_sort_order_desc -> PlaylistSortOrder.PLAYLIST_Z_A
            R.id.action_playlist_sort_order -> PlaylistSortOrder.PLAYLIST_SONG_COUNT
            R.id.action_playlist_sort_order_desc -> PlaylistSortOrder.PLAYLIST_SONG_COUNT_DESC
            R.id.action_playlist_sort_order_custom -> PlaylistSortOrder.PLAYLIST_CUSTOM
            else -> PreferenceUtil.playlistSortOrder
        }
        if (sortOrder != PreferenceUtil.playlistSortOrder) {
            item.isChecked = true
            setAndSaveSortOrder(sortOrder)
            return true
        }
        return false
    }

    private fun handleGridSizeMenuItem(item: MenuItem): Boolean {
        val gridSize = when (item.itemId) {
            R.id.action_grid_size_1 -> 1
            R.id.action_grid_size_2 -> 2
            R.id.action_grid_size_3 -> 3
            R.id.action_grid_size_4 -> 4
            R.id.action_grid_size_5 -> 5
            R.id.action_grid_size_6 -> 6
            R.id.action_grid_size_7 -> 7
            R.id.action_grid_size_8 -> 8
            else -> 0
        }
        if (gridSize > 0) {
            item.isChecked = true
            setAndSaveGridSize(gridSize)
            return true
        }
        return false
    }

    private fun createId(menu: SubMenu, id: Int, title: Int, checked: Boolean) {
        menu.add(0, id, 0, title).isChecked = checked
    }

    private fun updatePlaylistPositionsInDb() {
        val dataSet = adapter?.dataSet ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            dataSet.forEachIndexed { index, playlistWithSongs ->
                playlistWithSongs.playlistEntity.position = index
            }
            libraryViewModel.updatePlaylistPositions(dataSet.map { it.playlistEntity })
        }
    }

    private fun importPlaylists(uris: List<Uri>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var successfulImports = 0
            var failedImports = 0
            for (uri in uris) {
                try {
                    val playlistName = getFileNameFromUri(uri) ?: "Imported Playlist"
                    val contentResolver = requireContext().contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val songPaths = mutableListOf<String>()
                    reader.useLines { lines ->
                        lines.forEach { line ->
                            val trimmedLine = line.trim()
                            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                                songPaths.add(trimmedLine)
                            }
                        }
                    }

                    val allSongs = libraryViewModel.getSongs().value ?: emptyList()
                    val songsToAddToPlaylist = mutableListOf<Song>()

                    for (path in songPaths) {
                        val foundSong = allSongs.find { it.data == path }
                        if (foundSong != null) {
                            songsToAddToPlaylist.add(foundSong)
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Song not found: $path in playlist $playlistName", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    if (songsToAddToPlaylist.isNotEmpty()) {
                        val existingPlaylists = libraryViewModel.checkPlaylistExists(playlistName)
                        val playlistId: Long
                        if (existingPlaylists.isEmpty()) {
                            playlistId = libraryViewModel.createPlaylist(PlaylistEntity(playlistName = playlistName))
                        } else {
                            playlistId = existingPlaylists.first().playListId
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Playlist '$playlistName' already exists. Adding songs to it.", Toast.LENGTH_LONG).show()
                            }
                        }

                        val songEntities = songsToAddToPlaylist.map { it.toSongEntity(playlistId) }
                        libraryViewModel.insertSongs(songEntities)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Successfully imported ${songsToAddToPlaylist.size} songs to playlist '$playlistName'", Toast.LENGTH_LONG).show()
                        }
                        successfulImports++
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "No songs found in playlist file '$playlistName' or no songs could be resolved.", Toast.LENGTH_LONG).show()
                        }
                        failedImports++
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error importing playlist: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    e.printStackTrace()
                    failedImports++
                }
            }
            withContext(Dispatchers.Main) {
                if (successfulImports > 0) {
                    Toast.makeText(requireContext(), "Finished importing $successfulImports playlists. $failedImports failed.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "No playlists were imported.", Toast.LENGTH_LONG).show()
                }
            }
            libraryViewModel.forceReload(ReloadType.Playlists)
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name?.substringBeforeLast(".") // Remove file extension
    }

    override fun setGridSize(gridSize: Int) {
        adapter?.notifyDataSetChanged()
    }

    override fun setSortOrder(sortOrder: String) {
        libraryViewModel.forceReload(ReloadType.Playlists)
    }

    override fun loadSortOrder(): String {
        return PreferenceUtil.playlistSortOrder
    }

    override fun saveSortOrder(sortOrder: String) {
        PreferenceUtil.playlistSortOrder = sortOrder
    }

    override fun loadGridSize(): Int {
        return PreferenceUtil.playlistGridSize
    }

    override fun saveGridSize(gridColumns: Int) {
        PreferenceUtil.playlistGridSize = gridColumns
    }

    override fun loadGridSizeLand(): Int {
        return PreferenceUtil.playlistGridSizeLand
    }

    override fun saveGridSizeLand(gridColumns: Int) {
        PreferenceUtil.playlistGridSizeLand = gridColumns
    }

    override fun loadLayoutRes(): Int {
        return R.layout.layout_playlist
    }

    override fun saveLayoutRes(layoutRes: Int) {
        //Save layout
    }

    override fun onPlaylistClick(playlistWithSongs: PlaylistWithSongs, view: View) {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).addTarget(requireView())
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        findNavController().navigate(
            R.id.playlistDetailsFragment,
            bundleOf(EXTRA_PLAYLIST_ID to playlistWithSongs.playlistEntity.playListId)
        )
    }
}
