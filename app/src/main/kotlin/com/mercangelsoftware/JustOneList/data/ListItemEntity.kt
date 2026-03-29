package com.mercangelsoftware.JustOneList.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "list_items")
data class ListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val checked: Boolean = false,
    val position: Long = System.currentTimeMillis()
)
