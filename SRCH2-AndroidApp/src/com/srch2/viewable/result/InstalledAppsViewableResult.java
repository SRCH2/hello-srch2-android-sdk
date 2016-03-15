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
package com.srch2.viewable.result;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Toast;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.instantsearch.QueryResult;

public class InstalledAppsViewableResult extends ViewableResult {
	public InstalledAppsViewableResult(String searchinput, QueryResult qr) {
		super(searchinput, SearchCategory.InstalledApps, qr.getPrimaryKey(), qr.getInMemoryRecordData(), null, 
				DefaultResourceCache.getHighlightedText(qr.getInMemoryRecordData(), qr), DefaultResourceCache.getHighlightedText(null, qr));
		setMetaContent(qr.getPrimaryKey());
	}
	
	public InstalledAppsViewableResult(String searchinput, String iconkey, String title, String subtitle, String htitle, String hsubtitle, String encodedmetacontent) {
		super(searchinput, SearchCategory.InstalledApps, iconkey, title, subtitle, htitle, hsubtitle);
		setMetaContent(encodedmetacontent);
	}
	
	public InstalledAppsViewableResult(String searchinput, QueryResult qr, String[] inMemoryRecordData) {
		super(searchinput, SearchCategory.InstalledApps, qr.getPrimaryKey(), inMemoryRecordData[0], null, 
				DefaultResourceCache.getHighlightedText(inMemoryRecordData[0], qr), DefaultResourceCache.getHighlightedText(null, qr));
		setMetaContent(qr.getPrimaryKey());
	}
	
	
	@Override
	public void setMetaContent(String... args) {
		try {
			primaryLookupKey = args[0];
		} catch (NullPointerException npe) {
			primaryLookupKey = null;
		}
	}
	
	@Override
	protected String getEncodedMetaContentString() {
		return primaryLookupKey;
	}

	@Override
	public Bitmap getIcon(Context context) {
		Bitmap b = null;
		try {
			Drawable icon = context.getPackageManager().getApplicationIcon(iconKey);	
			b = ((BitmapDrawable) icon).getBitmap();
		} catch (Exception e) {
			b = icon;
		}
		return b;
	}
	
	@Override
	public void onClick(Context context) {
		super.onClick(context);
		
		boolean success = true;
		
		if (primaryLookupKey.equals("com.srch2")) {
			makeSrch2Toast(context);
			return;
		}
		try {
			Intent i = context.getPackageManager().getLaunchIntentForPackage(primaryLookupKey);
			if (i != null) {
				i.addCategory(Intent.CATEGORY_LAUNCHER);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);
			} else {
			    i = new Intent();
			    i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			    i.setData(Uri.parse("package:" + primaryLookupKey));
			    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			}	
		} catch (Exception e) {
			Pith.reportExceptionSilently(e);
			success = false;
		}
		
		if (!success) {
			Toast.makeText(context, "Sorry, Android seems to be having trouble handling this request!", Toast.LENGTH_LONG).show();
		}
	}
	
	private static final String[] phrases = new String[] { "SRCH2 it through the grapevine", "Stop me if you think you've srch2'd this one before!", "Give 'em the srch2 degree!", "close encounters of the srch2 kind", "Come on, you can do better than this!" };
	private static void makeSrch2Toast(Context context) {
		// randomly displays a srch2 toast randomly
		if((Math.sin((double)SystemClock.uptimeMillis()))>0.6)Toast.makeText(context,phrases[(int)((Math.abs(Math.sin((double)((SystemClock.uptimeMillis()+Math.random()*1000000))))*phrases.length))],Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o.getClass() == InstalledAppsViewableResult.class) {
			InstalledAppsViewableResult cvr = (InstalledAppsViewableResult) o;
			if (super.equals(cvr)) {
				if (cvr.primaryLookupKey.equals(primaryLookupKey)) {
					return true;
				}
			}
		}
		return false;
	}
}
