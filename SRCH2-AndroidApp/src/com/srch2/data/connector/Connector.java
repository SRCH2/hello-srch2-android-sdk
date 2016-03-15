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
package com.srch2.data.connector;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.ThreadPool;
import com.srch2.data.ThreadPool.ThreadTaskType;
import com.srch2.viewable.ViewableResultsPackager;
import com.srch2.viewable.ViewableResultsAvailableObserver;
import com.srch2.viewable.ViewableResultsBuffer;

public abstract class Connector implements ConnectorLifeCycle, ViewableResultsPackager {
	

	private SearchCategory category;
	public SearchCategory getCategory() { return category; }
	
	private static HashMap<SearchCategory, AtomicBoolean> connectorLockAccessMap = new HashMap<SearchCategory, AtomicBoolean>();
	protected static AtomicBoolean getAccessLock(SearchCategory forWhich) { return connectorLockAccessMap.get(forWhich); }
	protected static void addAndSetAccessLock(SearchCategory which, AtomicBoolean lock) { connectorLockAccessMap.put(which, lock); while (connectorLockAccessMap.get(which).getAndSet(true)) { connectorLockAccessMap.get(which).set(false); }; }
	protected static boolean checkIsLockAccessable(SearchCategory which) { return connectorLockAccessMap.get(which).get();	}
	
	private static ThreadPool threadPool = new ThreadPool();
	protected static void wakeUpThreadPool() { threadPool.wakeUp(); }
	protected static void haltSearchTasks() { threadPool.onPause(); }
	protected static void shutDownThreadPool() { threadPool.onDestroy(); }
	private static GooeyHandler gooeyHandler = new GooeyHandler();
	
	public static void doDelayedRunnableUpdateTask(Runnable r, int delay) {
		gooeyHandler.sendMessageDelayed(Message.obtain(gooeyHandler, HANDLER_POST_DELAYED_UPDATE, r), delay);
	}
	public static void doRunnableTask(Runnable r, ThreadTaskType taskType) { threadPool.doRunnableTask(r, taskType); }

	public static ViewableResultsAvailableObserver vrao;
	
	
	protected abstract void processSearch(String searchInput);
	
	public Connector(SearchCategory which) {
		category = which;
	}
	
	protected Connector() { }
	
	protected static boolean isIndexedConnector(SearchCategory whichSearchCategory) {
		return whichSearchCategory != SearchCategory.Web;
	}
	
	protected static final int HANDLER_POST_DELAYED_UPDATE = 10;
	
	private static class GooeyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case HANDLER_POST_DELAYED_UPDATE:
					Runnable updateRunnable;
					try {
						updateRunnable = (Runnable) msg.obj;
					} catch (ClassCastException cce) {
						updateRunnable = null;
						Pith.handleException(cce);
					}
					if (updateRunnable != null) {
						Connector.doRunnableTask(updateRunnable, ThreadTaskType.Update);
					}
					break;
			}
		}
		
	}
}
