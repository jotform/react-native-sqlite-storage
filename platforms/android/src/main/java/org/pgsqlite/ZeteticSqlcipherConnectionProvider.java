package org.pgsqlite;

import android.content.Context;
import android.database.Cursor;

import com.github.dryganets.sqlite.adapter.Database;
import com.github.dryganets.sqlite.adapter.DatabaseConnectionProvider;
import com.github.dryganets.sqlite.adapter.SQLStatement;

import net.zetetic.database.DatabaseErrorHandler;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook;

import java.io.IOException;

/**
 * SQLCipher connection provider compatible with net.zetetic:sqlcipher-android (v4.7+).
 *
 * This replaces the older net.sqlcipher.database.* API used by android-database-sqlcipher.
 */
final class ZeteticSqlcipherConnectionProvider implements DatabaseConnectionProvider {
    private static volatile boolean sLoaded;

    @SuppressWarnings("unused")
    ZeteticSqlcipherConnectionProvider(Context context) {
        // sqlcipher-android bundles libsqlcipher.so but does not expose the legacy
        // SQLiteDatabase.loadLibs(context) API (net.sqlcipher.*). Ensure the native library
        // is loaded before any SQLiteConnection native methods are invoked.
        //
        // Library name matches jni/<abi>/libsqlcipher.so in the AAR.
        if (!sLoaded) {
            synchronized (ZeteticSqlcipherConnectionProvider.class) {
                if (!sLoaded) {
                    System.loadLibrary("sqlcipher");
                    sLoaded = true;
                }
            }
        }
    }

    @Override
    public Database openDatabase(String path, String password, int openFlags) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(
                path,
                password,
                null,
                openFlags,
                new DatabaseErrorHandler() {
                    @Override
                    public void onCorruption(SQLiteDatabase dbObj, android.database.sqlite.SQLiteException error) {
                        // No special corruption handling; keep behavior close to legacy provider.
                    }
                },
                (SQLiteDatabaseHook) null
        );
        return new ZeteticSqlcipherDatabase(db);
    }

    private static final class ZeteticSqlcipherDatabase implements Database {
        private final SQLiteDatabase db;

        ZeteticSqlcipherDatabase(SQLiteDatabase db) {
            this.db = db;
        }

        @Override
        public void execSQL(String sql) {
            db.execSQL(sql);
        }

        @Override
        public SQLStatement compileStatement(String sql) {
            return new ZeteticSqlcipherStatement(db.compileStatement(sql));
        }

        @Override
        public com.github.dryganets.sqlite.adapter.Cursor rawQuery(String sql, String[] selectionArgs) {
            Cursor cursor = db.rawQuery(sql, selectionArgs);
            return new AndroidCursorAdapter(cursor);
        }

        @Override
        public boolean isOpen() {
            return db.isOpen();
        }

        @Override
        public void beginTransaction() {
            db.beginTransaction();
        }

        @Override
        public void setTransactionSuccessful() {
            db.setTransactionSuccessful();
        }

        @Override
        public void endTransaction() {
            db.endTransaction();
        }

        @Override
        public void close() throws IOException {
            db.close();
        }
    }

    private static final class ZeteticSqlcipherStatement implements SQLStatement {
        private final net.zetetic.database.sqlcipher.SQLiteStatement statement;

        ZeteticSqlcipherStatement(net.zetetic.database.sqlcipher.SQLiteStatement statement) {
            this.statement = statement;
        }

        @Override
        public void bindDouble(int index, double value) {
            statement.bindDouble(index, value);
        }

        @Override
        public void bindString(int index, String value) {
            statement.bindString(index, value);
        }

        @Override
        public void bindNull(int index) {
            statement.bindNull(index);
        }

        @Override
        public void bindLong(int index, long value) {
            statement.bindLong(index, value);
        }

        @Override
        public long executeInsert() {
            return statement.executeInsert();
        }

        @Override
        public int executeUpdateDelete() {
            return statement.executeUpdateDelete();
        }

        @Override
        public void close() throws IOException {
            statement.close();
        }
    }

    private static final class AndroidCursorAdapter implements com.github.dryganets.sqlite.adapter.Cursor {
        private final Cursor cursor;

        AndroidCursorAdapter(Cursor cursor) {
            this.cursor = cursor;
        }

        @Override
        public boolean moveToFirst() {
            return cursor.moveToFirst();
        }

        @Override
        public boolean moveToNext() {
            return cursor.moveToNext();
        }

        @Override
        public int getColumnCount() {
            return cursor.getColumnCount();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return cursor.getColumnName(columnIndex);
        }

        @Override
        public int getType(int columnIndex) {
            return cursor.getType(columnIndex);
        }

        @Override
        public long getLong(int columnIndex) {
            return cursor.getLong(columnIndex);
        }

        @Override
        public double getDouble(int columnIndex) {
            return cursor.getDouble(columnIndex);
        }

        @Override
        public String getString(int columnIndex) {
            return cursor.getString(columnIndex);
        }

        @Override
        public byte[] getBlob(int columnIndex) {
            return cursor.getBlob(columnIndex);
        }

        @Override
        public int getCount() {
            return cursor.getCount();
        }

        @Override
        public void close() throws IOException {
            cursor.close();
        }
    }
}

