package com.comet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.comet.data.model.EnumConverters
import kotlinx.serialization.json.Json

/** comet.db — Room-backed queue that survives process death (Part 9.1). */
@Database(
    entities = [
        DownloadEntity::class,
        PlaylistEntity::class,
        EngineStateEntity::class,
        WaveformEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(EnumConverters::class, WaveformConverter::class)
abstract class CometDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun engineStateDao(): EngineStateDao

    companion object {
        @Volatile
        private var instance: CometDatabase? = null

        fun get(context: Context): CometDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CometDatabase::class.java,
                    "comet.db",
                ).build().also { instance = it }
            }
    }
}

/** IntArray <-> JSON string for waveform amplitudes. */
class WaveformConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun intArrayToJson(value: IntArray?): String? =
        value?.let { json.encodeToString(kotlinx.serialization.builtins.IntArraySerializer(), it) }

    @TypeConverter
    fun jsonToIntArray(value: String?): IntArray? =
        value?.let { runCatching { json.decodeFromString(kotlinx.serialization.builtins.IntArraySerializer(), it) }.getOrNull() }
}
