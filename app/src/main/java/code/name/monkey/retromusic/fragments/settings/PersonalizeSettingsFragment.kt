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

import android.os.Bundle
import android.view.View
import code.name.monkey.appthemehelper.common.prefs.supportv7.ATEListPreference
import code.name.monkey.appthemehelper.common.prefs.supportv7.ATESwitchPreference
import androidx.preference.PreferenceManager
import code.name.monkey.appthemehelper.util.VersionUtils
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import code.name.monkey.retromusic.*
import androidx.preference.Preference
import androidx.navigation.fragment.findNavController
import code.name.monkey.retromusic.util.PreferenceUtil

class PersonalizeSettingsFragment : AbsSettingsFragment() {

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_ui)
        // Hide Blur album art preference on Android 11+ devices as the lockscreen album art feature was removed by Google
        // And if the feature is present in some Custom ROM's there is also an option to set blur so this preference is unnecessary on Android 11 and above
        val blurredAlbumArt: ATESwitchPreference? = findPreference(BLURRED_ALBUM_ART)
        blurredAlbumArt?.isVisible = !VersionUtils.hasR()
    }

    override fun invalidateSettings() {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val albumArtOnLockscreen: ATESwitchPreference? = findPreference(ALBUM_ART_ON_LOCK_SCREEN)
        albumArtOnLockscreen?.isVisible = !VersionUtils.hasT()

        val homeArtistStyle: ATEListPreference? = findPreference(HOME_ARTIST_GRID_STYLE)
        homeArtistStyle?.setOnPreferenceChangeListener { preference, newValue ->
            setSummary(preference, newValue)
            true
        }
        setSummary(homeArtistStyle)
        val homeAlbumStyle: ATEListPreference? = findPreference(HOME_ALBUM_GRID_STYLE)
        homeAlbumStyle?.setOnPreferenceChangeListener { preference, newValue ->
            setSummary(preference, newValue)
            true
        }
        setSummary(homeAlbumStyle)
        val tabTextMode: ATEListPreference? = findPreference(TAB_TEXT_MODE)
        tabTextMode?.setOnPreferenceChangeListener { prefs, newValue ->
            setSummary(prefs, newValue)
            true
        }
        setSummary(tabTextMode)
        val appBarMode: ATEListPreference? = findPreference(APPBAR_MODE)
        appBarMode?.setOnPreferenceChangeListener { _, newValue ->
            val currentAppBarMode = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(APPBAR_MODE, "1")
            if (newValue.toString() != currentAppBarMode) {
                restartActivity()
            }
            true
        }
        setSummary(appBarMode)

        val toggleHomeBanner: ATESwitchPreference? = findPreference("toggle_home_banner")
        toggleHomeBanner?.title = getString(R.string.pref_title_home_banner)

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    // Persist the URI
                    PreferenceUtil.customFallbackArtworkUri = it.toString()
                    Toast.makeText(requireContext(), "Custom fallback artwork set!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findPreference<Preference>("custom_fallback_artwork")?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
            true
        }

        findPreference<Preference>("reset_custom_fallback_artwork")?.setOnPreferenceClickListener {
            PreferenceUtil.customFallbackArtworkUri = null
            Toast.makeText(requireContext(), "Custom fallback artwork reset to default!", Toast.LENGTH_SHORT).show()
            true
        }
    }
}
