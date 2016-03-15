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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.Toast;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.connector.web.WebSearchType;

public class WebViewableResult extends ViewableResult {

	private String resolveTypeName;
	
	public WebViewableResult(String searchinput, WebSearchType resolvetype, String suggestionText) {
		super(searchinput, SearchCategory.Web, resolvetype, suggestionText);
		setMetaContent(suggestionText, resolvetype.name());
	}

	public WebViewableResult(String searchinput, String encodedMetaContent) {
		super(searchinput, SearchCategory.Web, WebSearchType.getFromString(encodedMetaContent.split(delim, 2)[1]), encodedMetaContent.split(delim, 2)[0]);
		setMetaContent(encodedMetaContent.split(delim, 2)[0], encodedMetaContent.split(delim, 2)[1]);
	}
	
	@Override
	protected String getEncodedMetaContentString() {
		return primaryLookupKey + delim + resolveTypeName;
	}

	@Override
	public void setMetaContent(String... args) {
		primaryLookupKey = args[0];
		resolveTypeName = args[1];
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
			Intent i = null;
			WebSearchType whichType = WebSearchType.getFromString(resolveTypeName);
			switch (whichType) {
				case Google:
					i = new Intent(Intent.ACTION_WEB_SEARCH);
					i.putExtra(SearchManager.QUERY, primaryLookupKey);
					context.startActivity(i);
					break;
				case PlayStore:
					final String appPackageName = primaryLookupKey; // getPackageName() from Context or Activity object
					try {
					    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + appPackageName)));
					} catch (android.content.ActivityNotFoundException anfe) {
						Pith.reportExceptionSilently(anfe);
						context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://market.android.com/search?q=" + appPackageName)));
					}
					break;	
				case Wikipedia:
					i = new Intent(Intent.ACTION_VIEW,  Uri.parse("http://en.wikipedia.org/wiki/" + primaryLookupKey));
					context.startActivity(i);
					break;	
				case Suggestion:
					i = new Intent(Intent.ACTION_WEB_SEARCH);
					i.putExtra(SearchManager.QUERY, primaryLookupKey);
					context.startActivity(i);
					break;
			} 
		} catch (Exception e) {
			Pith.reportExceptionSilently(e);
			success = false;
		}
		
		if (!success) {
			Toast.makeText(context, "Sorry, Android seems to be having trouble handling this request!", Toast.LENGTH_LONG).show();
		} 
	}
}
