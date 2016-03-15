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
package com.srch2.data.index;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.acra.ACRA;

import android.util.Log;

import com.srch2.Pith;
import com.srch2.R;
import com.srch2.data.SearchCategory;
import com.srch2.instantsearch.Analyzer;
import com.srch2.instantsearch.CreateException;
import com.srch2.instantsearch.IndexSearcher;
import com.srch2.instantsearch.Indexer;
import com.srch2.instantsearch.Logger;
import com.srch2.instantsearch.Query;
import com.srch2.instantsearch.QueryResult;
import com.srch2.instantsearch.Record;
import com.srch2.instantsearch.Schema;
import com.srch2.instantsearch.Term;
import com.srch2.instantsearch.Term.TermType;

public class Index {

	
	private Indexer nativeIndex;
	/** @return the current native index ("<code>Indexer</code>") this Index class uses to modify records and perform searches */
	public Indexer getIndex() { return nativeIndex; }
	
	/** Returns the previous index, which may not exist so that if it exists, it can be closed after 
	 * 	the connector, or other object invoking this method, after the fact as closing one index 
	 *  may take ~300 milliseconds. 
	 * @param newIndex to set as current
	 * @return previousIndex to close
	 */
	public Indexer setIndex(Indexer newIndex) {
		Indexer i = nativeIndex;
		nativeIndex = newIndex;
		return i;
	}
	
	/** Native <code>Indexer</code> configuration settings for this index. */
	private Configuration configuration;
	// should set as private ?
	public void setConfiguration(Configuration config) { configuration = config; }
	
	public Schema getConfiguredSchema() {
		return configuration.schema;
	}
	
	public Analyzer getConfiguredAnalyzer() {
		return configuration.analyzer;
	}
	
	public Index(SearchCategory whichSearchCategory, HashMap<String, Integer> schemaRuleSet) {
		configuration = Configuration.getSimpleConfiguration(whichSearchCategory, schemaRuleSet);
	}
	
	/**
	 * Getter for native index record instance. 
	 * @param schema to build record against.
	 * @return a clean instance of an index record.
	 */
	public static Record getRecordInstance(Schema schema) {
		Record r = null; try { r = new Record(schema); } catch (CreateException e) { e.printStackTrace(); } return r;
	}

	/** @return i is a new native index with an empty record set based on this <code>configuration</code> */
	public Indexer getBareNativeIndex() {
		return Indexer.createIndexer(
				Configuration.DEFAULT_MERGE_EVERY_NSECONDS,
				Configuration.DEFAULT_MERGE_EVERY_MWRITES,
				configuration.getSerializedIndexFilePath(),
				configuration.cacheByteSize,
				configuration.cacheEntryCount,
				configuration.analyzer,
				configuration.schema);
	}
	
	/** @return exists is <b>false</b> if the index file is null or missing from the disk, or <b>true</b> otherwise */
	public boolean checkIfSerializedIndexFileExists() {
		final File folder = new File(configuration.getSerializedIndexFilePath());
		final File[] files = folder.listFiles();
		final boolean exists = (files == null || files.length == 0) ? false : true;
		return exists;
	}

	public Indexer getSerializedIndex() {
		return Indexer.loadIndexer(
				Configuration.DEFAULT_MERGE_EVERY_NSECONDS,
				Configuration.DEFAULT_MERGE_EVERY_MWRITES,
				configuration.getSerializedIndexFilePath(),
				configuration.cacheByteSize,
				configuration.cacheEntryCount);
	}
	
	public void commitNativeIndex() {
		nativeIndex.commit();
	}
	
	public void saveNativeIndex() {
		nativeIndex.save();
	}
	
	public void closeNativeIndex() {
		nativeIndex.close();
	}
	
	
	public ArrayList<QueryResult> getSearchResults(String searchInput) {
		return doNativeSearch(searchInput);
	}
	
	private ArrayList<QueryResult> doNativeSearch(String searchInput) {
		final Configuration config = configuration;
		final int offset = 0;
		
		ArrayList<QueryResult> queryResultList = new ArrayList<QueryResult>();
		
		IndexSearcher indexSearcher = getIndexSearcher();
		
		if (indexSearcher != null) {
			ArrayList<String> queryKeyWords = config.analyzer.tokenizeQuery(searchInput);

			Query exactQuery = null;
			try {
				exactQuery = createExactQueryByDefaultSetting(queryKeyWords, config.editDistance);
			} catch (CreateException e) {
				e.printStackTrace();
			}
			
	
			if (exactQuery == null) {
				return new ArrayList<QueryResult>();
			}

			queryResultList = indexSearcher.searchTopK(exactQuery, offset, config.topK);

			
			if (config.editDistance > 0 && queryResultList.size() < config.topK) {
				HashSet<String> queryResultIds = new HashSet<String>(10);
				
				for (QueryResult qr : queryResultList) {
					queryResultIds.add(qr.getPrimaryKey());
				}

				Query fuzzyQuery = null;
				try {
					fuzzyQuery = createFuzzyQueryByDefaultSetting(queryKeyWords, config.editDistance);
				} catch (CreateException e) {
					e.printStackTrace();
				}
				
				ArrayList<QueryResult> fuzzyQueryResult = indexSearcher.searchTopK(fuzzyQuery,  offset, config.topK);
				
				

				
				int l = fuzzyQueryResult.size();
				for (int i = 0; i < l; ++i) {
					if (!queryResultIds.contains(fuzzyQueryResult.get(i).getPrimaryKey())) {
						queryResultList.add(fuzzyQueryResult.get(i));
					}
					
				}

			} 
			
			

			
		}

		
		return queryResultList;
	}
	
	private IndexSearcher getIndexSearcher() {
		if (nativeIndex != null) {
			return IndexSearcher.create(nativeIndex);
		} else {
			return null;
		}
	}

	private static Query createExactQueryByDefaultSetting(final ArrayList<String> queryKeyWords, final int editDistance) throws CreateException {
		Query query = new Query();
		for (int i = 0; i < queryKeyWords.size(); i++) {
			final String queryKeyWord = queryKeyWords.get(i);
			
			final int normEditDistance = 0;
			
			final TermType t = i < (queryKeyWords.size() - 1) ? TermType.TERM_TYPE_COMPLETE : TermType.TERM_TYPE_PREFIX;
			
			
			query.add(new Term(queryKeyWord, t, 1.0f, 0.5f, normEditDistance));
		}
		query.setPrefixMatchPenalty(.95f);
		return query;
	}	
	
	private static Query createFuzzyQueryByDefaultSetting(final ArrayList<String> queryKeyWords, final int editDistance) throws CreateException {
		Query query = new Query();
		for (int i = 0; i < queryKeyWords.size(); i++) {
			final String queryKeyWord = queryKeyWords.get(i);
			
			final int normEditDistance = (queryKeyWord.length() / 3) >= 3 ? 3 : (queryKeyWord.length() / 3);
			
			final TermType t = i < (queryKeyWords.size() - 1) ? TermType.TERM_TYPE_COMPLETE : TermType.TERM_TYPE_PREFIX;
			
			
			query.add(new Term(queryKeyWord, t, 1.0f, 0.5f, normEditDistance));
		}
		query.setPrefixMatchPenalty(.95f);
		return query;
	}	

	
	
	public static boolean initializeSRCH2NativeLibrary(String indexDirectoryFilePath) {
		boolean loadSuccess = true;
		
		// "goldfish" is Android SDK's emulator tag for hardware
		if (android.os.Build.HARDWARE.equals("goldfish")) {
			Pith.reportExceptionSilently(new Exception("Hardware was goldfish - emulated device enviroment."));
        	return false;
        }
      
		try {
			File indexDirectoryPathFile = new File(indexDirectoryFilePath);
			if (!indexDirectoryPathFile.exists()) {
				indexDirectoryPathFile.mkdirs();
			}
			Configuration.setDirectoryPathForSerializedIndexes(indexDirectoryFilePath);

			System.loadLibrary("srch2_core"); 
			Analyzer.initialize();
			QueryResult.initialize();
			Indexer.initialize();
			IndexSearcher.initialize();
			Logger.initialize();
			
			Logger.setLogLevel(Logger.LogLevel.SRCH2_LOG_SILENT);
			
			Query.initialize();
			Record.initialize();
			Schema.initialize();
			Term.initialize();
		} catch (Exception e) {
			loadSuccess = false;
			Pith.reportExceptionSilently(e);
		}

		return loadSuccess;
	}
	
	public static void uninitializeSRCH2NativeLibrary() {
		Analyzer.destroy();
		QueryResult.destroy();
		Indexer.destroy(); 
		IndexSearcher.destroy();
		Logger.destroy();
		Query.destroy();
		Record.destroy();
		Schema.destroy();
		Term.destroy();
	}
	
}
