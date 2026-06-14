package com.zixo.app.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zixo.app.data.local.room.dao.CallLogDao
import com.zixo.app.data.local.room.dao.ChatDao
import com.zixo.app.data.local.room.entity.CallLogEntity
import com.zixo.app.data.local.room.entity.ChatEntity

@Database(
    entities = [
        ChatEntity::class,
        CallLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ZixoDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    abstract fun callLogDao(): CallLogDao

    companion object {
        const val DATABASE_NAME = "zixo_database"
    }
}
