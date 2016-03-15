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
import java.util.Arrays;
import java.util.Collection;
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
import android.net.Uri;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContactsEntity;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Toast;

import com.srch2.Pith;
import com.srch2.R;
import com.srch2.data.SearchCategory;
import com.srch2.data.ThreadPool;
import com.srch2.data.connector.indexed.ConnectorRecord;
import com.srch2.data.connector.indexed.IndexedConnector;
import com.srch2.data.incremental.observer.ContentProviderObserver;
import com.srch2.data.index.BaseSchemaRuleSet;
import com.srch2.instantsearch.QueryResult;
import com.srch2.viewable.result.CalendarViewableResult;
import com.srch2.viewable.result.ContactsViewableResult;
import com.srch2.viewable.result.ViewableResult;



public class Contacts extends IndexedConnector {

	public static final String FLAG_NO_EMAIL = "NULL_@EMAIL";
	public static final String FLAG_NO_TELEPHONE = "NULL_@TELE";
	
	private static final String single_white_space = " ";
	private final static Uri contactsDisplayDataUri = ContactsContract.RawContacts.CONTENT_URI;
	private final static Uri contactsReversibleDataContentUri = ContactsContract.RawContactsEntity.CONTENT_URI;
	
	private final static Uri contentUri = contactsDisplayDataUri;
	
	protected static AtomicBoolean accessLock;

	private final Cursor getContactDisplayDataCursor() {
		return context.getContentResolver().query(
											contentUri, 
											new String[] { 
													ContactsContract.RawContacts.CONTACT_ID, 
													ContactsContract.RawContacts.VERSION,
													ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
													ContactsContract.RawContacts.TIMES_CONTACTED,
													ContactsContract.RawContacts.LAST_TIME_CONTACTED,
													ContactsContract.RawContacts.STARRED, 
													ContactsContract.RawContacts.RAW_CONTACT_IS_USER_PROFILE },
											ContactsContract.RawContacts.DELETED + " = 0", 
											null, 
											ContactsContract.RawContacts.CONTACT_ID + " ASC, " +
													ContactsContract.RawContacts.VERSION + " ASC"
										  );
	}
	
	private final Cursor getSingleContactDisplayDataCursor(String primaryKey) {
		return context.getContentResolver().query(
											contentUri, 
											new String[] { 
													ContactsContract.RawContacts.CONTACT_ID, 
													ContactsContract.RawContacts.VERSION,
													ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
													ContactsContract.RawContacts.TIMES_CONTACTED,
													ContactsContract.RawContacts.LAST_TIME_CONTACTED,
													ContactsContract.RawContacts.STARRED, 
													ContactsContract.RawContacts.RAW_CONTACT_IS_USER_PROFILE },
											ContactsContract.RawContacts.CONTACT_ID + " =?",
											new String[] { primaryKey }, 
											null
										  );
	}
	
	private final Cursor getContactReversibleDataCursor() {
		return context.getContentResolver().query(
											contactsReversibleDataContentUri, 
											new String[] { 
													ContactsContract.RawContactsEntity.CONTACT_ID,
													ContactsContract.RawContactsEntity.DATA_VERSION,  
													ContactsContract.RawContactsEntity.DATA_ID,
													ContactsContract.RawContactsEntity.MIMETYPE,
													ContactsContract.RawContactsEntity.DATA1,  },
											ContactsContract.RawContactsEntity.DELETED + " = 0", 
											null, 
											ContactsContract.RawContactsEntity.CONTACT_ID + " ASC, " +
													ContactsContract.RawContactsEntity.DATA_VERSION + " ASC"
										  );
	}
	
	private final Cursor getSingleContactReversibleDataCursor(String primaryKey) {
		return context.getContentResolver().query(
											contactsReversibleDataContentUri, 
											new String[] { 
													ContactsContract.RawContactsEntity.CONTACT_ID,
													ContactsContract.RawContactsEntity.DATA_VERSION,  
													ContactsContract.RawContactsEntity.DATA_ID,
													ContactsContract.RawContactsEntity.MIMETYPE,
													ContactsContract.RawContactsEntity.DATA1,  },
											ContactsContract.RawContactsEntity.CONTACT_ID + " =?",
											new String[] { primaryKey },
											null
										  );
	}
	
	private static long isUserProfileBoostValue = 999;
	public static long maximum_times_contacted = 100;
	
	private static final class SchemaAttributeSet {
		private static final String name = BaseSchemaRuleSet.attribute_recordPrimarySearchAttribute;
		private static final String reversibleLiteral = BaseSchemaRuleSet.attribute_recordSecondarySearchAttribute1;
	}
	
	public Contacts(Context contxt) {
		super(contxt, SearchCategory.Contacts);
		
		accessLock = new AtomicBoolean();
		super.addAndSetAccessLock(getCategory(), accessLock);
		
		inMemoryRecordSplitSize = 1;
		
		incrementalObserver = new ContentProviderObserver(contxt, contentUri, this);
	}

	@Override
	public final HashMap<String, Integer> getSchemaSearchAttributes() {
		HashMap<String, Integer> schemaSearchAttributes = new HashMap<String, Integer>();
		schemaSearchAttributes.put(SchemaAttributeSet.name, 5);
		schemaSearchAttributes.put(SchemaAttributeSet.reversibleLiteral, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		return schemaSearchAttributes;
	}
	
	
	private final HashSet<String> getProfileUserNames() {
		
		HashSet<String> names = new HashSet<String>();
		
		Cursor c = null;
		
		try {
			c = context.getContentResolver().query(
												ContactsContract.Profile.CONTENT_URI, 
												new String[] { ContactsContract.Profile.DISPLAY_NAME_PRIMARY,
												ContactsContract.Profile.DISPLAY_NAME}, 
												null,
												null, 
												null);
			if (c.moveToFirst()) {
				do {
					final String displayName = c.getString(0);
					final String displayName2 = c.getString(1);
					if (displayName != null && !displayName.equals("")) {
						names.add(displayName);
					}
					if (displayName2 != null && !displayName2.equals("")) {
						names.add(displayName2);
					}
					
				} while (c.moveToNext());
			}
		} finally {
			c.close();
		}
		
		return names;
	}
	
	private static final int CAP_MAXIMUM_TIMES_CONTACTED = 100;
	
	@Override
	public ArrayList<ConnectorRecord> getQuickScanIndexRecords() {
		HashMap<String, String> contactData_contactIdToDisplayNameAndBoostDataLiteral;
		Cursor c = null;
		
	//	StringBuilder sbOne = new StringBuilder();
		
		try {
			c = getContactDisplayDataCursor();
			if (c.moveToFirst()) {
				final int cursorCount = c.getCount();
				contactData_contactIdToDisplayNameAndBoostDataLiteral = new HashMap<String, String>(cursorCount);
				do {
					final String contact_id = c.getString(0);
					final String displayName = c.getString(2);
					if (contact_id == null || displayName == null) {
						continue;
					}
					
					String version = c.getString(1);
					String timesContacted = c.getString(3);
					String lastTimeContacted = c.getString(4);
					String starred = c.getString(5);

					starred = starred == null ? "0" : starred;
					timesContacted = timesContacted == null ? "0" : timesContacted;
					lastTimeContacted = lastTimeContacted == null ? "0" : lastTimeContacted;
					version = version == null ? "0" : version;
				
					
					if (contactData_contactIdToDisplayNameAndBoostDataLiteral.get(contact_id) == null) {
						contactData_contactIdToDisplayNameAndBoostDataLiteral.put(contact_id, starred + delimiter + timesContacted + delimiter + lastTimeContacted + delimiter + version + delimiter + displayName);
					} else {
						// TO DO JUST use version number to determine what one to use --test
						final String nameAndBoostLiteral = contactData_contactIdToDisplayNameAndBoostDataLiteral.get(contact_id);
						final String[] data = nameAndBoostLiteral.split(delimiter, 5);
						
						int starredi =  Integer.valueOf(starred);
						long timesContactedi =  Long.valueOf(timesContacted);
						long lastTimeContactedi =  Long.valueOf(lastTimeContacted);
						int versioni = Integer.valueOf(version);
						
						final int literal_starred = Integer.valueOf(data[0]);
						final long literal_timesContacted = Long.valueOf(data[1]);
						final long literal_lastTimeContacted = Long.valueOf(data[2]);	
						final int literal_version = Integer.valueOf(data[3]);	
						
						starredi = literal_starred > starredi ? literal_starred : starredi;
						timesContactedi = literal_timesContacted > timesContactedi ? literal_timesContacted : timesContactedi;
						lastTimeContactedi = literal_lastTimeContacted > lastTimeContactedi ? literal_lastTimeContacted : lastTimeContactedi;
						versioni = literal_version > versioni ? literal_version : versioni;

						contactData_contactIdToDisplayNameAndBoostDataLiteral.put(contact_id, String.valueOf(starredi) + delimiter + String.valueOf(timesContactedi) + delimiter + String.valueOf(lastTimeContactedi) + delimiter + String.valueOf(versioni) + delimiter + displayName);
					}
				} while (c.moveToNext());
			} else {
				contactData_contactIdToDisplayNameAndBoostDataLiteral = new HashMap<String, String>(0);
			}
		} finally {
			c.close();
		}
		
		final HashSet<String> profileUserNames = getProfileUserNames();
		
		ArrayList<ConnectorRecord> newQuickScanRecords = new ArrayList<ConnectorRecord>();
		final Set<String> contactIdKeys = contactData_contactIdToDisplayNameAndBoostDataLiteral.keySet();
		
		for (String contactId : contactIdKeys) {
			final String nameAndBoostLiteral = contactData_contactIdToDisplayNameAndBoostDataLiteral.get(contactId);
			
			final String[] data = nameAndBoostLiteral.split(delimiter, 5);
			
			ConnectorRecord ccr = new ConnectorRecord();
			ccr.primaryKey = contactId;
			ccr.attributeValues.put(SchemaAttributeSet.name, data[4]);

			ccr.boostValue = getRecordBoostValue(data[4], profileUserNames, CAP_MAXIMUM_TIMES_CONTACTED, Integer.valueOf(data[0]), Long.valueOf(data[1]), Long.valueOf(data[2]));

			ccr.incrementalValue = contactId + delimiter + data[3];
			
	
			ccr.inMemoryRecordData = data[4];
			//ccr.inMemoryRecordData = data[4] + delimiter + FLAG_NO_TELEPHONE + delimiter + FLAG_NO_EMAIL;
			newQuickScanRecords.add(ccr);
		}
		return newQuickScanRecords;
	}
	
	public static float getRecordBoostValue(String name, HashSet<String> profileIds, long maximumTimesContacted, int starred, long timesContacted, long lastTimeContacted) {
		if (profileIds.contains(name)) {
			return isUserProfileBoostValue;
		} 

		float boost = 50 * starred;
		if (timesContacted > 0) {
			
			boost += (Math.min(1,        (timesContacted / (float) (timesContacted + maximumTimesContacted))      ) * 50);
		}
	
		boost = boost < 1 ? 1 : boost;
		return boost;
	}
	
	
	StringBuilder sb_sb = new StringBuilder();

	@Override
	public ArrayList<ConnectorRecord> getReversibleScanIndexRecords(ArrayList<ConnectorRecord> records) {
		
		long t = SystemClock.uptimeMillis();
		
		HashMap<String, ConnectorRecord> contacts_contactIdToCursoryRecord = new HashMap<String, ConnectorRecord>();
		int size = 0;
		Set<String> contactIdKeys = null;
		try {
			for (ConnectorRecord cr : records) {
				contacts_contactIdToCursoryRecord.put(cr.primaryKey, cr);
			}
			contactIdKeys = contacts_contactIdToCursoryRecord.keySet();
			size = contactIdKeys.size();
		} catch (NullPointerException npe) {
			Pith.reportExceptionSilently(npe);
			contactIdKeys = new HashSet<String>(0);
		}
	
		HashMap<String, HashSet<String>> contactContent_contactIdToContentDataLiteral = new HashMap<String, HashSet<String>>(size);


		for (String contactId : contactIdKeys) {
			contactContent_contactIdToContentDataLiteral.put(contactId, new HashSet<String>());
		}

		int skipCount = 0;
	
		Cursor c = null;
		try {
			c = getContactReversibleDataCursor();

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
					
					final String contactId = c.getString(0);
					final String dataId = c.getString(2);
					

					if (contactId == null || dataId == null ) {
						continue;
					} 

					String mimelabel = c.getString(3);
					if (mimelabel != null && !reversibleLookUpMimeLabelExclusion.contains(mimelabel) && c.getType(4) != Cursor.FIELD_TYPE_BLOB) {
						final String mimeData = c.getString(4);
						
						if (mimeData != null) {
							final String[] data = mimeData.split(single_white_space);
							final int length = data.length;
							for (int x = 0; x < length; ++x) {
								if (data[x].length() > 1) {
									
									// Save this hashset, check if id returned from cursor is the same 
									// then update the local hashset reference if it has
									contactContent_contactIdToContentDataLiteral.get(contactId).add(data[x]);		
								
								}

							}
						}
					}
				
					/*  OLD WAY -- not necessary as column 1 ~98% of time has all the data
					 *  PLUS prevents duplicate data not covered by basic hashset check such as
					 *  (949)6007000 and 949-600-700
					 *  
					for (int i = 4; i < 19; ++i) {	
						if (c.getType(i) == Cursor.FIELD_TYPE_BLOB) {
							continue;
						} 
						final String mimeData = c.getString(i);
					
						if (mimeData != null) {
						

							//Log.d("reversible", "contactId " + contactId + " " + " " + mimelabel + " " + mimeData);
							
							
							final String[] data = mimeData.split(single_white_space);
							final int length = data.length;
							for (int j = 0; j < length; j++) {
								if (data[j].length() > 1) {
									contactContent_contactIdToContentDataLiteral.get(contactId).add(data[j]);		

								}
							}
						}
					}	
						*/
					
				} while (c.moveToNext());
			}
		} finally {
			c.close();
		}


		StringBuilder sbuildr = new StringBuilder();
		for (String contactId : contactIdKeys) {
			ConnectorRecord cr = contacts_contactIdToCursoryRecord.get(contactId);
			final HashSet<String> dataContentLiteral = contactContent_contactIdToContentDataLiteral.get(contactId);
			sbuildr.setLength(0);
			for (String datum : dataContentLiteral) {
			
				sbuildr.append(datum);
				sbuildr.append(single_white_space);
			}
			
			if (cr.boostValue == isUserProfileBoostValue) {
				sbuildr.append(" me Me ");
			}
			
			cr.attributeValues.put(SchemaAttributeSet.reversibleLiteral, sbuildr.toString());
			
			//cr.inMemoryRecordData = cr.attributeValues.get(SchemaAttributeSet.name) + ViewableResult.delim + getContactPhoneNumber(sbuildr, context, contactId) + ViewableResult.delim + getContactEmail(sbuildr, context, contactId);

		}
		
		long ae = SystemClock.uptimeMillis() - t;
		
	
		
		return records; 
	}
	
	private static final String[] MIME_LABEL_EXCLUSION_VALUES = 
		{ 
			"vnd.android.cursor.item/organization_svoice_dmetaphone_primary_encoding",
			"vnd.android.cursor.item/photo",
			"vnd.android.cursor.item/group_membership"
			
		};
	private static final HashSet<String> reversibleLookUpMimeLabelExclusion = new HashSet<String>(Arrays.asList(MIME_LABEL_EXCLUSION_VALUES));

	@Override
	public ArrayList<ConnectorRecord> resolveIncrementalDifferenceUpdate() {


		HashMap<String, String> contentProviderDisplayNameAndBoostLatestDataLiteral = null;
	
		StringBuilder sb_sb = new StringBuilder();
		StringBuilder sbOne = new StringBuilder();
		Cursor c = null;
		try { 
			c = getContactDisplayDataCursor();
			if (c.moveToFirst()) {
				final int cursorCount = c.getCount();
				contentProviderDisplayNameAndBoostLatestDataLiteral = new HashMap<String, String>(cursorCount);
				do {
					final String contact_id = c.getString(0);
					final String displayName = c.getString(2);
					if (contact_id == null || displayName == null) {
						continue;
					}
					
					String version = c.getString(1);
					String timesContacted = c.getString(3);
					String lastTimeContacted = c.getString(4);
					String starred = c.getString(5);

					starred = starred == null ? "0" : starred;
					timesContacted = timesContacted == null ? "0" : timesContacted;
					lastTimeContacted = lastTimeContacted == null ? "0" : lastTimeContacted;
					version = version == null ? "0" : version;
					
					sbOne.setLength(0);
					sbOne.append(starred);
					sbOne.append(delimiter);
					sbOne.append(timesContacted);
					sbOne.append(delimiter);
					sbOne.append(lastTimeContacted);
					sbOne.append(delimiter);
					sbOne.append(version);
					sbOne.append(delimiter);
					sbOne.append(displayName);

					if (!contentProviderDisplayNameAndBoostLatestDataLiteral.containsKey(contact_id)) {
						contentProviderDisplayNameAndBoostLatestDataLiteral.put(contact_id, sbOne.toString());
					} else {
						// TO DO JUST use version number to determine what one to use --test
						final String nameAndBoostLiteral = contentProviderDisplayNameAndBoostLatestDataLiteral.get(contact_id);
						final String[] data = nameAndBoostLiteral.split(delimiter, 5);
						
						int starredi =  Integer.valueOf(starred);
						long timesContactedi =  Long.valueOf(timesContacted);
						long lastTimeContactedi =  Long.valueOf(lastTimeContacted);
						int versioni = Integer.valueOf(version);
						
						final int literal_starred = Integer.valueOf(data[0]);
						final long literal_timesContacted = Long.valueOf(data[1]);
						final long literal_lastTimeContacted = Long.valueOf(data[2]);	
						final int literal_version = Integer.valueOf(data[3]);	
						
						starredi = literal_starred > starredi ? literal_starred : starredi;
						timesContactedi = literal_timesContacted > timesContactedi ? literal_timesContacted : timesContactedi;
						lastTimeContactedi = literal_lastTimeContacted > lastTimeContactedi ? literal_lastTimeContacted : lastTimeContactedi;
						versioni = literal_version > versioni ? literal_version : versioni;
						
						sbOne.setLength(0);
						sbOne.append(String.valueOf(starredi));
						sbOne.append(delimiter);
						sbOne.append(String.valueOf(timesContactedi));
						sbOne.append(delimiter);
						sbOne.append(String.valueOf(lastTimeContactedi));
						sbOne.append(delimiter);
						sbOne.append(String.valueOf(versioni));
						sbOne.append(delimiter);
						sbOne.append(displayName);
						contentProviderDisplayNameAndBoostLatestDataLiteral.put(contact_id, sbOne.toString());
					}
				} while (c.moveToNext());
			}
		} finally {
			c.close();
		}
		
		contentProviderDisplayNameAndBoostLatestDataLiteral = contentProviderDisplayNameAndBoostLatestDataLiteral == null ? new HashMap<String, String>(0) : contentProviderDisplayNameAndBoostLatestDataLiteral;
		
		HashMap<String, String> contentProviderDisplayNameAndBoostLatestData = new HashMap<String, String>(contentProviderDisplayNameAndBoostLatestDataLiteral.size());
		for (String idKey : contentProviderDisplayNameAndBoostLatestDataLiteral.keySet()) {
			String[] verdata = contentProviderDisplayNameAndBoostLatestDataLiteral.get(idKey).split(delimiter, 5);
			contentProviderDisplayNameAndBoostLatestData.put(idKey, idKey + delimiter + verdata[3]);
		}
		
		HashSet<String> currentIncrementalDataSnapShot = new HashSet<String>();
		
		final Set<String> idKeySet = contentProviderDisplayNameAndBoostLatestData.keySet();
		for (String contactId : idKeySet) {
			
			sb_sb.setLength(0);
			sb_sb.append(contentProviderDisplayNameAndBoostLatestData.get(contactId));
			
			currentIncrementalDataSnapShot.add(sb_sb.toString());
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
				String[] datums = data.split(delimiter, 2);
				ConnectorRecord dcr = new ConnectorRecord(datums[0], data);
				recordsToUpdate.add(dcr);
			}			
		}
		
		if (sizeOfAdditions > 0) {
			for (String data : additions) {
				String[] datums = data.split(delimiter, 2);
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
	
		if (Thread.currentThread().isInterrupted()) {
			return null;
		}
		
		ConnectorRecord cr = null;

		long maximumTimesContacted = 0;
		HashMap<String, String> contactData_contactIdToDisplayNameAndBoostDataLiteral;
		Cursor c = null;
		
		StringBuilder sbOne = new StringBuilder();
		try {
			c = getSingleContactDisplayDataCursor(primaryKey);
			if (c.moveToFirst()) {
				final int cursorCount = c.getCount();
				contactData_contactIdToDisplayNameAndBoostDataLiteral = new HashMap<String, String>(cursorCount);
				do {
					final String contact_id = c.getString(0);
					final String displayName = c.getString(2);
					if (contact_id == null || displayName == null) {
						continue;
					}
					
					String version = c.getString(1);
					String timesContacted = c.getString(3);
					String lastTimeContacted = c.getString(4);
					String starred = c.getString(5);

					starred = starred == null ? "0" : starred;
					timesContacted = timesContacted == null ? "0" : timesContacted;
					lastTimeContacted = lastTimeContacted == null ? "0" : lastTimeContacted;
					version = version == null ? "0" : version;

					if (contactData_contactIdToDisplayNameAndBoostDataLiteral.get(contact_id) == null) {
						sbOne.setLength(0);
						sbOne.append(starred);
						sbOne.append(delimiter);
						sbOne.append(timesContacted);
						sbOne.append(delimiter);
						sbOne.append(lastTimeContacted);
						sbOne.append(delimiter);
						sbOne.append(version);
						sbOne.append(delimiter);
						sbOne.append(displayName);
						contactData_contactIdToDisplayNameAndBoostDataLiteral.put(contact_id, sbOne.toString());
					} else {
						// TO DO JUST use version number to determine what one to use --test
						final String nameAndBoostLiteral = contactData_contactIdToDisplayNameAndBoostDataLiteral.get(contact_id);
						final String[] data = nameAndBoostLiteral.split(delimiter, 5);
						
						int starredi =  Integer.valueOf(starred);
						long timesContactedi =  Long.valueOf(timesContacted);
						long lastTimeContactedi =  Long.valueOf(lastTimeContacted);
						int versioni = Integer.valueOf(version);
						
						final int literal_starred = Integer.valueOf(data[0]);
						final long literal_timesContacted = Long.valueOf(data[1]);
						final long literal_lastTimeContacted = Long.valueOf(data[2]);	
						final int literal_version = Integer.valueOf(data[3]);	
						
						starredi = literal_starred > starredi ? literal_starred : starredi;
						timesContactedi = literal_timesContacted > timesContactedi ? literal_timesContacted : timesContactedi;
						lastTimeContactedi = literal_lastTimeContacted > lastTimeContactedi ? literal_lastTimeContacted : lastTimeContactedi;
						versioni = literal_version > versioni ? literal_version : versioni;
						
						sbOne.setLength(0);
						sbOne.append(String.valueOf(starredi));
						sbOne.append(delimiter);
						sbOne.append(String.valueOf(timesContactedi));
						sbOne.append(delimiter);
						sbOne.append(String.valueOf(lastTimeContactedi));
						sbOne.append(delimiter);
						sbOne.append(String.valueOf(versioni));
						sbOne.append(delimiter);
						sbOne.append(displayName);
						
						contactData_contactIdToDisplayNameAndBoostDataLiteral.put(contact_id, sbOne.toString());
					}
				} while (c.moveToNext());
			} else {
				contactData_contactIdToDisplayNameAndBoostDataLiteral = new HashMap<String, String>(0);
			}
		} finally {
			c.close();
		}
		
		final HashSet<String> profileUserNames = getProfileUserNames();
		
		final String nameAndBoostLiteral = contactData_contactIdToDisplayNameAndBoostDataLiteral.get(primaryKey);
		final String[] data = nameAndBoostLiteral.split(delimiter, 5);
		

		cr = new ConnectorRecord();
		cr.primaryKey = primaryKey;
		cr.attributeValues.put(SchemaAttributeSet.name, data[4]);
		cr.boostValue = getRecordBoostValue(data[4], profileUserNames, maximumTimesContacted, Integer.valueOf(data[0]), Long.valueOf(data[1]), Long.valueOf(data[2]));
		
		sbOne.setLength(0);
		sbOne.append(primaryKey);
		sbOne.append(delimiter);
		sbOne.append(data[3]);
		
		cr.incrementalValue = sbOne.toString();

		HashMap<String, HashSet<String>> contactContent_contactIdToContentDataLiteral = new HashMap<String, HashSet<String>>();
	
		contactContent_contactIdToContentDataLiteral.put(primaryKey, new HashSet<String>());


		c = null;
		try {

			c = getSingleContactReversibleDataCursor(primaryKey);
			
			if (c.moveToFirst()) {
				do {
					
					final String contactId = c.getString(0);
					final String dataId = c.getString(2);
					
					if (contactId == null || dataId == null) {
						continue;
					}
		
					
					
					String mimelabel = c.getString(3);
					if (mimelabel != null && !reversibleLookUpMimeLabelExclusion.contains(mimelabel) && c.getType(4) != Cursor.FIELD_TYPE_BLOB) {
						final String mimeData = c.getString(4);
						
						if (mimeData != null) {
							final String[] dataa = mimeData.split(single_white_space);
							final int length = dataa.length;
							for (int x = 0; x < length; ++x) {
								if (dataa[x].length() > 1) {
									contactContent_contactIdToContentDataLiteral.get(contactId).add(dataa[x]);		
								
								}

							}
						}
					}
					
	
				} while (c.moveToNext());
			}
		} finally {
			c.close();
		}
		
		StringBuilder sbuildr = new StringBuilder();
		sbuildr.setLength(0);
		
		final HashSet<String> dataContentLiteral = contactContent_contactIdToContentDataLiteral.get(primaryKey);
		for (String datum : dataContentLiteral) {
			sbuildr.append(datum);
			sbuildr.append(single_white_space);
		}

		if (cr.boostValue == isUserProfileBoostValue) {
			sbuildr.append(" me Me ");
		}
		cr.attributeValues.put(SchemaAttributeSet.reversibleLiteral, sbuildr.toString());
		
		//cr.inMemoryRecordData = data[4] + delimiter + FLAG_NO_TELEPHONE + delimiter + FLAG_NO_EMAIL;
		cr.inMemoryRecordData = data[4];
		return cr;
	}

	private static final StringBuilder getContactEmail(StringBuilder sb,Context context, String contactId) {
		sb.setLength(0);
        Cursor c = null;
        try {
        	c = context.getContentResolver().query(
												 ContactsContract.CommonDataKinds.Email.CONTENT_URI,
							                     new String[] { ContactsContract.CommonDataKinds.Email.DATA }, 
							                     ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
							                     new String[] {contactId}, 
							                     null);

        	if(c.moveToFirst()) {
        		sb.append(c.getString(0));
			}
        } finally {
        	c.close();
        }
        if (sb.length() < 1) {
        	sb.setLength(0);
        	sb.append(FLAG_NO_EMAIL);
        	return sb;
        } else {
        	return sb;
        }
	}
	
	private static final StringBuilder getContactPhoneNumber(StringBuilder sb, Context context, String contactId) {
		sb.setLength(0);
        Cursor c = null;
        try {
        	c = context.getContentResolver().query(
												 ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							                     new String[] { ContactsContract.CommonDataKinds.Phone.DATA }, 
							                     ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
							                     new String[] {contactId}, 
							                     null);

        	if(c.moveToFirst()) {
        		sb.append(c.getString(0));
			}
        } finally {
        	c.close();
        }
        if (sb.length() < 1) {
        	sb.setLength(0);
        	sb.append(FLAG_NO_TELEPHONE);
        	return sb;
        } else {
        	return sb;
        }
	}
	
	
	
	public static final String getContactEmail(Context context, String contactId) {
		String s = null;
        Cursor c = null;
        try {
        	c = context.getContentResolver().query(
												 ContactsContract.CommonDataKinds.Email.CONTENT_URI,
							                     new String[] { ContactsContract.CommonDataKinds.Email.DATA }, 
							                     ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
							                     new String[] {contactId}, 
							                     null);

        	if(c.moveToFirst()) {
        		s = c.getString(0);
			}
        } finally {
        	c.close();
        }
        if (s == null) {
        	return FLAG_NO_EMAIL;
        } else if (s.length() < 1) {
        	return FLAG_NO_EMAIL;
        } else {
        	return s;
        }
	}
	
	public static final String getContactPhoneNumber(Context context, String contactId) {
		String s = null;
        Cursor c = null;
        try {
        	c = context.getContentResolver().query(
												 ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							                     new String[] { ContactsContract.CommonDataKinds.Phone.DATA }, 
							                     ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
							                     new String[] {contactId}, 
							                     null);

        	if(c.moveToFirst()) {
        		s = c.getString(0);
			}
        } finally {
        	c.close();
        }
        if (s == null) {
        	return FLAG_NO_TELEPHONE;
        } else if (s.length() < 1) {
        	return FLAG_NO_TELEPHONE;
        } else {
        	return s;
        }
	}
	
	@Override
	public ArrayList<ViewableResult> getViewableResults(String searchInput, ArrayList<QueryResult> queryResults) {
		ArrayList<ViewableResult> vrs = new ArrayList<ViewableResult>(queryResults.size());
		for (QueryResult qr : queryResults) {
			try {
				vrs.add(new ContactsViewableResult(searchInput, qr, decodeInMemoryRecord(qr.getInMemoryRecordData())));
			} catch (Exception e) {
				vrs.clear();
				Pith.handleException(e);
			}
		}
		return vrs;
	}
}
