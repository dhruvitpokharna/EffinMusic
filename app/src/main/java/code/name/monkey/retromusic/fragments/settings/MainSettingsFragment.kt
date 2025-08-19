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
package code.name.monkey.retromusic.fragments.settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.retromusic.App
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentMainSettingsBinding
import code.name.monkey.retromusic.extensions.drawAboveSystemBarsWithPadding
import code.name.monkey.retromusic.model.SearchableSetting
import code.name.monkey.retromusic.views.SettingListItemView // Ensure this import is present
import androidx.navigation.NavOptions
import androidx.activity.OnBackPressedCallback


class MainSettingsFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentMainSettingsBinding? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val binding get() = _binding!!

    private lateinit var allSearchableSettings: List<SearchableSetting>
    private val originalSettingViews = mutableListOf<SettingListItemView>()
    private val currentSearchResultViews = mutableListOf<SettingListItemView>()

    override fun onClick(view: View) {
        findNavController().navigate(
            when (view.id) {
                R.id.generalSettings -> R.id.action_mainSettingsFragment_to_themeSettingsFragment
                R.id.audioSettings -> R.id.action_mainSettingsFragment_to_audioSettings
                R.id.personalizeSettings -> R.id.action_mainSettingsFragment_to_personalizeSettingsFragment
                R.id.imageSettings -> R.id.action_mainSettingsFragment_to_imageSettingFragment
                R.id.notificationSettings -> R.id.action_mainSettingsFragment_to_notificationSettingsFragment
                R.id.otherSettings -> R.id.action_mainSettingsFragment_to_otherSettingsFragment
                R.id.aboutSettings -> R.id.action_mainSettingsFragment_to_aboutActivity
                R.id.nowPlayingSettings -> R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment
                R.id.backup_restore_settings -> R.id.action_mainSettingsFragment_to_backupFragment
                else -> R.id.action_mainSettingsFragment_to_themeSettingsFragment
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (binding.searchView.query.isNotEmpty()) {
                    binding.searchView.setQuery("", false)
                } else {
                    isEnabled = false // Disable the callback to allow default back press
                    requireActivity().onBackPressed() // Call the default back press
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        // Populate originalSettingViews
        // Iterate through children after they are inflated from XML
        // This ensures the order is preserved
        originalSettingViews.clear()
        
        for (i in 0 until binding.container.childCount) {
            val child = binding.container.getChildAt(i)
            if (child is SettingListItemView) {
                originalSettingViews.add(child) // Add to list to preserve order
            }
        }

        // Initialize allSearchableSettings
        allSearchableSettings = listOf(
            // General Settings (R.id.generalSettings)
            SearchableSetting(R.id.generalSettings, getString(R.string.general_settings_title), getString(R.string.general_settings_summary), true, R.id.action_mainSettingsFragment_to_themeSettingsFragment),
            SearchableSetting(R.id.generalSettings, getString(R.string.pref_title_general_theme), null, false, R.id.action_mainSettingsFragment_to_themeSettingsFragment),
            SearchableSetting(R.id.generalSettings, getString(R.string.black_theme_name), null, false, R.id.action_mainSettingsFragment_to_themeSettingsFragment),
            SearchableSetting(R.id.generalSettings, getString(R.string.md3), null, false, R.id.action_mainSettingsFragment_to_themeSettingsFragment),
            SearchableSetting(R.id.generalSettings, getString(R.string.pref_title_custom_font), null, false, R.id.action_mainSettingsFragment_to_themeSettingsFragment),
            SearchableSetting(R.id.generalSettings, getString(R.string.colors), null, true, R.id.action_mainSettingsFragment_to_themeSettingsFragment), // Category title
            SearchableSetting(R.id.generalSettings, getString(R.string.pref_title_wallpaper_accent), getString(R.string.pref_summary_wallpaper_accent), false, R.id.action_mainSettingsFragment_to_themeSettingsFragment),
            SearchableSetting(R.id.generalSettings, getString(R.string.accent_color), getString(R.string.accent_color_desc), false, R.id.action_mainSettingsFragment_to_themeSettingsFragment),
            SearchableSetting(R.id.generalSettings, getString(R.string.pref_title_desaturated_color), getString(R.string.pref_summary_desaturated_color), false, R.id.action_mainSettingsFragment_to_themeSettingsFragment),
            SearchableSetting(R.id.generalSettings, getString(R.string.pref_title_colored_app), getString(R.string.pref_summary_colored_app), false, R.id.action_mainSettingsFragment_to_themeSettingsFragment),
            SearchableSetting(R.id.generalSettings, getString(R.string.pref_title_app_shortcuts), getString(R.string.pref_summary_colored_app_shortcuts), false, R.id.action_mainSettingsFragment_to_themeSettingsFragment),

            // Audio Settings (R.id.audioSettings)
            SearchableSetting(R.id.audioSettings, getString(R.string.pref_header_audio), getString(R.string.audio_settings_summary), true, R.id.action_mainSettingsFragment_to_audioSettings),
            SearchableSetting(R.id.audioSettings, getString(R.string.pref_title_audio_fade), getString(R.string.pref_summary_audio_fade), false, R.id.action_mainSettingsFragment_to_audioSettings),
            SearchableSetting(R.id.audioSettings, getString(R.string.pref_title_manage_audio_focus), getString(R.string.pref_summary_manage_audio_focus), false, R.id.action_mainSettingsFragment_to_audioSettings),
            SearchableSetting(R.id.audioSettings, getString(R.string.pref_title_cross_fade), getString(R.string.pref_summary_cross_fade), false, R.id.action_mainSettingsFragment_to_audioSettings),
            SearchableSetting(R.id.audioSettings, getString(R.string.pref_title_gapless_playback), getString(R.string.pref_summary_gapless_playback), false, R.id.action_mainSettingsFragment_to_audioSettings),
            SearchableSetting(R.id.audioSettings, getString(R.string.equalizer), null, false, R.id.action_mainSettingsFragment_to_audioSettings),
            SearchableSetting(R.id.audioSettings, getString(R.string.pref_title_toggle_toggle_headset), getString(R.string.pref_summary_toggle_headset), false, R.id.action_mainSettingsFragment_to_audioSettings),
            SearchableSetting(R.id.audioSettings, getString(R.string.pref_title_bluetooth_playback), getString(R.string.pref_summary_bluetooth_playback), false, R.id.action_mainSettingsFragment_to_audioSettings),

            // Personalize Settings (R.id.personalizeSettings)
            SearchableSetting(R.id.personalizeSettings, getString(R.string.personalize), getString(R.string.personalize_settings_summary), true, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.home), null, true, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment), // Category title
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_home_artist_grid_style), null, false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_home_album_grid_style), null, false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_home_banner), getString(R.string.pref_summary_home_banner), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_suggestions), getString(R.string.pref_summary_suggestions), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_pause_history), getString(R.string.pref_summary_pause_history), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_header_library), null, true, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment), // Category title
            SearchableSetting(R.id.personalizeSettings, getString(R.string.library_categories), getString(R.string.pref_summary_library_categories), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_remember_tab), getString(R.string.pref_summary_remember_tab), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_enable_search_playlist), getString(R.string.pref_summary_enable_search_playlist), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_tab_text_mode), null, false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_appbar_mode), null, false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_show_song_menu_grid), getString(R.string.pref_summary_show_song_menu_grid), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_enable_song_title_marquee), getString(R.string.pref_summary_enable_song_title_marquee), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_show_covers_in_list_views), getString(R.string.pref_summary_show_covers_in_list_views), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, "Floating Action Button", null, true, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment), // Category title (direct string)
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_songs_fab_action), getString(R.string.pref_summary_songs_fab_action), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_artists_fab_action), getString(R.string.pref_summary_artists_fab_action), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_albums_fab_action), getString(R.string.pref_summary_albums_fab_action), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_show_fab_on_scroll), getString(R.string.pref_summary_show_fab_on_scroll), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.window), null, true, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment), // Category title
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_toggle_full_screen), getString(R.string.pref_summary_toggle_full_screen), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_show_songs_search_button), getString(R.string.pref_summary_show_songs_search_button), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_show_cast_button), getString(R.string.pref_summary_show_cast_button), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.keep_header_visible_title), getString(R.string.keep_header_visible_summary), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.hide_header_title), getString(R.string.hide_header_summary), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_header_now_playing), null, true, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment), // Category title
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_tap_on_title), getString(R.string.pref_summary_tap_on_title), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_tap_on_artist), getString(R.string.pref_summary_tap_on_artist), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_mini_player_scrolling), getString(R.string.pref_summary_mini_player_scrolling), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_mini_player_time), getString(R.string.pref_summary_mini_player_time), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_auto_hide_mini_player), getString(R.string.pref_summary_auto_hide_mini_player), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_header_lockscreen), null, true, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment), // Category title
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_album_art_on_lockscreen), getString(R.string.pref_summary_album_art_on_lockscreen), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_blurred_album_art), getString(R.string.pref_summary_blurred_album_art), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),
            SearchableSetting(R.id.personalizeSettings, getString(R.string.pref_title_lock_screen), getString(R.string.pref_summary_lock_screen), false, R.id.action_mainSettingsFragment_to_personalizeSettingsFragment),

            // Image Settings (R.id.imageSettings)
            SearchableSetting(R.id.imageSettings, getString(R.string.pref_header_images), getString(R.string.image_settings_summary), true, R.id.action_mainSettingsFragment_to_imageSettingFragment),
            SearchableSetting(R.id.imageSettings, getString(R.string.pref_title_ignore_media_store_artwork), getString(R.string.pref_summary_ignore_media_store_artwork), false, R.id.action_mainSettingsFragment_to_imageSettingFragment),
            SearchableSetting(R.id.imageSettings, getString(R.string.pref_title_auto_download_artist_images), null, false, R.id.action_mainSettingsFragment_to_imageSettingFragment),

            // Notification Settings (R.id.notificationSettings)
            SearchableSetting(R.id.notificationSettings, getString(R.string.notification), getString(R.string.notification_settings_summary), true, R.id.action_mainSettingsFragment_to_notificationSettingsFragment),
            SearchableSetting(R.id.notificationSettings, getString(R.string.pref_title_classic_notification), getString(R.string.pref_summary_classic_notification), false, R.id.action_mainSettingsFragment_to_notificationSettingsFragment),
            SearchableSetting(R.id.notificationSettings, getString(R.string.pref_title_colored_notification), getString(R.string.pref_summary_colored_notification), false, R.id.action_mainSettingsFragment_to_notificationSettingsFragment),

            // Other Settings (R.id.otherSettings)
            SearchableSetting(R.id.otherSettings, getString(R.string.others), getString(R.string.other_settings_summary), true, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_header_blacklist), getString(R.string.pref_summary_blacklist), false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_title_whitelist), getString(R.string.pref_summary_whitelist), false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_header_playlists), null, true, R.id.action_mainSettingsFragment_to_otherSettingsFragment), // Category title
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_title_last_added_interval), null, false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_header_advanced), null, true, R.id.action_mainSettingsFragment_to_otherSettingsFragment), // Category title
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_filter_song_title), getString(R.string.pref_filter_song_summary), false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_keep_pause_on_zero_volume_title), getString(R.string.pref_keep_pause_on_zero_volume_summary), false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_keep_screen_on_title), getString(R.string.pref_keep_screen_on_summary), false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_show_when_locked_title), getString(R.string.pref_show_when_locked_summary), false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_title_offline_mode), getString(R.string.pref_summary_offline_mode), false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_title_show_song_only), getString(R.string.pref_summary_show_song_only), false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_title_fix_year), getString(R.string.pref_summary_fix_year), false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),
            SearchableSetting(R.id.otherSettings, getString(R.string.pref_language_name), null, false, R.id.action_mainSettingsFragment_to_otherSettingsFragment),

            // Now Playing Settings (R.id.nowPlayingSettings)
            SearchableSetting(R.id.nowPlayingSettings, getString(R.string.now_playing), getString(R.string.now_playing_summary), true, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Now Playing Screen Appearance", null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment), // Direct string
            SearchableSetting(R.id.nowPlayingSettings, "Artwork Click Action", null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment), // Direct string
            SearchableSetting(R.id.nowPlayingSettings, "Snowfall", null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment), // Direct string
            SearchableSetting(R.id.nowPlayingSettings, "Lyrics Type", null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment), // Direct string
            SearchableSetting(R.id.nowPlayingSettings, "Keep Screen On for Lyrics", getString(R.string.pref_keep_screen_on_summary), false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Circle Play Button", null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment), // Direct string
            SearchableSetting(R.id.nowPlayingSettings, "Swipe Anywhere to Control", getString(R.string.pref_summary_swipe_anywhere_now_playing), false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "General", null, true, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment), // Category title (direct string)
            SearchableSetting(R.id.nowPlayingSettings, getString(R.string.pref_title_album_cover_style), null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, getString(R.string.pref_title_album_cover_transform), null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, getString(R.string.pref_title_toggle_carousel_effect), getString(R.string.pref_summary_carousel_effect), false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Controls", null, true, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment), // Category title (direct string)
            SearchableSetting(R.id.nowPlayingSettings, "Swipe to Dismiss", getString(R.string.pref_summary_swipe_to_dismiss), false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Disable Swipe Down to Close", "Disable the swipe down gesture to close the expanded player. The back button will be required to close.", false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Extra Controls", getString(R.string.pref_summary_extra_controls), false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Toggle Volume", getString(R.string.pref_summary_toggle_volume), false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Expand Now Playing Panel", getString(R.string.pref_summary_expand_now_playing_panel), false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Blur Amount", getString(R.string.pref_blur_amount_summary), false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Action Buttons", null, true, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment), // Category title (direct string)
            SearchableSetting(R.id.nowPlayingSettings, "Show 'Sleep timer' Button", null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Show 'Lyrics' Button", null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Show 'Add to favorites' Button", null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Show 'Now playing queue' Button", null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),
            SearchableSetting(R.id.nowPlayingSettings, "Show 'More options' Menu", null, false, R.id.action_mainSettingsFragment_to_nowPlayingSettingsFragment),

            // Backup/Restore Settings (R.id.backup_restore_settings)
            SearchableSetting(R.id.backup_restore_settings, getString(R.string.backup_restore_title), getString(R.string.backup_restore_settings_summary), true, R.id.action_mainSettingsFragment_to_backupFragment),

            // About Settings (R.id.aboutSettings)
            SearchableSetting(R.id.aboutSettings, getString(R.string.action_about), getString(R.string.about_settings_summary), true, R.id.action_mainSettingsFragment_to_aboutActivity)
        )

        binding.generalSettings.setOnClickListener(this)
        binding.audioSettings.setOnClickListener(this)
        binding.nowPlayingSettings.setOnClickListener(this)
        binding.personalizeSettings.setOnClickListener(this)
        binding.imageSettings.setOnClickListener(this)
        binding.notificationSettings.setOnClickListener(this)
        binding.otherSettings.setOnClickListener(this)
        binding.aboutSettings.setOnClickListener(this)
        binding.backupRestoreSettings.setOnClickListener(this)

        binding.searchView.setOnQueryTextListener(null)
        
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                filterSettings(newText)
                // Enable the back button callback if there's a search query, disable otherwise
                onBackPressedCallback.isEnabled = !newText.isNullOrEmpty()
                return true
            }
        })

        binding.container.drawAboveSystemBarsWithPadding()
    }

    private fun filterSettings(query: String?) {
        val searchText = query.orEmpty().lowercase()

        // First, remove all previously added search result views
        currentSearchResultViews.forEach {
            (it.parent as? ViewGroup)?.removeView(it)
        }
        currentSearchResultViews.clear() // Clear the list of references

        if (searchText.isEmpty()) {
            // If query is empty, show all original category views and hide any dynamically added ones
            originalSettingViews.forEach {
                it.visibility = View.VISIBLE
                it.setSearchQuery(null)
            }
        } else {
            val matchedCategoryIds = mutableSetOf<Int>()
            val matchedSettings = mutableListOf<SearchableSetting>()

            // Find all matching settings
            allSearchableSettings.forEach { setting ->
                val titleMatches = setting.title.lowercase().contains(searchText)
                val summaryMatches = setting.summary?.lowercase()?.contains(searchText) ?: false

                if (titleMatches || summaryMatches) {
                    matchedSettings.add(setting)
                    matchedCategoryIds.add(setting.id)
                }
            }

            // Hide all original category views initially
            originalSettingViews.forEach { it.visibility = View.GONE }

            // Display categories and their matching sub-settings
            // Iterate through originalSettingViews to maintain category order
            originalSettingViews.forEach { originalCategoryView ->
                if (matchedCategoryIds.contains(originalCategoryView.id)) {
                    // Show the category view if it contains matching sub-settings
                    originalCategoryView.visibility = View.VISIBLE
                    originalCategoryView.setSearchQuery(query) // Highlight category title/summary if it matches

                    // Add matching sub-settings under this category
                    matchedSettings.filter { it.id == originalCategoryView.id && !it.isCategory }
                        .forEach { matchedSetting ->
                            val newSettingView = SettingListItemView(requireContext())
                            newSettingView.settingListItemTitle = matchedSetting.title
                            newSettingView.settingListItemText = matchedSetting.summary
                            newSettingView.setSearchQuery(query)

                            // Set click listener for the new setting view
                            matchedSetting.navigationAction?.let { actionId ->
                                newSettingView.setOnClickListener {
                                    // Clear search and reset view BEFORE navigating
                                    binding.searchView.setQuery("", false) // Clear the search query
                                    filterSettings("") // Reset to original settings display

                                    val navOptions = NavOptions.Builder()
                                        .setPopUpTo(R.id.mainSettingsFragment, true) // Pop up to mainSettingsFragment inclusively
                                        .setEnterAnim(R.anim.retro_fragment_open_enter) // Keep existing animations
                                        .setExitAnim(R.anim.retro_fragment_open_exit)
                                        .setPopEnterAnim(R.anim.retro_fragment_close_enter)
                                        .setPopExitAnim(R.anim.retro_fragment_close_exit)
                                        .build()
                                    findNavController().navigate(actionId, null, navOptions)
                                }
                            }

                            // Add padding for indentation
                            val paddingPx = resources.getDimensionPixelSize(R.dimen.list_item_padding_left_indent)
                            newSettingView.setPadding(paddingPx, newSettingView.paddingTop, newSettingView.paddingRight, newSettingView.paddingBottom)

                            // Add the new view to the container and keep a reference
                            binding.container.addView(newSettingView)
                            currentSearchResultViews.add(newSettingView) // Store reference
                        }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentSearchResultViews.clear()
        originalSettingViews.clear()
        _binding = null
    }
}
