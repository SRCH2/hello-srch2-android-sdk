/*
 * Copyright (c) 2016, SRCH2
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of the SRCH2 nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SRCH2 BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.srch2.data.incremental;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.srch2.Pith;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class IncrementalDatabase extends SQLiteOpenHelper {

	private static final String database_name = "INCREMENTAL_DB";
	private static final int database_version = 1;
	
	private static final String PRIMARY_KEY_NAME = "_id";
	private static final String FOREIGN_KEY_NAME = "fk";
	
	static private final String getCreateTableString(final String tableName) {
		return "CREATE TABLE IF NOT EXISTS " + tableName + 
				"(" +
				PRIMARY_KEY_NAME + " INTEGER PRIMARY KEY," + 
				FOREIGN_KEY_NAME + " INTEGER" + 
				")";
	}
	
	public IncrementalDatabase(Context context) {
		super(context, database_name, null, database_version);
	}

	public void serializeIncrementalState(HashMap<String, HashSet<String>> incrementalState) {
		final Set<String> tableNames = incrementalState.keySet();

		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		for (String tableNameKey : tableNames) {
			db.execSQL("DROP TABLE IF EXISTS " + tableNameKey + " ");
			db.execSQL(getCreateTableString(tableNameKey));
			final HashSet<String> incrementalValues = incrementalState.get(tableNameKey);
			if (incrementalValues.size() > 0) {
			
				final SQLiteStatement statement = db.compileStatement("INSERT INTO " + tableNameKey + " VALUES( NULL, ? )");
				for (String incrementalValue : incrementalValues) {
					statement.bindString(1, incrementalValue);
					statement.executeInsert();
				}
			}
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}
	
	public HashMap<String, HashSet<String>> getSerializedIncrementalState(String[] tableNames) {
		HashMap<String, HashSet<String>> incrementalSaveState_tableNameToHashSet = new HashMap<String, HashSet<String>>();
		final SQLiteDatabase db = getReadableDatabase();
		Cursor c = null;
		for (String tableName : tableNames) {
			db.execSQL(getCreateTableString(tableName));
			HashSet<String> incrementalState = new HashSet<String>();
			try {
				c = db.rawQuery("SELECT " + FOREIGN_KEY_NAME + " FROM " + tableName + " ORDER BY " + FOREIGN_KEY_NAME + " ASC", null);
				if (c.moveToFirst()) {
					do {
						incrementalState.add(c.getString(0));
					} while (c.moveToNext());
				}
			} catch (NullPointerException npe) {
				Pith.handleException(npe);
			} finally {
				c.close();
			}
			incrementalSaveState_tableNameToHashSet.put(tableName, incrementalState);
		}
		db.close();
		return incrementalSaveState_tableNameToHashSet;
	}

	@Override
	public void onCreate(SQLiteDatabase db) { }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
}
