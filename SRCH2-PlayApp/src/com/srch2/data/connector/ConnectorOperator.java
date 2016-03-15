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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.content.Context;
import android.widget.ListView;

import com.srch2.ActivityLifeCycle;
import com.srch2.DraggableItemGridView;
import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.SearchCategoryDisplayOption;
import com.srch2.data.ThreadPool.ThreadTaskType;
import com.srch2.data.connector.indexed.DisposeTask;
import com.srch2.data.connector.indexed.IndexedConnector;
import com.srch2.data.connector.indexed.OpenTask;
import com.srch2.data.connector.indexed.indices.Calendar;
import com.srch2.data.connector.indexed.indices.Contacts;
import com.srch2.data.connector.indexed.indices.Images;
import com.srch2.data.connector.indexed.indices.InstalledApps;
import com.srch2.data.connector.indexed.indices.Music;
import com.srch2.data.connector.indexed.indices.Sms;
import com.srch2.data.connector.indexed.indices.Video;
import com.srch2.data.connector.web.WebConnector;
import com.srch2.data.incremental.IncrementalDatabase;
import com.srch2.data.user.UserCache;
import com.srch2.data.user.UserPreferences;
import com.srch2.viewable.ViewableResultsAdapter;
import com.srch2.viewable.ViewableResultsAvailableObserver;
import com.srch2.viewable.ViewableResultsBuffer;
import com.srch2.viewable.result.DefaultResourceCache;

public class ConnectorOperator implements ActivityLifeCycle {
	
	protected Context context;
	protected HashMap<SearchCategory, Connector> connectors;
	
	ViewableResultsBuffer viewableResultsBuffer;
	ViewableResultsAdapter vra;
	
	
	
	public ConnectorOperator(Context contxt, ListView ll, DraggableItemGridView draggableItemGridViewMenu) {
		context = contxt;
		

		vra = new ViewableResultsAdapter(contxt);
		ll.setAdapter(vra);

		UserPreferences up = new UserPreferences();
		ArrayList<SearchCategoryDisplayOption> o = up.readUserPreferences(contxt);
	
		viewableResultsBuffer = new ViewableResultsBuffer(vra, o);
		Connector.vrao = (ViewableResultsAvailableObserver) viewableResultsBuffer;
		
		
		draggableItemGridViewMenu.initialize(o, viewableResultsBuffer);

		
		
		connectors = new HashMap<SearchCategory, Connector>();
		
		connectors.put(SearchCategory.Contacts, new Contacts(context)); 
		
		connectors.put(SearchCategory.Images, new Images(context)); 
		connectors.put(SearchCategory.Sms, new Sms(context));
		
		
		connectors.put(SearchCategory.Calendar, new Calendar(context));
		connectors.put(SearchCategory.InstalledApps, new InstalledApps(context));
		connectors.put(SearchCategory.Video, new Video(context)); 	
		connectors.put(SearchCategory.Music, new Music(context));
		connectors.put(SearchCategory.Web, new WebConnector(context)); 
	}

	private volatile boolean onCreateCalled = false;
	
    @Override
	public void onCreate() {
    	
    	Pith.i("Connector OPERATOR --------------------on CREATE");
	 	
    	onCreateCalled = true;
	 	Connector.wakeUpThreadPool();
	 	

	 	Connector.doRunnableTask(DefaultResourceCache.initialize(context), ThreadTaskType.Open);
	}
 	     
	@Override
	public void onResume() {
		Pith.i("Connector OPERATOR -------------------- on RESUME");
	 	
	
		if (onCreateCalled) {
			if (vra != null) {
		 		UserCache.onLoad(context, vra);
		 	}
			Connector.doRunnableTask(new DeserializeIncrementalStateTask(this), ThreadTaskType.Open);
			onCreateCalled = false;
		} else {
			Connector.wakeUpThreadPool();
		}
		
	 	final Set<SearchCategory> categoryKeySet = connectors.keySet();
    	for (SearchCategory cat : categoryKeySet) {
    		// add IS LOADING for all so can avoid casting and then check if loading before sending on quueue
    		Connector.doRunnableTask(new OpenTask(connectors.get(cat)), ThreadTaskType.Open);
    	}
	}
	
	
	
	public void onNewSearchInput(String searchInput) {
		
		if (searchInput.length() == 0) {
			viewableResultsBuffer.onSearchInputCleared();
		} else {
			viewableResultsBuffer.onNewSearchStarted();
	    	final Set<SearchCategory> categoryKeySet = connectors.keySet();
	    	for (SearchCategory cat : categoryKeySet) {
	    		connectors.get(cat).processSearch(searchInput);
	    	}
		}
	}
	
	
	
	@Override
	public void onPause() {
		UserCache.getUserCache().onSave(context);
		
		Connector.haltSearchTasks();

		UserPreferences up = new UserPreferences();
		up.saveUserPreferences(context, viewableResultsBuffer.getCurrentSearchCategoryDisplayOption());

    	final Set<SearchCategory> categoryKeySet = connectors.keySet();
    	for (SearchCategory cat : categoryKeySet) {
			if (Connector.isIndexedConnector(connectors.get(cat).getCategory())) {
				IndexedConnector ic = (IndexedConnector) connectors.get(cat);
				ic.onSave();
			}
    	}

		Connector.doRunnableTask(new SerializeIncrementalStateTask(this, new IncrementalDatabase(context)), ThreadTaskType.Close);
	}

	public void toggleInternetConnectivityIsAvailable(boolean isAvailable) {
		
		if (connectors.get(SearchCategory.Web) != null) {
			connectors.get(SearchCategory.Web).processSearch(null);
		}
		
	}
	
	@Override
	public void onDestroy() {	
		Thread t = new Thread(r, "ondestroy thread");
		t.start();
		
	}
	
	
	Runnable r = new Runnable() {
		
		// This forces the OS to keep the thread pool and saving/disposing tasks alive by postponing close calls until
		// save tasks have completed.
		/*
		When we quit the program and the snapshots and indexes of the data sources are dirty, we need to serialize them 
		to the disk. This step can be long (e.g., 20-30 seconds). To make sure this step is finished, we use a new thread
		that keeps waiting for the serialization tasks to finish. If the user re-launches the app before the serialization 
		tasks are done, some of the unfinished indexes will be viewed corrupted, and the app will rebuild these indexes. 
		*/
		
		@Override
		public void run() {
			ArrayList<IndexedConnector> indexedConnectors = new ArrayList<IndexedConnector>();
			
			final Set<SearchCategory> categoryKeySet = connectors.keySet();
			for (SearchCategory cat : categoryKeySet) {
				if (Connector.isIndexedConnector(connectors.get(cat).getCategory())) {
					IndexedConnector ic = (IndexedConnector) connectors.get(cat);
					indexedConnectors.add(ic);
				}
			}
			boolean whileStillDestroying = false;
			
			while (!whileStillDestroying) {
				try {
					Thread.currentThread().sleep(42);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					Pith.handleException(ie);
				}
				
				whileStillDestroying = true;
				
				for (IndexedConnector ic : indexedConnectors) {
					boolean didDestroy = !ic.indexAndSnapshotSerializable();
					
					whileStillDestroying = whileStillDestroying && didDestroy;
		    	}
			}
			
	    	for (SearchCategory cat : categoryKeySet) {
	    		Connector.doRunnableTask(new DisposeTask(connectors.get(cat)), ThreadTaskType.Close);
	    	}
	    	
	    	try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				Pith.handleException(ie);
			}
	    	
	    	Connector.shutDownThreadPool();
		}
	};
}
