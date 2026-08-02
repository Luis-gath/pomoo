package com.example.pomodoro.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.areas.data.ItemMark
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    /** Material de un área, lo más reciente primero: la vista es una línea temporal. */
    @Query("SELECT * FROM items WHERE areaId = :areaId ORDER BY createdAt DESC")
    fun getItemsForArea(areaId: Int): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE areaId = :areaId AND mark = :mark ORDER BY createdAt DESC")
    fun getItemsForAreaByMark(areaId: Int, mark: ItemMark): Flow<List<Item>>

    /** Entregas pendientes de todas las áreas, para una vista general de lo urgente. */
    @Query("""
        SELECT * FROM items
        WHERE mark = 'ENTREGA' AND dueAt IS NOT NULL
        ORDER BY dueAt ASC
    """)
    fun getUpcomingDeliverables(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Int): Item?

    @Query("SELECT COUNT(*) FROM items WHERE areaId = :areaId")
    fun countItemsForArea(areaId: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("DELETE FROM items WHERE areaId = :areaId")
    suspend fun deleteItemsForArea(areaId: Int)

    @Query("UPDATE items SET mark = :mark, dueAt = :dueAt WHERE id = :id")
    suspend fun updateMark(id: Int, mark: ItemMark, dueAt: Long?)
}
