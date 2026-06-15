package com.zixo.app.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zixo.app.data.local.room.dao.CallLogDao
import com.zixo.app.data.local.room.dao.ChatDao
import com.zixo.app.data.local.room.dao.ContactDao
import com.zixo.app.data.local.room.dao.MessageDao
import com.zixo.app.data.local.room.dao.StatusDao
import com.zixo.app.data.local.room.dao.UserDao
import com.zixo.app.data.local.room.entity.CallLogEntity
import com.zixo.app.data.local.room.entity.ChatEntity
import com.zixo.app.data.local.room.entity.ContactEntity
import com.zixo.app.data.local.room.entity.MessageEntity
import com.zixo.app.data.local.room.entity.StatusEntity
import com.zixo.app.data.local.room.entity.UserEntity

/**
 * Zixo Room Database — Offline-First Persistent Cache Layer.
 *
 * Registers all 6 entities with proper foreign key relationships,
 * indices for query performance, and DAOs for data access.
 *
 * Version history:
 * - v1: Initial schema (ChatEntity + CallLogEntity only)
 * - v2: Added MessageEntity, ContactEntity, StatusEntity, UserEntity
 * - v3: Added lastSyncedAt columns and sync-tracking indices
 *
 * Migrations are handled by [ZixoMigrations] — DO NOT use
 * fallbackToDestructiveMigration() as it wipes user data.
 */
@Database(
    entities = [
        ChatEntity::class,
        CallLogEntity::class,
        MessageEntity::class,
        ContactEntity::class,
        StatusEntity::class,
        UserEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(ZixoTypeConverters::class)
abstract class ZixoDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun callLogDao(): CallLogDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun statusDao(): StatusDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "zixo_database"
    }
}
