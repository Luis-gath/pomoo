package com.example.pomodoro.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pomodoro.features.areas.data.Area
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaDao {

    @Query("SELECT * FROM areas WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveAreas(): Flow<List<Area>>

    @Query("SELECT * FROM areas ORDER BY isActive DESC, name ASC")
    fun getAllAreas(): Flow<List<Area>>

    @Query("SELECT * FROM areas WHERE id = :id")
    suspend fun getAreaById(id: Int): Area?

    @Query("SELECT * FROM areas WHERE id = :id")
    fun getAreaByIdFlow(id: Int): Flow<Area?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: Area): Long

    @Delete
    suspend fun deleteArea(area: Area)

    @Query("UPDATE areas SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Int, active: Boolean)
}
