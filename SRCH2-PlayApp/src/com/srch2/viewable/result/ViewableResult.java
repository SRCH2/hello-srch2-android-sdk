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

import java.util.HashMap;
import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

import com.srch2.R;
import com.srch2.data.SearchCategory;
import com.srch2.data.connector.web.WebSearchType;
import com.srch2.data.user.SearchResultSelectedObserver;
import com.srch2.data.user.UserCache;

public abstract class ViewableResult {

	public static final String INVALID_KEY_CODE = "NULL@KEY";
	
	public static final String INVALID_DISPLAY_TEXT = "NULL@TITLE";
	
	public final static String delim = String.valueOf(((char) 007));

	
	protected abstract String getEncodedMetaContentString();
	public abstract void setMetaContent(String... args);
	
	public String getUserCacheId() { return searchCategory + delim + primaryLookupKey; }
	public String getIconCacheKey() { return searchCategory + delim + iconKey; }
	
	public boolean hasDefaultIcon() { return iconKey == null; }
	public boolean hasHighlightableText() { return !highlightedTitle.equals(INVALID_DISPLAY_TEXT); }
	public boolean hasRowButtons() { return rowButtonMap != null; }
	
	public void onClick(Context context) { onSearchResultSelectedObserver.onSearchResultSelected(this); }
	public void onSwipeLeft(Context context) { onSearchResultSelectedObserver.onSearchResultSelected(this); };
	public void onSwipeRight(Context context) { onSearchResultSelectedObserver.onSearchResultSelected(this); };
	public void onRowButtonClick(Context context, int whichRowButtonId) { onSearchResultSelectedObserver.onSearchResultSelected(this); };
	public SparseArray<Bitmap> rowButtonMap = null;
	
	public abstract Bitmap getIcon(Context context);
	
	public boolean isSwippable = false;

	public String searchInput;
	
	public SearchCategory searchCategory;
	
	public String primaryLookupKey;

	public int indicatorColor;

	public String iconKey;
	public Bitmap icon;
	
	public String rawTitle;
	public String highlightedTitle;
	public String rawSubtitle;
	public String highlightedSubtitle;
	
	private SearchResultSelectedObserver onSearchResultSelectedObserver = (SearchResultSelectedObserver) UserCache.getUserCache();
	
	public ViewableResult(
			String searchinput, SearchCategory searchcategory, String iconkey, 
			String rawtitle, String rawsubtitle, String htitle, String hsubtitle) {
		
		searchInput = searchinput == null ? "" : searchinput;
		
		searchCategory = searchcategory;
		
		indicatorColor = DefaultResourceCache.getDefaultIndicatorColor(searchcategory);

		icon = DefaultResourceCache.getDefaultIcon(searchcategory);
		iconKey = iconkey == null ? null : iconkey;

		if (rawtitle != null && rawtitle.length() < 1) {
			rawtitle = null;
		} 
		
		if (htitle != null && htitle.length() < 1) {
			htitle = null;
		}
		

		rawTitle = rawtitle == null ? SearchCategory.getTitleIfInMemoryRecordDataCorrupted(searchcategory) : rawtitle;

		highlightedTitle = htitle == null ? INVALID_DISPLAY_TEXT : htitle;
		rawSubtitle = rawsubtitle == null ? INVALID_DISPLAY_TEXT : rawsubtitle;
		highlightedSubtitle = hsubtitle == null ? INVALID_DISPLAY_TEXT : hsubtitle;
	}
	
	
	
	public ViewableResult(String searchinput, SearchCategory searchcategory, WebSearchType resolveType, String webSuggestionText) {
		if (searchcategory != SearchCategory.Web) {
			return;
		}
		
		iconKey = null;
		searchInput = searchinput == null ? "" : searchinput;
		
		searchCategory = searchcategory;
		
		indicatorColor = DefaultResourceCache.getDefaultIndicatorColor(searchcategory);
		icon = DefaultResourceCache.getWebSearchResolveTypeIcon(resolveType);
		rawTitle = DefaultResourceCache.getWebSearchResolveTypePrefix(resolveType) + webSuggestionText;
		highlightedTitle = DefaultResourceCache.getWebSearchResolveTypePrefix(resolveType) + DefaultResourceCache.formatHtmlWithBold(webSuggestionText);
		rawSubtitle = highlightedSubtitle = INVALID_DISPLAY_TEXT;
	}
	
	public ViewableResult(SearchCategory scategory, String title) {
		searchCategory = scategory;
		indicatorColor = DefaultResourceCache.getDefaultIndicatorColor(scategory);
		icon = DefaultResourceCache.getDefaultIcon(scategory);
		
		rawTitle = title;
		
		int[] ids = new int[] { R.id.iv_row_special_button_1, R.id.iv_row_special_button_2, R.id.iv_row_special_button_3 };

		if ((int) (Math.random() * 2) == 1) {
			int count = (int) (Math.random() * 3) + 1;
			if (count > 1) {
				isSwippable = true;
			}
			rowButtonMap = new SparseArray<Bitmap>();
			for (int i = 0; i < count; i++) {
				rowButtonMap.put(ids[i], DefaultResourceCache.getRandomRowButtonActionIcon((int) (Math.random() * 10)));
			}
		}
	}
	
	/*
	public String toSerialString() {
		return  
				getEncodedMetaContentString() + delim + 	// 0 METACONTENT
				searchInput + delim + 						// 1 SEARCH INPUT
				searchCategory + delim + 					// 2 SEARCH CATEGORY
				iconKey + delim + 							// 3 ICON LOOKUP KEY
				rawTitle + delim + 							// 4 RAW TITLE (no highlight)
				rawSubtitle + delim + 						// 5 RAW SUBTITLE (no highlight)
				highlightedTitle + delim + 					// 6
				highlightedSubtitle + delim;				// 7
	} */
	
	public String toSerialString() {
		return
				iconKey + delim + 							
				rawTitle + delim + 						
				rawSubtitle + delim + 
				getEncodedMetaContentString();

	}
	
	public static ViewableResult fromSerialString(SearchCategory forWhichCategory, String serialString) {
		
		String concatData[] = serialString.split(delim, 4);
		

		
		String metacontent = concatData[3];
		String iconkey = concatData[0];
		String rawtitle = concatData[1];
		String rawsubtitle = concatData[2];

		switch (forWhichCategory) {
			case Calendar:
				return new CalendarViewableResult(null, iconkey, rawtitle, rawsubtitle, null, null, metacontent);
			case Contacts:
				return new ContactsViewableResult(null, iconkey, rawtitle, rawsubtitle, null, null, metacontent);				
			case Sms:
				return new SmsViewableResult(null, iconkey, rawtitle, rawsubtitle, null, null, metacontent);
			case Music:
				return new MusicViewableResult(null, iconkey, rawtitle, rawsubtitle, null, null, metacontent);
			case Video:
				return new VideoViewableResult(null, iconkey, rawtitle, rawsubtitle, null, null, metacontent);
			case Images:
				return new ImagesViewableResult(null, iconkey, rawtitle, rawsubtitle, null, null, metacontent);
			case InstalledApps:
				return new InstalledAppsViewableResult(null, iconkey, rawtitle, rawsubtitle, null, null, metacontent);
			case Web:
				return new WebViewableResult(null, metacontent);
			default:
				return null;
		}
	}
/*
	public static ViewableResult fromSerialString(String serialString) {
		String concatData[] = serialString.split(delim, 4);
		ViewableResult vr = null;
		
		String metacontent = concatData[0];
		String sinput = null; // concatData[1];
		String iconkey = concatData[3];
		String rawtitle = concatData[4];
		String rawsubtitle = concatData[5];
		String htitle = null; //concatData[6];
		String hsubstitle = null; //concatData[7];
		
		switch (SearchCategory.getFromString(concatData[2])) {
			case Calendar:
				vr = new CalendarViewableResult(sinput, iconkey, rawtitle, rawsubtitle, htitle, hsubstitle, metacontent);
				break;
			case Contacts:
				vr = new ContactsViewableResult(sinput, iconkey, rawtitle, rawsubtitle, htitle, hsubstitle, metacontent);				
				break;
			case Sms:
				vr = new SmsViewableResult(sinput, iconkey, rawtitle, rawsubtitle, htitle, hsubstitle, metacontent);
				break;
			case Music:
				vr = new MusicViewableResult(sinput, iconkey, rawtitle, rawsubtitle, htitle, hsubstitle, metacontent);
				break;
			case Video:
				vr = new VideoViewableResult(sinput, iconkey, rawtitle, rawsubtitle, htitle, hsubstitle, metacontent);
				break;
			case Images:
				vr = new ImagesViewableResult(sinput, iconkey, rawtitle, rawsubtitle, htitle, hsubstitle, metacontent);
				break;
			case InstalledApps:
				vr = new InstalledAppsViewableResult(sinput, iconkey, rawtitle, rawsubtitle, htitle, hsubstitle, metacontent);
				break;
			case Web:
				vr = new WebViewableResult(sinput, metacontent);
				break;
		}
		return vr;
	}
*/
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("\n______________Viewable Result for " + searchCategory);
		
		sb.append("\n");
		if (searchInput == null) {
			sb.append("search input [NULL]");
		} else {
			sb.append("search input [" + searchInput + "]");
		}
		
		sb.append("\n");
		if (rawTitle == null) {
			sb.append("rawTitle input [NULL]");
		} else {
			sb.append("rawTitle input [" + rawTitle + "]");
		}
		
		sb.append("\n");
		if (primaryLookupKey == null) {
			sb.append("primaryLookupKey input [NULL]");
		} else {
			sb.append("primaryLookupKey input [" + primaryLookupKey + "]");
		}
		
		sb.append("\n");
		if (iconKey == null) {
			sb.append("iconKey input [NULL]");
		} else {
			sb.append("iconKey input [" + iconKey + "]");
		}
		
		sb.append("\n");
		sb.append("literal serial: " + toSerialString());
		sb.append("\n_____________________________________________________________");
	
		return sb.toString();
	}
}
