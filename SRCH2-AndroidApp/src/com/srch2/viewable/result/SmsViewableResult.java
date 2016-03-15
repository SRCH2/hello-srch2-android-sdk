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

import java.io.InputStream;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.widget.Toast;
import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.instantsearch.QueryResult;

public class SmsViewableResult extends ViewableResult {
	public SmsViewableResult(String searchinput, QueryResult qr) {
		super(searchinput, SearchCategory.Sms, qr.getInMemoryRecordData().split(delim, 3)[0], qr.getInMemoryRecordData().split(delim, 3)[2], null, 
				DefaultResourceCache.getHighlightedText(qr.getInMemoryRecordData().split(delim, 3)[2], qr), DefaultResourceCache.getHighlightedText(null, qr));
		setMetaContent(qr.getInMemoryRecordData().split(delim, 3)[1]);
	}
	
	public SmsViewableResult(String searchinput, String iconkey, String title, String subtitle, String htitle, String hsubtitle, String encodedmetacontent) {
		super(searchinput, SearchCategory.Sms, iconkey, title, subtitle, htitle, hsubtitle);
		setMetaContent(encodedmetacontent);
	}
	
	public SmsViewableResult(String searchinput, QueryResult qr, String[] inMemoryRecordData) {
		super(searchinput, SearchCategory.Sms, inMemoryRecordData[0], inMemoryRecordData[2], null, 
				DefaultResourceCache.getHighlightedText(inMemoryRecordData[2], qr), DefaultResourceCache.getHighlightedText(null, qr));
		setMetaContent(inMemoryRecordData[1]);
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
		Uri uri = null;
		Uri res = null;
		InputStream input = null;
		try {
			uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(iconKey));
			res = ContactsContract.Contacts.lookupContact(context.getContentResolver(), uri);
			/* ContactsContract.Contacts.openContactPhotoInputStream non-deterministically time consuming */
			if (res != null) {
				input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), res);
				b = BitmapFactory.decodeStream(input);
			}
		} catch (Exception e) {
			b = icon;
		}
		return b;
	}

	@SuppressLint("NewApi") @Override
	public void onClick(Context context) {
		super.onClick(context);
		
		boolean success = true;
		if (primaryLookupKey.equals(INVALID_KEY_CODE)) {
			success = false;
		} 

		Intent intent = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			/* OPENING DEFAULT SMS TO GIVEN SMS THREAD ID no longer supported! 
			 * Must create a dialog and pass sender/body/time/etc into it to view on click
			 * do with new SDK, for now do nothing.
			try {
			
				intent = new Intent(Intent.ACTION_SENDTO);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.setData(Uri.parse("smsto:" + Uri.encode(primaryLookupKey)));
				context.startActivity(intent); 
			} catch (Exception e) {
				Pith.reportExceptionSilently(e);
				success = false;
			} */
		} else {
			try {

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.withAppendedPath(Telephony.Sms.Conversations.CONTENT_URI, primaryLookupKey)));
				} else {
					Intent smsIntent = new Intent(Intent.ACTION_VIEW);
					smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					smsIntent.setData(Uri.parse("content://mms-sms/conversations/" + primaryLookupKey));
					context.startActivity(smsIntent);
				}
				
			} catch (Exception e) {
				// do not send silent crash report-- should not occur on most devices. 
				success = false;
			}
		}
		

		
		if (!success) {
			Toast.makeText(context, "Sorry, Android seems to be having trouble handling this request!", Toast.LENGTH_LONG).show();
		} 
	}
	
	@Override
	public boolean equals(Object o) {
		if (o.getClass() == SmsViewableResult.class) {
			SmsViewableResult cvr = (SmsViewableResult) o;
			if (super.equals(cvr)) {
				if (cvr.primaryLookupKey.equals(primaryLookupKey)) {
					if (cvr.rawTitle.equals(rawTitle)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
