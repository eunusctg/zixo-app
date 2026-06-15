package com.zixo.app.data.local.room.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.zixo.app.data.local.room.entity.UserEntity;
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
public final class UserDao_Impl implements UserDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserEntity> __insertionAdapterOfUserEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePresence;

  private final SharedSQLiteStatement __preparedStmtOfUpdateProfile;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  private final SharedSQLiteStatement __preparedStmtOfCleanupStale;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final EntityUpsertionAdapter<UserEntity> __upsertionAdapterOfUserEntity;

  public UserDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserEntity = new EntityInsertionAdapter<UserEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `users` (`uid`,`displayName`,`username`,`zixoNumber`,`photoUrl`,`bio`,`phoneNumber`,`hasPasskey`,`passkeyCredentialId`,`createdAt`,`lastSeenAt`,`isOnline`,`lastSyncedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserEntity entity) {
        statement.bindString(1, entity.getUid());
        statement.bindString(2, entity.getDisplayName());
        statement.bindString(3, entity.getUsername());
        statement.bindString(4, entity.getZixoNumber());
        statement.bindString(5, entity.getPhotoUrl());
        statement.bindString(6, entity.getBio());
        if (entity.getPhoneNumber() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPhoneNumber());
        }
        final int _tmp = entity.getHasPasskey() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getPasskeyCredentialId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPasskeyCredentialId());
        }
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getLastSeenAt());
        final int _tmp_1 = entity.isOnline() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        if (entity.getLastSyncedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getLastSyncedAt());
        }
      }
    };
    this.__preparedStmtOfUpdatePresence = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE users SET lastSeenAt = ?, isOnline = ? WHERE uid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateProfile = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE users SET displayName = ?, photoUrl = ?, bio = ?, lastSyncedAt = ? WHERE uid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE users SET lastSyncedAt = ? WHERE uid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfCleanupStale = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM users WHERE lastSyncedAt < ? AND uid != ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM users";
        return _query;
      }
    };
    this.__upsertionAdapterOfUserEntity = new EntityUpsertionAdapter<UserEntity>(new EntityInsertionAdapter<UserEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `users` (`uid`,`displayName`,`username`,`zixoNumber`,`photoUrl`,`bio`,`phoneNumber`,`hasPasskey`,`passkeyCredentialId`,`createdAt`,`lastSeenAt`,`isOnline`,`lastSyncedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserEntity entity) {
        statement.bindString(1, entity.getUid());
        statement.bindString(2, entity.getDisplayName());
        statement.bindString(3, entity.getUsername());
        statement.bindString(4, entity.getZixoNumber());
        statement.bindString(5, entity.getPhotoUrl());
        statement.bindString(6, entity.getBio());
        if (entity.getPhoneNumber() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPhoneNumber());
        }
        final int _tmp = entity.getHasPasskey() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getPasskeyCredentialId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPasskeyCredentialId());
        }
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getLastSeenAt());
        final int _tmp_1 = entity.isOnline() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        if (entity.getLastSyncedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getLastSyncedAt());
        }
      }
    }, new EntityDeletionOrUpdateAdapter<UserEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `users` SET `uid` = ?,`displayName` = ?,`username` = ?,`zixoNumber` = ?,`photoUrl` = ?,`bio` = ?,`phoneNumber` = ?,`hasPasskey` = ?,`passkeyCredentialId` = ?,`createdAt` = ?,`lastSeenAt` = ?,`isOnline` = ?,`lastSyncedAt` = ? WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserEntity entity) {
        statement.bindString(1, entity.getUid());
        statement.bindString(2, entity.getDisplayName());
        statement.bindString(3, entity.getUsername());
        statement.bindString(4, entity.getZixoNumber());
        statement.bindString(5, entity.getPhotoUrl());
        statement.bindString(6, entity.getBio());
        if (entity.getPhoneNumber() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPhoneNumber());
        }
        final int _tmp = entity.getHasPasskey() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getPasskeyCredentialId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPasskeyCredentialId());
        }
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getLastSeenAt());
        final int _tmp_1 = entity.isOnline() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        if (entity.getLastSyncedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getLastSyncedAt());
        }
        statement.bindString(14, entity.getUid());
      }
    });
  }

  @Override
  public Object insert(final UserEntity user, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserEntity.insert(user);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePresence(final String uid, final long lastSeen, final boolean isOnline,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePresence.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, lastSeen);
        _argIndex = 2;
        final int _tmp = isOnline ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 3;
        _stmt.bindString(_argIndex, uid);
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
          __preparedStmtOfUpdatePresence.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateProfile(final String uid, final String displayName, final String photoUrl,
      final String bio, final long syncTime, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateProfile.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, displayName);
        _argIndex = 2;
        _stmt.bindString(_argIndex, photoUrl);
        _argIndex = 3;
        _stmt.bindString(_argIndex, bio);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, syncTime);
        _argIndex = 5;
        _stmt.bindString(_argIndex, uid);
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
          __preparedStmtOfUpdateProfile.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final String uid, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, uid);
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
  public Object cleanupStale(final long threshold, final String currentUid,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfCleanupStale.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, threshold);
        _argIndex = 2;
        _stmt.bindString(_argIndex, currentUid);
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
          __preparedStmtOfCleanupStale.release(_stmt);
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
  public Object upsert(final UserEntity user, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfUserEntity.upsert(user);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<UserEntity> users,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfUserEntity.upsert(users);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByUid(final String uid, final Continuation<? super UserEntity> $completion) {
    final String _sql = "SELECT * FROM users WHERE uid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, uid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserEntity>() {
      @Override
      @Nullable
      public UserEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "zixoNumber");
          final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUrl");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfHasPasskey = CursorUtil.getColumnIndexOrThrow(_cursor, "hasPasskey");
          final int _cursorIndexOfPasskeyCredentialId = CursorUtil.getColumnIndexOrThrow(_cursor, "passkeyCredentialId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastSeenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenAt");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final UserEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpZixoNumber;
            _tmpZixoNumber = _cursor.getString(_cursorIndexOfZixoNumber);
            final String _tmpPhotoUrl;
            _tmpPhotoUrl = _cursor.getString(_cursorIndexOfPhotoUrl);
            final String _tmpBio;
            _tmpBio = _cursor.getString(_cursorIndexOfBio);
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final boolean _tmpHasPasskey;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasPasskey);
            _tmpHasPasskey = _tmp != 0;
            final String _tmpPasskeyCredentialId;
            if (_cursor.isNull(_cursorIndexOfPasskeyCredentialId)) {
              _tmpPasskeyCredentialId = null;
            } else {
              _tmpPasskeyCredentialId = _cursor.getString(_cursorIndexOfPasskeyCredentialId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastSeenAt;
            _tmpLastSeenAt = _cursor.getLong(_cursorIndexOfLastSeenAt);
            final boolean _tmpIsOnline;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp_1 != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _result = new UserEntity(_tmpUid,_tmpDisplayName,_tmpUsername,_tmpZixoNumber,_tmpPhotoUrl,_tmpBio,_tmpPhoneNumber,_tmpHasPasskey,_tmpPasskeyCredentialId,_tmpCreatedAt,_tmpLastSeenAt,_tmpIsOnline,_tmpLastSyncedAt);
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
  public Flow<UserEntity> observeByUid(final String uid) {
    final String _sql = "SELECT * FROM users WHERE uid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, uid);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"users"}, new Callable<UserEntity>() {
      @Override
      @Nullable
      public UserEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "zixoNumber");
          final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUrl");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfHasPasskey = CursorUtil.getColumnIndexOrThrow(_cursor, "hasPasskey");
          final int _cursorIndexOfPasskeyCredentialId = CursorUtil.getColumnIndexOrThrow(_cursor, "passkeyCredentialId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastSeenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenAt");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final UserEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpZixoNumber;
            _tmpZixoNumber = _cursor.getString(_cursorIndexOfZixoNumber);
            final String _tmpPhotoUrl;
            _tmpPhotoUrl = _cursor.getString(_cursorIndexOfPhotoUrl);
            final String _tmpBio;
            _tmpBio = _cursor.getString(_cursorIndexOfBio);
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final boolean _tmpHasPasskey;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasPasskey);
            _tmpHasPasskey = _tmp != 0;
            final String _tmpPasskeyCredentialId;
            if (_cursor.isNull(_cursorIndexOfPasskeyCredentialId)) {
              _tmpPasskeyCredentialId = null;
            } else {
              _tmpPasskeyCredentialId = _cursor.getString(_cursorIndexOfPasskeyCredentialId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastSeenAt;
            _tmpLastSeenAt = _cursor.getLong(_cursorIndexOfLastSeenAt);
            final boolean _tmpIsOnline;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp_1 != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _result = new UserEntity(_tmpUid,_tmpDisplayName,_tmpUsername,_tmpZixoNumber,_tmpPhotoUrl,_tmpBio,_tmpPhoneNumber,_tmpHasPasskey,_tmpPasskeyCredentialId,_tmpCreatedAt,_tmpLastSeenAt,_tmpIsOnline,_tmpLastSyncedAt);
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
  public Object getByZixoNumber(final String zixoNumber,
      final Continuation<? super UserEntity> $completion) {
    final String _sql = "SELECT * FROM users WHERE zixoNumber = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, zixoNumber);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserEntity>() {
      @Override
      @Nullable
      public UserEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "zixoNumber");
          final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUrl");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfHasPasskey = CursorUtil.getColumnIndexOrThrow(_cursor, "hasPasskey");
          final int _cursorIndexOfPasskeyCredentialId = CursorUtil.getColumnIndexOrThrow(_cursor, "passkeyCredentialId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastSeenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenAt");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final UserEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpZixoNumber;
            _tmpZixoNumber = _cursor.getString(_cursorIndexOfZixoNumber);
            final String _tmpPhotoUrl;
            _tmpPhotoUrl = _cursor.getString(_cursorIndexOfPhotoUrl);
            final String _tmpBio;
            _tmpBio = _cursor.getString(_cursorIndexOfBio);
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final boolean _tmpHasPasskey;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasPasskey);
            _tmpHasPasskey = _tmp != 0;
            final String _tmpPasskeyCredentialId;
            if (_cursor.isNull(_cursorIndexOfPasskeyCredentialId)) {
              _tmpPasskeyCredentialId = null;
            } else {
              _tmpPasskeyCredentialId = _cursor.getString(_cursorIndexOfPasskeyCredentialId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastSeenAt;
            _tmpLastSeenAt = _cursor.getLong(_cursorIndexOfLastSeenAt);
            final boolean _tmpIsOnline;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp_1 != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _result = new UserEntity(_tmpUid,_tmpDisplayName,_tmpUsername,_tmpZixoNumber,_tmpPhotoUrl,_tmpBio,_tmpPhoneNumber,_tmpHasPasskey,_tmpPasskeyCredentialId,_tmpCreatedAt,_tmpLastSeenAt,_tmpIsOnline,_tmpLastSyncedAt);
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
  public Object getStaleUsers(final long threshold,
      final Continuation<? super List<UserEntity>> $completion) {
    final String _sql = "SELECT * FROM users WHERE lastSyncedAt IS NULL OR lastSyncedAt < ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, threshold);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<UserEntity>>() {
      @Override
      @NonNull
      public List<UserEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "zixoNumber");
          final int _cursorIndexOfPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUrl");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfHasPasskey = CursorUtil.getColumnIndexOrThrow(_cursor, "hasPasskey");
          final int _cursorIndexOfPasskeyCredentialId = CursorUtil.getColumnIndexOrThrow(_cursor, "passkeyCredentialId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastSeenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenAt");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<UserEntity> _result = new ArrayList<UserEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UserEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpZixoNumber;
            _tmpZixoNumber = _cursor.getString(_cursorIndexOfZixoNumber);
            final String _tmpPhotoUrl;
            _tmpPhotoUrl = _cursor.getString(_cursorIndexOfPhotoUrl);
            final String _tmpBio;
            _tmpBio = _cursor.getString(_cursorIndexOfBio);
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final boolean _tmpHasPasskey;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasPasskey);
            _tmpHasPasskey = _tmp != 0;
            final String _tmpPasskeyCredentialId;
            if (_cursor.isNull(_cursorIndexOfPasskeyCredentialId)) {
              _tmpPasskeyCredentialId = null;
            } else {
              _tmpPasskeyCredentialId = _cursor.getString(_cursorIndexOfPasskeyCredentialId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastSeenAt;
            _tmpLastSeenAt = _cursor.getLong(_cursorIndexOfLastSeenAt);
            final boolean _tmpIsOnline;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp_1 != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new UserEntity(_tmpUid,_tmpDisplayName,_tmpUsername,_tmpZixoNumber,_tmpPhotoUrl,_tmpBio,_tmpPhoneNumber,_tmpHasPasskey,_tmpPasskeyCredentialId,_tmpCreatedAt,_tmpLastSeenAt,_tmpIsOnline,_tmpLastSyncedAt);
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
  public Object getCachedUserCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM users";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
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
