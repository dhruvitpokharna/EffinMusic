package code.name.monkey.retromusic.util

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.content.res.use
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.*
import code.name.monkey.retromusic.extensions.getIntRes
import code.name.monkey.retromusic.extensions.getStringOrDefault
import code.name.monkey.retromusic.fragments.AlbumCoverStyle
import code.name.monkey.retromusic.fragments.GridStyle
import code.name.monkey.retromusic.fragments.NowPlayingScreen
import code.name.monkey.retromusic.fragments.folder.FoldersFragment
import code.name.monkey.retromusic.helper.SortOrder.*
import code.name.monkey.retromusic.model.CategoryInfo
import code.name.monkey.retromusic.model.MetadataField
import code.name.monkey.retromusic.transform.*
import code.name.monkey.retromusic.util.theme.ThemeMode
import code.name.monkey.retromusic.views.TopAppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import code.name.monkey.retromusic.DISABLE_SWIPE_DOWN_TO_DISMISS
import java.io.File

object PreferenceUtil {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.getContext())

    val defaultCategories = listOf(
        CategoryInfo(CategoryInfo.Category.Home, true),
        CategoryInfo(CategoryInfo.Category.Songs, true),
        CategoryInfo(CategoryInfo.Category.Albums, true),
        CategoryInfo(CategoryInfo.Category.Artists, true),
        CategoryInfo(CategoryInfo.Category.Playlists, true),
        CategoryInfo(CategoryInfo.Category.Genres, false),
        CategoryInfo(CategoryInfo.Category.Folder, false),
        CategoryInfo(CategoryInfo.Category.Search, false)
    )

    private val libraryCategoryType = object : TypeToken<List<CategoryInfo>>() {}.type

    var libraryCategory: List<CategoryInfo>
        get() {
            val gson = Gson()
            val data = sharedPreferences.getStringOrDefault(
                LIBRARY_CATEGORIES,
                gson.toJson(defaultCategories, libraryCategoryType)
            )
            return try {
                Gson().fromJson(data, libraryCategoryType)
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                return defaultCategories
            }
        }
        set(value) {
            val gson = Gson()
            sharedPreferences.edit {
                putString(LIBRARY_CATEGORIES, gson.toJson(value, libraryCategoryType))
            }
        }

    fun registerOnSharedPreferenceChangedListener(
        listener: OnSharedPreferenceChangeListener,
    ) = sharedPreferences.registerOnSharedPreferenceChangeListener(listener)


    fun unregisterOnSharedPreferenceChangedListener(
        changeListener: OnSharedPreferenceChangeListener,
    ) = sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener)


    val baseTheme get() = sharedPreferences.getStringOrDefault(GENERAL_THEME, "auto")

    fun getGeneralThemeValue(isSystemDark: Boolean): ThemeMode {
        val themeMode: String =
            sharedPreferences.getStringOrDefault(GENERAL_THEME, "auto")
        return if (isBlackMode && isSystemDark && themeMode != "light") {
            ThemeMode.BLACK
        } else {
            if (isBlackMode && themeMode == "dark") {
                ThemeMode.BLACK
            } else {
                when (themeMode) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    "auto" -> ThemeMode.AUTO
                    else -> ThemeMode.AUTO
                }
            }
        }
    }

    var languageCode: String
        get() = sharedPreferences.getString(LANGUAGE_NAME, "auto") ?: "auto"
        set(value) = sharedPreferences.edit {
            putString(LANGUAGE_NAME, value)
        }

    var isLocaleAutoStorageEnabled: Boolean
        get() = sharedPreferences.getBoolean(
            LOCALE_AUTO_STORE_ENABLED,
            false
        )
        set(value) = sharedPreferences.edit {
            putBoolean(LOCALE_AUTO_STORE_ENABLED, value)
        }

    var Fragment.userName
        get() = sharedPreferences.getString(
            USER_NAME,
            getString(R.string.user_name)
        )
        set(value) = sharedPreferences.edit {
            putString(USER_NAME, value)
        }

    var safSdCardUri
        get() = sharedPreferences.getStringOrDefault(SAF_SDCARD_URI, "")
        set(value) = sharedPreferences.edit {
            putString(SAF_SDCARD_URI, value)
        }

    private val autoDownloadImagesPolicy
        get() = sharedPreferences.getStringOrDefault(
            AUTO_DOWNLOAD_IMAGES_POLICY,
            "only_wifi"
        )

    var albumArtistsOnly
        get() = sharedPreferences.getBoolean(
            ALBUM_ARTISTS_ONLY,
            false
        )
        set(value) = sharedPreferences.edit { putBoolean(ALBUM_ARTISTS_ONLY, value) }

    var albumDetailSongSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ALBUM_DETAIL_SONG_SORT_ORDER,
            AlbumSongSortOrder.SONG_TRACK_LIST
        )
        set(value) = sharedPreferences.edit { putString(ALBUM_DETAIL_SONG_SORT_ORDER, value) }

    var artistDetailSongSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ARTIST_DETAIL_SONG_SORT_ORDER,
            ArtistSongSortOrder.SONG_A_Z
        )
        set(value) = sharedPreferences.edit { putString(ARTIST_DETAIL_SONG_SORT_ORDER, value) }

    var songSortOrder
        get() = sharedPreferences.getStringOrDefault(
            SONG_SORT_ORDER,
            SongSortOrder.SONG_A_Z
        )
        set(value) = sharedPreferences.edit {
            putString(SONG_SORT_ORDER, value)
        }

    var albumSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ALBUM_SORT_ORDER,
            AlbumSortOrder.ALBUM_A_Z
        )
        set(value) = sharedPreferences.edit {
            putString(ALBUM_SORT_ORDER, value)
        }


    var artistSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ARTIST_SORT_ORDER,
            ArtistSortOrder.ARTIST_A_Z
        )
        set(value) = sharedPreferences.edit {
            putString(ARTIST_SORT_ORDER, value)
        }

    val albumSongSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ALBUM_SONG_SORT_ORDER,
            AlbumSongSortOrder.SONG_TRACK_LIST
        )

    val artistSongSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ARTIST_SONG_SORT_ORDER,
            AlbumSongSortOrder.SONG_TRACK_LIST
        )

    var artistAlbumSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ARTIST_ALBUM_SORT_ORDER,
            ArtistAlbumSortOrder.ALBUM_YEAR
        )
        set(value) = sharedPreferences.edit {
            putString(ARTIST_ALBUM_SORT_ORDER, value)
        }

    var playlistSortOrder
        get() = sharedPreferences.getStringOrDefault(
            PLAYLIST_SORT_ORDER,
            PlaylistSortOrder.PLAYLIST_A_Z
        )
        set(value) = sharedPreferences.edit {
            putString(PLAYLIST_SORT_ORDER, value)
        }

    val genreSortOrder
        get() = sharedPreferences.getStringOrDefault(
            GENRE_SORT_ORDER,
            GenreSortOrder.GENRE_A_Z
        )

    val isIgnoreMediaStoreArtwork
        get() = sharedPreferences.getBoolean(
            IGNORE_MEDIA_STORE_ARTWORK,
            false
        )

    val isVolumeVisibilityMode
        get() = sharedPreferences.getBoolean(
            TOGGLE_VOLUME, false
        )

    var isInitializedBlacklist
        get() = sharedPreferences.getBoolean(
            INITIALIZED_BLACKLIST, false
        )
        set(value) = sharedPreferences.edit {
            putBoolean(INITIALIZED_BLACKLIST, value)
        }

    private val isBlackMode
        get() = sharedPreferences.getBoolean(
            BLACK_THEME, false
        )

    val isExtraControls
        get() = sharedPreferences.getBoolean(
            TOGGLE_ADD_CONTROLS, false
        )

    val isHomeBanner
        get() = sharedPreferences.getBoolean(
            TOGGLE_HOME_BANNER, false
        )
    var isClassicNotification
        get() = sharedPreferences.getBoolean(CLASSIC_NOTIFICATION, false)
        set(value) = sharedPreferences.edit { putBoolean(CLASSIC_NOTIFICATION, value) }

    val isScreenOnEnabled get() = sharedPreferences.getBoolean(KEEP_SCREEN_ON, false)

    val isShowWhenLockedEnabled get() = sharedPreferences.getBoolean(SHOW_WHEN_LOCKED, false)

    val isSongInfo get() = sharedPreferences.getBoolean(EXTRA_SONG_INFO, false)

    val isPauseOnZeroVolume get() = sharedPreferences.getBoolean(PAUSE_ON_ZERO_VOLUME, false)

    var isSleepTimerFinishMusic
        get() = sharedPreferences.getBoolean(
            SLEEP_TIMER_FINISH_SONG, false
        )
        set(value) = sharedPreferences.edit {
            putBoolean(SLEEP_TIMER_FINISH_SONG, value)
        }

    val isExpandPanel get() = sharedPreferences.getBoolean(EXPAND_NOW_PLAYING_PANEL, false)

    val isHeadsetPlugged
        get() = sharedPreferences.getBoolean(
            TOGGLE_HEADSET, false
        )

    val isAlbumArtOnLockScreen
        get() = sharedPreferences.getBoolean(
            ALBUM_ART_ON_LOCK_SCREEN, true
        )

    val isBluetoothSpeaker
        get() = sharedPreferences.getBoolean(
            BLUETOOTH_PLAYBACK, false
        )

    val isBlurredAlbumArt
        get() = sharedPreferences.getBoolean(
            BLURRED_ALBUM_ART, false
        ) && !VersionUtils.hasR()

    val blurAmount get() = sharedPreferences.getInt(NEW_BLUR_AMOUNT, 25)

    val isCarouselEffect
        get() = sharedPreferences.getBoolean(
            CAROUSEL_EFFECT, false
        )

    var isColoredAppShortcuts
        get() = sharedPreferences.getBoolean(
            COLORED_APP_SHORTCUTS, true
        )
        set(value) = sharedPreferences.edit {
            putBoolean(COLORED_APP_SHORTCUTS, value)
        }

    var isColoredNotification
        get() = sharedPreferences.getBoolean(
            COLORED_NOTIFICATION, true
        )
        set(value) = sharedPreferences.edit {
            putBoolean(COLORED_NOTIFICATION, value)
        }

    var isDesaturatedColor
        get() = sharedPreferences.getBoolean(
            DESATURATED_COLOR, false
        )
        set(value) = sharedPreferences.edit {
            putBoolean(DESATURATED_COLOR, value)
        }

    val isGapLessPlayback
        get() = sharedPreferences.getBoolean(
            GAP_LESS_PLAYBACK, false
        )

    val isAdaptiveColor
        get() = sharedPreferences.getBoolean(
            ADAPTIVE_COLOR_APP, false
        )

    val isFullScreenMode
        get() = sharedPreferences.getBoolean(
            TOGGLE_FULL_SCREEN, false
        )

    val isAudioFocusEnabled
        get() = sharedPreferences.getBoolean(
            MANAGE_AUDIO_FOCUS, false
        )

    val isLockScreen get() = sharedPreferences.getBoolean(LOCK_SCREEN, false)

    @Suppress("deprecation")
    fun isAllowedToDownloadMetadata(context: Context): Boolean {
        return when (autoDownloadImagesPolicy) {
            "always" -> true
            "only_wifi" -> {
                val connectivityManager = context.getSystemService<ConnectivityManager>()
                if (VersionUtils.hasMarshmallow()) {
                    val network = connectivityManager?.activeNetwork
                    val capabilities = connectivityManager?.getNetworkCapabilities(network)
                    capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                } else {
                    val netInfo = connectivityManager?.activeNetworkInfo
                    netInfo != null && netInfo.type == ConnectivityManager.TYPE_WIFI && netInfo.isConnectedOrConnecting
                }
            }
            "never" -> false
            else -> false
        }
    }


    var lyricsOption
        get() = sharedPreferences.getInt(LYRICS_OPTIONS, 1)
        set(value) = sharedPreferences.edit {
            putInt(LYRICS_OPTIONS, value)
        }

    var songGridStyle: GridStyle
        get() {
            val id: Int = sharedPreferences.getInt(SONG_GRID_STYLE, 0)
            // We can directly use "first" kotlin extension function here but
            // there maybe layout id stored in this so to avoid a crash we use
            // "firstOrNull"
            return GridStyle.values().firstOrNull { gridStyle ->
                gridStyle.id == id
            } ?: GridStyle.Grid
        }
        set(value) = sharedPreferences.edit {
            putInt(SONG_GRID_STYLE, value.id)
        }

    var albumGridStyle: GridStyle
        get() {
            val id: Int = sharedPreferences.getInt(ALBUM_GRID_STYLE, 0)
            return GridStyle.values().firstOrNull { gridStyle ->
                gridStyle.id == id
            } ?: GridStyle.Grid
        }
        set(value) = sharedPreferences.edit {
            putInt(ALBUM_GRID_STYLE, value.id)
        }

    var artistGridStyle: GridStyle
        get() {
            val id: Int = sharedPreferences.getInt(ARTIST_GRID_STYLE, 3)
            return GridStyle.values().firstOrNull { gridStyle ->
                gridStyle.id == id
            } ?: GridStyle.Circular
        }
        set(value) = sharedPreferences.edit {
            putInt(ARTIST_GRID_STYLE, value.id)
        }

    val filterLength get() = sharedPreferences.getInt(FILTER_SONG, 20)

    var lastVersion
        // This was stored as an integer before now it's a long, so avoid a ClassCastException
        get() = try {
            sharedPreferences.getLong(LAST_CHANGELOG_VERSION, 0)
        } catch (e: ClassCastException) {
            sharedPreferences.edit { remove(LAST_CHANGELOG_VERSION) }
            0
        }
        set(value) = sharedPreferences.edit {
            putLong(LAST_CHANGELOG_VERSION, value)
        }

    var lastSleepTimerValue
        get() = sharedPreferences.getInt(
            LAST_SLEEP_TIMER_VALUE,
            30
        )
        set(value) = sharedPreferences.edit {
            putInt(LAST_SLEEP_TIMER_VALUE, value)
        }


    var nextSleepTimerElapsedRealTime
        get() = sharedPreferences.getInt(
            NEXT_SLEEP_TIMER_ELAPSED_REALTIME,
            -1
        )
        set(value) = sharedPreferences.edit {
            putInt(NEXT_SLEEP_TIMER_ELAPSED_REALTIME, value)
        }

    fun themeResFromPrefValue(themePrefValue: String): Int {
        return when (themePrefValue) {
            "light" -> R.style.Theme_RetroMusic_Light
            "dark" -> R.style.Theme_RetroMusic
            else -> R.style.Theme_RetroMusic
        }
    }

    val homeArtistGridStyle: Int
        get() {
            val position = sharedPreferences.getStringOrDefault(
                HOME_ARTIST_GRID_STYLE, "0"
            ).toInt()
            val layoutRes =
                App.getContext().resources.obtainTypedArray(R.array.pref_home_grid_style_layout)
                    .use {
                        it.getResourceId(position, 0)
                    }
            return if (layoutRes == 0) {
                R.layout.item_artist
            } else layoutRes
        }

    val homeAlbumGridStyle: Int
        get() {
            val position = sharedPreferences.getStringOrDefault(
                HOME_ALBUM_GRID_STYLE, "4"
            ).toInt()
            val layoutRes = App.getContext()
                .resources.obtainTypedArray(R.array.pref_home_grid_style_layout).use {
                    it.getResourceId(position, 0)
                }
            return if (layoutRes == 0) {
                R.layout.item_image
            } else layoutRes
        }

    val tabTitleMode: Int
        get() {
            return when (sharedPreferences.getStringOrDefault(
                TAB_TEXT_MODE, "0"
            ).toInt()) {
                0 -> BottomNavigationView.LABEL_VISIBILITY_AUTO
                1 -> BottomNavigationView.LABEL_VISIBILITY_LABELED
                2 -> BottomNavigationView.LABEL_VISIBILITY_SELECTED
                3 -> BottomNavigationView.LABEL_VISIBILITY_UNLABELED
                else -> BottomNavigationView.LABEL_VISIBILITY_LABELED
            }
        }


    var songGridSize
        get() = sharedPreferences.getInt(
            SONG_GRID_SIZE,
            App.getContext().getIntRes(R.integer.default_list_columns)
        )
        set(value) = sharedPreferences.edit {
            putInt(SONG_GRID_SIZE, value)
        }

    var songGridSizeLand
        get() = sharedPreferences.getInt(
            SONG_GRID_SIZE_LAND,
            App.getContext().getIntRes(R.integer.default_grid_columns_land)
        )
        set(value) = sharedPreferences.edit {
            putInt(SONG_GRID_SIZE_LAND, value)
        }


    var albumGridSize: Int
        get() = sharedPreferences.getInt(
            ALBUM_GRID_SIZE,
            App.getContext().getIntRes(R.integer.default_grid_columns)
        )
        set(value) = sharedPreferences.edit {
            putInt(ALBUM_GRID_SIZE, value)
        }


    var albumGridSizeLand
        get() = sharedPreferences.getInt(
            ALBUM_GRID_SIZE_LAND,
            App.getContext().getIntRes(R.integer.default_grid_columns_land)
        )
        set(value) = sharedPreferences.edit {
            putInt(ALBUM_GRID_SIZE_LAND, value)
        }


    var artistGridSize
        get() = sharedPreferences.getInt(
            ARTIST_GRID_SIZE,
            App.getContext().getIntRes(R.integer.default_grid_columns)
        )
        set(value) = sharedPreferences.edit {
            putInt(ARTIST_GRID_SIZE, value)
        }


    var artistGridSizeLand
        get() = sharedPreferences.getInt(
            ARTIST_GRID_SIZE_LAND,
            App.getContext().getIntRes(R.integer.default_grid_columns_land)
        )
        set(value) = sharedPreferences.edit {
            putInt(ALBUM_GRID_SIZE_LAND, value)
        }


    var playlistGridSize
        get() = sharedPreferences.getInt(
            PLAYLIST_GRID_SIZE,
            App.getContext().getIntRes(R.integer.default_grid_columns)
        )
        set(value) = sharedPreferences.edit {
            putInt(PLAYLIST_GRID_SIZE, value)
        }


    var playlistGridSizeLand
        get() = sharedPreferences.getInt(
            PLAYLIST_GRID_SIZE_LAND,
            App.getContext().getIntRes(R.integer.default_grid_columns_land)
        )
        set(value) = sharedPreferences.edit {
            putInt(PLAYLIST_GRID_SIZE, value)
        }

    var albumCoverStyle: AlbumCoverStyle
        get() {
            val id: Int = sharedPreferences.getInt(ALBUM_COVER_STYLE, 0)
            for (albumCoverStyle in AlbumCoverStyle.values()) {
                if (albumCoverStyle.id == id) {
                    return albumCoverStyle
                }
            }
            return AlbumCoverStyle.Card
        }
        set(value) = sharedPreferences.edit { putInt(ALBUM_COVER_STYLE, value.id) }


    var nowPlayingScreen: NowPlayingScreen
        get() {
            val id: Int = sharedPreferences.getInt(NOW_PLAYING_SCREEN_ID, 0)
            for (nowPlayingScreen in NowPlayingScreen.values()) {
                if (nowPlayingScreen.id == id) {
                    return nowPlayingScreen
                }
            }
            return NowPlayingScreen.Adaptive
        }
        set(value) = sharedPreferences.edit {
            putInt(NOW_PLAYING_SCREEN_ID, value.id)
            // Also set a cover theme for that now playing
            value.defaultCoverTheme?.let { coverTheme -> albumCoverStyle = coverTheme }
        }

    val albumCoverTransform: ViewPager.PageTransformer
        get() {
            val style = sharedPreferences.getStringOrDefault(
                ALBUM_COVER_TRANSFORM,
                "0"
            ).toInt()
            return when (style) {
                0 -> NormalPageTransformer()
                1 -> CascadingPageTransformer()
                2 -> DepthTransformation()
                3 -> HorizontalFlipTransformation()
                4 -> VerticalFlipTransformation()
                5 -> HingeTransformation()
                6 -> VerticalStackTransformer()
                else -> ViewPager.PageTransformer { _, _ -> }
            }
        }

    var startDirectory: File
        get() {
            val folderPath = FoldersFragment.defaultStartDirectory.path
            val filePath: String = sharedPreferences.getStringOrDefault(START_DIRECTORY, folderPath)
            return File(filePath)
        }
        set(value) = sharedPreferences.edit {
            putString(
                START_DIRECTORY,
                FileUtil.safeGetCanonicalPath(value)
            )
        }

    fun getRecentlyPlayedCutoffTimeMillis(): Long {
        val calendarUtil = CalendarUtil()
        val interval: Long = when (sharedPreferences.getString(RECENTLY_PLAYED_CUTOFF, "")) {
            "today" -> calendarUtil.elapsedToday
            "this_week" -> calendarUtil.elapsedWeek
            "past_seven_days" -> calendarUtil.getElapsedDays(7)
            "past_three_months" -> calendarUtil.getElapsedMonths(3)
            "this_year" -> calendarUtil.elapsedYear
            "this_month" -> calendarUtil.elapsedMonth
            else -> calendarUtil.elapsedMonth
        }
        return System.currentTimeMillis() - interval
    }

    val lastAddedCutoff: Long
        get() {
            val calendarUtil = CalendarUtil()
            val interval =
                when (sharedPreferences.getStringOrDefault(LAST_ADDED_CUTOFF, "this_month")) {
                    "today" -> calendarUtil.elapsedToday
                    "this_week" -> calendarUtil.elapsedWeek
                    "past_three_months" -> calendarUtil.getElapsedMonths(3)
                    "this_year" -> calendarUtil.elapsedYear
                    "this_month" -> calendarUtil.elapsedMonth
                    else -> calendarUtil.elapsedMonth
                }
            return (System.currentTimeMillis() - interval) / 1000
        }

    val homeSuggestions: Boolean
        get() = sharedPreferences.getBoolean(
            TOGGLE_SUGGESTIONS,
            true
        )

    val pauseHistory: Boolean
        get() = sharedPreferences.getBoolean(
            PAUSE_HISTORY,
            false
        )

    var audioFadeDuration
        get() = sharedPreferences
            .getInt(AUDIO_FADE_DURATION, 0)
        set(value) = sharedPreferences.edit { putInt(AUDIO_FADE_DURATION, value) }

    var showLyrics: Boolean
        get() = sharedPreferences.getBoolean(SHOW_LYRICS, false)
        set(value) = sharedPreferences.edit { putBoolean(SHOW_LYRICS, value) }

    val rememberLastTab: Boolean
        get() = sharedPreferences.getBoolean(REMEMBER_LAST_TAB, true)

    val enableSearchPlaylist: Boolean
        get() = sharedPreferences.getBoolean(ENABLE_SEARCH_PLAYLIST, true)

    var lastTab: Int
        get() = sharedPreferences
            .getInt(LAST_USED_TAB, 0)
        set(value) = sharedPreferences.edit { putInt(LAST_USED_TAB, value) }

    val isWhiteList: Boolean
        get() = sharedPreferences.getBoolean(WHITELIST_MUSIC, false)

    val crossFadeDuration
        get() = sharedPreferences
            .getInt(CROSS_FADE_DURATION, 0)

    val isCrossfadeEnabled get() = crossFadeDuration > 0

    val materialYou
        get() = sharedPreferences.getBoolean(MATERIAL_YOU, VersionUtils.hasS())

    val isCustomFont
        get() = sharedPreferences.getBoolean(CUSTOM_FONT, false)

    val isSnowFalling
        get() = sharedPreferences.getBoolean(SNOWFALL, false)

    val lyricsType: CoverLyricsType
        get() = if (sharedPreferences.getString(LYRICS_TYPE, "0") == "0") {
            CoverLyricsType.REPLACE_COVER
        } else {
            CoverLyricsType.OVER_COVER
        }

    var playbackSpeed
        get() = sharedPreferences
            .getFloat(PLAYBACK_SPEED, 1F)
        set(value) = sharedPreferences.edit { putFloat(PLAYBACK_SPEED, value) }

    var playbackPitch
        get() = sharedPreferences
            .getFloat(PLAYBACK_PITCH, 1F)
        set(value) = sharedPreferences.edit { putFloat(PLAYBACK_PITCH, value) }

    val appBarMode: TopAppBarLayout.AppBarMode
        get() = if (sharedPreferences.getString(APPBAR_MODE, "1") == "0") {
            TopAppBarLayout.AppBarMode.COLLAPSING
        } else {
            TopAppBarLayout.AppBarMode.SIMPLE
        }

    val wallpaperAccent
        get() = sharedPreferences.getBoolean(
            WALLPAPER_ACCENT,
            VersionUtils.hasOreoMR1() && !VersionUtils.hasS()
        )

    val lyricsScreenOn
        get() = sharedPreferences.getBoolean(SCREEN_ON_LYRICS, false)

    val circlePlayButton
        get() = sharedPreferences.getBoolean(CIRCLE_PLAY_BUTTON, false)

    val swipeAnywhereToChangeSong
        get() = sharedPreferences.getBoolean(SWIPE_ANYWHERE_NOW_PLAYING, true)

    val swipeDownToDismiss
        get() = sharedPreferences.getBoolean(SWIPE_DOWN_DISMISS, true)

    const val HIDE_ALL_ACTION_BUTTONS = "hide_action_buttons"
    const val SHOW_SLEEP_TIMER_BUTTON = "show_sleep_timer_button"
    const val SHOW_LYRICS_BUTTON = "show_lyrics_button"
    const val SHOW_FAVORITE_BUTTON = "show_favorite_button"


    const val SONGS_FAB_ACTION = "songs_fab_action"
    const val ARTISTS_FAB_ACTION = "artists_fab_action"
    const val ALBUMS_FAB_ACTION = "albums_fab_action"

    const val FAB_ACTION_SHUFFLE = "shuffle"
    const val FAB_ACTION_SEARCH = "search"
    const val FAB_ACTION_PLAY_NEXT = "play_next"
    const val FAB_ACTION_DISABLED = "disabled"

    val hideAllActionButtons: Boolean
        get() = sharedPreferences.getBoolean(
            HIDE_ALL_ACTION_BUTTONS, false
        )

    val showSleepTimerButton: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_SLEEP_TIMER_BUTTON, false
        )

    val showLyricsButton: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_LYRICS_BUTTON, false
        )

    val showFavoriteButton: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_FAVORITE_BUTTON, false
        )

    var songsFabAction: String
        get() = sharedPreferences.getString(SONGS_FAB_ACTION, null) ?: FAB_ACTION_SHUFFLE
        set(value) {
            sharedPreferences.edit {
                putString(SONGS_FAB_ACTION, value)
            }
        }

    var artistsFabAction: String
        get() = sharedPreferences.getString(ARTISTS_FAB_ACTION, null) ?: FAB_ACTION_SHUFFLE
        set(value) {
            sharedPreferences.edit {
                putString(ARTISTS_FAB_ACTION, value)
            }
        }

    var albumsFabAction: String
        get() = sharedPreferences.getString(ALBUMS_FAB_ACTION, null) ?: FAB_ACTION_SHUFFLE
        set(value) {
            sharedPreferences.edit {
                putString(ALBUMS_FAB_ACTION, value)
            }
        }

    val artworkClickAction: Int
        get() = sharedPreferences.getStringOrDefault(ARTWORK_CLICK_ACTION, "0").toInt()

    val disableSwipeDownToDismiss
        get() = sharedPreferences.getBoolean(DISABLE_SWIPE_DOWN_TO_DISMISS, false)

    const val PLAYER_ACTION_BUTTONS_ORDER = "player_action_buttons_order"
    const val NOW_PLAYING_ACTION_BUTTONS_ORDER = "now_playing_action_buttons_order"
    const val NOW_PLAYING_ACTION_BUTTONS_VISIBILITY = "now_playing_action_buttons_visibility"

    const val OFFLINE_MODE = "offline_mode"

    const val SHOW_SONG_ONLY = "show_song_only"

    var playerActionButtonsOrder: String
        get() = sharedPreferences.getStringOrDefault(PLAYER_ACTION_BUTTONS_ORDER, "")
        set(value) = sharedPreferences.edit { putString(PLAYER_ACTION_BUTTONS_ORDER, value) }

    var nowPlayingActionButtonsOrder: List<Int>
        get() {
            val json = sharedPreferences.getStringOrDefault(NOW_PLAYING_ACTION_BUTTONS_ORDER, "[]")
            return try {
                Gson().fromJson(json, object : TypeToken<List<Int>>() {}.type)
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                emptyList()
            }
        }
        set(value) {
            val json = Gson().toJson(value)
            sharedPreferences.edit { putString(NOW_PLAYING_ACTION_BUTTONS_ORDER, json) }
        }

    var nowPlayingActionButtonsVisibility: Map<String, Boolean>
        get() {
            val json = sharedPreferences.getStringOrDefault(NOW_PLAYING_ACTION_BUTTONS_VISIBILITY, "{}")
            return try {
                Gson().fromJson(json, object : TypeToken<Map<String, Boolean>>() {}.type)
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                emptyMap()
            }
        }
        set(value) {
            val json = Gson().toJson(value)
            sharedPreferences.edit { putString(NOW_PLAYING_ACTION_BUTTONS_VISIBILITY, json) }
        }

    var isOfflineMode: Boolean
        get() = sharedPreferences.getBoolean(OFFLINE_MODE, false)
        set(value) = sharedPreferences.edit { putBoolean(OFFLINE_MODE, value) }

    var showSongOnly: Boolean
        get() = sharedPreferences.getBoolean(SHOW_SONG_ONLY, false)
        set(value) = sharedPreferences.edit { putBoolean(SHOW_SONG_ONLY, value) }

    var fixYear: Boolean
        get() = sharedPreferences.getBoolean(FIX_YEAR, false)
        set(value) = sharedPreferences.edit { putBoolean(FIX_YEAR, value) }

    const val SHOW_NOW_PLAYING_QUEUE_BUTTON = "show_now_playing_queue_button"
    const val SHOW_OPTIONS_MENU = "show_options_menu"
    const val SHOW_CAST_BUTTON = "show_cast_button"

    val showNowPlayingQueueButton: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_NOW_PLAYING_QUEUE_BUTTON, true // Default to true, as it's visible by default
        )

    val showOptionsMenu: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_OPTIONS_MENU, true // Default to true, as it's visible by default
        )

    val showCastButton: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_CAST_BUTTON, false // Default to true, as it's visible by default
        )

    val showSongMenuGrid: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_SONG_MENU_GRID, true // Default to true, as it's visible by default
        )

    val showSongsSearchButton: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_SONGS_SEARCH_BUTTON, true // Default to true, as it's visible by default
        )

    val showCoversInSongsTab: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_COVERS_IN_SONGS_TAB, true // Default to true
        )

    const val TAP_ON_TITLE = "tap_on_title"
    val tapOnTitle: Boolean
        get() = sharedPreferences.getBoolean(
            TAP_ON_TITLE, true // Default to true
        )

    const val TAP_ON_ARTIST = "tap_on_artist"
    val tapOnArtist: Boolean
        get() = sharedPreferences.getBoolean(
            TAP_ON_ARTIST, true // Default to true
        )

    val miniPlayerScrolling: Boolean
        get() = sharedPreferences.getBoolean(
            MINI_PLAYER_SCROLLING, true // Default to true
        )

    val enableSongTitleMarquee: Boolean
        get() = sharedPreferences.getBoolean(
            ENABLE_SONG_TITLE_MARQUEE, false // Default to true
        )

    val showFabOnScroll: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_FAB_ON_SCROLL, false // Default to false
        )

    val swapShuffleRepeatButtons: Boolean
        get() = sharedPreferences.getBoolean(
            SWAP_SHUFFLE_REPEAT_BUTTONS, false // Default to false
        )

    val showArtistInSongs: Boolean
        get() = sharedPreferences.getBoolean(
            SHOW_ARTIST_IN_SONGS, true // Default to true
        )

    val hideDuplicateSongs: Boolean
        get() = sharedPreferences.getBoolean(
            HIDE_DUPLICATE_SONGS, false // Default to false
        )

    var preferHigherBitrateSongs: Boolean
        get() = sharedPreferences.getBoolean(
            PREFER_HIGHER_BITRATE_SONGS, false // Default to false
        )
        set(value) = sharedPreferences.edit { putBoolean(PREFER_HIGHER_BITRATE_SONGS, value) }

    var songTextSize: Int
        get() = sharedPreferences.getInt(
            SONG_TEXT_SIZE, 14 // Default font size
        )
        set(value) = sharedPreferences.edit { putInt(SONG_TEXT_SIZE, value) }

    var artistTextSize: Int
        get() = sharedPreferences.getInt(
            ARTIST_TEXT_SIZE, 12 // Default font size
        )
        set(value) = sharedPreferences.edit { putInt(ARTIST_TEXT_SIZE, value) }

    const val AUTO_HIDE_MINI_PLAYER = "auto_hide_mini_player"
    const val KEEP_HEADER_VISIBLE = "keep_header_visible"
    const val HIDE_HEADER = "hide_header"
    var autoHideMiniPlayer: Boolean
        get() = sharedPreferences.getBoolean(
            AUTO_HIDE_MINI_PLAYER, false // Default to false
        )
        set(value) = sharedPreferences.edit { putBoolean(AUTO_HIDE_MINI_PLAYER, value) }

    val keepHeaderVisible: Boolean
        get() = sharedPreferences.getBoolean(
            KEEP_HEADER_VISIBLE, false // Default to false
        )
    
    val hideHeader: Boolean
        get() = sharedPreferences.getBoolean(
            HIDE_HEADER, false // Default to false
        )

    const val CUSTOM_FALLBACK_ARTWORK_URI = "custom_fallback_artwork_uri"

    var customFallbackArtworkUri: String?
        get() = sharedPreferences.getString(CUSTOM_FALLBACK_ARTWORK_URI, null)
        set(value) = sharedPreferences.edit { putString(CUSTOM_FALLBACK_ARTWORK_URI, value) }

    const val TIME_DISPLAY_MODE = "time_display_mode"
    const val TIME_DISPLAY_MODE_TOTAL = "total"
    const val TIME_DISPLAY_MODE_REMAINING = "remaining"
    const val TIME_DISPLAY_MODE_TOGGLE = "toggle"
    const val TIME_DISPLAY_MODE_TOGGLE_REMEMBER = "time_display_mode_toggle_remember"

    var isShowingRemainingTime: Boolean
        get() = sharedPreferences.getBoolean(
            TIME_DISPLAY_MODE_TOGGLE_REMEMBER, false 
        )
        set(value) = sharedPreferences.edit { putBoolean(TIME_DISPLAY_MODE_TOGGLE_REMEMBER, value) }

    var timeDisplayMode: String
        get() {
            return when (val value = sharedPreferences.all[TIME_DISPLAY_MODE]) {
            is String -> value
            is Int -> {
                val stringValue = when (value) {
                    0 -> TIME_DISPLAY_MODE_TOTAL
                    1 -> TIME_DISPLAY_MODE_REMAINING
                    2 -> TIME_DISPLAY_MODE_TOGGLE
                    else -> TIME_DISPLAY_MODE_TOTAL 
                }
                sharedPreferences.edit {
                    putString(TIME_DISPLAY_MODE, stringValue)
                }
                stringValue
            }
            else -> TIME_DISPLAY_MODE_TOTAL 
        }
    }
    set(value) {
        sharedPreferences.edit { putString(TIME_DISPLAY_MODE, value) }
    }

    const val MINI_PLAYER_TIME = "mini_player_time"
    const val MINI_PLAYER_TIME_REMAINING = 0
    const val MINI_PLAYER_TIME_TOTAL = 1
    const val MINI_PLAYER_TIME_DISABLED = 2
    const val MINI_PLAYER_TIME_ELAPSED = 3

    var miniPlayerTime: Int
        get() {
            return try {
                sharedPreferences.getString(MINI_PLAYER_TIME, MINI_PLAYER_TIME_DISABLED.toString())?.toInt()
                    ?: MINI_PLAYER_TIME_DISABLED
            } catch (e: ClassCastException) {
                val intValue = sharedPreferences.getInt(MINI_PLAYER_TIME, MINI_PLAYER_TIME_DISABLED)
                sharedPreferences.edit {
                    remove(MINI_PLAYER_TIME) 
                    putString(MINI_PLAYER_TIME, intValue.toString()) 
                }
                intValue
            } catch (e: NumberFormatException) {
                sharedPreferences.edit { remove(MINI_PLAYER_TIME) }
                MINI_PLAYER_TIME_DISABLED
            }
        }
        set(value) = sharedPreferences.edit {
            putString(MINI_PLAYER_TIME, value.toString())
        }
}

enum class CoverLyricsType {
    REPLACE_COVER, OVER_COVER
}
