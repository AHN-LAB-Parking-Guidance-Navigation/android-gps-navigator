package gps.map.navigator.common.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import gps.map.navigator.common.Constants;
import gps.map.navigator.common.debug.Logger;

public class DatabaseStorage extends SQLiteOpenHelper implements Storage {
    @Inject
    Logger logger;
    @NonNull
    private Integer databaseVersion;

    private static final String TABLE_CHUNKED = "table_chunked";
    private static final String TABLE_CACHE = "table_cache";
    private static final String COLUMN_CACHE_KEY = "cache_key";
    private static final String COLUMN_CACHE_VALUE = "cache_value";
    private static final String COLUMN_CHUNKED_VALUE = "chunked_value";

    @Inject
    DatabaseStorage(@Named(Constants.ApplicationContext) Context context,
                    @Named(Constants.DatabaseInfo) String databaseName,
                    @NonNull @Named(Constants.DatabaseInfo) Integer databaseVersion) {
        super(context, databaseName, null, databaseVersion);
        this.databaseVersion = databaseVersion;
    }

    @Override
    public boolean hasData(@NonNull String key, boolean defaultValue) {
        Cursor cursor = null;
        try {
            cursor = getCacheCursor(key);
            return cursor != null && cursor.moveToFirst() && cursor.getCount() > 0;
        } catch (Throwable t) {
            logger.error(t);
            return defaultValue;
        } finally {
            closeCursorSilently(cursor);
        }
    }

    private Cursor getCacheCursor(@NonNull String key) {
        return makeQuery(TABLE_CACHE, new String[]{COLUMN_CACHE_KEY, COLUMN_CACHE_VALUE},
                String.format(Locale.US, "%s='%s'", COLUMN_CACHE_KEY, key));
    }

    private Cursor makeQuery(@NonNull String table, @NonNull String[] columns, @Nullable String selection) {
        return getReadableDatabase().query(table, columns, selection, null, null, null, null);
    }

    private void closeCursorSilently(@Nullable Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public boolean saveData(@NonNull String key, @Nullable byte[] data) {
        SQLiteStatement statement = null;
        try {
            statement = getWritableDatabase().compileStatement(getTableCacheInsertSql());
            statement.bindString(1, key);
            if (data == null) {
                statement.bindNull(2);
            } else {
                statement.bindBlob(2, data);
            }
            statement.executeInsert();
            return true;
        } catch (Throwable t) {
            logger.error(t);
            return false;
        } finally {
            closeStatementSilently(statement);
        }
    }

    @NonNull
    private String getTableCacheInsertSql() {
        return String.format(Locale.US,
                "insert or replace into '%s' (%s, %s) values (?,?)",
                TABLE_CACHE, COLUMN_CACHE_KEY, COLUMN_CACHE_VALUE);
    }

    private void closeStatementSilently(@Nullable SQLiteStatement statement) {
        if (statement != null) {
            statement.close();
        }
    }

    @Nullable
    @Override
    public byte[] getData(@NonNull String key, @Nullable byte[] defaultValue) {
        Cursor cursor = null;
        try {
            cursor = getCacheCursor(key);
            if (cursorCanMoveToFirst(cursor)) {
                return getBlob(cursor, cursor.getColumnIndex(COLUMN_CACHE_VALUE));
            }
        } catch (Throwable t) {
            logger.error(t);
        } finally {
            closeCursorSilently(cursor);
        }
        return defaultValue;
    }

    private boolean cursorCanMoveToFirst(@Nullable Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            return true;
        }
        return false;
    }

    private byte[] getBlob(@NonNull Cursor cursor, int columnIndex) {
        return cursor.getBlob(columnIndex);
    }

    @Override
    public boolean saveChunkedData(@NonNull List<byte[]> data) {
        SQLiteStatement statement = null;
        try {
            statement = getWritableDatabase().compileStatement(getTableChunkedInsertSql());
            cleanChunckedStorage();
            byte[] value;
            for (int i = 0; i < data.size(); i++) {
                value = data.get(i);
                if (value == null) {
                    statement.bindNull(1);
                } else {
                    statement.bindBlob(1, value);
                }
                statement.executeInsert();
            }
            return true;
        } catch (Throwable t) {
            logger.error(t);
            return false;
        } finally {
            closeStatementSilently(statement);
        }
    }

    private void cleanChunckedStorage() {
        getWritableDatabase().execSQL("delete from " + TABLE_CHUNKED);
    }

    @NonNull
    private String getTableChunkedInsertSql() {
        return String.format(Locale.US, "insert or replace into '%s' (%s) values (?)",
                TABLE_CHUNKED, COLUMN_CHUNKED_VALUE);
    }

    @NonNull
    @Override
    public List<byte[]> getChunkedData() {
        List<byte[]> data = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = makeQuery(TABLE_CHUNKED, new String[]{COLUMN_CHUNKED_VALUE}, null);
            if (cursorCanMoveToFirst(cursor)) {
                data.add(getRawChunckedData(cursor));
                while (cursor.moveToNext()) {
                    data.add(getRawChunckedData(cursor));
                }
            }
        } catch (Throwable t) {
            logger.error(t);
        } finally {
            closeCursorSilently(cursor);
        }
        return data;
    }

    private byte[] getRawChunckedData(Cursor cursor) {
        return getBlob(cursor, cursor.getColumnIndex(COLUMN_CHUNKED_VALUE));
    }

    @Override
    public void invalidate() {
        close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(getTableCacheCreateSql());
        db.execSQL(getTableChunkedCreateSql());
    }

    private String getTableCacheCreateSql() {
        return String.format(Locale.US,
                "create table if not exists '%s' (%s varchar(100) primary key unique, %s blob)",
                TABLE_CACHE, COLUMN_CACHE_KEY, COLUMN_CACHE_VALUE);
    }

    private String getTableChunkedCreateSql() {
        return String.format(Locale.US, "create table if not exists '%s' (%s blob)",
                TABLE_CHUNKED, COLUMN_CHUNKED_VALUE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (databaseVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHUNKED);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHE);
            onCreate(db);
        }
    }
}
