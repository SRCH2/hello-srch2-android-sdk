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
package com.srch2.data.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;

public class UserCacheDatabase extends SQLiteOpenHelper {
	
//	_________________________________________________ //
//	| pid | fk | hitcount | category | serialstring | // 
	
	private static final String databaseName = "ucashi";
	private static final int databaseVersion = 1;
	
	private static final String createTableString() { return "CREATE TABLE IF NOT EXISTS ucashi ( _id INTEGER PRIMARY KEY, _ucashid TEXT, hc INTEGER, scat TEXT, sstring TEXT )"; }
	
	public UserCacheDatabase(Context context) {
		super(context, databaseName, null, databaseVersion);
	}

	@Override public void onCreate(SQLiteDatabase db) { }
	@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
	
	public HashSet<ViewableResultStatistic> getAll() {
		final SQLiteDatabase db = getReadableDatabase();
		
		HashSet<ViewableResultStatistic> vrs = null;
		
	
		Cursor c = null;
		try {
			c = db.rawQuery("select * from ucashi order by hc desc limit 20", null);
			if (c.moveToFirst()) {
				vrs = new HashSet<ViewableResultStatistic>(c.getCount());
				do {
					vrs.add(new ViewableResultStatistic(c.getString(1), c.getInt(2), SearchCategory.getFromString(c.getString(3)), c.getString(4)));
				} while (c.moveToNext());
				
			}
			
		} catch (SQLiteException sqle) {
			Pith.handleException(sqle);
		} finally {
		
			if (c != null) {
				c.close();
			}
			
			if (db != null) {
				db.close();
			}
		}
		
		if (vrs == null) {
			vrs = new HashSet<ViewableResultStatistic>();
		}
		
		return vrs;
	}

	
	public void putAll(Collection<ViewableResultStatistic> collection) {
		
		if (collection.size() < 1) {
			return;
		}
	
		SQLiteDatabase db = null;

		try {
			db = getWritableDatabase();
			db.beginTransaction();
			
			db.execSQL("DROP TABLE IF EXISTS ucashi");
			db.execSQL(createTableString());
			final SQLiteStatement st = db.compileStatement("INSERT INTO ucashi VALUES(NULL, ?, ?, ?, ?)");
			for (ViewableResultStatistic vrs : collection) {
				st.bindString(1, vrs.userCacheId);
				st.bindLong(2, vrs.hitCount);
				st.bindString(3, vrs.searchCategory.name());
				st.bindString(4, vrs.serialString);
				st.executeInsert();
			}
			
			db.setTransactionSuccessful();
		} catch (Exception e) {
			Pith.handleException(e);
		} finally {
			if (db != null) {
				db.endTransaction();
				db.close();
			}
		}
		
		
	}
}
