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
import com.zixo.app.data.local.room.entity.ContactEntity;
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
public final class ContactDao_Impl implements ContactDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ContactEntity> __insertionAdapterOfContactEntity;

  private final EntityDeletionOrUpdateAdapter<ContactEntity> __deletionAdapterOfContactEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfSetBlocked;

  private final SharedSQLiteStatement __preparedStmtOfSetPinned;

  private final SharedSQLiteStatement __preparedStmtOfSetMuted;

  private final SharedSQLiteStatement __preparedStmtOfSetMutual;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final EntityUpsertionAdapter<ContactEntity> __upsertionAdapterOfContactEntity;

  public ContactDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfContactEntity = new EntityInsertionAdapter<ContactEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `contacts` (`id`,`userId`,`contactUserId`,`contactDisplayName`,`contactUsername`,`contactZixoNumber`,`contactAvatarUrl`,`contactBio`,`isMutual`,`isVerifiedContact`,`isBlocked`,`isPinned`,`isMuted`,`addedAt`,`mutualVerifiedAt`,`lastSyncedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ContactEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getContactUserId());
        statement.bindString(4, entity.getContactDisplayName());
        statement.bindString(5, entity.getContactUsername());
        statement.bindString(6, entity.getContactZixoNumber());
        statement.bindString(7, entity.getContactAvatarUrl());
        statement.bindString(8, entity.getContactBio());
        final int _tmp = entity.isMutual() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.isVerifiedContact() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.isBlocked() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        final int _tmp_3 = entity.isPinned() ? 1 : 0;
        statement.bindLong(12, _tmp_3);
        final int _tmp_4 = entity.isMuted() ? 1 : 0;
        statement.bindLong(13, _tmp_4);
        statement.bindLong(14, entity.getAddedAt());
        if (entity.getMutualVerifiedAt() == null) {
          statement.bindNull(15);
        } else {
          statement.bindLong(15, entity.getMutualVerifiedAt());
        }
        if (entity.getLastSyncedAt() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getLastSyncedAt());
        }
      }
    };
    this.__deletionAdapterOfContactEntity = new EntityDeletionOrUpdateAdapter<ContactEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `contacts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ContactEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM contacts WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetBlocked = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE contacts SET isBlocked = ? WHERE contactUserId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetPinned = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE contacts SET isPinned = ? WHERE contactUserId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetMuted = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE contacts SET isMuted = ? WHERE contactUserId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetMutual = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE contacts SET isMutual = ?, mutualVerifiedAt = ? WHERE contactUserId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE contacts SET lastSyncedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM contacts";
        return _query;
      }
    };
    this.__upsertionAdapterOfContactEntity = new EntityUpsertionAdapter<ContactEntity>(new EntityInsertionAdapter<ContactEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `contacts` (`id`,`userId`,`contactUserId`,`contactDisplayName`,`contactUsername`,`contactZixoNumber`,`contactAvatarUrl`,`contactBio`,`isMutual`,`isVerifiedContact`,`isBlocked`,`isPinned`,`isMuted`,`addedAt`,`mutualVerifiedAt`,`lastSyncedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ContactEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getContactUserId());
        statement.bindString(4, entity.getContactDisplayName());
        statement.bindString(5, entity.getContactUsername());
        statement.bindString(6, entity.getContactZixoNumber());
        statement.bindString(7, entity.getContactAvatarUrl());
        statement.bindString(8, entity.getContactBio());
        final int _tmp = entity.isMutual() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.isVerifiedContact() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.isBlocked() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        final int _tmp_3 = entity.isPinned() ? 1 : 0;
        statement.bindLong(12, _tmp_3);
        final int _tmp_4 = entity.isMuted() ? 1 : 0;
        statement.bindLong(13, _tmp_4);
        statement.bindLong(14, entity.getAddedAt());
        if (entity.getMutualVerifiedAt() == null) {
          statement.bindNull(15);
        } else {
          statement.bindLong(15, entity.getMutualVerifiedAt());
        }
        if (entity.getLastSyncedAt() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getLastSyncedAt());
        }
      }
    }, new EntityDeletionOrUpdateAdapter<ContactEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `contacts` SET `id` = ?,`userId` = ?,`contactUserId` = ?,`contactDisplayName` = ?,`contactUsername` = ?,`contactZixoNumber` = ?,`contactAvatarUrl` = ?,`contactBio` = ?,`isMutual` = ?,`isVerifiedContact` = ?,`isBlocked` = ?,`isPinned` = ?,`isMuted` = ?,`addedAt` = ?,`mutualVerifiedAt` = ?,`lastSyncedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ContactEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getContactUserId());
        statement.bindString(4, entity.getContactDisplayName());
        statement.bindString(5, entity.getContactUsername());
        statement.bindString(6, entity.getContactZixoNumber());
        statement.bindString(7, entity.getContactAvatarUrl());
        statement.bindString(8, entity.getContactBio());
        final int _tmp = entity.isMutual() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.isVerifiedContact() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.isBlocked() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        final int _tmp_3 = entity.isPinned() ? 1 : 0;
        statement.bindLong(12, _tmp_3);
        final int _tmp_4 = entity.isMuted() ? 1 : 0;
        statement.bindLong(13, _tmp_4);
        statement.bindLong(14, entity.getAddedAt());
        if (entity.getMutualVerifiedAt() == null) {
          statement.bindNull(15);
        } else {
          statement.bindLong(15, entity.getMutualVerifiedAt());
        }
        if (entity.getLastSyncedAt() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getLastSyncedAt());
        }
        statement.bindString(17, entity.getId());
      }
    });
  }

  @Override
  public Object insert(final ContactEntity contact, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfContactEntity.insert(contact);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final ContactEntity contact, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfContactEntity.handle(contact);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
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
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setBlocked(final String contactUserId, final boolean blocked,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetBlocked.acquire();
        int _argIndex = 1;
        final int _tmp = blocked ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, contactUserId);
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
          __preparedStmtOfSetBlocked.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setPinned(final String contactUserId, final boolean pinned,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetPinned.acquire();
        int _argIndex = 1;
        final int _tmp = pinned ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, contactUserId);
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
          __preparedStmtOfSetPinned.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setMuted(final String contactUserId, final boolean muted,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetMuted.acquire();
        int _argIndex = 1;
        final int _tmp = muted ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, contactUserId);
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
          __preparedStmtOfSetMuted.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setMutual(final String contactUserId, final boolean isMutual, final Long verifiedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetMutual.acquire();
        int _argIndex = 1;
        final int _tmp = isMutual ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        if (verifiedAt == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, verifiedAt);
        }
        _argIndex = 3;
        _stmt.bindString(_argIndex, contactUserId);
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
          __preparedStmtOfSetMutual.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final String id, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
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
  public Object upsert(final ContactEntity contact, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfContactEntity.upsert(contact);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<ContactEntity> contacts,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfContactEntity.upsert(contacts);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ContactEntity>> getAllContacts(final String uid) {
    final String _sql = "SELECT * FROM contacts WHERE userId = ? ORDER BY isPinned DESC, contactDisplayName ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, uid);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts"}, new Callable<List<ContactEntity>>() {
      @Override
      @NonNull
      public List<ContactEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfContactUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUserId");
          final int _cursorIndexOfContactDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactDisplayName");
          final int _cursorIndexOfContactUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUsername");
          final int _cursorIndexOfContactZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "contactZixoNumber");
          final int _cursorIndexOfContactAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "contactAvatarUrl");
          final int _cursorIndexOfContactBio = CursorUtil.getColumnIndexOrThrow(_cursor, "contactBio");
          final int _cursorIndexOfIsMutual = CursorUtil.getColumnIndexOrThrow(_cursor, "isMutual");
          final int _cursorIndexOfIsVerifiedContact = CursorUtil.getColumnIndexOrThrow(_cursor, "isVerifiedContact");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfMutualVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "mutualVerifiedAt");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<ContactEntity> _result = new ArrayList<ContactEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ContactEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpContactUserId;
            _tmpContactUserId = _cursor.getString(_cursorIndexOfContactUserId);
            final String _tmpContactDisplayName;
            _tmpContactDisplayName = _cursor.getString(_cursorIndexOfContactDisplayName);
            final String _tmpContactUsername;
            _tmpContactUsername = _cursor.getString(_cursorIndexOfContactUsername);
            final String _tmpContactZixoNumber;
            _tmpContactZixoNumber = _cursor.getString(_cursorIndexOfContactZixoNumber);
            final String _tmpContactAvatarUrl;
            _tmpContactAvatarUrl = _cursor.getString(_cursorIndexOfContactAvatarUrl);
            final String _tmpContactBio;
            _tmpContactBio = _cursor.getString(_cursorIndexOfContactBio);
            final boolean _tmpIsMutual;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMutual);
            _tmpIsMutual = _tmp != 0;
            final boolean _tmpIsVerifiedContact;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsVerifiedContact);
            _tmpIsVerifiedContact = _tmp_1 != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_2 != 0;
            final boolean _tmpIsPinned;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_3 != 0;
            final boolean _tmpIsMuted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_4 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final Long _tmpMutualVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfMutualVerifiedAt)) {
              _tmpMutualVerifiedAt = null;
            } else {
              _tmpMutualVerifiedAt = _cursor.getLong(_cursorIndexOfMutualVerifiedAt);
            }
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new ContactEntity(_tmpId,_tmpUserId,_tmpContactUserId,_tmpContactDisplayName,_tmpContactUsername,_tmpContactZixoNumber,_tmpContactAvatarUrl,_tmpContactBio,_tmpIsMutual,_tmpIsVerifiedContact,_tmpIsBlocked,_tmpIsPinned,_tmpIsMuted,_tmpAddedAt,_tmpMutualVerifiedAt,_tmpLastSyncedAt);
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
  public Object getByContactUserId(final String contactUserId,
      final Continuation<? super ContactEntity> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE contactUserId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, contactUserId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ContactEntity>() {
      @Override
      @Nullable
      public ContactEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfContactUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUserId");
          final int _cursorIndexOfContactDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactDisplayName");
          final int _cursorIndexOfContactUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUsername");
          final int _cursorIndexOfContactZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "contactZixoNumber");
          final int _cursorIndexOfContactAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "contactAvatarUrl");
          final int _cursorIndexOfContactBio = CursorUtil.getColumnIndexOrThrow(_cursor, "contactBio");
          final int _cursorIndexOfIsMutual = CursorUtil.getColumnIndexOrThrow(_cursor, "isMutual");
          final int _cursorIndexOfIsVerifiedContact = CursorUtil.getColumnIndexOrThrow(_cursor, "isVerifiedContact");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfMutualVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "mutualVerifiedAt");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final ContactEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpContactUserId;
            _tmpContactUserId = _cursor.getString(_cursorIndexOfContactUserId);
            final String _tmpContactDisplayName;
            _tmpContactDisplayName = _cursor.getString(_cursorIndexOfContactDisplayName);
            final String _tmpContactUsername;
            _tmpContactUsername = _cursor.getString(_cursorIndexOfContactUsername);
            final String _tmpContactZixoNumber;
            _tmpContactZixoNumber = _cursor.getString(_cursorIndexOfContactZixoNumber);
            final String _tmpContactAvatarUrl;
            _tmpContactAvatarUrl = _cursor.getString(_cursorIndexOfContactAvatarUrl);
            final String _tmpContactBio;
            _tmpContactBio = _cursor.getString(_cursorIndexOfContactBio);
            final boolean _tmpIsMutual;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMutual);
            _tmpIsMutual = _tmp != 0;
            final boolean _tmpIsVerifiedContact;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsVerifiedContact);
            _tmpIsVerifiedContact = _tmp_1 != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_2 != 0;
            final boolean _tmpIsPinned;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_3 != 0;
            final boolean _tmpIsMuted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_4 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final Long _tmpMutualVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfMutualVerifiedAt)) {
              _tmpMutualVerifiedAt = null;
            } else {
              _tmpMutualVerifiedAt = _cursor.getLong(_cursorIndexOfMutualVerifiedAt);
            }
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _result = new ContactEntity(_tmpId,_tmpUserId,_tmpContactUserId,_tmpContactDisplayName,_tmpContactUsername,_tmpContactZixoNumber,_tmpContactAvatarUrl,_tmpContactBio,_tmpIsMutual,_tmpIsVerifiedContact,_tmpIsBlocked,_tmpIsPinned,_tmpIsMuted,_tmpAddedAt,_tmpMutualVerifiedAt,_tmpLastSyncedAt);
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
  public Object getByZixoNumber(final String zixoNumber,
      final Continuation<? super ContactEntity> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE contactZixoNumber = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, zixoNumber);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ContactEntity>() {
      @Override
      @Nullable
      public ContactEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfContactUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUserId");
          final int _cursorIndexOfContactDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactDisplayName");
          final int _cursorIndexOfContactUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUsername");
          final int _cursorIndexOfContactZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "contactZixoNumber");
          final int _cursorIndexOfContactAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "contactAvatarUrl");
          final int _cursorIndexOfContactBio = CursorUtil.getColumnIndexOrThrow(_cursor, "contactBio");
          final int _cursorIndexOfIsMutual = CursorUtil.getColumnIndexOrThrow(_cursor, "isMutual");
          final int _cursorIndexOfIsVerifiedContact = CursorUtil.getColumnIndexOrThrow(_cursor, "isVerifiedContact");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfMutualVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "mutualVerifiedAt");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final ContactEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpContactUserId;
            _tmpContactUserId = _cursor.getString(_cursorIndexOfContactUserId);
            final String _tmpContactDisplayName;
            _tmpContactDisplayName = _cursor.getString(_cursorIndexOfContactDisplayName);
            final String _tmpContactUsername;
            _tmpContactUsername = _cursor.getString(_cursorIndexOfContactUsername);
            final String _tmpContactZixoNumber;
            _tmpContactZixoNumber = _cursor.getString(_cursorIndexOfContactZixoNumber);
            final String _tmpContactAvatarUrl;
            _tmpContactAvatarUrl = _cursor.getString(_cursorIndexOfContactAvatarUrl);
            final String _tmpContactBio;
            _tmpContactBio = _cursor.getString(_cursorIndexOfContactBio);
            final boolean _tmpIsMutual;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMutual);
            _tmpIsMutual = _tmp != 0;
            final boolean _tmpIsVerifiedContact;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsVerifiedContact);
            _tmpIsVerifiedContact = _tmp_1 != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_2 != 0;
            final boolean _tmpIsPinned;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_3 != 0;
            final boolean _tmpIsMuted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_4 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final Long _tmpMutualVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfMutualVerifiedAt)) {
              _tmpMutualVerifiedAt = null;
            } else {
              _tmpMutualVerifiedAt = _cursor.getLong(_cursorIndexOfMutualVerifiedAt);
            }
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _result = new ContactEntity(_tmpId,_tmpUserId,_tmpContactUserId,_tmpContactDisplayName,_tmpContactUsername,_tmpContactZixoNumber,_tmpContactAvatarUrl,_tmpContactBio,_tmpIsMutual,_tmpIsVerifiedContact,_tmpIsBlocked,_tmpIsPinned,_tmpIsMuted,_tmpAddedAt,_tmpMutualVerifiedAt,_tmpLastSyncedAt);
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
  public Flow<List<ContactEntity>> getMutualContacts() {
    final String _sql = "SELECT * FROM contacts WHERE isMutual = 1 ORDER BY contactDisplayName ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts"}, new Callable<List<ContactEntity>>() {
      @Override
      @NonNull
      public List<ContactEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfContactUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUserId");
          final int _cursorIndexOfContactDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactDisplayName");
          final int _cursorIndexOfContactUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUsername");
          final int _cursorIndexOfContactZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "contactZixoNumber");
          final int _cursorIndexOfContactAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "contactAvatarUrl");
          final int _cursorIndexOfContactBio = CursorUtil.getColumnIndexOrThrow(_cursor, "contactBio");
          final int _cursorIndexOfIsMutual = CursorUtil.getColumnIndexOrThrow(_cursor, "isMutual");
          final int _cursorIndexOfIsVerifiedContact = CursorUtil.getColumnIndexOrThrow(_cursor, "isVerifiedContact");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfMutualVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "mutualVerifiedAt");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<ContactEntity> _result = new ArrayList<ContactEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ContactEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpContactUserId;
            _tmpContactUserId = _cursor.getString(_cursorIndexOfContactUserId);
            final String _tmpContactDisplayName;
            _tmpContactDisplayName = _cursor.getString(_cursorIndexOfContactDisplayName);
            final String _tmpContactUsername;
            _tmpContactUsername = _cursor.getString(_cursorIndexOfContactUsername);
            final String _tmpContactZixoNumber;
            _tmpContactZixoNumber = _cursor.getString(_cursorIndexOfContactZixoNumber);
            final String _tmpContactAvatarUrl;
            _tmpContactAvatarUrl = _cursor.getString(_cursorIndexOfContactAvatarUrl);
            final String _tmpContactBio;
            _tmpContactBio = _cursor.getString(_cursorIndexOfContactBio);
            final boolean _tmpIsMutual;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMutual);
            _tmpIsMutual = _tmp != 0;
            final boolean _tmpIsVerifiedContact;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsVerifiedContact);
            _tmpIsVerifiedContact = _tmp_1 != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_2 != 0;
            final boolean _tmpIsPinned;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_3 != 0;
            final boolean _tmpIsMuted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_4 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final Long _tmpMutualVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfMutualVerifiedAt)) {
              _tmpMutualVerifiedAt = null;
            } else {
              _tmpMutualVerifiedAt = _cursor.getLong(_cursorIndexOfMutualVerifiedAt);
            }
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new ContactEntity(_tmpId,_tmpUserId,_tmpContactUserId,_tmpContactDisplayName,_tmpContactUsername,_tmpContactZixoNumber,_tmpContactAvatarUrl,_tmpContactBio,_tmpIsMutual,_tmpIsVerifiedContact,_tmpIsBlocked,_tmpIsPinned,_tmpIsMuted,_tmpAddedAt,_tmpMutualVerifiedAt,_tmpLastSyncedAt);
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
  public Flow<List<ContactEntity>> getBlockedContacts() {
    final String _sql = "SELECT * FROM contacts WHERE isBlocked = 1 ORDER BY contactDisplayName ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts"}, new Callable<List<ContactEntity>>() {
      @Override
      @NonNull
      public List<ContactEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfContactUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUserId");
          final int _cursorIndexOfContactDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactDisplayName");
          final int _cursorIndexOfContactUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUsername");
          final int _cursorIndexOfContactZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "contactZixoNumber");
          final int _cursorIndexOfContactAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "contactAvatarUrl");
          final int _cursorIndexOfContactBio = CursorUtil.getColumnIndexOrThrow(_cursor, "contactBio");
          final int _cursorIndexOfIsMutual = CursorUtil.getColumnIndexOrThrow(_cursor, "isMutual");
          final int _cursorIndexOfIsVerifiedContact = CursorUtil.getColumnIndexOrThrow(_cursor, "isVerifiedContact");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfMutualVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "mutualVerifiedAt");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<ContactEntity> _result = new ArrayList<ContactEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ContactEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpContactUserId;
            _tmpContactUserId = _cursor.getString(_cursorIndexOfContactUserId);
            final String _tmpContactDisplayName;
            _tmpContactDisplayName = _cursor.getString(_cursorIndexOfContactDisplayName);
            final String _tmpContactUsername;
            _tmpContactUsername = _cursor.getString(_cursorIndexOfContactUsername);
            final String _tmpContactZixoNumber;
            _tmpContactZixoNumber = _cursor.getString(_cursorIndexOfContactZixoNumber);
            final String _tmpContactAvatarUrl;
            _tmpContactAvatarUrl = _cursor.getString(_cursorIndexOfContactAvatarUrl);
            final String _tmpContactBio;
            _tmpContactBio = _cursor.getString(_cursorIndexOfContactBio);
            final boolean _tmpIsMutual;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMutual);
            _tmpIsMutual = _tmp != 0;
            final boolean _tmpIsVerifiedContact;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsVerifiedContact);
            _tmpIsVerifiedContact = _tmp_1 != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_2 != 0;
            final boolean _tmpIsPinned;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_3 != 0;
            final boolean _tmpIsMuted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_4 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final Long _tmpMutualVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfMutualVerifiedAt)) {
              _tmpMutualVerifiedAt = null;
            } else {
              _tmpMutualVerifiedAt = _cursor.getLong(_cursorIndexOfMutualVerifiedAt);
            }
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new ContactEntity(_tmpId,_tmpUserId,_tmpContactUserId,_tmpContactDisplayName,_tmpContactUsername,_tmpContactZixoNumber,_tmpContactAvatarUrl,_tmpContactBio,_tmpIsMutual,_tmpIsVerifiedContact,_tmpIsBlocked,_tmpIsPinned,_tmpIsMuted,_tmpAddedAt,_tmpMutualVerifiedAt,_tmpLastSyncedAt);
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
  public Flow<List<ContactEntity>> searchContacts(final String query) {
    final String _sql = "SELECT * FROM contacts WHERE contactDisplayName LIKE '%' || ? || '%' OR contactUsername LIKE '%' || ? || '%'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts"}, new Callable<List<ContactEntity>>() {
      @Override
      @NonNull
      public List<ContactEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfContactUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUserId");
          final int _cursorIndexOfContactDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactDisplayName");
          final int _cursorIndexOfContactUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUsername");
          final int _cursorIndexOfContactZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "contactZixoNumber");
          final int _cursorIndexOfContactAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "contactAvatarUrl");
          final int _cursorIndexOfContactBio = CursorUtil.getColumnIndexOrThrow(_cursor, "contactBio");
          final int _cursorIndexOfIsMutual = CursorUtil.getColumnIndexOrThrow(_cursor, "isMutual");
          final int _cursorIndexOfIsVerifiedContact = CursorUtil.getColumnIndexOrThrow(_cursor, "isVerifiedContact");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfMutualVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "mutualVerifiedAt");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<ContactEntity> _result = new ArrayList<ContactEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ContactEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpContactUserId;
            _tmpContactUserId = _cursor.getString(_cursorIndexOfContactUserId);
            final String _tmpContactDisplayName;
            _tmpContactDisplayName = _cursor.getString(_cursorIndexOfContactDisplayName);
            final String _tmpContactUsername;
            _tmpContactUsername = _cursor.getString(_cursorIndexOfContactUsername);
            final String _tmpContactZixoNumber;
            _tmpContactZixoNumber = _cursor.getString(_cursorIndexOfContactZixoNumber);
            final String _tmpContactAvatarUrl;
            _tmpContactAvatarUrl = _cursor.getString(_cursorIndexOfContactAvatarUrl);
            final String _tmpContactBio;
            _tmpContactBio = _cursor.getString(_cursorIndexOfContactBio);
            final boolean _tmpIsMutual;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMutual);
            _tmpIsMutual = _tmp != 0;
            final boolean _tmpIsVerifiedContact;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsVerifiedContact);
            _tmpIsVerifiedContact = _tmp_1 != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_2 != 0;
            final boolean _tmpIsPinned;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_3 != 0;
            final boolean _tmpIsMuted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_4 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final Long _tmpMutualVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfMutualVerifiedAt)) {
              _tmpMutualVerifiedAt = null;
            } else {
              _tmpMutualVerifiedAt = _cursor.getLong(_cursorIndexOfMutualVerifiedAt);
            }
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new ContactEntity(_tmpId,_tmpUserId,_tmpContactUserId,_tmpContactDisplayName,_tmpContactUsername,_tmpContactZixoNumber,_tmpContactAvatarUrl,_tmpContactBio,_tmpIsMutual,_tmpIsVerifiedContact,_tmpIsBlocked,_tmpIsPinned,_tmpIsMuted,_tmpAddedAt,_tmpMutualVerifiedAt,_tmpLastSyncedAt);
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
  public Flow<Integer> getMutualContactCount() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE isMutual = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts"}, new Callable<Integer>() {
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
  public Flow<Integer> getBlockedContactCount() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE isBlocked = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts"}, new Callable<Integer>() {
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
  public Object getStaleContacts(final long threshold,
      final Continuation<? super List<ContactEntity>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE lastSyncedAt IS NULL OR lastSyncedAt < ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, threshold);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ContactEntity>>() {
      @Override
      @NonNull
      public List<ContactEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfContactUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUserId");
          final int _cursorIndexOfContactDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactDisplayName");
          final int _cursorIndexOfContactUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "contactUsername");
          final int _cursorIndexOfContactZixoNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "contactZixoNumber");
          final int _cursorIndexOfContactAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "contactAvatarUrl");
          final int _cursorIndexOfContactBio = CursorUtil.getColumnIndexOrThrow(_cursor, "contactBio");
          final int _cursorIndexOfIsMutual = CursorUtil.getColumnIndexOrThrow(_cursor, "isMutual");
          final int _cursorIndexOfIsVerifiedContact = CursorUtil.getColumnIndexOrThrow(_cursor, "isVerifiedContact");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfIsMuted = CursorUtil.getColumnIndexOrThrow(_cursor, "isMuted");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final int _cursorIndexOfMutualVerifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "mutualVerifiedAt");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<ContactEntity> _result = new ArrayList<ContactEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ContactEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpContactUserId;
            _tmpContactUserId = _cursor.getString(_cursorIndexOfContactUserId);
            final String _tmpContactDisplayName;
            _tmpContactDisplayName = _cursor.getString(_cursorIndexOfContactDisplayName);
            final String _tmpContactUsername;
            _tmpContactUsername = _cursor.getString(_cursorIndexOfContactUsername);
            final String _tmpContactZixoNumber;
            _tmpContactZixoNumber = _cursor.getString(_cursorIndexOfContactZixoNumber);
            final String _tmpContactAvatarUrl;
            _tmpContactAvatarUrl = _cursor.getString(_cursorIndexOfContactAvatarUrl);
            final String _tmpContactBio;
            _tmpContactBio = _cursor.getString(_cursorIndexOfContactBio);
            final boolean _tmpIsMutual;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMutual);
            _tmpIsMutual = _tmp != 0;
            final boolean _tmpIsVerifiedContact;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsVerifiedContact);
            _tmpIsVerifiedContact = _tmp_1 != 0;
            final boolean _tmpIsBlocked;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_2 != 0;
            final boolean _tmpIsPinned;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_3 != 0;
            final boolean _tmpIsMuted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsMuted);
            _tmpIsMuted = _tmp_4 != 0;
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            final Long _tmpMutualVerifiedAt;
            if (_cursor.isNull(_cursorIndexOfMutualVerifiedAt)) {
              _tmpMutualVerifiedAt = null;
            } else {
              _tmpMutualVerifiedAt = _cursor.getLong(_cursorIndexOfMutualVerifiedAt);
            }
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new ContactEntity(_tmpId,_tmpUserId,_tmpContactUserId,_tmpContactDisplayName,_tmpContactUsername,_tmpContactZixoNumber,_tmpContactAvatarUrl,_tmpContactBio,_tmpIsMutual,_tmpIsVerifiedContact,_tmpIsBlocked,_tmpIsPinned,_tmpIsMuted,_tmpAddedAt,_tmpMutualVerifiedAt,_tmpLastSyncedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
