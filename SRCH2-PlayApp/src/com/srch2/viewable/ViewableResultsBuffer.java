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
package com.srch2.viewable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.srch2.DraggableItemGridView.OnUserPreferencesChangedObserver;
import com.srch2.data.SearchCategory;
import com.srch2.data.SearchCategoryDisplayOption;
import com.srch2.viewable.result.ViewableResult;

import com.srch2.Pith;
public class ViewableResultsBuffer implements ViewableResultsAvailableObserver, OnUserPreferencesChangedObserver {

private static final int COUNTDOWN_TO_INVALIDATION_DURATION_MS = 120;
	
	private GooeyHandler uiHandler = null;
	private ViewableResultsAdapter adapter = null;
	
	private boolean bufferIsCountingDownToInvalidation = false;
	private boolean resultsShouldBePublished = true;
	
	private ArrayList<SearchCategory> SearchCategoryOrdering = null;
	private HashMap<SearchCategory, Boolean> SearchCategoryToIfDisplayedMap = null;
	
	private HashMap<SearchCategory, ArrayList<ViewableResult>> SearchCategoryNameToViewableResultSetMap = null;
	private HashMap<SearchCategory, Boolean> SearchCategoryNameToDirtyMap = null;
	
	public ViewableResultsBuffer(ViewableResultsAdapter vra, ArrayList<SearchCategoryDisplayOption> userSearchCategoryOptions) {
		uiHandler = new GooeyHandler(this);
		adapter = vra;
		
		SearchCategoryNameToViewableResultSetMap = new HashMap<SearchCategory, ArrayList<ViewableResult>>();
		SearchCategoryNameToDirtyMap = new HashMap<SearchCategory, Boolean>();
		
		SearchCategoryToIfDisplayedMap = new HashMap<SearchCategory, Boolean>();
		SearchCategoryOrdering = new ArrayList<SearchCategory>();

		final int length = userSearchCategoryOptions.size();
		for (int i = 0; i < length; ++i) {
			final SearchCategoryDisplayOption io = userSearchCategoryOptions.get(i);
			final SearchCategory categoryKey = io.category;
			final boolean ifCategoryIsDisplayed = io.isDisplayed;
			
			SearchCategoryOrdering.add(categoryKey);
			SearchCategoryToIfDisplayedMap.put(categoryKey, ifCategoryIsDisplayed);
			SearchCategoryNameToDirtyMap.put(categoryKey, false);
			SearchCategoryNameToViewableResultSetMap.put(categoryKey, new ArrayList<ViewableResult>());
		}
	}
	
	public ArrayList<SearchCategoryDisplayOption> getCurrentSearchCategoryDisplayOption() {
		
		ArrayList<SearchCategoryDisplayOption> currentOptions = new ArrayList<SearchCategoryDisplayOption>();
		
		final int l = SearchCategoryOrdering.size();
		for (int i = 0; i < l; ++i) {
			SearchCategory sc = SearchCategoryOrdering.get(i);
			boolean isDisplayed = SearchCategoryToIfDisplayedMap.get(sc);
			currentOptions.add(SearchCategoryDisplayOption.getSaveStateSearchCategoryDisplayOption(sc, isDisplayed));
		}
		
		return currentOptions;
	}
	
	
	
	

	public void onNewSearchStarted() {
		//bufferIsCountingDownToInvalidation = true;
		resultsShouldBePublished = true;
		//uiHandler.removeCallbacks(postNewViewableResultsSetToAdapterAfterCountingDownTask);
		//uiHandler.postDelayed(postNewViewableResultsSetToAdapterAfterCountingDownTask, COUNTDOWN_TO_INVALIDATION_DURATION_MS);
		
		final Set<SearchCategory> categoryIndexKeySet = SearchCategoryNameToDirtyMap.keySet();
		for (SearchCategory categoryKey : categoryIndexKeySet) {
			SearchCategoryNameToDirtyMap.put(categoryKey, true);
		}
	}
	
	public void onSearchInputCleared() {
		resultsShouldBePublished = false;
		clearBuffer();
	}
	
	private void clearBuffer() {
		uiHandler.removeCallbacks(postNewViewableResultsSetToAdapterAfterCountingDownTask);
		
		bufferIsCountingDownToInvalidation = false;
		
		final Set<SearchCategory> categoryKeySet = SearchCategoryNameToViewableResultSetMap.keySet();
		for (SearchCategory categoryKey : categoryKeySet) {
			SearchCategoryNameToDirtyMap.put(categoryKey, false);
			SearchCategoryNameToViewableResultSetMap.get(categoryKey).clear();
		}

		uiHandler.sendMessage(Message.obtain(uiHandler, PUSH_VIEWABLE_RESULTS_TO_ADAPTER, new ArrayList<ViewableResult>()));
	}
	
	private void addNewViewableResultsToBuffer(final SearchCategory category, ArrayList<ViewableResult> newResults) {

	
		if (!resultsShouldBePublished) {
			return;
		}
		
		newResults = newResults == null ? new ArrayList<ViewableResult>() : newResults;
		
		//HashSet<ViewableResult> diff = new HashSet<ViewableResult>();
		//diff.removeAll(SearchCategoryNameToViewableResultSetMap.get(category));
		//diff.addAll(newResults);
		
		SearchCategoryNameToViewableResultSetMap.put(category, newResults);
		publishNewViewableResultsToAdapter();
		
		//boolean needsInvalidating = true;
		//if (diff.size() != 0) {
		//	Log.d("SEARCHSEARCH", "was different result set!");
		//	SearchCategoryNameToViewableResultSetMap.put(category, newResults);
		////} else {
		//	needsInvalidating = false;

		//	Log.d("SEARCHSEARCH", "was NOOOOOOOOOOOO different result set!");
		//}
		/*
	
		if (bufferIsCountingDownToInvalidation) {

			int dirtyCount = 0;
			if (!needsInvalidating) {
				SearchCategoryNameToDirtyMap.put(category, true);
				
				final Set<SearchCategory> categoryIndexKeySet = SearchCategoryNameToDirtyMap.keySet();
				for (SearchCategory categoryKey : categoryIndexKeySet) {
					if (SearchCategoryNameToDirtyMap.get(categoryKey)) {
						++dirtyCount;
					}
				}
				++dirtyCount;
			}

			final int indexCategoriesCount = 1; //SearchCategoryOrdering.size();
			if (dirtyCount == indexCategoriesCount && needsInvalidating) {
				bufferIsCountingDownToInvalidation = false;
				uiHandler.removeCallbacks(postNewViewableResultsSetToAdapterAfterCountingDownTask);
				publishNewViewableResultsToAdapter();
			}
		} else {
			if (needsInvalidating) {
				publishNewViewableResultsToAdapter();
			}
		} */
	}
	
	
	private void publishNewViewableResultsToAdapter() {
		ArrayList<ViewableResult> viewableResultSetToPublish = new ArrayList<ViewableResult>();

		final int indexCategoriesCount = SearchCategoryOrdering.size();
		for (int i = 0; i < indexCategoriesCount; ++i) {
			final SearchCategory category = SearchCategoryOrdering.get(i);
			
			if (!SearchCategoryNameToViewableResultSetMap.containsKey(category)) {
				continue;
			}
			
			final boolean isDisplayed = SearchCategoryToIfDisplayedMap.get(category);
			if (isDisplayed) {
				viewableResultSetToPublish.addAll(SearchCategoryNameToViewableResultSetMap.get(category));
			}
		}

		uiHandler.sendMessage(Message.obtain(
										uiHandler, 
										PUSH_VIEWABLE_RESULTS_TO_ADAPTER, 
										viewableResultSetToPublish == null ? new ArrayList<ViewableResult>() : viewableResultSetToPublish));
	
	}
	
	private Runnable postNewViewableResultsSetToAdapterAfterCountingDownTask = new Runnable() {
		@Override
		public void  run() {
		
	
			ArrayList<ViewableResult> viewableResultSetToPublish = new ArrayList<ViewableResult>();

			final int indexCategoriesCount = SearchCategoryOrdering.size();
			for (int i = 0; i < indexCategoriesCount; ++i) {
				final SearchCategory category = SearchCategoryOrdering.get(i);
				if (!SearchCategoryNameToViewableResultSetMap.containsKey(category)) {
					continue;
				}
				final boolean isDisplayed = SearchCategoryToIfDisplayedMap.get(category);
				if (isDisplayed && SearchCategoryNameToDirtyMap.get(category)) {
					viewableResultSetToPublish.addAll(SearchCategoryNameToViewableResultSetMap.get(category));
				}
	
			}

			uiHandler.sendMessage(Message.obtain(
											uiHandler, 
											PUSH_VIEWABLE_RESULTS_TO_ADAPTER, 
											viewableResultSetToPublish == null ? new ArrayList<ViewableResult>() : viewableResultSetToPublish));

			bufferIsCountingDownToInvalidation = false;
		}
	};
	
	
	private static final int PUSH_VIEWABLE_RESULTS_TO_ADAPTER = 11259375;
	
	private static class GooeyHandler extends Handler {
		WeakReference<ViewableResultsBuffer> viewableResultBufferHandle;
		
		public GooeyHandler(ViewableResultsBuffer vrb) {
			viewableResultBufferHandle = new WeakReference<ViewableResultsBuffer>(vrb);
		}

		@Override
		public void handleMessage(Message msg) {
			final int messageWhat = msg.what;
			if (messageWhat == PUSH_VIEWABLE_RESULTS_TO_ADAPTER) {
				ArrayList<ViewableResult> newResults;
				try {
					newResults = (ArrayList<ViewableResult>) msg.obj;
				} catch (ClassCastException cce) {
					Pith.handleException(cce);
					newResults = new ArrayList<ViewableResult>();
				} catch (NullPointerException npe) {
					Pith.handleException(npe);
					newResults = new ArrayList<ViewableResult>();
				}
						
				viewableResultBufferHandle.get().adapter.onNewViewableResultsToPublish(newResults);
				
			} else {
				super.handleMessage(msg);
			}
		}
	}

	

	
	
	
	@Override
	public void userChangedIndexCatagoryOrder(int sourceIndex, int targetIndex) {
		updateSearchCategoryDisplayOrder(sourceIndex, targetIndex);
	}
	
	private void updateSearchCategoryDisplayOrder(int sourcePosition, int targetPosition) {
		final SearchCategory targetCategory = SearchCategoryOrdering.get(sourcePosition);
		SearchCategoryOrdering.remove(sourcePosition);
		SearchCategoryOrdering.add(targetPosition, targetCategory);
		
		final Set<SearchCategory> categoryKeySet = SearchCategoryNameToDirtyMap.keySet();
		for (SearchCategory categoryKey : categoryKeySet) {
			SearchCategoryNameToDirtyMap.put(categoryKey, true);
		}
		
		if (!bufferIsCountingDownToInvalidation) {
			publishNewViewableResultsToAdapter();
		}
	}
	
	@Override
	public void userChangedIfIndexCatagoryDisplayed(SearchCategory whichCategory, boolean isDisplayed) {
		updateIfSearchCategoryIsDisplayed(whichCategory, isDisplayed);
	}

	private void updateIfSearchCategoryIsDisplayed(final SearchCategory categoryToToggleDisplay, final boolean isToBeDisplayed) {
		final boolean currentlyDisplayed = SearchCategoryToIfDisplayedMap.get(categoryToToggleDisplay);
		SearchCategoryToIfDisplayedMap.put(categoryToToggleDisplay, isToBeDisplayed);
		
		if (currentlyDisplayed != isToBeDisplayed) {
			SearchCategoryNameToDirtyMap.put(categoryToToggleDisplay, true);
			
			if (!bufferIsCountingDownToInvalidation) {
				publishNewViewableResultsToAdapter();
			}
		}
	}



	@Override
	public void onNewViewableResultsAvailable(SearchCategory whichCategory, ArrayList<ViewableResult> newResults) {
		addNewViewableResultsToBuffer(whichCategory, newResults);
	}
}
