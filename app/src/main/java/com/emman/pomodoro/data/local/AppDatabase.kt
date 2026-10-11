package com.emman.pomodoro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.emman.pomodoro.data.local.dao.SessionDao
import com.emman.pomodoro.data.local.dao.TagDao
import com.emman.pomodoro.data.local.entity.SessionEntity
import com.emman.pomodoro.data.local.entity.TagEntity

@Database(
    entities = [SessionEntity::class, TagEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun tagDao(): TagDao
}
