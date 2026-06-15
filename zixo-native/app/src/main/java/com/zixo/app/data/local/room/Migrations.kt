package com.zixo.app.data.local.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

/**
 * Production migration scripts for ZixoDatabase.
 *
 * Replaces `fallbackToDestructiveMigration()` with proper schema migrations
 * that preserve user data across app updates.
 *
 * MIGRATION_1_2: Adds messages, contacts, statuses, and users tables
 * with all columns, indices, and foreign keys.
 *
 * MIGRATION_2_3: Adds lastSyncedAt columns and indices for sync tracking.
 */
object ZixoMigrations {

    /**
     * Migration from v1 (chats + call_logs only) to v2
     * (adds messages, contacts, statuses, users tables).
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                Timber.d("Migrating ZixoDatabase from v1 to v2")

                // ── Messages table ──────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `messages` (
                        `id` TEXT NOT NULL,
                        `chatId` TEXT NOT NULL,
                        `senderId` TEXT NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `senderAvatarUrl` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `messageType` TEXT NOT NULL,
                        `mediaUrl` TEXT,
                        `thumbnailUrl` TEXT,
                        `replyToId` TEXT,
                        `forwardedFrom` TEXT,
                        `reactionsJson` TEXT,
                        `isRead` INTEGER NOT NULL DEFAULT 0,
                        `isDelivered` INTEGER NOT NULL DEFAULT 0,
                        `isDeletedForMe` INTEGER NOT NULL DEFAULT 0,
                        `isDeletedForEveryone` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `syncedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId_createdAt` ON `messages` (`chatId`, `createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId_senderId` ON `messages` (`chatId`, `senderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_senderId` ON `messages` (`senderId`)")

                // ── Contacts table ──────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `contacts` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `contactUserId` TEXT NOT NULL,
                        `contactDisplayName` TEXT NOT NULL,
                        `contactUsername` TEXT NOT NULL,
                        `contactZixoNumber` TEXT NOT NULL,
                        `contactAvatarUrl` TEXT NOT NULL,
                        `contactBio` TEXT NOT NULL,
                        `isMutual` INTEGER NOT NULL DEFAULT 0,
                        `isVerifiedContact` INTEGER NOT NULL DEFAULT 1,
                        `isBlocked` INTEGER NOT NULL DEFAULT 0,
                        `isPinned` INTEGER NOT NULL DEFAULT 0,
                        `isMuted` INTEGER NOT NULL DEFAULT 0,
                        `addedAt` INTEGER NOT NULL,
                        `mutualVerifiedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_contactUserId` ON `contacts` (`contactUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_contactZixoNumber` ON `contacts` (`contactZixoNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_isMutual` ON `contacts` (`isMutual`)")

                // ── Statuses table ──────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `statuses` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `userName` TEXT NOT NULL,
                        `userAvatarUrl` TEXT NOT NULL,
                        `text` TEXT,
                        `mediaUrl` TEXT,
                        `mediaType` TEXT NOT NULL DEFAULT 'TEXT',
                        `backgroundColor` TEXT,
                        `fontName` TEXT,
                        `visibility` TEXT NOT NULL DEFAULT 'ALL_CONTACTS',
                        `viewersJson` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL,
                        `isViewed` INTEGER NOT NULL DEFAULT 0,
                        `isMyStatus` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_statuses_userId_createdAt` ON `statuses` (`userId`, `createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_statuses_expiresAt` ON `statuses` (`expiresAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_statuses_isMyStatus` ON `statuses` (`isMyStatus`)")

                // ── Users table ─────────────────────────────────────────
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `users` (
                        `uid` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `zixoNumber` TEXT NOT NULL,
                        `photoUrl` TEXT NOT NULL,
                        `bio` TEXT NOT NULL,
                        `phoneNumber` TEXT,
                        `hasPasskey` INTEGER NOT NULL DEFAULT 0,
                        `passkeyCredentialId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `lastSeenAt` INTEGER NOT NULL,
                        `isOnline` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`uid`)
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_zixoNumber` ON `users` (`zixoNumber`)")

                Timber.d("ZixoDatabase v1→v2 migration completed successfully")
            } catch (e: Exception) {
                Timber.e(e, "FATAL: ZixoDatabase v1→v2 migration failed")
                throw e
            }
        }
    }

    /**
     * Migration from v2 to v3 — adds lastSyncedAt columns
     * and sync-tracking indices for WorkManager sync engine.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                Timber.d("Migrating ZixoDatabase from v2 to v3")

                // Add lastSyncedAt to contacts
                db.execSQL("ALTER TABLE `contacts` ADD COLUMN `lastSyncedAt` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_lastSyncedAt` ON `contacts` (`lastSyncedAt`)")

                // Add lastSyncedAt to statuses
                db.execSQL("ALTER TABLE `statuses` ADD COLUMN `lastSyncedAt` INTEGER")

                // Add lastSyncedAt to users
                db.execSQL("ALTER TABLE `users` ADD COLUMN `lastSyncedAt` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_lastSyncedAt` ON `users` (`lastSyncedAt`)")

                // Add syncedAt index to messages
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_syncedAt` ON `messages` (`syncedAt`)")

                Timber.d("ZixoDatabase v2→v3 migration completed successfully")
            } catch (e: Exception) {
                Timber.e(e, "FATAL: ZixoDatabase v2→v3 migration failed")
                throw e
            }
        }
    }

    /**
     * Complete list of all migrations for Room database builder.
     */
    val ALL_MIGRATIONS: List<Migration> = listOf(MIGRATION_1_2, MIGRATION_2_3)
}
