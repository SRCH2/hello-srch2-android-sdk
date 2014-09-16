package com.srch2.android.demo.sqlite;

import android.database.sqlite.SQLiteOpenHelper;

import com.srch2.android.sdk.Highlighter;
import com.srch2.android.sdk.SQLiteIndexable;

public class SQLiteBookIndex extends SQLiteIndexable {

    // For clarity, SQLiteBookIndex defined as top-level class;
    // could also be nested, non-static class of the DatabaseHelper.
    // Pass in as DatabaseHelper to automatically insert the records
    // when onIndexReady() call back is executed.
    private final DatabaseHelper mSQLiteOpenHelper;

    public SQLiteBookIndex(DatabaseHelper databaseHelper) {
        mSQLiteOpenHelper = databaseHelper;
    }

    @Override
    public String getIndexName() {
        // For convenience, make the same as the table name.
        return DatabaseHelper.TABLE_NAME;
    }

    @Override
    public SQLiteOpenHelper getSQLiteOpenHelper() {
        return mSQLiteOpenHelper;
    }

    @Override
    public String getDatabaseName() {
        return DatabaseHelper.DATABASE_NAME;
    }

    @Override
    public String getTableName() {
        return DatabaseHelper.TABLE_NAME;
    }

    @Override
    public String getRecordBoostColumnName() {
        // Is equivalent to setting the RecordBoostField for a Schema in
        // the Indexable getSchema() function.
        return DatabaseHelper.COLUMN_SCORE;
    }

    @Override
    public int getColumnBoostValue(String textTypeColumnName) {
        // Since books are often looked up by title, give title the
        // greatest field boost relative to the other searchable fields/columns.
        int fieldBoostValue = 1;
        if (textTypeColumnName.equals(DatabaseHelper.COLUMN_AUTHOR)) {
            fieldBoostValue = 25;
        } else if (textTypeColumnName.equals(DatabaseHelper.COLUMN_TITLE)) {
            fieldBoostValue = 50;
        }
        return fieldBoostValue;
    }

    @Override
    public boolean getColumnIsHighlighted(String textTypeColumnName) {
        // Highlight all searchable / of type TEXT columns.
        return true;
    }

    @Override
    public Highlighter getHighlighter() {
        return Highlighter.createHighlighter()
                .formatExactTextMatches(true, false, "#FF0000")
                .formatFuzzyTextMatches(true, false, "#FF00FF");
    }

    @Override
    public void onIndexReady() {
        super.onIndexReady();
        if (getRecordCount() == 0) {
            mSQLiteOpenHelper.insertRecords(Book.getBookList());
        }
    }
}
