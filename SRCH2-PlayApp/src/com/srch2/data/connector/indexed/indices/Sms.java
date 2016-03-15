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
import java.io.FileNotFoundException;
import java.io.IOException;
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
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
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
import com.srch2.viewable.result.ContactsViewableResult;
import com.srch2.viewable.result.SmsViewableResult;
import com.srch2.viewable.result.ViewableResult;

public class Sms extends IndexedConnector {

	private Uri contentUri = Uri.parse("content://sms/inbox");
	
	private HashMap<String, String> addressToContactNameMap = new HashMap<String, String>();
	
	private static final class SmsContentProviderTableData {
		public static final String _id = "_id";
		public static final String thread_id = "thread_id"; 
		public static final String address = "address"; 
		public static final String person = "person"; 
		public static final String date = "date"; 
		public static final String protocol = "protocol"; 
		public static final String read = "read";    
		public static final String status = "status"; 
		public static final String type = "type"; 
		public static final String reply_path_present  = "reply_path_present"; 
		public static final String subject = "subject"; 
		public static final String body = "body"; 
		public static final String service_center  = "service_center"; 
		public static final String locked = "locked"; 		
		
		public static final String[] getProjection() {
			return new String[] { _id, thread_id, address, date, body, subject };
		}
	}
	
	
	protected static AtomicBoolean accessLock;

	private final Cursor getQuickScanCursor() {
		return context.getContentResolver().query(
											contentUri, 
											new String[] { 
													SmsContentProviderTableData._id, 
													SmsContentProviderTableData.thread_id, 
													SmsContentProviderTableData.body }, 
											null, 
											null, 
											null
										  );
	}
	
	private final Cursor getReversibleScanCursor() {
		return context.getContentResolver().query(
											contentUri, 
											new String[] { 
													SmsContentProviderTableData._id,
													SmsContentProviderTableData.date,
													SmsContentProviderTableData.address,
													SmsContentProviderTableData.subject },
											null, 
											null, 
											SmsContentProviderTableData._id + " ASC, " +
													SmsContentProviderTableData.date + " ASC"
										  );
	}
	
	private final Cursor getIncrementalDataCursor() {
		return context.getContentResolver().query(
											contentUri, 
											new String[] { 
													SmsContentProviderTableData._id, 
													SmsContentProviderTableData.date,	
													SmsContentProviderTableData.body },
											null, 
											null, 
											SmsContentProviderTableData._id + " ASC, " +
													SmsContentProviderTableData.date + " ASC"
										  );
	}
	
	private final Cursor getSingleRecordQueryCursor(String primaryKeyId) {
		return context.getContentResolver().query(
											contentUri, 
											new String[] { 
													SmsContentProviderTableData._id,
													SmsContentProviderTableData.date,
													SmsContentProviderTableData.body,
													SmsContentProviderTableData.address,
													SmsContentProviderTableData.subject,
													SmsContentProviderTableData.thread_id },
											SmsContentProviderTableData._id + " =?", 
											new String[] { primaryKeyId }, 
											null
										  );
	}
	
	private static final class SchemaAttributeSet {
		private static final String body = BaseSchemaRuleSet.attribute_recordPrimarySearchAttribute;
		private static final String sender = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute1;
		private static final String sender_contact_name = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute2;
		private static final String subject = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute3;
	}
	
	public Sms(Context contxt) {
		super(contxt, SearchCategory.Sms);
		
		accessLock = new AtomicBoolean();
		super.addAndSetAccessLock(getCategory(), accessLock);
		
		inMemoryRecordSplitSize = 3;
		
		
		incrementalObserver = new ContentProviderObserver(contxt, contentUri, this);
	}

	@Override
	public final HashMap<String, Integer> getSchemaSearchAttributes() {
		HashMap<String, Integer> schemaSearchAttributes = new HashMap<String, Integer>();
		schemaSearchAttributes.put(SchemaAttributeSet.body, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		schemaSearchAttributes.put(SchemaAttributeSet.sender, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		schemaSearchAttributes.put(SchemaAttributeSet.sender_contact_name, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		schemaSearchAttributes.put(SchemaAttributeSet.subject, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		return schemaSearchAttributes;
	}
	
	@Override
	public ArrayList<ConnectorRecord> getQuickScanIndexRecords() {
		ArrayList<ConnectorRecord> newQuickScanRecords = new ArrayList<ConnectorRecord>();
		
		Cursor c = null;
		
		StringBuilder sb_sb = new StringBuilder();
		
		try {
			c = getQuickScanCursor();
			
			if (c.moveToFirst()) {
				do {
					String id = c.getString(0);
					String body = c.getString(2);
					
					if (body == null) {
						continue;
					}

					String threadId = c.getString(1);
					threadId = threadId == null ? ViewableResult.INVALID_KEY_CODE : threadId;
					
					ConnectorRecord r = new ConnectorRecord();
					r.primaryKey = id;
					r.attributeValues.put(SchemaAttributeSet.body, body);
					
					sb_sb.setLength(0);
					sb_sb.append(ViewableResult.INVALID_KEY_CODE);
					sb_sb.append(delimiter);
					sb_sb.append(threadId);
					sb_sb.append(delimiter);
					sb_sb.append(body);
					
					r.inMemoryRecordData = sb_sb.toString();
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
		
		StringBuilder sb = new StringBuilder();
		final int addressSwappingPosition = ViewableResult.INVALID_KEY_CODE.length();
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

						String address = c.getString(2);
						String subject = c.getString(3);

						address = address == null ? ViewableResult.INVALID_KEY_CODE : address;
						subject = subject == null ? "" : subject;
		
						String addressName = getContactDisplayNameFromAddress(address);
						
						cr.attributeValues.put(SchemaAttributeSet.sender, address);
						cr.attributeValues.put(SchemaAttributeSet.sender_contact_name, addressName);
						cr.attributeValues.put(SchemaAttributeSet.subject, subject);

						sb.setLength(0);
						sb.append(cr.inMemoryRecordData);
						sb.delete(0, addressSwappingPosition);
						sb.insert(0, address);
						
						cr.inMemoryRecordData = sb.toString();

						String date = c.getString(1);
						date = date == null ? "0" : date;
						
						sb.setLength(0);
						sb.append(id);
						sb.append(delimiter);
						sb.append(date);
						
						cr.incrementalValue = sb.toString();
					}
				} while (c.moveToNext());
			}
		} finally {
			c.close();
		}
		return records;
	}

	
	public String getContactDisplayNameFromAddress(String address) {
		String name = null;
		
		if (address.equals(ViewableResult.INVALID_KEY_CODE)) {
			return "";
		}
		
		if (!addressToContactNameMap.containsKey(address)) {
			name = queryContactsContractForDisplayNameFromSmsAddress(address);
			addressToContactNameMap.put(address, name);
		} else {
			return addressToContactNameMap.get(address);	
		}
		return name; 
	}
	
	private String queryContactsContractForDisplayNameFromSmsAddress(String address) {
		String name = null;
		Cursor c = null;
		
		
		try {
			Uri displayNameUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address));
			
			c =	context.getContentResolver().query(
														displayNameUri,
														new String[] {Contacts.DISPLAY_NAME_PRIMARY, Contacts.PHOTO_THUMBNAIL_URI}, 
														null, 
														null, 
														null);
			

			if (c.moveToFirst()) {
				name = c.getString(0);

			}
		} finally {
			c.close();
		}
	
		return name == null ? address : name;
	}	
	
	HashMap<String, Uri> addressToThumbnailUriMap = new HashMap<String, Uri>();
	

	
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
					String body = c.getString(2);
					
					if (body == null) {
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
				String body = c.getString(2);
				
				if (body == null) {
					return null;
				}

				cr = new ConnectorRecord();

				String threadId = c.getString(5);
				String address = c.getString(3);
				String subject = c.getString(4);

				address = address == null ? ViewableResult.INVALID_KEY_CODE : address;
				subject = subject == null ? "" : subject;
				threadId = threadId == null ? ViewableResult.INVALID_KEY_CODE : threadId;
				
				String addressName = getContactDisplayNameFromAddress(address);
				
				cr.primaryKey = id;
				cr.attributeValues.put(SchemaAttributeSet.body, body);
				cr.attributeValues.put(SchemaAttributeSet.sender, address);
				cr.attributeValues.put(SchemaAttributeSet.sender_contact_name, addressName);
				cr.attributeValues.put(SchemaAttributeSet.subject, subject);

				sb_sb.setLength(0);
				sb_sb.append(address);
				sb_sb.append(delimiter);
				sb_sb.append(threadId);
				sb_sb.append(delimiter);
				sb_sb.append(body);
				
				cr.inMemoryRecordData = sb_sb.toString();
				
				String date = c.getString(1);
				date = date == null ? "0" : date;
				
				sb_sb.setLength(0);
				sb_sb.append(id);
				sb_sb.append(delimiter);
				sb_sb.append(date);
				
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
				vrs.add(new SmsViewableResult(searchInput, qr, decodeInMemoryRecord(qr.getInMemoryRecordData())));
			} catch (Exception e) {
				vrs.clear();
				Pith.handleException(e);
			}
		}
		return vrs;
	}
}
