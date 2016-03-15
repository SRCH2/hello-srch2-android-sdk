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
package com.srch2.data.user;

import java.util.ArrayList;

import com.srch2.data.SearchCategory;
import com.srch2.data.SearchCategoryDisplayOption;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferences {
	private final static String delimiter = "~";
	private final static String sharedPreferencesKey = "SEARCH_PREFERENCES_SAVE_STATE_STRING";

	public void saveUserPreferences(Context context, ArrayList<SearchCategoryDisplayOption> sortableAdapterSetToSave) {
		ArrayList<SearchCategoryDisplayOption> prefsToSave = new ArrayList<SearchCategoryDisplayOption>();
		for (int i = 0; i < sortableAdapterSetToSave.size(); i++) {
			prefsToSave.add(new SearchCategoryDisplayOption(sortableAdapterSetToSave.get(i).category,
					sortableAdapterSetToSave.get(i).isDisplayed));
		}
	   	SharedPreferences prefs = ((Activity) context).getPreferences(Context.MODE_PRIVATE);
	   	SharedPreferences.Editor editor = prefs.edit();
	   	editor.putString(sharedPreferencesKey, 	encodeSaveStateString(prefsToSave));
	   	editor.commit();
	}
	
	private String encodeSaveStateString(ArrayList<SearchCategoryDisplayOption> prefs) {
	   	StringBuilder sb = new StringBuilder();
	   	for (int i = 0; i < prefs.size(); i++) {
	   		if (prefs.get(i).isDisplayed) {
	   			sb.append("T");
	   		} else {
	   			sb.append("F");
	   		}
	   		sb.append(prefs.get(i).category + delimiter);
	   	}
	   	return sb.toString();
	}
	
	public ArrayList<SearchCategoryDisplayOption> readUserPreferences(Context context) {
	   	SharedPreferences prefs = ((Activity) context).getPreferences(Context.MODE_PRIVATE);
		String saveStateString = prefs.getString(sharedPreferencesKey, "empty");
		if (!saveStateString.equals("empty")) {
			ArrayList<SearchCategoryDisplayOption> userPrefs = new ArrayList<SearchCategoryDisplayOption>();
			String[] indexOptions = saveStateString.split(delimiter);
			for (int i = 0; i < indexOptions.length; i++) {
				boolean isEnabled = (indexOptions[i].charAt(0) == 'T') ? true : false;
				String key = indexOptions[i].substring(1, indexOptions[i].length());
				SearchCategory ic = SearchCategory.valueOf(key);
				userPrefs.add(new SearchCategoryDisplayOption(ic, isEnabled));
			}
			return userPrefs;
		} else {
			return getDefaultUserPreferences();
		}
	}
	
	public ArrayList<SearchCategoryDisplayOption> getDefaultUserPreferences() {
		ArrayList<SearchCategoryDisplayOption> defaults = new ArrayList<SearchCategoryDisplayOption>();	
		SearchCategory[] defaultValues = SearchCategory.values();
		for (int i = 0; i < defaultValues.length; i++) {
			SearchCategoryDisplayOption iOption = new SearchCategoryDisplayOption(defaultValues[i], true);	
			defaults.add(iOption);
		}
		return defaults;
	}
}
