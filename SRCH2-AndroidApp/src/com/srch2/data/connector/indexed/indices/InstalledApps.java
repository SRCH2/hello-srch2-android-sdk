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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.ThreadPool;
import com.srch2.data.connector.indexed.ConnectorRecord;
import com.srch2.data.connector.indexed.IndexedConnector;
import com.srch2.data.incremental.observer.BroadcastRecieverObserver;
import com.srch2.data.index.BaseSchemaRuleSet;
import com.srch2.instantsearch.QueryResult;
import com.srch2.viewable.result.ContactsViewableResult;
import com.srch2.viewable.result.InstalledAppsViewableResult;
import com.srch2.viewable.result.ViewableResult;

public class InstalledApps extends IndexedConnector {

	protected static AtomicBoolean accessLock;
	
	private PackageManager packageManager = null;
	
	private static final IntentFilter getIntentListenerForAppPackageChanges() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
		intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		intentFilter.addDataScheme("package");
		return intentFilter;
	}
	
	private static final class SchemaAttributeSet {
		private static final String app_name = BaseSchemaRuleSet.attribute_recordPrimarySearchAttribute;
	}
	
	public InstalledApps(Context contxt) {
		super(contxt, SearchCategory.InstalledApps);
		
		accessLock = new AtomicBoolean();
		super.addAndSetAccessLock(getCategory(), accessLock);
		
		inMemoryRecordSplitSize = 1;
		
		
		packageManager = contxt.getPackageManager();
		
		incrementalObserver = new BroadcastRecieverObserver(contxt, getIntentListenerForAppPackageChanges(), this);
	}

	@Override
	public final HashMap<String, Integer> getSchemaSearchAttributes() {
		HashMap<String, Integer> schemaSearchAttributes = new HashMap<String, Integer>();
		schemaSearchAttributes.put(SchemaAttributeSet.app_name, BaseSchemaRuleSet.defaultSearchAttributeBoost);
		return schemaSearchAttributes;
	}
	
	@Override
	public ArrayList<ConnectorRecord> getQuickScanIndexRecords() {
		ArrayList<ConnectorRecord> newQuickScanRecords = new ArrayList<ConnectorRecord>();		
		ConnectorRecord cr = new ConnectorRecord();
		cr.primaryKey = "com.srch2";
		cr.attributeValues.put(SchemaAttributeSet.app_name, "SRCH2");
		cr.inMemoryRecordData = "SRCH2";
		newQuickScanRecords.add(cr);
		return newQuickScanRecords;
	}
	
	@Override
	public ArrayList<ConnectorRecord> getReversibleScanIndexRecords(ArrayList<ConnectorRecord> records) {
		ArrayList<ConnectorRecord> reversibleRecords = new ArrayList<ConnectorRecord>();		

		Intent mainIntentMask = new Intent(Intent.ACTION_MAIN, null);
		mainIntentMask.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> list = packageManager.queryIntentActivities(mainIntentMask, 0);

		StringBuilder sb_sb = new StringBuilder();
		
		int cursorCount = 10000;
		try {
			cursorCount = list.size();
		} catch (NullPointerException npe) {
			Pith.handleException(npe);
		}

		int count = 0;
		int stepSize = ThreadPool.isSingleCore() ? 250 : 100000; 

		ApplicationInfo appInfo = null;
		
		
		
		for (ResolveInfo resolveInfo : list) {
			
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
			
		
			
			appInfo = resolveInfo.activityInfo.applicationInfo;
			final String packageName = appInfo.packageName;
			String applicationName = packageManager.getApplicationLabel(appInfo).toString();

			
			
			
			
			if (applicationName == null) {
				applicationName = "NoApp";
			}

			if (!applicationName.equals("NoApp")) {
				final String appFile = appInfo.sourceDir;
				final String dateModified = String.valueOf(new File(appFile).lastModified());
				
				ConnectorRecord r = new ConnectorRecord();
				r.primaryKey = packageName;
				r.attributeValues.put(SchemaAttributeSet.app_name, applicationName);
				r.inMemoryRecordData = applicationName;
				
				sb_sb.setLength(0);
				sb_sb.append(packageName);
				sb_sb.append(delimiter);
				sb_sb.append(dateModified);

				
				r.incrementalValue = sb_sb.toString();
				reversibleRecords.add(r);
			}
		}
		
		return reversibleRecords;
	}
	
	private String getCurrentAppName(String targetPackage) {
		try {
			ApplicationInfo info = packageManager.getApplicationInfo(targetPackage, 0);
			return packageManager.getApplicationLabel(info).toString();
		} catch (NameNotFoundException e) {
			Pith.handleException(e);
			return "noApp";
		}
	}
	
	@Override
	public ArrayList<ConnectorRecord> resolveIncrementalDifferenceUpdate() {
		ArrayList<ConnectorRecord> recordsToUpdate = new ArrayList<ConnectorRecord>();
		
		HashSet<String> contentProviderLatestData = new HashSet<String>();
		
		Intent mainIntentMask = new Intent(Intent.ACTION_MAIN, null);
		mainIntentMask.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> list = packageManager.queryIntentActivities(mainIntentMask, 0);

		StringBuilder sb_sb = new StringBuilder();
		
		for (ResolveInfo resolveInfo : list) {
			final ActivityInfo activityInfo = resolveInfo.activityInfo;

			final ApplicationInfo appInfo = activityInfo.applicationInfo;
			final String packageName = appInfo.packageName;
			final String applicationName = getCurrentAppName(packageName);
			
			if (!applicationName.equals("NoApp")) {
				final String appFile = appInfo.sourceDir;
				final String dateModified = String.valueOf(new File(appFile).lastModified());
				
				sb_sb.setLength(0);
				sb_sb.append(packageName);
				sb_sb.append(delimiter);
				sb_sb.append(dateModified);

				contentProviderLatestData.add(sb_sb.toString());
			}
		}		
				
		HashSet<String> incrementalData = getLatestIncrementalSnapShot();
		
		HashSet<String> additions = new HashSet<String>();
		additions.addAll(contentProviderLatestData);
		additions.removeAll(incrementalData);
		
		HashSet<String> deletions = new HashSet<String>();
		deletions.addAll(incrementalData);
		deletions.removeAll(contentProviderLatestData);
		
		final int sizeOfAdditions = additions.size();
		final int sizeOfDeletions = deletions.size();
		
		if (sizeOfDeletions > 0) {
			for (String data : deletions) {
				String[] datums = data.split(delimiter);
				if (datums[0].equals("com.srch2")) {
					continue;
				}
				ConnectorRecord dcr = new ConnectorRecord(datums[0], data);
				recordsToUpdate.add(dcr);
			}			
		}
		if (sizeOfAdditions > 0) {
			for (String data : additions) {	
				String[] datums = data.split(delimiter);
				ConnectorRecord dccr = new ConnectorRecord();
				dccr.primaryKey = datums[0];
				dccr.attributeValues.put(SchemaAttributeSet.app_name, getCurrentAppName(datums[0]));
				dccr.inMemoryRecordData = getCurrentAppName(datums[0]);
				dccr.incrementalValue = data;
				recordsToUpdate.add(dccr);
			}			
		}
		
		return recordsToUpdate;
	}
	
	@Override
	public ConnectorRecord getSingleRecord(String primaryKey) {
		// unusued by this indexed connector
		return null;
	}
	
	@Override
	public ArrayList<ViewableResult> getViewableResults(String searchInput, ArrayList<QueryResult> queryResults) {
		ArrayList<ViewableResult> vrs = new ArrayList<ViewableResult>(queryResults.size());
		for (QueryResult qr : queryResults) {
			try {
				vrs.add(new InstalledAppsViewableResult(searchInput, qr, decodeInMemoryRecord(qr.getInMemoryRecordData())));
			} catch (Exception e) {
				Pith.handleException(e);
				vrs.clear();
			}
		}
		return vrs;
	}
}
