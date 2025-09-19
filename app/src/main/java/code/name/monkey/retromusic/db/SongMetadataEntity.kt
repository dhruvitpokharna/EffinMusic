package code.name.monkey.retromusic.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_metadata")
data class SongMetadataEntity(
    @PrimaryKey val id: Long,
    val title: String?,
    val trackNumber: Int?,
    val year: String?,
    val duration: Long?,
    val data: String?,           
    val dateModified: Long?,     
    val artistId: Long?,
    val albumId: Long?,          
    val albumName: String?,      
    val artistName: String?,     
    val composer: String?,
    val albumArtist: String?,
    val artistIds: String?,
    val artistNames: String?
    
)
