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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum SearchCategory {
	Contacts,
	Sms,
	Calendar,
	InstalledApps,
	Music,
	Video,
	Images,
	Web;
	
	/* Initial target ordering should be: (as of 2/19)
	Contacts,
	Sms,
	Calendar,
	InstalledApps,
	Music,
	Video,
	Images,
	Web;
	 */
	
	private final static Map<String, SearchCategory> map = new HashMap<String, SearchCategory>(SearchCategory.values().length, 1.0f);
	
	static {
		for (SearchCategory e : SearchCategory.values()) {
			map.put(e.name(), e);
		}
	}
	
	public static SearchCategory getFromString(String which) {
		return map.get(which);
	}
	
	public static String[] toNamesArray() {
		String[] names = new String[values().length];
		int index = 0;
		for (SearchCategory category : values()) {
			names[index++] = category.name();
		}
		return names;
	}
	
	public static SearchCategory getRandom() {
		return getFromString(toNamesArray()[(int) (Math.random() * values().length)]);
	}
	
	
	private static final HashMap<SearchCategory, String> inMemoryRecordIfCorruptedTitles = new HashMap<SearchCategory, String>(7);
	static { 
		inMemoryRecordIfCorruptedTitles.put(Contacts, "Stu Bytes");
		inMemoryRecordIfCorruptedTitles.put(Calendar, "The Singularity");
		inMemoryRecordIfCorruptedTitles.put(Sms, "check out The Art of Writing Letters..");
		inMemoryRecordIfCorruptedTitles.put(InstalledApps, "Appception");
		inMemoryRecordIfCorruptedTitles.put(Images, "Digital still life");
		inMemoryRecordIfCorruptedTitles.put(Video, "Reality App Life");
		inMemoryRecordIfCorruptedTitles.put(Music, "The sound of silence");
		inMemoryRecordIfCorruptedTitles.put(Web, "Click here to view this web site!");
	}
	
	public static String getTitleIfInMemoryRecordDataCorrupted(SearchCategory whichSearchCategory) {
		return inMemoryRecordIfCorruptedTitles.get(whichSearchCategory);
	}
}
