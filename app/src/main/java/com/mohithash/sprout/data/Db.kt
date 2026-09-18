package com.mohithash.sprout.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val location: String = "",
    /** domain.PlantProfile JSON */
    val profile: String,
    val waterEveryDays: Int,
    val lastWatered: Long = System.currentTimeMillis(),
    val lastFertilized: Long = 0,
    /** Small JPEG thumbnail, base64 (optional). */
    val thumb: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "events")
data class CareEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val kind: String,      // water | fertilize | note | diagnosis
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY (lastWatered + waterEveryDays * 86400000)") fun all(): Flow<List<Plant>>
    @Insert suspend fun insert(p: Plant): Long
    @Update suspend fun update(p: Plant)
    @Query("DELETE FROM plants WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE plantId = :pid ORDER BY timestamp DESC LIMIT 50") fun forPlant(pid: Long): Flow<List<CareEvent>>
    @Insert suspend fun insert(e: CareEvent)
    @Query("DELETE FROM events WHERE plantId = :pid") suspend fun deleteForPlant(pid: Long)
}

@Database(entities = [Plant::class, CareEvent::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() { abstract fun plants(): PlantDao; abstract fun events(): EventDao }
