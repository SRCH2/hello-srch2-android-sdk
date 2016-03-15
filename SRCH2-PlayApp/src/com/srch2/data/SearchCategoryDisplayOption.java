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
package com.srch2.data;

import com.srch2.R;

public final class SearchCategoryDisplayOption {
	public SearchCategory category;
	public boolean isDisplayed;
	
	public int enabledIconResourceId;
	public int disableIconResourceId;
	public int enabledBorderlessResourceId;
	public int disabledBorderlessResourceId;
	

	private SearchCategoryDisplayOption() { }
	
	public SearchCategoryDisplayOption(SearchCategory whichSearchCategory, boolean isDisplayd) {
		category = whichSearchCategory;
		isDisplayed = isDisplayd;
		
		switch (category) {
			case Web:
				enabledIconResourceId = R.drawable.menu_icon_web_search_red;
				disableIconResourceId = R.drawable.menu_icon_web_search_grey;
				enabledBorderlessResourceId = R.drawable.menu_icon_borderless_web_search_red;
				disabledBorderlessResourceId = R.drawable.menu_icon_borderless_web_search_grey;
				break;
			case Sms:
				enabledIconResourceId = R.drawable.menu_icon_sms_red;
				disableIconResourceId = R.drawable.menu_icon_sms_grey;
				enabledBorderlessResourceId = R.drawable.menu_icon_borderless_sms_red;
				disabledBorderlessResourceId = R.drawable.menu_icon_borderless_sms_grey;
				break;	
			case Images:
				enabledIconResourceId = R.drawable.menu_icon_images_red;
				disableIconResourceId = R.drawable.menu_icon_images_grey;
				enabledBorderlessResourceId = R.drawable.menu_icon_borderless_images_red;
				disabledBorderlessResourceId = R.drawable.menu_icon_borderless_images_grey;
				break;	
			case Music:
				enabledIconResourceId = R.drawable.menu_icon_music_red;
				disableIconResourceId = R.drawable.menu_icon_music_grey;
				enabledBorderlessResourceId = R.drawable.menu_icon_borderless_music_red;
				disabledBorderlessResourceId = R.drawable.menu_icon_borderless_music_grey;
				break;	
			case Video:
				enabledIconResourceId = R.drawable.menu_icon_video_red;
				disableIconResourceId = R.drawable.menu_icon_video_grey;
				enabledBorderlessResourceId = R.drawable.menu_icon_borderless_video_red;
				disabledBorderlessResourceId = R.drawable.menu_icon_borderless_video_grey;
				break;	
			case InstalledApps:
				enabledIconResourceId = R.drawable.menu_icon_installed_apps_red;
				disableIconResourceId = R.drawable.menu_icon_installed_apps_grey;
				enabledBorderlessResourceId = R.drawable.menu_icon_borderless_installed_apps_red;
				disabledBorderlessResourceId = R.drawable.menu_icon_borderless_installed_apps_grey;
				break;					
			case Calendar:
				enabledIconResourceId = R.drawable.menu_icon_calendar_red;
				disableIconResourceId = R.drawable.menu_icon_calendar_grey;
				enabledBorderlessResourceId = R.drawable.menu_icon_borderless_calendar_red;
				disabledBorderlessResourceId = R.drawable.menu_icon_borderless_calendar_grey;
				break;	
			case Contacts:
				enabledIconResourceId = R.drawable.menu_icon_contacts_red;
				disableIconResourceId = R.drawable.menu_icon_contacts_grey;
				enabledBorderlessResourceId = R.drawable.menu_icon_borderless_contacts_red;
				disabledBorderlessResourceId = R.drawable.menu_icon_borderless_contacts_grey;
				break;			
			default:				
				enabledIconResourceId = R.drawable.srch2_logo;
				disableIconResourceId = R.drawable.srch2_logo;
				break;
		}
	}
	
	public static SearchCategoryDisplayOption getSaveStateSearchCategoryDisplayOption(SearchCategory which, boolean isDisplayd) {
		SearchCategoryDisplayOption scdo = new SearchCategoryDisplayOption();
		scdo.category = which;
		scdo.isDisplayed = isDisplayd;
		return scdo;
	}
}
