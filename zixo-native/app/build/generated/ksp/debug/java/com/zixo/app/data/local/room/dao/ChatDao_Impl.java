package com.zixo.app.data.local.room.dao;

import android.database.Cursor;
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
import com.zixo.app.data.local.room.entity.ChatEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class ChatDao_Impl implements ChatDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChatEntity> __insertionAdapterOfChatEntity;

  private final EntityDeletionOrUpdateAdapter<ChatEntity> __updateAdapterOfChatEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteThread;

  public ChatDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChatEntity = new EntityInsertionAdapter<ChatEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `chat_threads` (`id`,`participantUids`,`lastMessage`,`lastMessageTimestamp`,`unreadCount`,`isPinned`,`isMuted`,`threadType`,`groupName`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChatEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getParticipantUids());
        if (entity.getLastMessage() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getLastMessage());
        }
        if (entity.getLastMessageTimestamp() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getLastMessageTimestamp());
        }
        statement.bindLong(5, entity.getUnreadCount());
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.isMuted() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        statement.bindString(8, entity.getThreadType());
        if (entity.getGroupName() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getGroupName());
        }
      }
    };
    this.__updateAdapterOfChatEntity = new EntityDeletionOrUpdateAdapter<ChatEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR REPLACE `chat_threads` SET `id` = ?,`participantUids` = ?,`lastMessage` = ?,`lastMessageTimestamp` = ?,`unreadCount` = ?,`isPinned` = ?,`isMuted` = ?,`threadType` = ?,`groupName` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChatEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getParticipantUids());
        if (entity.getLastMessage() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getLastMessage());
        }
        if (entity.getLastMessageTimestamp() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getLastMessageTimestamp());
        }
        statement.bindLong(5, entity.getUnreadCount());
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.isMuted() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        statement.bindString(8, entity.getThreadType());
        if (entity.getGroupName() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getGroupName());
        }
        statement.bindString(10, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteThread = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM chat_threads WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertThread(final ChatEntity thread,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChatEntity.insert(thread);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<ChatEntity> threads,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChatEntity.insert(threads);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateThread(final ChatEntity thread,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfChatEntity.handle(thread);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteThread(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteThread.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDeleteThread.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChatEntity>> getAllThreads() {
    final String _sql = "SELECT * FROM chat_threads ORDER BY isPinned DESC, lastMessageTimestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chat_threads"}, new Callable<List<ChatEntity>>() {
      @Override
      @NonNull
      public List<ChatEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfParticipantUids = CursorUtil.getColumnIndexOrThrow(_cursor, "participantUids");
          final int _cursorIndexOfLastMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessage");
          final int _cursorIndexOfLastMessageTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTimestamp");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfThreadType = CursorUtil.getColumnIndexOrThrow(_cursor, "threadType");
          final int _cursorIndexOfGroupName = CursorUtil.getColumnIndexOrThrow(_cursor, "groupName");
          final List<ChatEntity> _result = new ArrayList<ChatEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpParticipantUids;
            _tmpParticipantUids = _cursor.getString(_cursorIndexOfParticipantUids);
            final String _tmpLastMessage;
            if (_cursor.isNull(_cursorIndexOfLastMessage)) {
              _tmpLastMessage = null;
            } else {
              _tmpLastMessage = _cursor.getString(_cursorIndexOfLastMessage);
            }
            final Long _tmpLastMessageTimestamp;
            if (_cursor.isNull(_cursorIndexOfLastMessageTimestamp)) {
              _tmpLastMessageTimestamp = null;
            } else {
              _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp);
            }
            final int _tmpUnreadCount;
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsMuted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_1 != 0;
            final String _tmpThreadType;
            _tmpThreadType = _cursor.getString(_cursorIndexOfThreadType);
            final String _tmpGroupName;
            if (_cursor.isNull(_cursorIndexOfGroupName)) {
              _tmpGroupName = null;
            } else {
              _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            }
            _item = new ChatEntity(_tmpId,_tmpParticipantUids,_tmpLastMessage,_tmpLastMessageTimestamp,_tmpUnreadCount,_tmpIsPinned,_tmpIsMuted,_tmpThreadType,_tmpGroupName);
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
  public Flow<ChatEntity> getThreadById(final String id) {
    final String _sql = "SELECT * FROM chat_threads WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chat_threads"}, new Callable<ChatEntity>() {
      @Override
      @Nullable
      public ChatEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfParticipantUids = CursorUtil.getColumnIndexOrThrow(_cursor, "participantUids");
          final int _cursorIndexOfLastMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessage");
          final int _cursorIndexOfLastMessageTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTimestamp");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfThreadType = CursorUtil.getColumnIndexOrThrow(_cursor, "threadType");
          final int _cursorIndexOfGroupName = CursorUtil.getColumnIndexOrThrow(_cursor, "groupName");
          final ChatEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpParticipantUids;
            _tmpParticipantUids = _cursor.getString(_cursorIndexOfParticipantUids);
            final String _tmpLastMessage;
            if (_cursor.isNull(_cursorIndexOfLastMessage)) {
              _tmpLastMessage = null;
            } else {
              _tmpLastMessage = _cursor.getString(_cursorIndexOfLastMessage);
            }
            final Long _tmpLastMessageTimestamp;
            if (_cursor.isNull(_cursorIndexOfLastMessageTimestamp)) {
              _tmpLastMessageTimestamp = null;
            } else {
              _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp);
            }
            final int _tmpUnreadCount;
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsMuted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_1 != 0;
            final String _tmpThreadType;
            _tmpThreadType = _cursor.getString(_cursorIndexOfThreadType);
            final String _tmpGroupName;
            if (_cursor.isNull(_cursorIndexOfGroupName)) {
              _tmpGroupName = null;
            } else {
              _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            }
            _result = new ChatEntity(_tmpId,_tmpParticipantUids,_tmpLastMessage,_tmpLastMessageTimestamp,_tmpUnreadCount,_tmpIsPinned,_tmpIsMuted,_tmpThreadType,_tmpGroupName);
          } else {
            _result = null;
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
  public Flow<List<ChatEntity>> searchThreads(final String query) {
    final String _sql = "\n"
            + "        SELECT * FROM chat_threads\n"
            + "        WHERE lastMessage LIKE ?\n"
            + "           OR participantUids LIKE ?\n"
            + "        ORDER BY isPinned DESC, lastMessageTimestamp DESC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chat_threads"}, new Callable<List<ChatEntity>>() {
      @Override
      @NonNull
      public List<ChatEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfParticipantUids = CursorUtil.getColumnIndexOrThrow(_cursor, "participantUids");
          final int _cursorIndexOfLastMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessage");
          final int _cursorIndexOfLastMessageTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTimestamp");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfThreadType = CursorUtil.getColumnIndexOrThrow(_cursor, "threadType");
          final int _cursorIndexOfGroupName = CursorUtil.getColumnIndexOrThrow(_cursor, "groupName");
          final List<ChatEntity> _result = new ArrayList<ChatEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpParticipantUids;
            _tmpParticipantUids = _cursor.getString(_cursorIndexOfParticipantUids);
            final String _tmpLastMessage;
            if (_cursor.isNull(_cursorIndexOfLastMessage)) {
              _tmpLastMessage = null;
            } else {
              _tmpLastMessage = _cursor.getString(_cursorIndexOfLastMessage);
            }
            final Long _tmpLastMessageTimestamp;
            if (_cursor.isNull(_cursorIndexOfLastMessageTimestamp)) {
              _tmpLastMessageTimestamp = null;
            } else {
              _tmpLastMessageTimestamp = _cursor.getLong(_cursorIndexOfLastMessageTimestamp);
            }
            final int _tmpUnreadCount;
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final boolean _tmpIsMuted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_1 != 0;
            final String _tmpThreadType;
            _tmpThreadType = _cursor.getString(_cursorIndexOfThreadType);
            final String _tmpGroupName;
            if (_cursor.isNull(_cursorIndexOfGroupName)) {
              _tmpGroupName = null;
            } else {
              _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            }
            _item = new ChatEntity(_tmpId,_tmpParticipantUids,_tmpLastMessage,_tmpLastMessageTimestamp,_tmpUnreadCount,_tmpIsPinned,_tmpIsMuted,_tmpThreadType,_tmpGroupName);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
