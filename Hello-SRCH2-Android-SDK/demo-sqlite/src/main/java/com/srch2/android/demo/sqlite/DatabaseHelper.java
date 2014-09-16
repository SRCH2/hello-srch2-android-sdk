package com.srch2.android.demo.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "library";
    public static final String TABLE_NAME = "books";
    public static final String COLUMN_PRIMARY_KEY = "id";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_GENRE = "genre";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_THUMBNAIL = "thumbnail";

    private static final String getCreateTableString() {
        return "CREATE TABLE " + TABLE_NAME +
               " ( " +
                COLUMN_PRIMARY_KEY + " INTEGER PRIMARY KEY NOT NULL, " +
                COLUMN_AUTHOR + " TEXT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_GENRE + " TEXT, " +
                COLUMN_YEAR + " INTEGER, " +
                COLUMN_SCORE + " REAL, " +
                COLUMN_THUMBNAIL + " BLOB " +
               " )";
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(getCreateTableString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertRecords(ArrayList<Book> insertSet) {
        boolean insertSuccess = true;
        SQLiteDatabase db = getReadableDatabase();

        int rowCount = 0;
        Cursor c = null;
        try {
            c = db.rawQuery("select * FROM " + DatabaseHelper.TABLE_NAME, null);
            if (c.moveToFirst()) {
                rowCount = c.getCount();
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        if (rowCount == insertSet.size()) {
            return true;
        }

        db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Probably want to use SQLiteStatement in release code.
            ContentValues cv = new ContentValues();
            for (Book b : insertSet) {
                cv.clear();
                cv.put(DatabaseHelper.COLUMN_AUTHOR, b.mAuthor);
                cv.put(DatabaseHelper.COLUMN_TITLE, b.mTitle);
                cv.put(DatabaseHelper.COLUMN_GENRE, b.mGenre);
                cv.put(DatabaseHelper.COLUMN_YEAR, b.mYear);
                cv.put(DatabaseHelper.COLUMN_SCORE, b.mUserRating);
                cv.put(DatabaseHelper.COLUMN_THUMBNAIL,
                        b.mThumbnail.getBytes(Charset.forName("UTF-8")));
                db.insert(TABLE_NAME, null, cv);
            }
            db.setTransactionSuccessful();
        } catch (SQLiteDatabaseLockedException oops) {
            insertSuccess = false;
        } finally {
            db.endTransaction();
        }
        return insertSuccess;
    }
}
