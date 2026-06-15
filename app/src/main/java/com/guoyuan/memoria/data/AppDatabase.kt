package com.guoyuan.memoria.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TextEntity::class, PunctuationEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
