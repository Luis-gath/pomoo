package com.example.pomodoro.features.areas.data

import com.example.pomodoro.core.database.AreaDao
import com.example.pomodoro.core.database.ItemDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AreaRepositoryImpl @Inject constructor(
    private val areaDao: AreaDao,
    private val itemDao: ItemDao
) : AreaRepository {

    // --- Áreas ---

    override fun getActiveAreas(): Flow<List<Area>> = areaDao.getActiveAreas()

    override fun getAllAreas(): Flow<List<Area>> = areaDao.getAllAreas()

    override suspend fun getAreaById(id: Int): Area? = areaDao.getAreaById(id)

    override fun getAreaByIdFlow(id: Int): Flow<Area?> = areaDao.getAreaByIdFlow(id)

    override suspend fun saveArea(area: Area): Long = areaDao.insertArea(area)

    override suspend fun archiveArea(id: Int) = areaDao.setActive(id, false)

    override suspend fun deleteAreaWithItems(area: Area) {
        itemDao.deleteItemsForArea(area.id)
        areaDao.deleteArea(area)
    }

    // --- Material ---

    override fun getItemsForArea(areaId: Int): Flow<List<Item>> =
        itemDao.getItemsForArea(areaId)

    override fun getAllItems(): Flow<List<Item>> = itemDao.getAllItems()

    override fun getItemsForAreaByMark(areaId: Int, mark: ItemMark): Flow<List<Item>> =
        itemDao.getItemsForAreaByMark(areaId, mark)

    override fun getUpcomingDeliverables(): Flow<List<Item>> =
        itemDao.getUpcomingDeliverables()

    override fun countItemsForArea(areaId: Int): Flow<Int> =
        itemDao.countItemsForArea(areaId)

    override suspend fun getItemById(id: Int): Item? = itemDao.getItemById(id)

    override suspend fun saveItem(item: Item): Long = itemDao.insertItem(item)

    override suspend fun deleteItem(item: Item) = itemDao.deleteItem(item)

    override suspend fun updateMark(id: Int, mark: ItemMark, dueAt: Long?) =
        itemDao.updateMark(id, mark, dueAt)
}
