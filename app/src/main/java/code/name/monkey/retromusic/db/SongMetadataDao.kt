package code.name.monkey.retromusic.db

import androidx.room.*

@Dao
interface SongMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SongMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metadataList: List<SongMetadataEntity>)

    @Query("SELECT * FROM song_metadata WHERE id = :songId LIMIT 1")
    suspend fun getMetadataById(songId: Long): SongMetadataEntity?

    @Query("SELECT * FROM song_metadata")
    suspend fun getAllMetadata(): List<SongMetadataEntity>

    @Query("DELETE FROM song_metadata")
    suspend fun clearAll()
}
