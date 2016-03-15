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
package com.srch2.data.connector.indexed;

import java.util.ArrayList;
import java.util.HashSet;

import android.content.Context;
import android.os.SystemClock;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.ThreadPool;
import com.srch2.data.ThreadPool.ThreadTaskType;
import com.srch2.data.connector.Connector;
import com.srch2.data.incremental.IncrementalUpdateObserver;
import com.srch2.data.incremental.observer.IncrementalObserver;
import com.srch2.data.index.Index;
import com.srch2.instantsearch.Indexer;
import com.srch2.instantsearch.QueryResult;
import com.srch2.instantsearch.Record;


public abstract class IndexedConnector extends Connector implements IncrementalUpdateObserver, IndexingOperations {

	
	protected Index index;
	public Index getConnectorIndex() { return index; }
	
	private volatile boolean incrementalSnapShotIsDirty = true;
	
	private volatile boolean connectorIndex_isSearchable;
	public boolean getConnectorIndexIsSearchable() { return connectorIndex_isSearchable; }

	private volatile boolean connectorIndex_isReversible;
	public boolean getConnectorIndexIsReversible() { return connectorIndex_isReversible; }
	
	private volatile boolean connectorIndex_isSerializable;
	public boolean indexAndSnapshotSerializable() {
		return connectorIndex_isSerializable && incrementalSnapShotIsDirty; 
	}


	
	private volatile boolean connectorIndex_isLoading = true;
	public boolean getConnectorIndexIsFinishedLoading() { return connectorIndex_isLoading; }
	public void setConnectorIndexIsLoading() { connectorIndex_isLoading = true; }
	
	private void setConnectorIndexIsFloating() { connectorIndex_isSearchable = connectorIndex_isSerializable = connectorIndex_isReversible = false; incrementalSnapShotIsDirty = true;}
	
	private String getRunTimeStateStatusLog() {
		return "[ isSearchable: " + connectorIndex_isSearchable + " ] " + " [ isReversible: " + connectorIndex_isReversible + " ] " + " [ isSerializable: " + connectorIndex_isSerializable + " ] " + " [ isLoading: " + connectorIndex_isLoading + " ] ";
	}
	
	public void setIsReadyToBeDisposed() { connectorIndex_isSerializable = false; incrementalSnapShotIsDirty = false; }

	protected IncrementalObserver incrementalObserver;
	protected HashSet<String> latestIncrementalSnapShot;
	public HashSet<String> getLatestIncrementalSnapShot() { return latestIncrementalSnapShot == null ? new HashSet<String>() : latestIncrementalSnapShot; }
	

	public static final String delimiter = String.valueOf(((char) 007));
	
	protected int inMemoryRecordSplitSize = 1;
	
	protected String[] decodeInMemoryRecord(String fromInMemoryRecord) {
		String[] safeData = null;
		String[] data = fromInMemoryRecord.split(delimiter, inMemoryRecordSplitSize);
		if (data.length < inMemoryRecordSplitSize) {
			
			safeData = new String[inMemoryRecordSplitSize];
			
			int imrLength = data.length;
			
			for (int i = 0; i < imrLength; i++) {
				safeData[i] = data[i];
			}
			
			for (int i = imrLength; i < inMemoryRecordSplitSize; ++i) {
				if (i == inMemoryRecordSplitSize - 1) {
					safeData[i] = SearchCategory.getTitleIfInMemoryRecordDataCorrupted(getCategory());
				} else {
					safeData[i] = " ";
				}
			}
			return safeData;
		} else {
			return data;
		}
	}
	

	protected Context context;
	
	public IndexedConnector(Context contxt, SearchCategory whichSearchCategory) {
		super(whichSearchCategory);
		context = contxt;
		setConnectorIndexIsFloating();
		index = new Index(whichSearchCategory, getSchemaSearchAttributes());
		latestIncrementalSnapShot = new HashSet<String>();
	}

	@Override
	public void load(HashSet<String> serializedIncrementalMemory) {

		connectorIndex_isLoading = true;
		

		boolean needsIndexing = false;
		if (index.checkIfSerializedIndexFileExists()) {

	
			if (serializedIncrementalMemory != null && serializedIncrementalMemory.size() > 0) {
				incrementalSnapShotIsDirty = false;
				latestIncrementalSnapShot = serializedIncrementalMemory;
				readAndLoadSerializedIndex();
				

			
		
			} else {


				needsIndexing  = true;
			}
		} else {
			
	
			
			needsIndexing  = true;
		}
	
		
		if (needsIndexing) {

			startTwoPhaseIndexing();	
		} else {

			hasPendingIncrementalUpdate = true;
			processSearch(null);
		}
		
		connectorIndex_isLoading = false;
		
		if (needsIndexing) {
			processSearch(null);
		}
		
		
	
	}

	protected void restartTwoPhaseIndexing() {
		startTwoPhaseIndexing();
	}
	
	private void startTwoPhaseIndexing() {
		ArrayList<ConnectorRecord> records = null;
		
		setConnectorIndexIsFloating();
		
		
		try {
			records = getQuickScanIndexRecords();
		} catch (Exception e) {
			Pith.handleException(e);
		} 
		
		if (records != null && !Thread.currentThread().isInterrupted()) {
			presetIndex(records);
			processSearch(null);
		} else {
			return;
		}

		if (ThreadPool.isSingleCore()) {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
		}

		ArrayList<ConnectorRecord> reversibleRecords = null;
		try {
			reversibleRecords = getReversibleScanIndexRecords(records);
		
		} catch (Exception e) {
			Pith.handleException(e);
		} 
	
		if (ThreadPool.isSingleCore()) {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
		}

		
		if (reversibleRecords != null && !Thread.currentThread().isInterrupted()) {
			presetIndexAndIncrementalMemory(reversibleRecords);
		}

		if (reversibleRecords != null) {
			reversibleRecords.clear();
		}
		if (records != null) {
			records.clear();
		}
	}
	
	public void presetIndex(ArrayList<ConnectorRecord> recordsWithoutIncrementalValues) {
		final SearchCategory category = super.getCategory();
		
		Indexer currentIndex = index.getBareNativeIndex();
		if (currentIndex != null) {
			Record r = Index.getRecordInstance(index.getConfiguredSchema());
			for (ConnectorRecord cr : recordsWithoutIncrementalValues) {
				currentIndex.addRecord(ConnectorRecord.getRecordAsIndexerRecord(r, cr), index.getConfiguredAnalyzer());
			}
		}		
		
		Indexer previousIndex = null;
		while (Connector.getAccessLock(category).getAndSet(true)) {
			try {
				previousIndex = index.setIndex(currentIndex);
				if (index.getIndex() != null) {
					index.commitNativeIndex();
				}
				connectorIndex_isSearchable = true;
			} catch (Exception e) {
				Pith.handleException(e);
			} finally {
				Connector.getAccessLock(category).set(false);
			}
		}
	
		if (previousIndex != null) {
			previousIndex.close();
		}
	}
	
	public void presetIndexAndIncrementalMemory(ArrayList<ConnectorRecord> recordsWithIncrementalValues) {
		final SearchCategory category = super.getCategory();
		Indexer currentIndex = index.getBareNativeIndex();
		if (currentIndex != null) {
			
			if (currentIndex != null) {
				Record r = Index.getRecordInstance(index.getConfiguredSchema());
				for (ConnectorRecord cr : recordsWithIncrementalValues) {
					currentIndex.addRecord(ConnectorRecord.getRecordAsIndexerRecord(r, cr), index.getConfiguredAnalyzer());
					latestIncrementalSnapShot.add(cr.incrementalValue);
				}
			}
		}		
		
		Indexer previousIndex = null;
		while (Connector.getAccessLock(category).getAndSet(true)) {
			try {
				previousIndex = index.setIndex(currentIndex);
				
				connectorIndex_isReversible = true;
				connectorIndex_isSerializable = true;
				
				if (index.getIndex() != null) {
					index.commitNativeIndex();
					
				}
			} catch (Exception e) {
				Pith.handleException(e);
			} finally {
				Connector.getAccessLock(category).set(false);
			}
		}
		
		incrementalObserver.startObserving();
		
		if (previousIndex != null) {
			previousIndex.close();
		}
	}
	
	private void readAndLoadSerializedIndex() {
		boolean success = true;

		Indexer serializedIndex = null;		
		if (index.checkIfSerializedIndexFileExists()) {
			serializedIndex = index.getSerializedIndex();
		}
		if (serializedIndex == null) {
			success = false;
		} else {
			success = IndexedConnector.checkReferentialIntegrity(
													serializedIndex, 
													latestIncrementalSnapShot);
		}
		if (!Thread.currentThread().isInterrupted()) {
			if (success) {
				presetIndex(serializedIndex);
			} else {
				startTwoPhaseIndexing();
			}
		}
	}
	
	public void presetIndex(Indexer serializedIndex) {
		Indexer previousIndex = null;

		final SearchCategory category = super.getCategory();
		while (Connector.getAccessLock(category).getAndSet(true)) {
			try {
				previousIndex = index.setIndex(serializedIndex);
				connectorIndex_isSearchable = true;
				connectorIndex_isSerializable = true;
				connectorIndex_isReversible = true;
			} catch (Exception e) {
				Pith.handleException(e);
			} finally {
				Connector.getAccessLock(category).set(false);
			}
		}
		incrementalObserver.startObserving();
		if (previousIndex != null) {
			previousIndex.close();
		}
	}
	
	public static boolean checkReferentialIntegrity(Indexer nativeIndex, HashSet<String> latestIncrementalSnapShot) {
		if (nativeIndex != null && latestIncrementalSnapShot != null) {
			final int nativeIndexRecordCount = nativeIndex.getNumberOfRecordsInIndex();
			final int incrementalSnapShotCount = latestIncrementalSnapShot.size();
			if (nativeIndexRecordCount == incrementalSnapShotCount) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void open() {
		if (connectorIndex_isLoading) {
			if (index.getIndex() != null) {
				connectorIndex_isSearchable = true;	
			} 
			return;
		}
		
		boolean indexAndIncrementalHasIntegrity = true;
		
		if (connectorIndex_isReversible && index.getIndex() != null) {
			boolean indexIsStable = checkReferentialIntegrity(index.getIndex(), latestIncrementalSnapShot);// incrementalMemory.getlatestIncrementalSnapShot());;
			if (indexIsStable) {
				
				connectorIndex_isSearchable = true;
				connectorIndex_isSerializable = true;
				
				incrementalObserver.startObserving();
				
				hasPendingIncrementalUpdate = false;
				doDelayedRunnableUpdateTask(new UpdateTask(this), 1000);
				
			} else {
				indexAndIncrementalHasIntegrity = false;
			}
		} else {
			indexAndIncrementalHasIntegrity = false;
		}
		
		if (!indexAndIncrementalHasIntegrity && !Thread.currentThread().isInterrupted()) {
	
			setConnectorIndexIsFloating();
			startTwoPhaseIndexing();
		} 
	}

	
	@Override
	public void onIncrementalDifferenceDetected() {
	
		handleIncrementalRequest();
	}
	
	protected void handleIncrementalRequest() {
	
		hasPendingIncrementalUpdate = false;
		Connector.doRunnableTask(new UpdateTask(this), ThreadTaskType.Update);
	}
	
	protected void rectifyIndexRecords(final ArrayList<ConnectorRecord> records) {

		if (records.size() > 0) {
			incrementalSnapShotIsDirty = true;
			connectorIndex_isSerializable = true;
		} else {
			return;
		}
		
		final SearchCategory category = super.getCategory();
		while (Connector.getAccessLock(category).getAndSet(true)) {
			try {
				Indexer currentIndex = index.getIndex();
				HashSet<String> latestIncrementalSnapShott = latestIncrementalSnapShot; //incrementalMemory.getlatestIncrementalSnapShot();

				if (currentIndex != null) {
					Record r = Index.getRecordInstance(index.getConfiguredSchema());
					
					for (ConnectorRecord cr : records) {
	
						
						if (!cr.isToBeAdded) {
							currentIndex.deleteRecord(cr.primaryKey);
							latestIncrementalSnapShott.remove(cr.incrementalValue);		
						} else {
							currentIndex.addRecord(ConnectorRecord.getRecordAsIndexerRecord(r, cr), index.getConfiguredAnalyzer());
							latestIncrementalSnapShott.add(cr.incrementalValue);	
						}
					}
					
					index.commitNativeIndex();
				}
			} catch (Exception e) {
				Pith.handleException(e);
			} finally {
				Connector.getAccessLock(category).set(false);
			}
		}
	}
	
	
	
	
	
	
	
	@Override
	protected void processSearch(String searchInput) {
	
		
		if (searchInput != null) {
			latestSearchInput = searchInput;
		}
		
		if (searchInput != null && searchInput.length() > 0) {
			handleSearchRequest(searchInput);
		} else if (latestSearchInput != null && latestSearchInput.length() > 0) {
			handleSearchRequest(latestSearchInput);
		} else if (hasPendingIncrementalUpdate) {
			handleIncrementalRequest();
		}
	}
	
	private static String latestSearchInput = "";
	protected boolean hasPendingSearch = false, hasPendingIncrementalUpdate = false;
	
	private void handleSearchRequest(String searchInput) {
	
	
		if (connectorIndex_isSearchable && super.checkIsLockAccessable(getCategory())) {
			hasPendingSearch = false;
			Connector.doRunnableTask(new SearchTask(this, searchInput), ThreadTaskType.Search);
		} else {
			hasPendingSearch = true;
		}
	}
	

	protected ArrayList<QueryResult> getQueryResultsFromSearchInput(String searchInput) {
		
	
		ArrayList<QueryResult> results = null;
		final SearchCategory category = getCategory();
		while (Connector.getAccessLock(category).getAndSet(true)) {
			try {
				results = index.getSearchResults(searchInput);
			} catch (Exception e) {
				Pith.handleException(e);
			} finally {
				Connector.getAccessLock(category).set(false);
			}
		}
		
		return results;
	}

	
	public void onSave() {
		incrementalObserver.stopObserving();
	}
	
	@Override
	public void save() {

		connectorIndex_isSearchable = false;
	
		final SearchCategory category = super.getCategory();
		if (indexAndSnapshotSerializable()) {
			while (Connector.getAccessLock(category).getAndSet(true)) {
				try {
					if (index.getIndex() != null && latestIncrementalSnapShot != null) { // incrementalMemory.getlatestIncrementalSnapShot() != null) {
						index.saveNativeIndex();
						
					}
				} catch (Exception e) {
					Pith.handleException(e);
				} finally {
					connectorIndex_isSerializable = false;
					incrementalSnapShotIsDirty = false;
					Connector.getAccessLock(category).set(false);
				}
			}
		}
		

		connectorIndex_isSearchable = true;

	}
	
	@Override
	public void dispose() {
	
	
		latestSearchInput = "";
		
		while (indexAndSnapshotSerializable()) {
			try {
				Thread.currentThread().sleep(42);
			} catch (InterruptedException e) {
				Pith.handleException(e);
			}
		}
		
		if (index.getIndex() != null) {
			index.closeNativeIndex();
		}
		connectorIndex_isSerializable = true;
	}

}
