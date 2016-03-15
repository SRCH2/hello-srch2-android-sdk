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

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.instantsearch.QueryResult;

public class VideoViewableResult extends ViewableResult {

	public VideoViewableResult(String searchinput, QueryResult qr) {
		super(searchinput, SearchCategory.Video, qr.getPrimaryKey(), qr.getInMemoryRecordData(), null, 
				DefaultResourceCache.getHighlightedText(qr.getInMemoryRecordData(), qr), DefaultResourceCache.getHighlightedText(null, qr));
		setMetaContent(qr.getPrimaryKey());
	}
	
	public VideoViewableResult(String searchinput, String iconkey, String title, String subtitle, String htitle, String hsubtitle, String encodedmetacontent) {
		super(searchinput, SearchCategory.Video, iconkey, title, subtitle, htitle, hsubtitle);
		setMetaContent(encodedmetacontent);
	}

	public VideoViewableResult(String searchinput, QueryResult qr, String[] inMemoryRecordData) {
		super(searchinput, SearchCategory.Video, qr.getPrimaryKey(), inMemoryRecordData[0], null, 
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
			b = MediaStore.Video.Thumbnails.getThumbnail(
												context.getContentResolver(), 
												Long.valueOf(iconKey),
												MediaStore.Video.Thumbnails.MINI_KIND,
												null );
		} catch (Exception e) {
			b = icon;
		}
		return b;	
	}

	@Override
	public void onClick(Context context) {
		super.onClick(context);
		
		Cursor c = null;
		String path = null;
		boolean success = true;
		try {
			c = context.getContentResolver().query(
					MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
					new String[] { MediaStore.Video.Media.DATA }, 
					MediaStore.Video.Media._ID + " = ?",
					new String[] { primaryLookupKey }, 
					null);
			if (c.moveToFirst()) {
				path = c.getString(c.getColumnIndex(MediaStore.Video.Media.DATA));
			}
		} catch (Exception e) {
			success = false;
		} finally {
			c.close();
		}

		success = (path != null);
		
		if (success) {
			try {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setDataAndType(Uri.fromFile(new File(path)), "video/*");
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				context.startActivity(i);
			} catch (Exception e) {
				Pith.reportExceptionSilently(e);
				success = false;
			}
		} 
		
		if (!success) { 
			Toast.makeText(context, "Sorry, Android seems to be having trouble handling this request!", Toast.LENGTH_LONG).show();
		}

	}
	
	@Override
	public boolean equals(Object o) {
		if (o.getClass() == VideoViewableResult.class) {
			VideoViewableResult cvr = (VideoViewableResult) o;
			if (super.equals(cvr)) {
				if (cvr.primaryLookupKey.equals(primaryLookupKey)) {
					return true;
				}
			}
		}
		return false;
	}
}

