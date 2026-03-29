package com.mercangelsoftware.JustOneList.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ListItemEntity::class], version = 1, exportSchema = false)
abstract class ListDatabase : RoomDatabase() {
    abstract fun listItemDao(): ListItemDao

    companion object {
        @Volatile private var INSTANCE: ListDatabase? = null

        fun getInstance(context: Context): ListDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ListDatabase::class.java,
                    "just_one_list.db"
                ).build().also { INSTANCE = it }
            }
    }
}
