package com.zixo.app.data.local.room.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.zixo.app.data.local.room.entity.MessageEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MessageDao_Impl implements MessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MessageEntity> __insertionAdapterOfMessageEntity;

  private final EntityDeletionOrUpdateAdapter<MessageEntity> __deletionAdapterOfMessageEntity;

  private final EntityDeletionOrUpdateAdapter<MessageEntity> __updateAdapterOfMessageEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfMarkAllRead;

  private final SharedSQLiteStatement __preparedStmtOfMarkRead;

  private final SharedSQLiteStatement __preparedStmtOfMarkDelivered;

  private final SharedSQLiteStatement __preparedStmtOfMarkDeletedForMe;

  private final SharedSQLiteStatement __preparedStmtOfMarkDeletedForEveryone;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  private final SharedSQLiteStatement __preparedStmtOfCleanupDeletedForMe;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByChatId;

  public MessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMessageEntity = new EntityInsertionAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `messages` (`id`,`chatId`,`senderId`,`senderName`,`senderAvatarUrl`,`content`,`messageType`,`mediaUrl`,`thumbnailUrl`,`replyToId`,`forwardedFrom`,`reactionsJson`,`isRead`,`isDelivered`,`isDeletedForMe`,`isDeletedForEveryone`,`createdAt`,`updatedAt`,`syncedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getChatId());
        statement.bindString(3, entity.getSenderId());
        statement.bindString(4, entity.getSenderName());
        statement.bindString(5, entity.getSenderAvatarUrl());
        statement.bindString(6, entity.getContent());
        statement.bindString(7, entity.getMessageType());
        if (entity.getMediaUrl() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getMediaUrl());
        }
        if (entity.getThumbnailUrl() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getThumbnailUrl());
        }
        if (entity.getReplyToId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getReplyToId());
        }
        if (entity.getForwardedFrom() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getForwardedFrom());
        }
        if (entity.getReactionsJson() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getReactionsJson());
        }
        final int _tmp = entity.isRead() ? 1 : 0;
        statement.bindLong(13, _tmp);
        final int _tmp_1 = entity.isDelivered() ? 1 : 0;
        statement.bindLong(14, _tmp_1);
        final int _tmp_2 = entity.isDeletedForMe() ? 1 : 0;
        statement.bindLong(15, _tmp_2);
        final int _tmp_3 = entity.isDeletedForEveryone() ? 1 : 0;
        statement.bindLong(16, _tmp_3);
        statement.bindLong(17, entity.getCreatedAt());
        statement.bindLong(18, entity.getUpdatedAt());
        if (entity.getSyncedAt() == null) {
          statement.bindNull(19);
        } else {
          statement.bindLong(19, entity.getSyncedAt());
        }
      }
    };
    this.__deletionAdapterOfMessageEntity = new EntityDeletionOrUpdateAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `messages` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfMessageEntity = new EntityDeletionOrUpdateAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `messages` SET `id` = ?,`chatId` = ?,`senderId` = ?,`senderName` = ?,`senderAvatarUrl` = ?,`content` = ?,`messageType` = ?,`mediaUrl` = ?,`thumbnailUrl` = ?,`replyToId` = ?,`forwardedFrom` = ?,`reactionsJson` = ?,`isRead` = ?,`isDelivered` = ?,`isDeletedForMe` = ?,`isDeletedForEveryone` = ?,`createdAt` = ?,`updatedAt` = ?,`syncedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getChatId());
        statement.bindString(3, entity.getSenderId());
        statement.bindString(4, entity.getSenderName());
        statement.bindString(5, entity.getSenderAvatarUrl());
        statement.bindString(6, entity.getContent());
        statement.bindString(7, entity.getMessageType());
        if (entity.getMediaUrl() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getMediaUrl());
        }
        if (entity.getThumbnailUrl() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getThumbnailUrl());
        }
        if (entity.getReplyToId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getReplyToId());
        }
        if (entity.getForwardedFrom() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getForwardedFrom());
        }
        if (entity.getReactionsJson() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getReactionsJson());
        }
        final int _tmp = entity.isRead() ? 1 : 0;
        statement.bindLong(13, _tmp);
        final int _tmp_1 = entity.isDelivered() ? 1 : 0;
        statement.bindLong(14, _tmp_1);
        final int _tmp_2 = entity.isDeletedForMe() ? 1 : 0;
        statement.bindLong(15, _tmp_2);
        final int _tmp_3 = entity.isDeletedForEveryone() ? 1 : 0;
        statement.bindLong(16, _tmp_3);
        statement.bindLong(17, entity.getCreatedAt());
        statement.bindLong(18, entity.getUpdatedAt());
        if (entity.getSyncedAt() == null) {
          statement.bindNull(19);
        } else {
          statement.bindLong(19, entity.getSyncedAt());
        }
        statement.bindString(20, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkAllRead = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET isRead = 1 WHERE chatId = ? AND senderId != ? AND isRead = 0";
        return _query;
      }
    };
    this.__preparedStmtOfMarkRead = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET isRead = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkDelivered = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET isDelivered = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkDeletedForMe = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET isDeletedForMe = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkDeletedForEveryone = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET isDeletedForEveryone = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET syncedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfCleanupDeletedForMe = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE chatId = ? AND isDeletedForMe = 1";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByChatId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE chatId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MessageEntity message, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMessageEntity.insert(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<MessageEntity> messages,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMessageEntity.insert(messages);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final MessageEntity message, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMessageEntity.handle(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final MessageEntity message, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMessageEntity.handle(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String messageId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, messageId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markAllRead(final String chatId, final String currentUid,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAllRead.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, chatId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, currentUid);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkAllRead.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markRead(final String messageId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkRead.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, messageId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkRead.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markDelivered(final String messageId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkDelivered.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, messageId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkDelivered.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markDeletedForMe(final String messageId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkDeletedForMe.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, messageId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkDeletedForMe.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markDeletedForEveryone(final String messageId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkDeletedForEveryone.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, messageId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkDeletedForEveryone.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final String messageId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, messageId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object cleanupDeletedForMe(final String chatId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfCleanupDeletedForMe.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, chatId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfCleanupDeletedForMe.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByChatId(final String chatId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByChatId.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, chatId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByChatId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final String messageId,
      final Continuation<? super MessageEntity> $completion) {
    final String _sql = "SELECT * FROM messages WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, messageId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MessageEntity>() {
      @Override
      @Nullable
      public MessageEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfSenderAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "senderAvatarUrl");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfMessageType = CursorUtil.getColumnIndexOrThrow(_cursor, "messageType");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfForwardedFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "forwardedFrom");
          final int _cursorIndexOfReactionsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "reactionsJson");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfIsDelivered = CursorUtil.getColumnIndexOrThrow(_cursor, "isDelivered");
          final int _cursorIndexOfIsDeletedForMe = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForMe");
          final int _cursorIndexOfIsDeletedForEveryone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForEveryone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final MessageEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpSenderAvatarUrl;
            _tmpSenderAvatarUrl = _cursor.getString(_cursorIndexOfSenderAvatarUrl);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpMessageType;
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType);
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpReplyToId;
            if (_cursor.isNull(_cursorIndexOfReplyToId)) {
              _tmpReplyToId = null;
            } else {
              _tmpReplyToId = _cursor.getString(_cursorIndexOfReplyToId);
            }
            final String _tmpForwardedFrom;
            if (_cursor.isNull(_cursorIndexOfForwardedFrom)) {
              _tmpForwardedFrom = null;
            } else {
              _tmpForwardedFrom = _cursor.getString(_cursorIndexOfForwardedFrom);
            }
            final String _tmpReactionsJson;
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null;
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson);
            }
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final boolean _tmpIsDelivered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDelivered);
            _tmpIsDelivered = _tmp_1 != 0;
            final boolean _tmpIsDeletedForMe;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeletedForMe);
            _tmpIsDeletedForMe = _tmp_2 != 0;
            final boolean _tmpIsDeletedForEveryone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeletedForEveryone);
            _tmpIsDeletedForEveryone = _tmp_3 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            _result = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpSenderName,_tmpSenderAvatarUrl,_tmpContent,_tmpMessageType,_tmpMediaUrl,_tmpThumbnailUrl,_tmpReplyToId,_tmpForwardedFrom,_tmpReactionsJson,_tmpIsRead,_tmpIsDelivered,_tmpIsDeletedForMe,_tmpIsDeletedForEveryone,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MessageEntity>> getMessagesForChat(final String chatId) {
    final String _sql = "SELECT * FROM messages WHERE chatId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"messages"}, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfSenderAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "senderAvatarUrl");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfMessageType = CursorUtil.getColumnIndexOrThrow(_cursor, "messageType");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfForwardedFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "forwardedFrom");
          final int _cursorIndexOfReactionsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "reactionsJson");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfIsDelivered = CursorUtil.getColumnIndexOrThrow(_cursor, "isDelivered");
          final int _cursorIndexOfIsDeletedForMe = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForMe");
          final int _cursorIndexOfIsDeletedForEveryone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForEveryone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpSenderAvatarUrl;
            _tmpSenderAvatarUrl = _cursor.getString(_cursorIndexOfSenderAvatarUrl);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpMessageType;
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType);
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpReplyToId;
            if (_cursor.isNull(_cursorIndexOfReplyToId)) {
              _tmpReplyToId = null;
            } else {
              _tmpReplyToId = _cursor.getString(_cursorIndexOfReplyToId);
            }
            final String _tmpForwardedFrom;
            if (_cursor.isNull(_cursorIndexOfForwardedFrom)) {
              _tmpForwardedFrom = null;
            } else {
              _tmpForwardedFrom = _cursor.getString(_cursorIndexOfForwardedFrom);
            }
            final String _tmpReactionsJson;
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null;
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson);
            }
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final boolean _tmpIsDelivered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDelivered);
            _tmpIsDelivered = _tmp_1 != 0;
            final boolean _tmpIsDeletedForMe;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeletedForMe);
            _tmpIsDeletedForMe = _tmp_2 != 0;
            final boolean _tmpIsDeletedForEveryone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeletedForEveryone);
            _tmpIsDeletedForEveryone = _tmp_3 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            _item = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpSenderName,_tmpSenderAvatarUrl,_tmpContent,_tmpMessageType,_tmpMediaUrl,_tmpThumbnailUrl,_tmpReplyToId,_tmpForwardedFrom,_tmpReactionsJson,_tmpIsRead,_tmpIsDelivered,_tmpIsDeletedForMe,_tmpIsDeletedForEveryone,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getMessagesForChatPaginated(final String chatId, final int limit, final int offset,
      final Continuation<? super List<MessageEntity>> $completion) {
    final String _sql = "SELECT * FROM messages WHERE chatId = ? ORDER BY createdAt DESC LIMIT ? OFFSET ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    _argIndex = 3;
    _statement.bindLong(_argIndex, offset);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfSenderAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "senderAvatarUrl");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfMessageType = CursorUtil.getColumnIndexOrThrow(_cursor, "messageType");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfForwardedFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "forwardedFrom");
          final int _cursorIndexOfReactionsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "reactionsJson");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfIsDelivered = CursorUtil.getColumnIndexOrThrow(_cursor, "isDelivered");
          final int _cursorIndexOfIsDeletedForMe = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForMe");
          final int _cursorIndexOfIsDeletedForEveryone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForEveryone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpSenderAvatarUrl;
            _tmpSenderAvatarUrl = _cursor.getString(_cursorIndexOfSenderAvatarUrl);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpMessageType;
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType);
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpReplyToId;
            if (_cursor.isNull(_cursorIndexOfReplyToId)) {
              _tmpReplyToId = null;
            } else {
              _tmpReplyToId = _cursor.getString(_cursorIndexOfReplyToId);
            }
            final String _tmpForwardedFrom;
            if (_cursor.isNull(_cursorIndexOfForwardedFrom)) {
              _tmpForwardedFrom = null;
            } else {
              _tmpForwardedFrom = _cursor.getString(_cursorIndexOfForwardedFrom);
            }
            final String _tmpReactionsJson;
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null;
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson);
            }
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final boolean _tmpIsDelivered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDelivered);
            _tmpIsDelivered = _tmp_1 != 0;
            final boolean _tmpIsDeletedForMe;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeletedForMe);
            _tmpIsDeletedForMe = _tmp_2 != 0;
            final boolean _tmpIsDeletedForEveryone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeletedForEveryone);
            _tmpIsDeletedForEveryone = _tmp_3 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            _item = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpSenderName,_tmpSenderAvatarUrl,_tmpContent,_tmpMessageType,_tmpMediaUrl,_tmpThumbnailUrl,_tmpReplyToId,_tmpForwardedFrom,_tmpReactionsJson,_tmpIsRead,_tmpIsDelivered,_tmpIsDeletedForMe,_tmpIsDeletedForEveryone,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MessageEntity>> searchMessages(final String chatId, final String query) {
    final String _sql = "SELECT * FROM messages WHERE chatId = ? AND content LIKE '%' || ? || '%' ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"messages"}, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfSenderAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "senderAvatarUrl");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfMessageType = CursorUtil.getColumnIndexOrThrow(_cursor, "messageType");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfForwardedFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "forwardedFrom");
          final int _cursorIndexOfReactionsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "reactionsJson");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfIsDelivered = CursorUtil.getColumnIndexOrThrow(_cursor, "isDelivered");
          final int _cursorIndexOfIsDeletedForMe = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForMe");
          final int _cursorIndexOfIsDeletedForEveryone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForEveryone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpSenderAvatarUrl;
            _tmpSenderAvatarUrl = _cursor.getString(_cursorIndexOfSenderAvatarUrl);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpMessageType;
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType);
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpReplyToId;
            if (_cursor.isNull(_cursorIndexOfReplyToId)) {
              _tmpReplyToId = null;
            } else {
              _tmpReplyToId = _cursor.getString(_cursorIndexOfReplyToId);
            }
            final String _tmpForwardedFrom;
            if (_cursor.isNull(_cursorIndexOfForwardedFrom)) {
              _tmpForwardedFrom = null;
            } else {
              _tmpForwardedFrom = _cursor.getString(_cursorIndexOfForwardedFrom);
            }
            final String _tmpReactionsJson;
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null;
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson);
            }
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final boolean _tmpIsDelivered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDelivered);
            _tmpIsDelivered = _tmp_1 != 0;
            final boolean _tmpIsDeletedForMe;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeletedForMe);
            _tmpIsDeletedForMe = _tmp_2 != 0;
            final boolean _tmpIsDeletedForEveryone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeletedForEveryone);
            _tmpIsDeletedForEveryone = _tmp_3 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            _item = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpSenderName,_tmpSenderAvatarUrl,_tmpContent,_tmpMessageType,_tmpMediaUrl,_tmpThumbnailUrl,_tmpReplyToId,_tmpForwardedFrom,_tmpReactionsJson,_tmpIsRead,_tmpIsDelivered,_tmpIsDeletedForMe,_tmpIsDeletedForEveryone,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object searchAllMessages(final String query, final int limit,
      final Continuation<? super List<MessageEntity>> $completion) {
    final String _sql = "SELECT * FROM messages WHERE content LIKE '%' || ? || '%' ORDER BY createdAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfSenderAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "senderAvatarUrl");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfMessageType = CursorUtil.getColumnIndexOrThrow(_cursor, "messageType");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfForwardedFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "forwardedFrom");
          final int _cursorIndexOfReactionsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "reactionsJson");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfIsDelivered = CursorUtil.getColumnIndexOrThrow(_cursor, "isDelivered");
          final int _cursorIndexOfIsDeletedForMe = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForMe");
          final int _cursorIndexOfIsDeletedForEveryone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForEveryone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpSenderAvatarUrl;
            _tmpSenderAvatarUrl = _cursor.getString(_cursorIndexOfSenderAvatarUrl);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpMessageType;
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType);
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpReplyToId;
            if (_cursor.isNull(_cursorIndexOfReplyToId)) {
              _tmpReplyToId = null;
            } else {
              _tmpReplyToId = _cursor.getString(_cursorIndexOfReplyToId);
            }
            final String _tmpForwardedFrom;
            if (_cursor.isNull(_cursorIndexOfForwardedFrom)) {
              _tmpForwardedFrom = null;
            } else {
              _tmpForwardedFrom = _cursor.getString(_cursorIndexOfForwardedFrom);
            }
            final String _tmpReactionsJson;
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null;
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson);
            }
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final boolean _tmpIsDelivered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDelivered);
            _tmpIsDelivered = _tmp_1 != 0;
            final boolean _tmpIsDeletedForMe;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeletedForMe);
            _tmpIsDeletedForMe = _tmp_2 != 0;
            final boolean _tmpIsDeletedForEveryone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeletedForEveryone);
            _tmpIsDeletedForEveryone = _tmp_3 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            _item = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpSenderName,_tmpSenderAvatarUrl,_tmpContent,_tmpMessageType,_tmpMediaUrl,_tmpThumbnailUrl,_tmpReplyToId,_tmpForwardedFrom,_tmpReactionsJson,_tmpIsRead,_tmpIsDelivered,_tmpIsDeletedForMe,_tmpIsDeletedForEveryone,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Integer> getUnreadCount(final String chatId, final String currentUid) {
    final String _sql = "SELECT COUNT(*) FROM messages WHERE chatId = ? AND isRead = 0 AND senderId != ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    _argIndex = 2;
    _statement.bindString(_argIndex, currentUid);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"messages"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getTotalUnreadCount(final String currentUid) {
    final String _sql = "SELECT COUNT(*) FROM messages WHERE isRead = 0 AND senderId != ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, currentUid);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"messages"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getUnsyncedMessages(final Continuation<? super List<MessageEntity>> $completion) {
    final String _sql = "SELECT * FROM messages WHERE syncedAt IS NULL ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfSenderAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "senderAvatarUrl");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfMessageType = CursorUtil.getColumnIndexOrThrow(_cursor, "messageType");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfForwardedFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "forwardedFrom");
          final int _cursorIndexOfReactionsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "reactionsJson");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfIsDelivered = CursorUtil.getColumnIndexOrThrow(_cursor, "isDelivered");
          final int _cursorIndexOfIsDeletedForMe = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForMe");
          final int _cursorIndexOfIsDeletedForEveryone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForEveryone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpSenderAvatarUrl;
            _tmpSenderAvatarUrl = _cursor.getString(_cursorIndexOfSenderAvatarUrl);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpMessageType;
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType);
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpReplyToId;
            if (_cursor.isNull(_cursorIndexOfReplyToId)) {
              _tmpReplyToId = null;
            } else {
              _tmpReplyToId = _cursor.getString(_cursorIndexOfReplyToId);
            }
            final String _tmpForwardedFrom;
            if (_cursor.isNull(_cursorIndexOfForwardedFrom)) {
              _tmpForwardedFrom = null;
            } else {
              _tmpForwardedFrom = _cursor.getString(_cursorIndexOfForwardedFrom);
            }
            final String _tmpReactionsJson;
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null;
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson);
            }
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final boolean _tmpIsDelivered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDelivered);
            _tmpIsDelivered = _tmp_1 != 0;
            final boolean _tmpIsDeletedForMe;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeletedForMe);
            _tmpIsDeletedForMe = _tmp_2 != 0;
            final boolean _tmpIsDeletedForEveryone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeletedForEveryone);
            _tmpIsDeletedForEveryone = _tmp_3 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            _item = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpSenderName,_tmpSenderAvatarUrl,_tmpContent,_tmpMessageType,_tmpMediaUrl,_tmpThumbnailUrl,_tmpReplyToId,_tmpForwardedFrom,_tmpReactionsJson,_tmpIsRead,_tmpIsDelivered,_tmpIsDeletedForMe,_tmpIsDeletedForEveryone,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLatestMessage(final String chatId,
      final Continuation<? super MessageEntity> $completion) {
    final String _sql = "SELECT * FROM messages WHERE chatId = ? ORDER BY createdAt DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MessageEntity>() {
      @Override
      @Nullable
      public MessageEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfSenderAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "senderAvatarUrl");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfMessageType = CursorUtil.getColumnIndexOrThrow(_cursor, "messageType");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfReplyToId = CursorUtil.getColumnIndexOrThrow(_cursor, "replyToId");
          final int _cursorIndexOfForwardedFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "forwardedFrom");
          final int _cursorIndexOfReactionsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "reactionsJson");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final int _cursorIndexOfIsDelivered = CursorUtil.getColumnIndexOrThrow(_cursor, "isDelivered");
          final int _cursorIndexOfIsDeletedForMe = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForMe");
          final int _cursorIndexOfIsDeletedForEveryone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeletedForEveryone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final MessageEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpSenderAvatarUrl;
            _tmpSenderAvatarUrl = _cursor.getString(_cursorIndexOfSenderAvatarUrl);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpMessageType;
            _tmpMessageType = _cursor.getString(_cursorIndexOfMessageType);
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpReplyToId;
            if (_cursor.isNull(_cursorIndexOfReplyToId)) {
              _tmpReplyToId = null;
            } else {
              _tmpReplyToId = _cursor.getString(_cursorIndexOfReplyToId);
            }
            final String _tmpForwardedFrom;
            if (_cursor.isNull(_cursorIndexOfForwardedFrom)) {
              _tmpForwardedFrom = null;
            } else {
              _tmpForwardedFrom = _cursor.getString(_cursorIndexOfForwardedFrom);
            }
            final String _tmpReactionsJson;
            if (_cursor.isNull(_cursorIndexOfReactionsJson)) {
              _tmpReactionsJson = null;
            } else {
              _tmpReactionsJson = _cursor.getString(_cursorIndexOfReactionsJson);
            }
            final boolean _tmpIsRead;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp != 0;
            final boolean _tmpIsDelivered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDelivered);
            _tmpIsDelivered = _tmp_1 != 0;
            final boolean _tmpIsDeletedForMe;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDeletedForMe);
            _tmpIsDeletedForMe = _tmp_2 != 0;
            final boolean _tmpIsDeletedForEveryone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsDeletedForEveryone);
            _tmpIsDeletedForEveryone = _tmp_3 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            _result = new MessageEntity(_tmpId,_tmpChatId,_tmpSenderId,_tmpSenderName,_tmpSenderAvatarUrl,_tmpContent,_tmpMessageType,_tmpMediaUrl,_tmpThumbnailUrl,_tmpReplyToId,_tmpForwardedFrom,_tmpReactionsJson,_tmpIsRead,_tmpIsDelivered,_tmpIsDeletedForMe,_tmpIsDeletedForEveryone,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getMessageCount(final String chatId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM messages WHERE chatId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
