package com.mercangelsoftware.JustOneList.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListItemDao {
    @Query("SELECT * FROM list_items ORDER BY checked ASC, position ASC")
    fun observeAll(): Flow<List<ListItemEntity>>

    @Insert
    suspend fun insert(item: ListItemEntity)

    @Query("UPDATE list_items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("UPDATE list_items SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Long)

    @Query("DELETE FROM list_items")
    suspend fun deleteAll()
}
