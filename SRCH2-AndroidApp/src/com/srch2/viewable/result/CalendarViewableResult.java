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

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.widget.Toast;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.instantsearch.QueryResult;

public class CalendarViewableResult extends ViewableResult {

	public String beginTime, endTime;
	
	public CalendarViewableResult(String searchinput, QueryResult qr) {
		super(
				searchinput, SearchCategory.Calendar, 
				null, qr.getInMemoryRecordData().split(delim, 3)[2],
				null, 
				DefaultResourceCache.getHighlightedText(qr.getInMemoryRecordData().split(delim, 3)[2], qr), 
				DefaultResourceCache.getHighlightedText(null, qr)
			);
		
		setMetaContent(qr.getPrimaryKey(), qr.getInMemoryRecordData().split(delim, 3)[0], qr.getInMemoryRecordData().split(delim, 3)[1]);
	}
	
	public CalendarViewableResult(String searchinput, QueryResult qr, String[] inMemoryRecordData) {
		super(
				searchinput, SearchCategory.Calendar, 
				null, inMemoryRecordData[2],
				null, 
				DefaultResourceCache.getHighlightedText(inMemoryRecordData[2], qr), 
				DefaultResourceCache.getHighlightedText(null, qr)
			);
		
		setMetaContent(qr.getPrimaryKey(), inMemoryRecordData[0], inMemoryRecordData[1]);
	}
	
	
	public CalendarViewableResult(String searchinput, String iconkey, String title, String subtitle, String htitle, String hsubtitle, String encodedmetacontent) {
		super(searchinput, SearchCategory.Calendar, iconkey, title, subtitle, htitle, hsubtitle);
		
		String metaVals[] = encodedmetacontent.split(ViewableResult.delim, 3);
		setMetaContent(metaVals[0], metaVals[1], metaVals[2]);
	}

	@Override
	public void setMetaContent(String... args) {
		try {
			primaryLookupKey = args[0];
			beginTime = args[1];
			endTime = args[2];
		} catch (NullPointerException npe) {
			Pith.handleException(npe);
			primaryLookupKey = null;
			beginTime = null;
			endTime = null;
		}
	}

	@Override
	protected String getEncodedMetaContentString() {
		return primaryLookupKey + ViewableResult.delim + beginTime + ViewableResult.delim + endTime;
	}

	@Override
	public Bitmap getIcon(Context context) {
		return icon;
	}
	
	@Override
	public void onClick(Context context) {
		super.onClick(context);
		
		boolean success = true;
		try {
			
			if (rawTitle.equals(SearchCategory.getTitleIfInMemoryRecordDataCorrupted(searchCategory))) {
				Toast.makeText(context, "Don't panic! \n The robots are here for your benefit.", Toast.LENGTH_LONG).show();
				return;
			}
			
			// Two different function calls because on HTC phones, the else functionality
			// will cause phone to freeze, not crashing the app, but freezing as unresponsive
			// for ~5 seconds. Ideal is else: jumps straight to event; if is HTC, open the calendar
			// specific to the date, which means a user must click on that date and event to get to
			// same point
			if (android.os.Build.MANUFACTURER.equals("HTC")) {
				// REMEMBER TO FILE AS A BUG 
				Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
				builder.appendPath("time");
				ContentUris.appendId(builder, Long.valueOf(beginTime));
				Intent intent = new Intent(Intent.ACTION_VIEW)
				    .setData(builder.build());
				context.startActivity(intent);
			} else {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.putExtra("beginTime", beginTime);
				i.putExtra("endTime", endTime);
				
				Uri.Builder uri = Events.CONTENT_URI.buildUpon();
				uri.appendPath(primaryLookupKey);
				i.setData(uri.build());
				context.startActivity(i);

			}
			
			 /*
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
			        | Intent.FLAG_ACTIVITY_SINGLE_TOP
			        | Intent.FLAG_ACTIVITY_CLEAR_TOP
			        | Intent.FLAG_ACTIVITY_NO_HISTORY
			        | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			context.startActivity(intent);
			*/

		} catch (Exception e) {
			Pith.reportExceptionSilently(e);
			success = false;
		}
		
		if (!success) {
			Toast.makeText(context, "Sorry, Android seems to be having trouble handling this request!", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o.getClass() == CalendarViewableResult.class) {
			CalendarViewableResult cvr = (CalendarViewableResult) o;
			if (super.equals(cvr)) {
				if (cvr.primaryLookupKey.equals(primaryLookupKey)) {
					if (cvr.beginTime.equals(beginTime)) {
						if (cvr.endTime.equals(endTime)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
