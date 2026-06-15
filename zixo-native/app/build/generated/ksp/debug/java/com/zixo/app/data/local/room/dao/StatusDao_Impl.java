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
import com.zixo.app.data.local.room.entity.StatusEntity;
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
public final class StatusDao_Impl implements StatusDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StatusEntity> __insertionAdapterOfStatusEntity;

  private final EntityDeletionOrUpdateAdapter<StatusEntity> __deletionAdapterOfStatusEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfMarkViewed;

  private final SharedSQLiteStatement __preparedStmtOfDeleteExpired;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public StatusDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStatusEntity = new EntityInsertionAdapter<StatusEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `statuses` (`id`,`userId`,`userName`,`userAvatarUrl`,`text`,`mediaUrl`,`mediaType`,`backgroundColor`,`fontName`,`visibility`,`viewersJson`,`createdAt`,`expiresAt`,`isViewed`,`isMyStatus`,`lastSyncedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StatusEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getUserName());
        statement.bindString(4, entity.getUserAvatarUrl());
        if (entity.getText() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getText());
        }
        if (entity.getMediaUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getMediaUrl());
        }
        statement.bindString(7, entity.getMediaType());
        if (entity.getBackgroundColor() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getBackgroundColor());
        }
        if (entity.getFontName() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getFontName());
        }
        statement.bindString(10, entity.getVisibility());
        if (entity.getViewersJson() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getViewersJson());
        }
        statement.bindLong(12, entity.getCreatedAt());
        statement.bindLong(13, entity.getExpiresAt());
        final int _tmp = entity.isViewed() ? 1 : 0;
        statement.bindLong(14, _tmp);
        final int _tmp_1 = entity.isMyStatus() ? 1 : 0;
        statement.bindLong(15, _tmp_1);
        if (entity.getLastSyncedAt() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getLastSyncedAt());
        }
      }
    };
    this.__deletionAdapterOfStatusEntity = new EntityDeletionOrUpdateAdapter<StatusEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `statuses` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StatusEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM statuses WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkViewed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE statuses SET isViewed = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteExpired = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM statuses WHERE expiresAt <= ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE statuses SET lastSyncedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM statuses";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final StatusEntity status, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStatusEntity.insert(status);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<StatusEntity> statuses,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStatusEntity.insert(statuses);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final StatusEntity status, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfStatusEntity.handle(status);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String statusId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, statusId);
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
  public Object markViewed(final String statusId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkViewed.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, statusId);
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
          __preparedStmtOfMarkViewed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteExpired(final long currentTime,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteExpired.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, currentTime);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteExpired.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final String statusId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, statusId);
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
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<StatusEntity>> getByUser(final String userId) {
    final String _sql = "SELECT * FROM statuses WHERE userId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"statuses"}, new Callable<List<StatusEntity>>() {
      @Override
      @NonNull
      public List<StatusEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfUserAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "userAvatarUrl");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundColor");
          final int _cursorIndexOfFontName = CursorUtil.getColumnIndexOrThrow(_cursor, "fontName");
          final int _cursorIndexOfVisibility = CursorUtil.getColumnIndexOrThrow(_cursor, "visibility");
          final int _cursorIndexOfViewersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "viewersJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfExpiresAt = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAt");
          final int _cursorIndexOfIsViewed = CursorUtil.getColumnIndexOrThrow(_cursor, "isViewed");
          final int _cursorIndexOfIsMyStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "isMyStatus");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<StatusEntity> _result = new ArrayList<StatusEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StatusEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpUserAvatarUrl;
            _tmpUserAvatarUrl = _cursor.getString(_cursorIndexOfUserAvatarUrl);
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpMediaType;
            _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            final String _tmpBackgroundColor;
            if (_cursor.isNull(_cursorIndexOfBackgroundColor)) {
              _tmpBackgroundColor = null;
            } else {
              _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            }
            final String _tmpFontName;
            if (_cursor.isNull(_cursorIndexOfFontName)) {
              _tmpFontName = null;
            } else {
              _tmpFontName = _cursor.getString(_cursorIndexOfFontName);
            }
            final String _tmpVisibility;
            _tmpVisibility = _cursor.getString(_cursorIndexOfVisibility);
            final String _tmpViewersJson;
            if (_cursor.isNull(_cursorIndexOfViewersJson)) {
              _tmpViewersJson = null;
            } else {
              _tmpViewersJson = _cursor.getString(_cursorIndexOfViewersJson);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpExpiresAt;
            _tmpExpiresAt = _cursor.getLong(_cursorIndexOfExpiresAt);
            final boolean _tmpIsViewed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsViewed);
            _tmpIsViewed = _tmp != 0;
            final boolean _tmpIsMyStatus;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMyStatus);
            _tmpIsMyStatus = _tmp_1 != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new StatusEntity(_tmpId,_tmpUserId,_tmpUserName,_tmpUserAvatarUrl,_tmpText,_tmpMediaUrl,_tmpMediaType,_tmpBackgroundColor,_tmpFontName,_tmpVisibility,_tmpViewersJson,_tmpCreatedAt,_tmpExpiresAt,_tmpIsViewed,_tmpIsMyStatus,_tmpLastSyncedAt);
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
  public Flow<List<StatusEntity>> getMyStatuses() {
    final String _sql = "SELECT * FROM statuses WHERE isMyStatus = 1 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"statuses"}, new Callable<List<StatusEntity>>() {
      @Override
      @NonNull
      public List<StatusEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfUserAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "userAvatarUrl");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundColor");
          final int _cursorIndexOfFontName = CursorUtil.getColumnIndexOrThrow(_cursor, "fontName");
          final int _cursorIndexOfVisibility = CursorUtil.getColumnIndexOrThrow(_cursor, "visibility");
          final int _cursorIndexOfViewersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "viewersJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfExpiresAt = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAt");
          final int _cursorIndexOfIsViewed = CursorUtil.getColumnIndexOrThrow(_cursor, "isViewed");
          final int _cursorIndexOfIsMyStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "isMyStatus");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<StatusEntity> _result = new ArrayList<StatusEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StatusEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpUserAvatarUrl;
            _tmpUserAvatarUrl = _cursor.getString(_cursorIndexOfUserAvatarUrl);
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpMediaType;
            _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            final String _tmpBackgroundColor;
            if (_cursor.isNull(_cursorIndexOfBackgroundColor)) {
              _tmpBackgroundColor = null;
            } else {
              _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            }
            final String _tmpFontName;
            if (_cursor.isNull(_cursorIndexOfFontName)) {
              _tmpFontName = null;
            } else {
              _tmpFontName = _cursor.getString(_cursorIndexOfFontName);
            }
            final String _tmpVisibility;
            _tmpVisibility = _cursor.getString(_cursorIndexOfVisibility);
            final String _tmpViewersJson;
            if (_cursor.isNull(_cursorIndexOfViewersJson)) {
              _tmpViewersJson = null;
            } else {
              _tmpViewersJson = _cursor.getString(_cursorIndexOfViewersJson);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpExpiresAt;
            _tmpExpiresAt = _cursor.getLong(_cursorIndexOfExpiresAt);
            final boolean _tmpIsViewed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsViewed);
            _tmpIsViewed = _tmp != 0;
            final boolean _tmpIsMyStatus;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMyStatus);
            _tmpIsMyStatus = _tmp_1 != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new StatusEntity(_tmpId,_tmpUserId,_tmpUserName,_tmpUserAvatarUrl,_tmpText,_tmpMediaUrl,_tmpMediaType,_tmpBackgroundColor,_tmpFontName,_tmpVisibility,_tmpViewersJson,_tmpCreatedAt,_tmpExpiresAt,_tmpIsViewed,_tmpIsMyStatus,_tmpLastSyncedAt);
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
  public Flow<List<StatusEntity>> getActiveStatuses(final long currentTime) {
    final String _sql = "SELECT * FROM statuses WHERE expiresAt > ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, currentTime);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"statuses"}, new Callable<List<StatusEntity>>() {
      @Override
      @NonNull
      public List<StatusEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfUserAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "userAvatarUrl");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundColor");
          final int _cursorIndexOfFontName = CursorUtil.getColumnIndexOrThrow(_cursor, "fontName");
          final int _cursorIndexOfVisibility = CursorUtil.getColumnIndexOrThrow(_cursor, "visibility");
          final int _cursorIndexOfViewersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "viewersJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfExpiresAt = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAt");
          final int _cursorIndexOfIsViewed = CursorUtil.getColumnIndexOrThrow(_cursor, "isViewed");
          final int _cursorIndexOfIsMyStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "isMyStatus");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<StatusEntity> _result = new ArrayList<StatusEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StatusEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpUserAvatarUrl;
            _tmpUserAvatarUrl = _cursor.getString(_cursorIndexOfUserAvatarUrl);
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpMediaType;
            _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            final String _tmpBackgroundColor;
            if (_cursor.isNull(_cursorIndexOfBackgroundColor)) {
              _tmpBackgroundColor = null;
            } else {
              _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            }
            final String _tmpFontName;
            if (_cursor.isNull(_cursorIndexOfFontName)) {
              _tmpFontName = null;
            } else {
              _tmpFontName = _cursor.getString(_cursorIndexOfFontName);
            }
            final String _tmpVisibility;
            _tmpVisibility = _cursor.getString(_cursorIndexOfVisibility);
            final String _tmpViewersJson;
            if (_cursor.isNull(_cursorIndexOfViewersJson)) {
              _tmpViewersJson = null;
            } else {
              _tmpViewersJson = _cursor.getString(_cursorIndexOfViewersJson);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpExpiresAt;
            _tmpExpiresAt = _cursor.getLong(_cursorIndexOfExpiresAt);
            final boolean _tmpIsViewed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsViewed);
            _tmpIsViewed = _tmp != 0;
            final boolean _tmpIsMyStatus;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMyStatus);
            _tmpIsMyStatus = _tmp_1 != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new StatusEntity(_tmpId,_tmpUserId,_tmpUserName,_tmpUserAvatarUrl,_tmpText,_tmpMediaUrl,_tmpMediaType,_tmpBackgroundColor,_tmpFontName,_tmpVisibility,_tmpViewersJson,_tmpCreatedAt,_tmpExpiresAt,_tmpIsViewed,_tmpIsMyStatus,_tmpLastSyncedAt);
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
  public Flow<List<StatusEntity>> getContactStatuses(final String currentUserId,
      final long currentTime) {
    final String _sql = "SELECT * FROM statuses WHERE expiresAt > ? AND userId != ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, currentTime);
    _argIndex = 2;
    _statement.bindString(_argIndex, currentUserId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"statuses"}, new Callable<List<StatusEntity>>() {
      @Override
      @NonNull
      public List<StatusEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfUserAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "userAvatarUrl");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundColor");
          final int _cursorIndexOfFontName = CursorUtil.getColumnIndexOrThrow(_cursor, "fontName");
          final int _cursorIndexOfVisibility = CursorUtil.getColumnIndexOrThrow(_cursor, "visibility");
          final int _cursorIndexOfViewersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "viewersJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfExpiresAt = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAt");
          final int _cursorIndexOfIsViewed = CursorUtil.getColumnIndexOrThrow(_cursor, "isViewed");
          final int _cursorIndexOfIsMyStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "isMyStatus");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<StatusEntity> _result = new ArrayList<StatusEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StatusEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpUserAvatarUrl;
            _tmpUserAvatarUrl = _cursor.getString(_cursorIndexOfUserAvatarUrl);
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpMediaType;
            _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            final String _tmpBackgroundColor;
            if (_cursor.isNull(_cursorIndexOfBackgroundColor)) {
              _tmpBackgroundColor = null;
            } else {
              _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            }
            final String _tmpFontName;
            if (_cursor.isNull(_cursorIndexOfFontName)) {
              _tmpFontName = null;
            } else {
              _tmpFontName = _cursor.getString(_cursorIndexOfFontName);
            }
            final String _tmpVisibility;
            _tmpVisibility = _cursor.getString(_cursorIndexOfVisibility);
            final String _tmpViewersJson;
            if (_cursor.isNull(_cursorIndexOfViewersJson)) {
              _tmpViewersJson = null;
            } else {
              _tmpViewersJson = _cursor.getString(_cursorIndexOfViewersJson);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpExpiresAt;
            _tmpExpiresAt = _cursor.getLong(_cursorIndexOfExpiresAt);
            final boolean _tmpIsViewed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsViewed);
            _tmpIsViewed = _tmp != 0;
            final boolean _tmpIsMyStatus;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMyStatus);
            _tmpIsMyStatus = _tmp_1 != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new StatusEntity(_tmpId,_tmpUserId,_tmpUserName,_tmpUserAvatarUrl,_tmpText,_tmpMediaUrl,_tmpMediaType,_tmpBackgroundColor,_tmpFontName,_tmpVisibility,_tmpViewersJson,_tmpCreatedAt,_tmpExpiresAt,_tmpIsViewed,_tmpIsMyStatus,_tmpLastSyncedAt);
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
  public Object getUnsyncedStatuses(final Continuation<? super List<StatusEntity>> $completion) {
    final String _sql = "SELECT * FROM statuses WHERE lastSyncedAt IS NULL";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<StatusEntity>>() {
      @Override
      @NonNull
      public List<StatusEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfUserAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "userAvatarUrl");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundColor");
          final int _cursorIndexOfFontName = CursorUtil.getColumnIndexOrThrow(_cursor, "fontName");
          final int _cursorIndexOfVisibility = CursorUtil.getColumnIndexOrThrow(_cursor, "visibility");
          final int _cursorIndexOfViewersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "viewersJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfExpiresAt = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAt");
          final int _cursorIndexOfIsViewed = CursorUtil.getColumnIndexOrThrow(_cursor, "isViewed");
          final int _cursorIndexOfIsMyStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "isMyStatus");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<StatusEntity> _result = new ArrayList<StatusEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StatusEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpUserAvatarUrl;
            _tmpUserAvatarUrl = _cursor.getString(_cursorIndexOfUserAvatarUrl);
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpMediaType;
            _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            final String _tmpBackgroundColor;
            if (_cursor.isNull(_cursorIndexOfBackgroundColor)) {
              _tmpBackgroundColor = null;
            } else {
              _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            }
            final String _tmpFontName;
            if (_cursor.isNull(_cursorIndexOfFontName)) {
              _tmpFontName = null;
            } else {
              _tmpFontName = _cursor.getString(_cursorIndexOfFontName);
            }
            final String _tmpVisibility;
            _tmpVisibility = _cursor.getString(_cursorIndexOfVisibility);
            final String _tmpViewersJson;
            if (_cursor.isNull(_cursorIndexOfViewersJson)) {
              _tmpViewersJson = null;
            } else {
              _tmpViewersJson = _cursor.getString(_cursorIndexOfViewersJson);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpExpiresAt;
            _tmpExpiresAt = _cursor.getLong(_cursorIndexOfExpiresAt);
            final boolean _tmpIsViewed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsViewed);
            _tmpIsViewed = _tmp != 0;
            final boolean _tmpIsMyStatus;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMyStatus);
            _tmpIsMyStatus = _tmp_1 != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new StatusEntity(_tmpId,_tmpUserId,_tmpUserName,_tmpUserAvatarUrl,_tmpText,_tmpMediaUrl,_tmpMediaType,_tmpBackgroundColor,_tmpFontName,_tmpVisibility,_tmpViewersJson,_tmpCreatedAt,_tmpExpiresAt,_tmpIsViewed,_tmpIsMyStatus,_tmpLastSyncedAt);
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
  public Object getById(final String statusId,
      final Continuation<? super StatusEntity> $completion) {
    final String _sql = "SELECT * FROM statuses WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, statusId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<StatusEntity>() {
      @Override
      @Nullable
      public StatusEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfUserAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "userAvatarUrl");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaUrl");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundColor");
          final int _cursorIndexOfFontName = CursorUtil.getColumnIndexOrThrow(_cursor, "fontName");
          final int _cursorIndexOfVisibility = CursorUtil.getColumnIndexOrThrow(_cursor, "visibility");
          final int _cursorIndexOfViewersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "viewersJson");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfExpiresAt = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAt");
          final int _cursorIndexOfIsViewed = CursorUtil.getColumnIndexOrThrow(_cursor, "isViewed");
          final int _cursorIndexOfIsMyStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "isMyStatus");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final StatusEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpUserAvatarUrl;
            _tmpUserAvatarUrl = _cursor.getString(_cursorIndexOfUserAvatarUrl);
            final String _tmpText;
            if (_cursor.isNull(_cursorIndexOfText)) {
              _tmpText = null;
            } else {
              _tmpText = _cursor.getString(_cursorIndexOfText);
            }
            final String _tmpMediaUrl;
            if (_cursor.isNull(_cursorIndexOfMediaUrl)) {
              _tmpMediaUrl = null;
            } else {
              _tmpMediaUrl = _cursor.getString(_cursorIndexOfMediaUrl);
            }
            final String _tmpMediaType;
            _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            final String _tmpBackgroundColor;
            if (_cursor.isNull(_cursorIndexOfBackgroundColor)) {
              _tmpBackgroundColor = null;
            } else {
              _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            }
            final String _tmpFontName;
            if (_cursor.isNull(_cursorIndexOfFontName)) {
              _tmpFontName = null;
            } else {
              _tmpFontName = _cursor.getString(_cursorIndexOfFontName);
            }
            final String _tmpVisibility;
            _tmpVisibility = _cursor.getString(_cursorIndexOfVisibility);
            final String _tmpViewersJson;
            if (_cursor.isNull(_cursorIndexOfViewersJson)) {
              _tmpViewersJson = null;
            } else {
              _tmpViewersJson = _cursor.getString(_cursorIndexOfViewersJson);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpExpiresAt;
            _tmpExpiresAt = _cursor.getLong(_cursorIndexOfExpiresAt);
            final boolean _tmpIsViewed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsViewed);
            _tmpIsViewed = _tmp != 0;
            final boolean _tmpIsMyStatus;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsMyStatus);
            _tmpIsMyStatus = _tmp_1 != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _result = new StatusEntity(_tmpId,_tmpUserId,_tmpUserName,_tmpUserAvatarUrl,_tmpText,_tmpMediaUrl,_tmpMediaType,_tmpBackgroundColor,_tmpFontName,_tmpVisibility,_tmpViewersJson,_tmpCreatedAt,_tmpExpiresAt,_tmpIsViewed,_tmpIsMyStatus,_tmpLastSyncedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
