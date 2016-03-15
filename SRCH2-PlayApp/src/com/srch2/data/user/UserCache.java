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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import android.content.Context;
import android.util.Log;

import com.srch2.Pith;
import com.srch2.data.ThreadPool.ThreadTaskType;
import com.srch2.data.connector.Connector;
import com.srch2.viewable.ViewableResultsAdapter;
import com.srch2.viewable.result.DefaultResourceCache;
import com.srch2.viewable.result.ViewableResult;


public class UserCache implements SearchResultSelectedObserver {

	private HashMap<String, ViewableResultStatistic> stats;
	
	private static UserCache userCacheInstance;
	
	
	private UserCache() {
		stats = new HashMap<String, ViewableResultStatistic>(100);
	}
	
	public static UserCache getUserCache() {
		if (userCacheInstance == null) {
			initializeInstance();
		}
		return userCacheInstance;
	}
	
	public static void initializeInstance() {
		if (userCacheInstance == null) {
			userCacheInstance = new UserCache();
		}
	}
	
	public synchronized void set(HashSet<ViewableResultStatistic> from) {
		try {
			for (ViewableResultStatistic vrss : from) {
				stats.put(vrss.userCacheId, vrss);
			}
		} catch (NullPointerException npe) {
			Pith.handleException(npe);
		}

	}

	@Override
	public synchronized void onSearchResultSelected(ViewableResult avr) {
		try {
			final String userCacheId = avr.getUserCacheId();
			if (stats.containsKey(userCacheId)) {
				++stats.get(userCacheId).hitCount;
			} else {
				stats.put(userCacheId, new ViewableResultStatistic(avr.getUserCacheId(), 1, avr.searchCategory, avr.toSerialString()));
			}
		} catch (NullPointerException npe) {
			Pith.handleException(npe);
		}
	}

	public synchronized void onSave(Context context) {
		if (stats != null && stats.size() > 0) {
		    Connector.doRunnableTask(new WriteTask(context, stats.values()), ThreadTaskType.Close);
		}
	}

	public static void onLoad(Context context, ViewableResultsAdapter vra) {
		Connector.doRunnableTask(new ReadClass(context, vra), ThreadTaskType.Open);
	}
}
