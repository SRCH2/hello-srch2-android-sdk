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
package com.srch2.data.connector.indexed.indices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.ThreadPool;
import com.srch2.data.connector.indexed.ConnectorRecord;
import com.srch2.data.connector.indexed.IndexedConnector;
import com.srch2.data.incremental.observer.ContentProviderObserver;
import com.srch2.data.index.BaseSchemaRuleSet;
import com.srch2.instantsearch.QueryResult;
import com.srch2.viewable.result.MusicViewableResult;
import com.srch2.viewable.result.SmsViewableResult;
import com.srch2.viewable.result.ViewableResult;


public class Music extends IndexedConnector {

	//private Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
	
	protected static AtomicBoolean accessLock;

	private Cursor getQuickScanCursor() {
		return context.getContentResolver().query(
											MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
											new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE }, 
											MediaStore.Audio.Media.IS_MUSIC + " != 0", 
											null, 
											null
										  );
	}
	
	private final Cursor getReversibleScanCursor() {
		return context.getContentResolver().query(
											MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
											new String[] { 
													MediaStore.Audio.Media._ID,
													MediaStore.Audio.Media.DATE_MODIFIED,
													MediaStore.Audio.Media.ALBUM,
													MediaStore.Audio.Media.ARTIST,
													MediaStore.Audio.Media.DISPLAY_NAME },
											MediaStore.Audio.Media.IS_MUSIC + " != 0", 
											null, 
											MediaStore.Audio.Media._ID + " ASC, " +
													MediaStore.Audio.Media.DATE_MODIFIED + " ASC"
										  );
	}
	
	private final Cursor getIncrementalDataCursor() {
		return context.getContentResolver().query(
											MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
											new String[] { 
													MediaStore.Audio.Media._ID,
													MediaStore.Audio.Media.DATE_MODIFIED,		
													MediaStore.Audio.Media.TITLE },
											MediaStore.Audio.Media.IS_MUSIC + " != 0", 
											null, 
											MediaStore.Audio.Media._ID + " ASC, " +
													MediaStore.Audio.Media.DATE_MODIFIED + " ASC"
										  );
	}
	
	private final Cursor getSingleRecordQueryCursor(String primaryKeyId) {
		return context.getContentResolver().query(
											MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
											new String[] { 
													MediaStore.Audio.Media._ID,
													MediaStore.Audio.Media.DATE_MODIFIED,
													MediaStore.Audio.Media.TITLE,
													MediaStore.Audio.Media.ALBUM,
													MediaStore.Audio.Media.ARTIST,
													MediaStore.Audio.Media.DISPLAY_NAME },
											MediaStore.Audio.Media._ID + " =?", 
											new String[] { primaryKeyId }, 
											null
										  );
	}
	
	private static final class SchemaAttributeSet {
		private static final String title = BaseSchemaRuleSet.attribute_recordPrimarySearchAttribute;
		private static final String artist = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute1;
		private static final String album = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute2;
		private static final String displayName = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute3;
	}
	
	public Music(Context contxt) {
		super(contxt, SearchCategory.Music);
		
		accessLock = new AtomicBoolean();
		super.addAndSetAccessLock(getCategory(), accessLock);
		
		inMemoryRecordSplitSize = 1;
		
		
		incrementalObserver = new ContentProviderObserver(contxt, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this);
	}

	@Override
	public final HashMap<String, Integer> getSchemaSearchAttributes() {
		HashMap<String, Integer> schemaSearchAttributes = new HashMap<String, Integer>();
		schemaSearchAttributes.put(SchemaAttributeSet.title, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		schemaSearchAttributes.put(SchemaAttributeSet.artist, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		schemaSearchAttributes.put(SchemaAttributeSet.displayName, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		schemaSearchAttributes.put(SchemaAttributeSet.album, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		return schemaSearchAttributes;
	}
	
	@Override
	public ArrayList<ConnectorRecord> getQuickScanIndexRecords() {
		ArrayList<ConnectorRecord> newQuickScanRecords = new ArrayList<ConnectorRecord>();
		
		Cursor c = null;
		
		try {
			c = getQuickScanCursor();
			
			if (c.moveToFirst()) {
				do {
					String id = c.getString(0);
					String title = c.getString(1);
					
					if (id == null || title == null) {
						continue;
					}

					ConnectorRecord r = new ConnectorRecord();
					r.primaryKey = id;
					r.attributeValues.put(SchemaAttributeSet.title, title);
					r.inMemoryRecordData = title;
					newQuickScanRecords.add(r);
					
				} while (c.moveToNext());
			}
		} catch (Exception e) {
			Pith.handleException(e);
		
		} finally {
			c.close();
		}
		return newQuickScanRecords;
	}
	
	@Override
	public ArrayList<ConnectorRecord> getReversibleScanIndexRecords(ArrayList<ConnectorRecord> records) {
		HashMap<String, ConnectorRecord> recordsPrimaryKeyMap = new HashMap<String, ConnectorRecord>(records.size());
		
		for (ConnectorRecord cr : records) {
			recordsPrimaryKeyMap.put(cr.primaryKey, cr);
		}
		
		Cursor c = null;
		
		StringBuilder sb_sb = new StringBuilder();
		
		try {
			c = getReversibleScanCursor();
			
			
			
			
			
			if (c.moveToFirst()) {
				
				
				int cursorCount = c.getCount();
				int count = 0;
				int stepSize = ThreadPool.isSingleCore() ? 250 : 100000; 
	
				do {
					++count;
					if (count % stepSize == 0) {
						try {
							Thread.currentThread().sleep(30);
						} catch (InterruptedException e) {
							Pith.handleException(e);
						}
					}
					
					if (count > (cursorCount / 2) && Thread.currentThread().isInterrupted()) {
						return null;
					}
					
					String id = c.getString(0);
					
					if (id != null && recordsPrimaryKeyMap.containsKey(id)) {
						ConnectorRecord cr = recordsPrimaryKeyMap.get(id);
						
						String artist = c.getString(3);
						String album = c.getString(2);
						String displayName = c.getString(4);
						
						artist = artist == null ? "" : artist;
						displayName = displayName == null ? "" : displayName;
						album = album == null ? "" : album;
						
						cr.attributeValues.put(SchemaAttributeSet.album, album);
						cr.attributeValues.put(SchemaAttributeSet.displayName, displayName);
						cr.attributeValues.put(SchemaAttributeSet.artist, artist);
						
						sb_sb.setLength(0);
						sb_sb.append(cr.inMemoryRecordData);
						sb_sb.append(" ");
						sb_sb.append(artist);
						sb_sb.append(" ");
						sb_sb.append(album);
						cr.inMemoryRecordData = sb_sb.toString();
						
						String dateModified = c.getString(1);
						dateModified = dateModified == null ? "0" : dateModified;
						
						
						sb_sb.setLength(0);
						sb_sb.append(id);
						sb_sb.append(delimiter);
						sb_sb.append(dateModified);
						cr.incrementalValue = sb_sb.toString();
					}
				} while (c.moveToNext());
			}
		} finally {
			c.close();
		}
		return records;
	}
	
	@Override
	public ArrayList<ConnectorRecord> resolveIncrementalDifferenceUpdate() {

		StringBuilder sb_sb = new StringBuilder();
		
		HashSet<String> currentIncrementalDataSnapShot = new HashSet<String>();
		Cursor c = null;
		try { 
			c = getIncrementalDataCursor();
			if (c.moveToFirst()) {
				do {
					
					String id = c.getString(0);
					String title = c.getString(2);
					
					if (id == null || title == null) {
						continue;
					}
					
					String dateModified = c.getString(1);
					dateModified = dateModified == null ? "0" : dateModified;
					
					
					sb_sb.setLength(0);
					sb_sb.append(id);
					sb_sb.append(delimiter);
					sb_sb.append(dateModified);
					
					currentIncrementalDataSnapShot.add(sb_sb.toString());
				} while (c.moveToNext());
			}
		} finally {
			c.close();
		}
		
		HashSet<String> latestIncrementalDataSnapShot = getLatestIncrementalSnapShot();
		
		HashSet<String> additions = new HashSet<String>();
		additions.addAll(currentIncrementalDataSnapShot);
		additions.removeAll(latestIncrementalDataSnapShot);
		
		HashSet<String> deletions = new HashSet<String>();
		deletions.addAll(latestIncrementalDataSnapShot);
		deletions.removeAll(currentIncrementalDataSnapShot);
		
		final int sizeOfAdditions = additions.size();
		final int sizeOfDeletions = deletions.size();
		
		ArrayList<ConnectorRecord> recordsToUpdate = new ArrayList<ConnectorRecord>(sizeOfAdditions + sizeOfDeletions);
		
		if (sizeOfDeletions > 0) {
			for (String data : deletions) {
				String[] datums = data.split(delimiter);
				ConnectorRecord dcr = new ConnectorRecord(datums[0], data);
				recordsToUpdate.add(dcr);
			}			
		}
		if (sizeOfAdditions > 0) {
			for (String data : additions) {
				String[] datums = data.split(delimiter);
				ConnectorRecord dccr = getSingleRecord(datums[0]);
				if (dccr != null) {
					recordsToUpdate.add(dccr);
				}
			}			
		}
		return recordsToUpdate;
	}
	
	@Override
	public ConnectorRecord getSingleRecord(String primaryKey) {
		ConnectorRecord cr = null;
		Cursor c = null;
		
		StringBuilder sb = new StringBuilder();
		
		try {
			c = getSingleRecordQueryCursor(primaryKey);
			
			if (c.moveToFirst()) {
				
				String id = c.getString(0);
				String title = c.getString(2);
				
				if (id == null || title == null) {
					return null;
				}
				
				String dateModified = c.getString(1);
				String artist = c.getString(3);
				String album = c.getString(2);
				String displayName = c.getString(4);
				
				dateModified = dateModified == null ? "0" : dateModified;
				artist = artist == null ? "" : artist;
				displayName = displayName == null ? "" : displayName;
				album = album == null ? "" : album;
			
				cr = new ConnectorRecord();
				cr.primaryKey = id;
				cr.attributeValues.put(SchemaAttributeSet.title, title);
				cr.attributeValues.put(SchemaAttributeSet.album, album);
				cr.attributeValues.put(SchemaAttributeSet.artist, artist);
				cr.attributeValues.put(SchemaAttributeSet.displayName, displayName);
				
				sb.setLength(0);
				sb.append(title);
				sb.append(" ");
				sb.append(artist);
				sb.append(" ");
				sb.append(album);

				cr.inMemoryRecordData = sb.toString();
				
				sb.setLength(0);
				sb.append(id);
				sb.append(delimiter);
				sb.append(dateModified);
				
				cr.incrementalValue = sb.toString();
			}
		} finally {
			c.close();
		}
		return cr;
	}


	@Override
	public ArrayList<com.srch2.viewable.result.ViewableResult> getViewableResults(String searchInput, ArrayList<QueryResult> queryResults) {
		ArrayList<ViewableResult> vrs = new ArrayList<ViewableResult>(queryResults.size());
		for (QueryResult qr : queryResults) {
			try {
				vrs.add(new MusicViewableResult(searchInput, qr, decodeInMemoryRecord(qr.getInMemoryRecordData())));
			} catch (Exception e) {
				vrs.clear();
				Pith.handleException(e);
			}
		}		
		return vrs;
	}
}
