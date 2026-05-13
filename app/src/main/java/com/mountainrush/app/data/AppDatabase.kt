package com.mountainrush.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RunSession::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext, AppDatabase::class.java, "mountainrush.db"
            ).build().also { instance = it }
        }
    }
}
