package code.name.monkey.retromusic.repository

import android.content.Context
import android.content.ContentResolver
import android.util.Log
import code.name.monkey.retromusic.db.SongMetadataDao
import code.name.monkey.retromusic.db.SongMetadataEntity
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.UriUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kyant.taglib.Metadata
import com.kyant.taglib.TagLib
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import android.os.Environment
import java.io.File

class MetadataScanner(
    private val dao: SongMetadataDao,
) {

    private val artistIdMap = mutableMapOf<String, Long>()
    private var nextArtistId = 1L

    private suspend fun initExistingArtists() {
        val existingArtists = dao.getAllMetadata()
        
        existingArtists.forEach { entity ->
            val names = entity.artistNames?.split(",")?.map { it.trim() } ?: emptyList()
            val ids = entity.artistIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
            
            names.zip(ids).forEach { (name, id) ->
                if (!artistIdMap.containsKey(name)) {
                    artistIdMap[name] = id
                    if (id >= nextArtistId) nextArtistId = id + 1
                }
            }
        }
    }

    private fun generateArtistId(artistName: String): Long {
        val existing = artistIdMap[artistName]
        return if (existing != null) {
            existing
        } else {
            val newId = nextArtistId++
            artistIdMap[artistName] = newId
            newId
        }
    }

    suspend fun scanIfNotExists(
        songList: List<Song>,
        context: Context,
        onProgress: (songTitle: String, index: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {

        initExistingArtists()
        
        songList.forEachIndexed { idx, song ->
            if (dao.getMetadataById(song.id) != null) return@forEachIndexed

            onProgress(song.title, idx + 1, songList.size)

            val uri = UriUtil.getUriFromPath(context, song.data)

            val resolver = context.contentResolver

            val tag: Map<String, List<String>> = resolver.openFileDescriptor(uri, "r")?.use { fdParcel ->
                TagLib.getMetadata(fdParcel.dup().detachFd(), readPictures = false)?.propertyMap?.mapValues { it.value.toList() }
            } ?: emptyMap()

            val artistNames = tag["ARTIST"]
            val delimiters = PreferenceUtil.artistDelimiters ?: PreferenceUtil.defaultDelimiters
            val splitNames = if (!PreferenceUtil.artistDelimiters.isNullOrEmpty()) {
                val delimiterRegex = delimiters
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString("|") { Regex.escape(it) }
                    .toRegex()
                    
                artistNames.orEmpty() 
                    .flatMap { artist -> artist.split(delimiterRegex).map { it.trim() } }
                    .filter { it.isNotEmpty() }
                    .distinct()
            } else {
                artistNames
            }
            
            val artistIds = splitNames?.map { generateArtistId(it.trim()) } ?: emptyList()
            val artistIdsString = artistIds.joinToString(",")
            val artistNamesString = splitNames?.joinToString(", ") ?: ""

            val entity = SongMetadataEntity(
                id = song.id,
                title = song.title,
                albumName = song.albumName,
                artistName = song.artistName, 
                composer = song.composer,
                year = tag["DATE"]?.firstOrNull() ?: "",
                trackNumber = song.trackNumber,
                duration = song.duration,
                albumArtist = song.albumArtist,
                data = song.data,
                dateModified = song.dateModified,
                albumId = song.albumId,
                artistId = song.artistId,
                artistIds = artistIdsString,
                artistNames = artistNamesString
            )
            dao.insert(entity)

            // appendSongMetadata(entity)
        }
    }

    private suspend fun appendSongMetadata(entity: SongMetadataEntity) = withContext(Dispatchers.IO) {
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outFile = File(downloadsDir, "metadata_dump.json")
            
            val list: MutableList<SongMetadataEntity> =
                if (outFile.exists()) {
                    val text = outFile.readText()
                    if (text.isNotBlank()) {
                        gson.fromJson(text, object : TypeToken<MutableList<SongMetadataEntity>>() {}.type)
                    } else mutableListOf()
                } else mutableListOf()
            
            list.add(entity)

            outFile.writeText(gson.toJson(list))
        
            Log.d("MetadataScanner", "Metadata dump saved for: ${entity.title} -> ${outFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("MetadataScanner", "Failed to save metadata dump for ${entity.title}", e)
        }
    }

}
