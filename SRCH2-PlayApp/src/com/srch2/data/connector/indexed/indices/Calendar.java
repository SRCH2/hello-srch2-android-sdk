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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.DashPathEffect;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.CalendarContract.Events;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.ThreadPool;
import com.srch2.data.connector.indexed.ConnectorRecord;
import com.srch2.data.connector.indexed.IndexedConnector;
import com.srch2.data.incremental.observer.ContentProviderObserver;
import com.srch2.data.index.BaseSchemaRuleSet;
import com.srch2.instantsearch.QueryResult;
import com.srch2.viewable.result.CalendarViewableResult;
import com.srch2.viewable.result.ImagesViewableResult;
import com.srch2.viewable.result.ViewableResult;

public class Calendar extends IndexedConnector {

	private Uri calendarEventsDataSetUri = CalendarContract.EventsEntity.CONTENT_URI;
	private Uri calendarAttendeesDataSetUri = CalendarContract.Attendees.CONTENT_URI;
	
	private Uri contentUri = calendarEventsDataSetUri;
	
	
	protected static AtomicBoolean accessLock;

	private final Cursor getQuickScanCursor() {
		return context.getContentResolver().query(
											contentUri, 
											new String[] { 
													CalendarContract.EventsEntity._ID,
													CalendarContract.EventsEntity.TITLE,
													CalendarContract.EventsEntity.DTSTART,
													CalendarContract.EventsEntity.DTEND },
											CalendarContract.EventsEntity.DELETED + " =0", 
											null, 
											null
										  );
	}
	
	private final Cursor getReversibleScanCursor() {
		return context.getContentResolver().query(
											contentUri, 
											new String[] { 
													CalendarContract.EventsEntity._ID,
													CalendarContract.EventsEntity.SYNC_DATA5,
													CalendarContract.EventsEntity.HAS_ALARM,
													CalendarContract.EventsEntity.EVENT_COLOR,
													CalendarContract.EventsEntity.DESCRIPTION },
											CalendarContract.EventsEntity.DELETED + " =0", 
											null, 
											CalendarContract.EventsEntity._ID + " ASC, " +
													CalendarContract.EventsEntity.SYNC_DATA5 + " ASC"
										  );
	}
	
	private final Cursor getReversibleAttendeesScanCursor() {
		return context.getContentResolver().query(
											calendarAttendeesDataSetUri, 
											new String[] { 
													CalendarContract.Attendees.EVENT_ID,
													CalendarContract.Attendees.ATTENDEE_NAME,
													CalendarContract.Attendees.ATTENDEE_EMAIL },
											null, 
											null, 
											CalendarContract.Attendees.EVENT_ID + " ASC "
										  );
	}
	
	private final Cursor getIncrementalDataCursor() {
		return context.getContentResolver().query(
											contentUri, 
											new String[] { 
													CalendarContract.EventsEntity._ID,
													CalendarContract.EventsEntity.SYNC_DATA5,	
													CalendarContract.EventsEntity.TITLE },
											CalendarContract.EventsEntity.DELETED + " =0",  
											null, 
											CalendarContract.EventsEntity._ID + " ASC, " +
													CalendarContract.EventsEntity.SYNC_DATA5 + " ASC"
										  );
	}
	
	private final Cursor getSingleRecordQueryCursor(String primaryKeyId) {
		return context.getContentResolver().query(
											contentUri, 
											new String[] { 
													CalendarContract.EventsEntity._ID,
													CalendarContract.EventsEntity.SYNC_DATA5,
													CalendarContract.EventsEntity.HAS_ALARM,
													CalendarContract.EventsEntity.EVENT_COLOR,
													CalendarContract.EventsEntity.TITLE,
													CalendarContract.EventsEntity.DESCRIPTION,
													CalendarContract.EventsEntity.DTSTART,
													CalendarContract.EventsEntity.DTEND },
											CalendarContract.Events._ID + " =?", 
											new String[] { primaryKeyId }, 
											null
										  );
	}
	
	private final Cursor getSingleCalendarEventQueryAttendeesCursor(String primaryKeyId) {
		return context.getContentResolver().query(
											calendarAttendeesDataSetUri, 
											new String[] { 
													CalendarContract.Attendees.EVENT_ID,
													CalendarContract.Attendees.ATTENDEE_NAME,
													CalendarContract.Attendees.ATTENDEE_EMAIL },
											CalendarContract.Attendees.EVENT_ID + " =?", 
											new String[] { primaryKeyId }, 
											CalendarContract.Attendees.EVENT_ID + " ASC "
										  );
	}
	
	
	private static final class SchemaAttributeSet {
		private static final String title = BaseSchemaRuleSet.attribute_recordPrimarySearchAttribute;
		private static final String description = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute1;
		private static final String attendees = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute2;
	}
	
	public Calendar(Context contxt) {
		super(contxt, SearchCategory.Calendar);
		
		inMemoryRecordSplitSize = 3; // start ~ end ~ title
		
		accessLock = new AtomicBoolean();
		super.addAndSetAccessLock(getCategory(), accessLock);
		
		incrementalObserver = new ContentProviderObserver(contxt, contentUri, this);
	}

	@Override
	public final HashMap<String, Integer> getSchemaSearchAttributes() {
		HashMap<String, Integer> schemaSearchAttributes = new HashMap<String, Integer>();
		schemaSearchAttributes.put(SchemaAttributeSet.title, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		schemaSearchAttributes.put(SchemaAttributeSet.description, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		schemaSearchAttributes.put(SchemaAttributeSet.attendees, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		return schemaSearchAttributes;
	}
	
	@Override
	public ArrayList<ConnectorRecord> getQuickScanIndexRecords() {
		ArrayList<ConnectorRecord> newQuickScanRecords = new ArrayList<ConnectorRecord>();
		
		Cursor c = null;
		StringBuilder sb_inMemoryRecordData = new StringBuilder();

		try {
			c = getQuickScanCursor();
			
			if (c.moveToFirst()) {

				do {
					String id = c.getString(0);
					String title = c.getString(1);
					
					if (id == null || title == null) {
						continue;
					}

					String start = c.getString(2);
					String end = c.getString(3);
					
					start = start == null ? "0" : start;
					end = end == null ? "0" : end;
					
					ConnectorRecord r = new ConnectorRecord();
					r.primaryKey = id;
					r.attributeValues.put(SchemaAttributeSet.title, title);
					
					sb_inMemoryRecordData.setLength(0);
					sb_inMemoryRecordData.append(start);
					sb_inMemoryRecordData.append(delimiter);
					sb_inMemoryRecordData.append(end);
					sb_inMemoryRecordData.append(delimiter);
					sb_inMemoryRecordData.append(title);
					r.inMemoryRecordData = sb_inMemoryRecordData.toString();
					
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
		StringBuilder sb_incrementalValue = new StringBuilder();
		
		try {
			c = getReversibleScanCursor();
			
			if (c.moveToFirst()) {
				
				int cursorCount = c.getCount();
				int count = 0;
				int stepSize = ThreadPool.isSingleCore() ? 250 : 100000; 
	
				do {
					String id = c.getString(0);
					
					if (id != null && recordsPrimaryKeyMap.containsKey(id)) {
						
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
						
						ConnectorRecord cr = recordsPrimaryKeyMap.get(id);
						
						String dateModified = c.getString(1);
						String alarm = c.getString(2);
						//String color = c.getString(3);
						String description = c.getString(4);
	
						dateModified = dateModified == null ? "0" : dateModified;
						description = description == null ? "" : description;
						alarm = alarm == null ? "0" : alarm;

					
						cr.attributeValues.put(SchemaAttributeSet.description, description);

						
						dateModified = dateModified == null ? "0" : dateModified;
						
						sb_incrementalValue.setLength(0);
						sb_incrementalValue.append(id);
						sb_incrementalValue.append(delimiter);
						sb_incrementalValue.append(dateModified);
						cr.incrementalValue = sb_incrementalValue.toString();
					}
				} while (c.moveToNext());
			}
		} finally {
			c.close();
		}
		
		final Set<String> eventIdKeySet = recordsPrimaryKeyMap.keySet();
		HashMap<String, String> eventIdsToAttendeesDataMap = new HashMap<String, String>();
		
		Cursor ac = null;
		
		try {
			ac = getReversibleAttendeesScanCursor();
			if (ac.moveToFirst()) {
				do {
					String eventId = ac.getString(0);
					
					if (eventId != null && eventIdKeySet.contains(eventId)) {
						String name = ac.getString(1);
						String email = ac.getString(2);
						
						name = name == null ? "" : name;
						email = email == null ? "" : email;
						
						if (!eventIdsToAttendeesDataMap.containsKey(eventId)) {
							eventIdsToAttendeesDataMap.put(eventId, name + " " + email + " ");
						} else {
							eventIdsToAttendeesDataMap.put(eventId, eventIdsToAttendeesDataMap.get(eventId) + name + " " + email + " ");
						}
					}
				} while (ac.moveToNext());
			}
		} finally {
			ac.close();
		}
		
		for (String eventIdKey : eventIdKeySet) {
			final String attendeeLiteralData = eventIdsToAttendeesDataMap.get(eventIdKey);		
			recordsPrimaryKeyMap.get(eventIdKey).attributeValues.put(SchemaAttributeSet.attendees, attendeeLiteralData);	
		}

		return records;
	}

	@Override
	public ArrayList<ConnectorRecord> resolveIncrementalDifferenceUpdate() {

		HashSet<String> currentIncrementalDataSnapShot = new HashSet<String>();
		Cursor c = null;
		try { 
			StringBuilder sb_incrementalValue = new StringBuilder();
			
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
					
					sb_incrementalValue.setLength(0);
					sb_incrementalValue.append(id);
					sb_incrementalValue.append(delimiter);
					sb_incrementalValue.append(dateModified);
					
					currentIncrementalDataSnapShot.add(sb_incrementalValue.toString());
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
				String title = c.getString(4);
				
				if (id == null || title == null) {
					return null;
				}
				
				String dateModified = c.getString(1);
				String alarm = c.getString(2);
				//String color = c.getString(3);
				String description = c.getString(5);
				String startTime = c.getString(6);
				String endTime = c.getString(7);
				
				dateModified = dateModified == null ? "0" : dateModified;
				description = description == null ? "" : description;
				alarm = alarm == null ? "0" : alarm;
				//color = color == null ? "FFFFFF" : color;
				startTime = startTime == null ? "0" : startTime;
				endTime = endTime == null ? "0" : endTime;
				
				cr = new ConnectorRecord();
				cr.primaryKey = id;
				cr.attributeValues.put(SchemaAttributeSet.title, title);
				cr.attributeValues.put(SchemaAttributeSet.description, description);
				
				sb_sb.setLength(0);
				sb_sb.append(startTime);
				sb_sb.append(delimiter);
				sb_sb.append(endTime);
				sb_sb.append(delimiter);
				sb_sb.append(title);

				cr.inMemoryRecordData = sb_sb.toString();
				
				sb_sb.setLength(0);
				sb_sb.append(id);
				sb_sb.append(delimiter);
				sb_sb.append(dateModified);
				cr.incrementalValue = sb_sb.toString();
			}
		} finally {
			c.close();
		}
		
		HashMap<String, String> eventIdsToAttendeesDataMap = new HashMap<String, String>();
		
		Cursor ac = null;
		
		try {
			ac = getSingleCalendarEventQueryAttendeesCursor(primaryKey);
			
			if (ac.moveToFirst()) {
				do {
					String eventId = ac.getString(0);
					
					if (eventId != null && eventId == primaryKey) {
						String name = ac.getString(1);
						String email = ac.getString(2);
						
						name = name == null ? "" : name;
						email = email == null ? "" : email;
						
						if (!eventIdsToAttendeesDataMap.containsKey(primaryKey)) {
							eventIdsToAttendeesDataMap.put(primaryKey, name + " " + email + " ");
						} else {
							eventIdsToAttendeesDataMap.put(primaryKey, eventIdsToAttendeesDataMap.get(eventId) + name + " " + email + " ");
						}
					}
				} while (ac.moveToNext());
			}
		} finally {
			ac.close();
		}
		cr.attributeValues.put(SchemaAttributeSet.attendees, eventIdsToAttendeesDataMap.get(eventIdsToAttendeesDataMap.get(primaryKey)));
		return cr;
	}

	@Override
	public ArrayList<ViewableResult> getViewableResults(String searchInput, ArrayList<QueryResult> queryResults) {
		ArrayList<ViewableResult> vrs = new ArrayList<ViewableResult>(queryResults.size());
		for (QueryResult qr : queryResults) {
			try {
				vrs.add(new CalendarViewableResult(searchInput, qr, decodeInMemoryRecord(qr.getInMemoryRecordData())));
			} catch (Exception e) {
				Pith.handleException(e);
				vrs.clear();
			}
		}
		return vrs;
	}

}
