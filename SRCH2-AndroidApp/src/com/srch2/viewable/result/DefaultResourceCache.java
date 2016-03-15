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

import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.srch2.R;
import com.srch2.data.SearchCategory;
import com.srch2.data.connector.web.WebSearchType;
import com.srch2.instantsearch.QueryResult;

public class DefaultResourceCache {
	public static final String EXACT_MACHING_PREFIX = "<font color='red'><b>";
	public static final String EXACT_MACHING_SURFIX = "</b></font>";
	public static final String FUZZY_MACHING_PREFIX = "<font color='#ff00ff'><b>";
	public static final String FUZZY_MACHING_SURFIX = "</b></font>";
	
	private static HashMap<SearchCategory, Bitmap> defaultIcon = null;
	private static HashMap<SearchCategory, Integer> defaultIndicatorColor = null;
	private static HashMap<WebSearchType, Bitmap> webSearchResolveTypeIcon = null;
	private static HashMap<WebSearchType, String> webSearchResolveTypePrefix = null;
	private static HashMap<RowButtonType, Bitmap> rowButton = null;
	
	private static HashMap<Integer, Bitmap> randomActionRowButton = null;
	
	private static Bitmap srch2Icon = null;
	private static int srch2IndicatorColorCode = -12345;
	private static Bitmap transparentSquare = null;
	
	public static class InitializorTask implements Runnable {
		private WeakReference<Context> context;
		
		public InitializorTask(Context contxt) {
			context = new WeakReference<Context>(contxt);
		}
		
		public void run() {
			init(context.get());
		}
	};
	
	public static InitializorTask initialize(Context context) {
		return new InitializorTask(context);
	}
	
	private static void init(Context context) {
		long t = SystemClock.uptimeMillis();
		if (srch2IndicatorColorCode == -12345) {
			srch2IndicatorColorCode = context.getResources().getColor(R.color.row_indicator_default);
		}
		if (defaultIndicatorColor == null) {
			defaultIndicatorColor = new HashMap<SearchCategory, Integer>();
			defaultIndicatorColor.put(SearchCategory.Video, context.getResources().getColor(R.color.row_indicator_video));
			defaultIndicatorColor.put(SearchCategory.Images, context.getResources().getColor(R.color.row_indicator_images));
			defaultIndicatorColor.put(SearchCategory.Music, context.getResources().getColor(R.color.row_indicator_music));
			defaultIndicatorColor.put(SearchCategory.Sms, context.getResources().getColor(R.color.row_indicator_sms));
			defaultIndicatorColor.put(SearchCategory.InstalledApps, context.getResources().getColor(R.color.row_indicator_installed_apps));
			defaultIndicatorColor.put(SearchCategory.Calendar, context.getResources().getColor(R.color.row_indicator_calendar));
			defaultIndicatorColor.put(SearchCategory.Web, context.getResources().getColor(R.color.row_indicator_web));
			defaultIndicatorColor.put(SearchCategory.Contacts, context.getResources().getColor(R.color.row_indicator_contacts));
		} 
		if (srch2Icon == null) {
			srch2Icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.srch2_logo);
		}
		if (defaultIcon == null) {
			defaultIcon = new HashMap<SearchCategory, Bitmap>();
			defaultIcon.put(SearchCategory.Video, BitmapFactory.decodeResource(context.getResources(), R.drawable.row_icon_default_video_grey));
			defaultIcon.put(SearchCategory.Images, BitmapFactory.decodeResource(context.getResources(), R.drawable.row_icon_default_images_grey));
			defaultIcon.put(SearchCategory.Music, BitmapFactory.decodeResource(context.getResources(), R.drawable.row_icon_default_music_grey));
			defaultIcon.put(SearchCategory.Sms, BitmapFactory.decodeResource(context.getResources(), R.drawable.row_icon_default_sms_grey));
			defaultIcon.put(SearchCategory.InstalledApps, BitmapFactory.decodeResource(context.getResources(), R.drawable.row_icon_default_installed_apps_grey));
			defaultIcon.put(SearchCategory.Calendar, BitmapFactory.decodeResource(context.getResources(), R.drawable.row_icon_default_calendar_grey));
			defaultIcon.put(SearchCategory.Web, BitmapFactory.decodeResource(context.getResources(), R.drawable.row_icon_default_web_search_grey));
			defaultIcon.put(SearchCategory.Contacts, BitmapFactory.decodeResource(context.getResources(), R.drawable.row_icon_default_contacts_grey));
		}
		
		if (webSearchResolveTypeIcon == null) {
			webSearchResolveTypeIcon = new HashMap<WebSearchType, Bitmap>();
			webSearchResolveTypeIcon.put(WebSearchType.Google,  BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_search));
			webSearchResolveTypeIcon.put(WebSearchType.PlayStore,  BitmapFactory.decodeResource(context.getResources(), R.drawable.play_icon));
			webSearchResolveTypeIcon.put(WebSearchType.Wikipedia,  BitmapFactory.decodeResource(context.getResources(), R.drawable.row_icon_default_wikipedia_grey));
			webSearchResolveTypeIcon.put(WebSearchType.Suggestion,  BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_search));
		}
		
		if (webSearchResolveTypePrefix == null) {
			webSearchResolveTypePrefix = new HashMap<WebSearchType, String>();
			webSearchResolveTypePrefix.put(WebSearchType.Google,  "Search: ");
			webSearchResolveTypePrefix.put(WebSearchType.PlayStore,  "Play: ");
			webSearchResolveTypePrefix.put(WebSearchType.Wikipedia,  "Wikipedia: ");
			webSearchResolveTypePrefix.put(WebSearchType.Suggestion,  "Search: ");
		}
		if (rowButton == null) {
			rowButton = new HashMap<RowButtonType, Bitmap>(3);
			rowButton.put(RowButtonType.ContactActionCall, BitmapFactory.decodeResource(context.getResources(), R.drawable.action_call_icon));
			rowButton.put(RowButtonType.ContactActionMessage, BitmapFactory.decodeResource(context.getResources(), R.drawable.action_sms_icon));
			rowButton.put(RowButtonType.ContactActionEmail, BitmapFactory.decodeResource(context.getResources(), R.drawable.action_mail_icon));
		}
		if (transparentSquare == null) {
			transparentSquare = BitmapFactory.decodeResource(context.getResources(), R.drawable.transparent_square_pixel);
		}
		if (randomActionRowButton == null) {
			randomActionRowButton = new HashMap<Integer, Bitmap>(10);
			randomActionRowButton.put(0, BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_camera));
			randomActionRowButton.put(1, BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_call));
			randomActionRowButton.put(2, BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_agenda));
			randomActionRowButton.put(3, BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_add));
			randomActionRowButton.put(4, BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_delete));
			randomActionRowButton.put(5, BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_crop));
			randomActionRowButton.put(6, BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_send));
			randomActionRowButton.put(7, BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_sort_by_size));
			randomActionRowButton.put(8, BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_zoom));
			randomActionRowButton.put(9, BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_myplaces));
		}
		long e = SystemClock.uptimeMillis() - t;

	}
	
	public static Bitmap getDefaultIcon(SearchCategory forWhichCategory) {
		try {
			return defaultIcon.get(forWhichCategory);
		} catch (NullPointerException npe) {
			return srch2Icon;
		}
	}
	
	public static int getDefaultIndicatorColor(SearchCategory forWhichCategory) {
		try {
			return defaultIndicatorColor.get(forWhichCategory);
		} catch (NullPointerException npe) {
			return srch2IndicatorColorCode;
		}
	}
	
	public static Bitmap getWebSearchResolveTypeIcon(WebSearchType forWhichResolveType) {
		try {
			return webSearchResolveTypeIcon.get(forWhichResolveType);
		} catch (NullPointerException npe) {
			return srch2Icon;
		}
	}
	
	public static String getWebSearchResolveTypePrefix(WebSearchType forWhichResolveType) {
		try {
			return webSearchResolveTypePrefix.get(forWhichResolveType);
		} catch (NullPointerException npe) {
			return "Web: ";
		}
	}
	
	public static Bitmap getRowButtonActionIcon(RowButtonType forWhat) {
		try {
			return rowButton.get(forWhat);
		} catch (NullPointerException npe) {
			return transparentSquare;
		}
	}
	
	public static Bitmap getRandomRowButtonActionIcon(int forWhat) {
		try {
			return randomActionRowButton.get(forWhat);
		} catch (NullPointerException npe) {
			return srch2Icon;
		}
	}
	
	public static String getHighlightedText(String textToHighlight, QueryResult sourceQueryResult) {
		
		if (textToHighlight == null) {
			return null;
		}
		
		boolean success = true;
		String returnString = null;

		try {
			returnString = sourceQueryResult.highlight(textToHighlight, EXACT_MACHING_PREFIX, EXACT_MACHING_SURFIX, FUZZY_MACHING_PREFIX, FUZZY_MACHING_SURFIX);
		} catch (NullPointerException npe) {
			success = false;
		}
		
		if (success) {
			textToHighlight = returnString;
		} else {
			textToHighlight = null;
		}
		
		return textToHighlight;
	}
	
	public static String getHighlightedText(String textToHighlight, String highlightAgainst) {
		return null;
	}
	
	public final static String formatHtmlWithBold(String toBold) {
		return "<b>" + toBold + "</b>";
	}
	
	public static enum RowButtonType {
		ContactActionCall,
		ContactActionMessage,
		ContactActionEmail,
	}
	
	public static float convertPixelsToDp(Context context, float px){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return dp;
	}
	
	public static float convertDpToPixels(Context context,float dipValue) {
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}
}
