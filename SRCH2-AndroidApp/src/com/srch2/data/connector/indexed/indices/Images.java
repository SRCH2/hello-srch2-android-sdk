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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.widget.Toast;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.ThreadPool;
import com.srch2.data.connector.indexed.ConnectorRecord;
import com.srch2.data.connector.indexed.IndexedConnector;
import com.srch2.data.incremental.observer.ContentProviderObserver;
import com.srch2.data.index.BaseSchemaRuleSet;
import com.srch2.instantsearch.QueryResult;
import com.srch2.viewable.result.ImagesViewableResult;
import com.srch2.viewable.result.ViewableResult;

public class Images extends IndexedConnector {

	protected static AtomicBoolean accessLock;

	private final Cursor getQuickScanCursor() {
		return context.getContentResolver().query(
											MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
											new String[] { MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.TITLE }, 
											null, 
											null, 
											null
										  );
	}
	
	private final Cursor getReversibleScanCursor() {
		return context.getContentResolver().query(
											MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
											new String[] { 
													MediaStore.Images.ImageColumns._ID,
													MediaStore.Images.ImageColumns.DATE_MODIFIED,
													MediaStore.Images.ImageColumns.DESCRIPTION,
													MediaStore.Images.ImageColumns.DISPLAY_NAME },
											null, 
											null, 
											MediaStore.Images.ImageColumns._ID + " ASC, " +
													MediaStore.Images.ImageColumns.DATE_MODIFIED + " ASC"
										  );
	}
	
	private final Cursor getIncrementalDataCursor() {
		return context.getContentResolver().query(
											MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
											new String[] { 
													MediaStore.Images.ImageColumns._ID,
													MediaStore.Images.ImageColumns.DATE_MODIFIED,		
													MediaStore.Images.ImageColumns.TITLE },
											null, 
											null, 
											MediaStore.Images.ImageColumns._ID + " ASC, " +
													MediaStore.Images.ImageColumns.DATE_MODIFIED + " ASC"
										  );
	}
	
	private final Cursor getSingleRecordQueryCursor(String primaryKeyId) {
		return context.getContentResolver().query(
											MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
											new String[] { 
													MediaStore.Images.ImageColumns._ID,
													MediaStore.Images.ImageColumns.DATE_MODIFIED,
													MediaStore.Images.ImageColumns.TITLE,
													MediaStore.Images.ImageColumns.DESCRIPTION,
													MediaStore.Images.ImageColumns.DISPLAY_NAME },
											MediaStore.Images.ImageColumns._ID + " =?", 
											new String[] { primaryKeyId }, 
											null
										  );
	}
	
	private static final class SchemaAttributeSet {
		private static final String title = BaseSchemaRuleSet.attribute_recordPrimarySearchAttribute;
		private static final String description = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute1;
		private static final String displayName = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute2;
	}
	
	public Images(Context contxt) {
		super(contxt, SearchCategory.Images);
		
		accessLock = new AtomicBoolean();
		super.addAndSetAccessLock(getCategory(), accessLock);
		
		inMemoryRecordSplitSize = 1;

		incrementalObserver = new ContentProviderObserver(contxt, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, this);
	}

	@Override
	public final HashMap<String, Integer> getSchemaSearchAttributes() {
		HashMap<String, Integer> schemaSearchAttributes = new HashMap<String, Integer>();
		schemaSearchAttributes.put(SchemaAttributeSet.title, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		schemaSearchAttributes.put(SchemaAttributeSet.description, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		schemaSearchAttributes.put(SchemaAttributeSet.displayName, BaseSchemaRuleSet.defaultSearchAttributeBoost);
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
						
						String description = c.getString(2);
						String displayName = c.getString(3);
						description = description == null ? "" : description;
						displayName = displayName == null ? "" : displayName;
						cr.attributeValues.put(SchemaAttributeSet.description, description);
						cr.attributeValues.put(SchemaAttributeSet.displayName, displayName);
						
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

		HashSet<String> currentIncrementalDataSnapShot = new HashSet<String>();
		Cursor c = null;
		
		StringBuilder sb_sb = new StringBuilder();
		
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
		
		StringBuilder sb_sb = new StringBuilder();
		
		try {
			c = getSingleRecordQueryCursor(primaryKey);
			
			if (c.moveToFirst()) {
				
				String id = c.getString(0);
				String title = c.getString(2);
				
				if (id == null || title == null) {
					return null;
				}
				
				String dateModified = c.getString(1);
				String description = c.getString(3);
				String displayName = c.getString(4); 
				
				dateModified = dateModified == null ? "0" : dateModified;
				description = description == null ? "" : description;
				displayName = displayName == null ? "" : displayName;
				
				cr = new ConnectorRecord();
				cr.primaryKey = id;
				cr.attributeValues.put(SchemaAttributeSet.title, title);
				cr.attributeValues.put(SchemaAttributeSet.description, description);
				cr.attributeValues.put(SchemaAttributeSet.displayName, displayName);
				cr.inMemoryRecordData = title;
				
				sb_sb.setLength(0);
				sb_sb.append(id);
				sb_sb.append(delimiter);
				sb_sb.append(dateModified);
				cr.incrementalValue = sb_sb.toString();
			}
		} finally {
			c.close();
		}
		return cr;
	}


	@Override
	public ArrayList<ViewableResult> getViewableResults(String searchInput, ArrayList<QueryResult> queryResults) {
		ArrayList<ViewableResult> vrs = new ArrayList<ViewableResult>(queryResults.size());
		for (QueryResult qr : queryResults) {
			try {
				vrs.add(new ImagesViewableResult(searchInput, qr, decodeInMemoryRecord(qr.getInMemoryRecordData())));
			} catch (Exception e) {
				vrs.clear();
				Pith.handleException(e);
			}
		}
		return vrs;
	}
}
