package code.name.monkey.retromusic.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE LyricsEntity")
        database.execSQL("DROP TABLE BlackListStoreEntity")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE PlaylistEntity ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS song_metadata (
                id INTEGER PRIMARY KEY NOT NULL,
                title TEXT,
                trackNumber INTEGER,
                year TEXT,
                duration INTEGER,
                data TEXT,
                dateModified INTEGER,
                artistId INTEGER,
                albumId INTEGER,
                albumName TEXT,
                artistName TEXT,
                composer TEXT,
                albumArtist TEXT,
                allArtists TEXT
            )
        """.trimIndent())
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Rename old table
        database.execSQL("ALTER TABLE song_metadata RENAME TO song_metadata_old")

        // 2. Create new table without allArtists
        database.execSQL("""
            CREATE TABLE song_metadata (
                id INTEGER PRIMARY KEY NOT NULL,
                title TEXT,
                trackNumber INTEGER,
                year TEXT,
                duration INTEGER,
                data TEXT,
                dateModified INTEGER,
                artistId INTEGER,
                albumId INTEGER,
                albumName TEXT,
                artistName TEXT,
                composer TEXT,
                albumArtist TEXT
            )
        """.trimIndent())

        // 3. Copy all columns except allArtists
        database.execSQL("""
            INSERT INTO song_metadata (
                id, title, trackNumber, year, duration, data, dateModified,
                artistId, albumId, albumName, artistName, composer, albumArtist
            )
            SELECT 
                id, title, trackNumber, year, duration, data, dateModified,
                artistId, albumId, albumName, artistName, composer, albumArtist
            FROM song_metadata_old
        """.trimIndent())

        // 4. Drop old table
        database.execSQL("DROP TABLE song_metadata_old")
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Rename old table
        database.execSQL("ALTER TABLE song_metadata RENAME TO song_metadata_old")

        // 2. Create new table with artistIds and artistNames
        database.execSQL("""
            CREATE TABLE song_metadata (
                id INTEGER PRIMARY KEY NOT NULL,
                title TEXT,
                trackNumber INTEGER,
                year TEXT,
                duration INTEGER,
                data TEXT,
                dateModified INTEGER,
                artistId INTEGER,
                albumId INTEGER,
                albumName TEXT,
                artistName TEXT,
                composer TEXT,
                albumArtist TEXT,
                artistIds TEXT,
                artistNames TEXT
            )
        """.trimIndent())

        // 3. Copy old data, leave new columns null
        database.execSQL("""
            INSERT INTO song_metadata (
                id, title, trackNumber, year, duration, data, dateModified,
                artistId, albumId, albumName, artistName, composer, albumArtist
            )
            SELECT 
                id, title, trackNumber, year, duration, data, dateModified,
                artistId, albumId, albumName, artistName, composer, albumArtist
            FROM song_metadata_old
        """.trimIndent())

        // 4. Drop old table
        database.execSQL("DROP TABLE song_metadata_old")
    }
}

val allMigrations = arrayOf(
    MIGRATION_23_24,
    MIGRATION_25_26,
    MIGRATION_26_27,
    MIGRATION_27_28,
    MIGRATION_28_29
)
