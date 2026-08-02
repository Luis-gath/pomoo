package com.example.pomodoro.features.areas.data

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a áreas y a su material. Implementación concreta en
 * [AreaRepositoryImpl]; depender de la interfaz permite usar fakes en tests.
 */
interface AreaRepository {

    // --- Áreas ---

    fun getActiveAreas(): Flow<List<Area>>

    fun getAllAreas(): Flow<List<Area>>

    suspend fun getAreaById(id: Int): Area?

    fun getAreaByIdFlow(id: Int): Flow<Area?>

    suspend fun saveArea(area: Area): Long

    suspend fun archiveArea(id: Int)

    /** Borra el área y su material. Los archivos copiados hay que limpiarlos aparte. */
    suspend fun deleteAreaWithItems(area: Area)

    // --- Material ---

    fun getItemsForArea(areaId: Int): Flow<List<Item>>

    fun getItemsForAreaByMark(areaId: Int, mark: ItemMark): Flow<List<Item>>

    /** Entregas pendientes de todas las áreas, ordenadas por fecha. */
    fun getUpcomingDeliverables(): Flow<List<Item>>

    fun countItemsForArea(areaId: Int): Flow<Int>

    suspend fun getItemById(id: Int): Item?

    suspend fun saveItem(item: Item): Long

    suspend fun deleteItem(item: Item)

    suspend fun updateMark(id: Int, mark: ItemMark, dueAt: Long?)
}
