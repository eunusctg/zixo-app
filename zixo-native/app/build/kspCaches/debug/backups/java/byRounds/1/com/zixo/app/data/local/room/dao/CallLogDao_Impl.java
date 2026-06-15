package com.zixo.app.data.local.room.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.zixo.app.data.local.room.entity.CallLogEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class CallLogDao_Impl implements CallLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CallLogEntity> __insertionAdapterOfCallLogEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCall;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllCalls;

  public CallLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallLogEntity = new EntityInsertionAdapter<CallLogEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `call_log` (`id`,`callId`,`callerUid`,`calleeUid`,`callerName`,`calleeName`,`callerAvatar`,`calleeAvatar`,`type`,`isVideoCall`,`isGroupCall`,`duration`,`timestamp`,`endReason`,`threadId`,`isRead`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallLogEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getCallId());
        statement.bindString(3, entity.getCallerUid());
        statement.bindString(4, entity.getCalleeUid());
        statement.bindString(5, entity.getCallerName());
        statement.bindString(6, entity.getCalleeName());
        if (entity.getCallerAvatar() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getCallerAvatar());
        }
        if (entity.getCalleeAvatar() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getCalleeAvatar());
        }
        statement.bindString(9, entity.getType());
        final int _tmp = entity.isVideoCall() ? 1 : 0;
        statement.bindLong(10, _tmp);
        final int _tmp_1 = entity.isGroupCall() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        statement.bindLong(12, entity.getDuration());
        statement.bindLong(13, entity.getTimestamp());
        statement.bindString(14, entity.getEndReason());
        statement.bindString(15, entity.getThreadId());
        final int _tmp_2 = entity.isRead() ? 1 : 0;
        statement.bindLong(16, _tmp_2);
      }
    };
    this.__preparedStmtOfDeleteCall = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM call_log WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllCalls = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM call_log";
        return _query;
      }
    };
  }

  @Override
  public Object insertCall(final CallLogEntity call, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallLogEntity.insert(call);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<CallLogEntity> calls,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallLogEntity.insert(calls);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCall(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCall.acquire();
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
          __preparedStmtOfDeleteCall.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllCalls(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllCalls.acquire();
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
          __preparedStmtOfDeleteAllCalls.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CallLogEntity>> getAllCalls() {
    final String _sql = "SELECT * FROM call_log ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_log"}, new Callable<List<CallLogEntity>>() {
      @Override
      @NonNull
      public List<CallLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCallId = CursorUtil.getColumnIndexOrThrow(_cursor, "callId");
          final int _cursorIndexOfCallerUid = CursorUtil.getColumnIndexOrThrow(_cursor, "callerUid");
          final int _cursorIndexOfCalleeUid = CursorUtil.getColumnIndexOrThrow(_cursor, "calleeUid");
          final int _cursorIndexOfCallerName = CursorUtil.getColumnIndexOrThrow(_cursor, "callerName");
          final int _cursorIndexOfCalleeName = CursorUtil.getColumnIndexOrThrow(_cursor, "calleeName");
          final int _cursorIndexOfCallerAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "callerAvatar");
          final int _cursorIndexOfCalleeAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "calleeAvatar");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsVideoCall = CursorUtil.getColumnIndexOrThrow(_cursor, "isVideoCall");
          final int _cursorIndexOfIsGroupCall = CursorUtil.getColumnIndexOrThrow(_cursor, "isGroupCall");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfEndReason = CursorUtil.getColumnIndexOrThrow(_cursor, "endReason");
          final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "threadId");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final List<CallLogEntity> _result = new ArrayList<CallLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallLogEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCallId;
            _tmpCallId = _cursor.getString(_cursorIndexOfCallId);
            final String _tmpCallerUid;
            _tmpCallerUid = _cursor.getString(_cursorIndexOfCallerUid);
            final String _tmpCalleeUid;
            _tmpCalleeUid = _cursor.getString(_cursorIndexOfCalleeUid);
            final String _tmpCallerName;
            _tmpCallerName = _cursor.getString(_cursorIndexOfCallerName);
            final String _tmpCalleeName;
            _tmpCalleeName = _cursor.getString(_cursorIndexOfCalleeName);
            final String _tmpCallerAvatar;
            if (_cursor.isNull(_cursorIndexOfCallerAvatar)) {
              _tmpCallerAvatar = null;
            } else {
              _tmpCallerAvatar = _cursor.getString(_cursorIndexOfCallerAvatar);
            }
            final String _tmpCalleeAvatar;
            if (_cursor.isNull(_cursorIndexOfCalleeAvatar)) {
              _tmpCalleeAvatar = null;
            } else {
              _tmpCalleeAvatar = _cursor.getString(_cursorIndexOfCalleeAvatar);
            }
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final boolean _tmpIsVideoCall;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsVideoCall);
            _tmpIsVideoCall = _tmp != 0;
            final boolean _tmpIsGroupCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsGroupCall);
            _tmpIsGroupCall = _tmp_1 != 0;
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpEndReason;
            _tmpEndReason = _cursor.getString(_cursorIndexOfEndReason);
            final String _tmpThreadId;
            _tmpThreadId = _cursor.getString(_cursorIndexOfThreadId);
            final boolean _tmpIsRead;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp_2 != 0;
            _item = new CallLogEntity(_tmpId,_tmpCallId,_tmpCallerUid,_tmpCalleeUid,_tmpCallerName,_tmpCalleeName,_tmpCallerAvatar,_tmpCalleeAvatar,_tmpType,_tmpIsVideoCall,_tmpIsGroupCall,_tmpDuration,_tmpTimestamp,_tmpEndReason,_tmpThreadId,_tmpIsRead);
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
  public Flow<List<CallLogEntity>> getCallsByType(final String type) {
    final String _sql = "SELECT * FROM call_log WHERE type = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, type);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_log"}, new Callable<List<CallLogEntity>>() {
      @Override
      @NonNull
      public List<CallLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCallId = CursorUtil.getColumnIndexOrThrow(_cursor, "callId");
          final int _cursorIndexOfCallerUid = CursorUtil.getColumnIndexOrThrow(_cursor, "callerUid");
          final int _cursorIndexOfCalleeUid = CursorUtil.getColumnIndexOrThrow(_cursor, "calleeUid");
          final int _cursorIndexOfCallerName = CursorUtil.getColumnIndexOrThrow(_cursor, "callerName");
          final int _cursorIndexOfCalleeName = CursorUtil.getColumnIndexOrThrow(_cursor, "calleeName");
          final int _cursorIndexOfCallerAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "callerAvatar");
          final int _cursorIndexOfCalleeAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "calleeAvatar");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsVideoCall = CursorUtil.getColumnIndexOrThrow(_cursor, "isVideoCall");
          final int _cursorIndexOfIsGroupCall = CursorUtil.getColumnIndexOrThrow(_cursor, "isGroupCall");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfEndReason = CursorUtil.getColumnIndexOrThrow(_cursor, "endReason");
          final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "threadId");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final List<CallLogEntity> _result = new ArrayList<CallLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallLogEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCallId;
            _tmpCallId = _cursor.getString(_cursorIndexOfCallId);
            final String _tmpCallerUid;
            _tmpCallerUid = _cursor.getString(_cursorIndexOfCallerUid);
            final String _tmpCalleeUid;
            _tmpCalleeUid = _cursor.getString(_cursorIndexOfCalleeUid);
            final String _tmpCallerName;
            _tmpCallerName = _cursor.getString(_cursorIndexOfCallerName);
            final String _tmpCalleeName;
            _tmpCalleeName = _cursor.getString(_cursorIndexOfCalleeName);
            final String _tmpCallerAvatar;
            if (_cursor.isNull(_cursorIndexOfCallerAvatar)) {
              _tmpCallerAvatar = null;
            } else {
              _tmpCallerAvatar = _cursor.getString(_cursorIndexOfCallerAvatar);
            }
            final String _tmpCalleeAvatar;
            if (_cursor.isNull(_cursorIndexOfCalleeAvatar)) {
              _tmpCalleeAvatar = null;
            } else {
              _tmpCalleeAvatar = _cursor.getString(_cursorIndexOfCalleeAvatar);
            }
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final boolean _tmpIsVideoCall;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsVideoCall);
            _tmpIsVideoCall = _tmp != 0;
            final boolean _tmpIsGroupCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsGroupCall);
            _tmpIsGroupCall = _tmp_1 != 0;
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpEndReason;
            _tmpEndReason = _cursor.getString(_cursorIndexOfEndReason);
            final String _tmpThreadId;
            _tmpThreadId = _cursor.getString(_cursorIndexOfThreadId);
            final boolean _tmpIsRead;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp_2 != 0;
            _item = new CallLogEntity(_tmpId,_tmpCallId,_tmpCallerUid,_tmpCalleeUid,_tmpCallerName,_tmpCalleeName,_tmpCallerAvatar,_tmpCalleeAvatar,_tmpType,_tmpIsVideoCall,_tmpIsGroupCall,_tmpDuration,_tmpTimestamp,_tmpEndReason,_tmpThreadId,_tmpIsRead);
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
  public Flow<List<CallLogEntity>> getMissedCalls() {
    final String _sql = "SELECT * FROM call_log WHERE type = 'MISSED' ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_log"}, new Callable<List<CallLogEntity>>() {
      @Override
      @NonNull
      public List<CallLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCallId = CursorUtil.getColumnIndexOrThrow(_cursor, "callId");
          final int _cursorIndexOfCallerUid = CursorUtil.getColumnIndexOrThrow(_cursor, "callerUid");
          final int _cursorIndexOfCalleeUid = CursorUtil.getColumnIndexOrThrow(_cursor, "calleeUid");
          final int _cursorIndexOfCallerName = CursorUtil.getColumnIndexOrThrow(_cursor, "callerName");
          final int _cursorIndexOfCalleeName = CursorUtil.getColumnIndexOrThrow(_cursor, "calleeName");
          final int _cursorIndexOfCallerAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "callerAvatar");
          final int _cursorIndexOfCalleeAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "calleeAvatar");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsVideoCall = CursorUtil.getColumnIndexOrThrow(_cursor, "isVideoCall");
          final int _cursorIndexOfIsGroupCall = CursorUtil.getColumnIndexOrThrow(_cursor, "isGroupCall");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfEndReason = CursorUtil.getColumnIndexOrThrow(_cursor, "endReason");
          final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "threadId");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final List<CallLogEntity> _result = new ArrayList<CallLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallLogEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCallId;
            _tmpCallId = _cursor.getString(_cursorIndexOfCallId);
            final String _tmpCallerUid;
            _tmpCallerUid = _cursor.getString(_cursorIndexOfCallerUid);
            final String _tmpCalleeUid;
            _tmpCalleeUid = _cursor.getString(_cursorIndexOfCalleeUid);
            final String _tmpCallerName;
            _tmpCallerName = _cursor.getString(_cursorIndexOfCallerName);
            final String _tmpCalleeName;
            _tmpCalleeName = _cursor.getString(_cursorIndexOfCalleeName);
            final String _tmpCallerAvatar;
            if (_cursor.isNull(_cursorIndexOfCallerAvatar)) {
              _tmpCallerAvatar = null;
            } else {
              _tmpCallerAvatar = _cursor.getString(_cursorIndexOfCallerAvatar);
            }
            final String _tmpCalleeAvatar;
            if (_cursor.isNull(_cursorIndexOfCalleeAvatar)) {
              _tmpCalleeAvatar = null;
            } else {
              _tmpCalleeAvatar = _cursor.getString(_cursorIndexOfCalleeAvatar);
            }
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final boolean _tmpIsVideoCall;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsVideoCall);
            _tmpIsVideoCall = _tmp != 0;
            final boolean _tmpIsGroupCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsGroupCall);
            _tmpIsGroupCall = _tmp_1 != 0;
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpEndReason;
            _tmpEndReason = _cursor.getString(_cursorIndexOfEndReason);
            final String _tmpThreadId;
            _tmpThreadId = _cursor.getString(_cursorIndexOfThreadId);
            final boolean _tmpIsRead;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp_2 != 0;
            _item = new CallLogEntity(_tmpId,_tmpCallId,_tmpCallerUid,_tmpCalleeUid,_tmpCallerName,_tmpCalleeName,_tmpCallerAvatar,_tmpCalleeAvatar,_tmpType,_tmpIsVideoCall,_tmpIsGroupCall,_tmpDuration,_tmpTimestamp,_tmpEndReason,_tmpThreadId,_tmpIsRead);
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
  public Flow<Integer> getCallCount() {
    final String _sql = "SELECT COUNT(*) FROM call_log";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_log"}, new Callable<Integer>() {
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
