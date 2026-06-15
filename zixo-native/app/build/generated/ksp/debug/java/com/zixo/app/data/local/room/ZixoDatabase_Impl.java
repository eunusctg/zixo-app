package com.zixo.app.data.local.room;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.zixo.app.data.local.room.dao.CallLogDao;
import com.zixo.app.data.local.room.dao.CallLogDao_Impl;
import com.zixo.app.data.local.room.dao.ChatDao;
import com.zixo.app.data.local.room.dao.ChatDao_Impl;
import com.zixo.app.data.local.room.dao.ContactDao;
import com.zixo.app.data.local.room.dao.ContactDao_Impl;
import com.zixo.app.data.local.room.dao.MessageDao;
import com.zixo.app.data.local.room.dao.MessageDao_Impl;
import com.zixo.app.data.local.room.dao.StatusDao;
import com.zixo.app.data.local.room.dao.StatusDao_Impl;
import com.zixo.app.data.local.room.dao.UserDao;
import com.zixo.app.data.local.room.dao.UserDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ZixoDatabase_Impl extends ZixoDatabase {
  private volatile ChatDao _chatDao;

  private volatile CallLogDao _callLogDao;

  private volatile MessageDao _messageDao;

  private volatile ContactDao _contactDao;

  private volatile StatusDao _statusDao;

  private volatile UserDao _userDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `chat_threads` (`id` TEXT NOT NULL, `participantUids` TEXT NOT NULL, `lastMessage` TEXT, `lastMessageTimestamp` INTEGER, `unreadCount` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `isMuted` INTEGER NOT NULL, `threadType` TEXT NOT NULL, `groupName` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_threads_participant_uids` ON `chat_threads` (`participantUids`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_threads_last_message_timestamp` ON `chat_threads` (`lastMessageTimestamp`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `call_log` (`id` TEXT NOT NULL, `callId` TEXT NOT NULL, `callerUid` TEXT NOT NULL, `calleeUid` TEXT NOT NULL, `callerName` TEXT NOT NULL, `calleeName` TEXT NOT NULL, `callerAvatar` TEXT, `calleeAvatar` TEXT, `type` TEXT NOT NULL, `isVideoCall` INTEGER NOT NULL, `isGroupCall` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `endReason` TEXT NOT NULL, `threadId` TEXT NOT NULL, `isRead` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_log_timestamp` ON `call_log` (`timestamp`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_log_type` ON `call_log` (`type`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `chatId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `senderName` TEXT NOT NULL, `senderAvatarUrl` TEXT NOT NULL, `content` TEXT NOT NULL, `messageType` TEXT NOT NULL, `mediaUrl` TEXT, `thumbnailUrl` TEXT, `replyToId` TEXT, `forwardedFrom` TEXT, `reactionsJson` TEXT, `isRead` INTEGER NOT NULL, `isDelivered` INTEGER NOT NULL, `isDeletedForMe` INTEGER NOT NULL, `isDeletedForEveryone` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `syncedAt` INTEGER DEFAULT 0, PRIMARY KEY(`id`), FOREIGN KEY(`chatId`) REFERENCES `chat_threads`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId_createdAt` ON `messages` (`chatId`, `createdAt`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId_senderId` ON `messages` (`chatId`, `senderId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_senderId` ON `messages` (`senderId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_syncedAt` ON `messages` (`syncedAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `contactUserId` TEXT NOT NULL, `contactDisplayName` TEXT NOT NULL, `contactUsername` TEXT NOT NULL, `contactZixoNumber` TEXT NOT NULL, `contactAvatarUrl` TEXT NOT NULL, `contactBio` TEXT NOT NULL, `isMutual` INTEGER NOT NULL, `isVerifiedContact` INTEGER NOT NULL, `isBlocked` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `isMuted` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, `mutualVerifiedAt` INTEGER, `lastSyncedAt` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_contactUserId` ON `contacts` (`contactUserId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_contactZixoNumber` ON `contacts` (`contactZixoNumber`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_isMutual` ON `contacts` (`isMutual`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_isBlocked` ON `contacts` (`isBlocked`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_lastSyncedAt` ON `contacts` (`lastSyncedAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `statuses` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `userName` TEXT NOT NULL, `userAvatarUrl` TEXT NOT NULL, `text` TEXT, `mediaUrl` TEXT, `mediaType` TEXT NOT NULL, `backgroundColor` TEXT, `fontName` TEXT, `visibility` TEXT NOT NULL, `viewersJson` TEXT, `createdAt` INTEGER NOT NULL, `expiresAt` INTEGER NOT NULL, `isViewed` INTEGER NOT NULL, `isMyStatus` INTEGER NOT NULL, `lastSyncedAt` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_statuses_userId_createdAt` ON `statuses` (`userId`, `createdAt`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_statuses_expiresAt` ON `statuses` (`expiresAt`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_statuses_isMyStatus` ON `statuses` (`isMyStatus`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_statuses_lastSyncedAt` ON `statuses` (`lastSyncedAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`uid` TEXT NOT NULL, `displayName` TEXT NOT NULL, `username` TEXT NOT NULL, `zixoNumber` TEXT NOT NULL, `photoUrl` TEXT NOT NULL, `bio` TEXT NOT NULL, `phoneNumber` TEXT, `hasPasskey` INTEGER NOT NULL, `passkeyCredentialId` TEXT, `createdAt` INTEGER NOT NULL, `lastSeenAt` INTEGER NOT NULL, `isOnline` INTEGER NOT NULL, `lastSyncedAt` INTEGER, PRIMARY KEY(`uid`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_zixoNumber` ON `users` (`zixoNumber`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_lastSyncedAt` ON `users` (`lastSyncedAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f0e6acef0c21bc4bf6d214b4170efd92')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `chat_threads`");
        db.execSQL("DROP TABLE IF EXISTS `call_log`");
        db.execSQL("DROP TABLE IF EXISTS `messages`");
        db.execSQL("DROP TABLE IF EXISTS `contacts`");
        db.execSQL("DROP TABLE IF EXISTS `statuses`");
        db.execSQL("DROP TABLE IF EXISTS `users`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsChatThreads = new HashMap<String, TableInfo.Column>(9);
        _columnsChatThreads.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatThreads.put("participantUids", new TableInfo.Column("participantUids", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatThreads.put("lastMessage", new TableInfo.Column("lastMessage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatThreads.put("lastMessageTimestamp", new TableInfo.Column("lastMessageTimestamp", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatThreads.put("unreadCount", new TableInfo.Column("unreadCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatThreads.put("isPinned", new TableInfo.Column("isPinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatThreads.put("isMuted", new TableInfo.Column("isMuted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatThreads.put("threadType", new TableInfo.Column("threadType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatThreads.put("groupName", new TableInfo.Column("groupName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChatThreads = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChatThreads = new HashSet<TableInfo.Index>(2);
        _indicesChatThreads.add(new TableInfo.Index("index_chat_threads_participant_uids", false, Arrays.asList("participantUids"), Arrays.asList("ASC")));
        _indicesChatThreads.add(new TableInfo.Index("index_chat_threads_last_message_timestamp", false, Arrays.asList("lastMessageTimestamp"), Arrays.asList("ASC")));
        final TableInfo _infoChatThreads = new TableInfo("chat_threads", _columnsChatThreads, _foreignKeysChatThreads, _indicesChatThreads);
        final TableInfo _existingChatThreads = TableInfo.read(db, "chat_threads");
        if (!_infoChatThreads.equals(_existingChatThreads)) {
          return new RoomOpenHelper.ValidationResult(false, "chat_threads(com.zixo.app.data.local.room.entity.ChatEntity).\n"
                  + " Expected:\n" + _infoChatThreads + "\n"
                  + " Found:\n" + _existingChatThreads);
        }
        final HashMap<String, TableInfo.Column> _columnsCallLog = new HashMap<String, TableInfo.Column>(16);
        _columnsCallLog.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("callId", new TableInfo.Column("callId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("callerUid", new TableInfo.Column("callerUid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("calleeUid", new TableInfo.Column("calleeUid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("callerName", new TableInfo.Column("callerName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("calleeName", new TableInfo.Column("calleeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("callerAvatar", new TableInfo.Column("callerAvatar", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("calleeAvatar", new TableInfo.Column("calleeAvatar", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("isVideoCall", new TableInfo.Column("isVideoCall", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("isGroupCall", new TableInfo.Column("isGroupCall", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("endReason", new TableInfo.Column("endReason", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("threadId", new TableInfo.Column("threadId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCallLog.put("isRead", new TableInfo.Column("isRead", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCallLog = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCallLog = new HashSet<TableInfo.Index>(2);
        _indicesCallLog.add(new TableInfo.Index("index_call_log_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        _indicesCallLog.add(new TableInfo.Index("index_call_log_type", false, Arrays.asList("type"), Arrays.asList("ASC")));
        final TableInfo _infoCallLog = new TableInfo("call_log", _columnsCallLog, _foreignKeysCallLog, _indicesCallLog);
        final TableInfo _existingCallLog = TableInfo.read(db, "call_log");
        if (!_infoCallLog.equals(_existingCallLog)) {
          return new RoomOpenHelper.ValidationResult(false, "call_log(com.zixo.app.data.local.room.entity.CallLogEntity).\n"
                  + " Expected:\n" + _infoCallLog + "\n"
                  + " Found:\n" + _existingCallLog);
        }
        final HashMap<String, TableInfo.Column> _columnsMessages = new HashMap<String, TableInfo.Column>(19);
        _columnsMessages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("chatId", new TableInfo.Column("chatId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("senderId", new TableInfo.Column("senderId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("senderName", new TableInfo.Column("senderName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("senderAvatarUrl", new TableInfo.Column("senderAvatarUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("messageType", new TableInfo.Column("messageType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("mediaUrl", new TableInfo.Column("mediaUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("thumbnailUrl", new TableInfo.Column("thumbnailUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("replyToId", new TableInfo.Column("replyToId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("forwardedFrom", new TableInfo.Column("forwardedFrom", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("reactionsJson", new TableInfo.Column("reactionsJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isRead", new TableInfo.Column("isRead", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isDelivered", new TableInfo.Column("isDelivered", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isDeletedForMe", new TableInfo.Column("isDeletedForMe", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isDeletedForEveryone", new TableInfo.Column("isDeletedForEveryone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("syncedAt", new TableInfo.Column("syncedAt", "INTEGER", false, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessages = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysMessages.add(new TableInfo.ForeignKey("chat_threads", "CASCADE", "NO ACTION", Arrays.asList("chatId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesMessages = new HashSet<TableInfo.Index>(4);
        _indicesMessages.add(new TableInfo.Index("index_messages_chatId_createdAt", false, Arrays.asList("chatId", "createdAt"), Arrays.asList("ASC", "ASC")));
        _indicesMessages.add(new TableInfo.Index("index_messages_chatId_senderId", false, Arrays.asList("chatId", "senderId"), Arrays.asList("ASC", "ASC")));
        _indicesMessages.add(new TableInfo.Index("index_messages_senderId", false, Arrays.asList("senderId"), Arrays.asList("ASC")));
        _indicesMessages.add(new TableInfo.Index("index_messages_syncedAt", false, Arrays.asList("syncedAt"), Arrays.asList("ASC")));
        final TableInfo _infoMessages = new TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages);
        final TableInfo _existingMessages = TableInfo.read(db, "messages");
        if (!_infoMessages.equals(_existingMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "messages(com.zixo.app.data.local.room.entity.MessageEntity).\n"
                  + " Expected:\n" + _infoMessages + "\n"
                  + " Found:\n" + _existingMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsContacts = new HashMap<String, TableInfo.Column>(16);
        _columnsContacts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("contactUserId", new TableInfo.Column("contactUserId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("contactDisplayName", new TableInfo.Column("contactDisplayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("contactUsername", new TableInfo.Column("contactUsername", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("contactZixoNumber", new TableInfo.Column("contactZixoNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("contactAvatarUrl", new TableInfo.Column("contactAvatarUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("contactBio", new TableInfo.Column("contactBio", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("isMutual", new TableInfo.Column("isMutual", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("isVerifiedContact", new TableInfo.Column("isVerifiedContact", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("isBlocked", new TableInfo.Column("isBlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("isPinned", new TableInfo.Column("isPinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("isMuted", new TableInfo.Column("isMuted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("mutualVerifiedAt", new TableInfo.Column("mutualVerifiedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("lastSyncedAt", new TableInfo.Column("lastSyncedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysContacts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesContacts = new HashSet<TableInfo.Index>(5);
        _indicesContacts.add(new TableInfo.Index("index_contacts_contactUserId", false, Arrays.asList("contactUserId"), Arrays.asList("ASC")));
        _indicesContacts.add(new TableInfo.Index("index_contacts_contactZixoNumber", false, Arrays.asList("contactZixoNumber"), Arrays.asList("ASC")));
        _indicesContacts.add(new TableInfo.Index("index_contacts_isMutual", false, Arrays.asList("isMutual"), Arrays.asList("ASC")));
        _indicesContacts.add(new TableInfo.Index("index_contacts_isBlocked", false, Arrays.asList("isBlocked"), Arrays.asList("ASC")));
        _indicesContacts.add(new TableInfo.Index("index_contacts_lastSyncedAt", false, Arrays.asList("lastSyncedAt"), Arrays.asList("ASC")));
        final TableInfo _infoContacts = new TableInfo("contacts", _columnsContacts, _foreignKeysContacts, _indicesContacts);
        final TableInfo _existingContacts = TableInfo.read(db, "contacts");
        if (!_infoContacts.equals(_existingContacts)) {
          return new RoomOpenHelper.ValidationResult(false, "contacts(com.zixo.app.data.local.room.entity.ContactEntity).\n"
                  + " Expected:\n" + _infoContacts + "\n"
                  + " Found:\n" + _existingContacts);
        }
        final HashMap<String, TableInfo.Column> _columnsStatuses = new HashMap<String, TableInfo.Column>(16);
        _columnsStatuses.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("userName", new TableInfo.Column("userName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("userAvatarUrl", new TableInfo.Column("userAvatarUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("text", new TableInfo.Column("text", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("mediaUrl", new TableInfo.Column("mediaUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("mediaType", new TableInfo.Column("mediaType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("backgroundColor", new TableInfo.Column("backgroundColor", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("fontName", new TableInfo.Column("fontName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("visibility", new TableInfo.Column("visibility", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("viewersJson", new TableInfo.Column("viewersJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("expiresAt", new TableInfo.Column("expiresAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("isViewed", new TableInfo.Column("isViewed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("isMyStatus", new TableInfo.Column("isMyStatus", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStatuses.put("lastSyncedAt", new TableInfo.Column("lastSyncedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStatuses = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStatuses = new HashSet<TableInfo.Index>(4);
        _indicesStatuses.add(new TableInfo.Index("index_statuses_userId_createdAt", false, Arrays.asList("userId", "createdAt"), Arrays.asList("ASC", "ASC")));
        _indicesStatuses.add(new TableInfo.Index("index_statuses_expiresAt", false, Arrays.asList("expiresAt"), Arrays.asList("ASC")));
        _indicesStatuses.add(new TableInfo.Index("index_statuses_isMyStatus", false, Arrays.asList("isMyStatus"), Arrays.asList("ASC")));
        _indicesStatuses.add(new TableInfo.Index("index_statuses_lastSyncedAt", false, Arrays.asList("lastSyncedAt"), Arrays.asList("ASC")));
        final TableInfo _infoStatuses = new TableInfo("statuses", _columnsStatuses, _foreignKeysStatuses, _indicesStatuses);
        final TableInfo _existingStatuses = TableInfo.read(db, "statuses");
        if (!_infoStatuses.equals(_existingStatuses)) {
          return new RoomOpenHelper.ValidationResult(false, "statuses(com.zixo.app.data.local.room.entity.StatusEntity).\n"
                  + " Expected:\n" + _infoStatuses + "\n"
                  + " Found:\n" + _existingStatuses);
        }
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(13);
        _columnsUsers.put("uid", new TableInfo.Column("uid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("username", new TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("zixoNumber", new TableInfo.Column("zixoNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("photoUrl", new TableInfo.Column("photoUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("bio", new TableInfo.Column("bio", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("phoneNumber", new TableInfo.Column("phoneNumber", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("hasPasskey", new TableInfo.Column("hasPasskey", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("passkeyCredentialId", new TableInfo.Column("passkeyCredentialId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("lastSeenAt", new TableInfo.Column("lastSeenAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("isOnline", new TableInfo.Column("isOnline", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("lastSyncedAt", new TableInfo.Column("lastSyncedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(2);
        _indicesUsers.add(new TableInfo.Index("index_users_zixoNumber", false, Arrays.asList("zixoNumber"), Arrays.asList("ASC")));
        _indicesUsers.add(new TableInfo.Index("index_users_lastSyncedAt", false, Arrays.asList("lastSyncedAt"), Arrays.asList("ASC")));
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(com.zixo.app.data.local.room.entity.UserEntity).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f0e6acef0c21bc4bf6d214b4170efd92", "195da93e2b9398ee7b9b17e3b26ac099");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "chat_threads","call_log","messages","contacts","statuses","users");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `chat_threads`");
      _db.execSQL("DELETE FROM `call_log`");
      _db.execSQL("DELETE FROM `messages`");
      _db.execSQL("DELETE FROM `contacts`");
      _db.execSQL("DELETE FROM `statuses`");
      _db.execSQL("DELETE FROM `users`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ChatDao.class, ChatDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CallLogDao.class, CallLogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MessageDao.class, MessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ContactDao.class, ContactDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StatusDao.class, StatusDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ChatDao chatDao() {
    if (_chatDao != null) {
      return _chatDao;
    } else {
      synchronized(this) {
        if(_chatDao == null) {
          _chatDao = new ChatDao_Impl(this);
        }
        return _chatDao;
      }
    }
  }

  @Override
  public CallLogDao callLogDao() {
    if (_callLogDao != null) {
      return _callLogDao;
    } else {
      synchronized(this) {
        if(_callLogDao == null) {
          _callLogDao = new CallLogDao_Impl(this);
        }
        return _callLogDao;
      }
    }
  }

  @Override
  public MessageDao messageDao() {
    if (_messageDao != null) {
      return _messageDao;
    } else {
      synchronized(this) {
        if(_messageDao == null) {
          _messageDao = new MessageDao_Impl(this);
        }
        return _messageDao;
      }
    }
  }

  @Override
  public ContactDao contactDao() {
    if (_contactDao != null) {
      return _contactDao;
    } else {
      synchronized(this) {
        if(_contactDao == null) {
          _contactDao = new ContactDao_Impl(this);
        }
        return _contactDao;
      }
    }
  }

  @Override
  public StatusDao statusDao() {
    if (_statusDao != null) {
      return _statusDao;
    } else {
      synchronized(this) {
        if(_statusDao == null) {
          _statusDao = new StatusDao_Impl(this);
        }
        return _statusDao;
      }
    }
  }

  @Override
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }
}
