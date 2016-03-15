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

import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.connector.Connector;
import com.srch2.instantsearch.QueryResult;
import com.srch2.viewable.ViewableResultsBuffer;
import com.srch2.viewable.result.ViewableResult;

public class SearchTask implements Runnable {

	private IndexedConnector connector;
	private String searchInput;
	
	public SearchTask(IndexedConnector connectr, String srchInput) {
		connector = connectr;
		searchInput = srchInput;
	}
	
	@Override
	public void run() {
		if (Pith.isLogging) {
			Thread.currentThread().setName(connector.getCategory() + ":: SEARCH START");
		}
		
		
		ArrayList<QueryResult> results;
		

	
		try {
			results = connector.getQueryResultsFromSearchInput(searchInput);
		} catch (Exception e) {
			Pith.reportExceptionSilently(e);
			results = new ArrayList<QueryResult>();
		}
	

		ArrayList<ViewableResult> vrs = new ArrayList<ViewableResult>();
		if (!Thread.currentThread().isInterrupted()) {
			if (results.size() != 0) {
				vrs = connector.getViewableResults(searchInput, results);
			} 
		} else {
			return;
		}
		

	
		
		if (!Thread.currentThread().isInterrupted()) {
			Connector.vrao.onNewViewableResultsAvailable(connector.getCategory(), vrs);

			if (connector.hasPendingSearch) {
				connector.processSearch(null);
			} else if (connector.hasPendingIncrementalUpdate) {
				connector.handleIncrementalRequest();
			}
		}

		
		if (Pith.isLogging) {
			Thread.currentThread().setName(connector.getCategory() + ":: SEARCH FINISHED");
		}
		
	}

	
}
