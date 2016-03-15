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
import android.util.Log;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.connector.Connector;
import com.srch2.instantsearch.QueryResult;

public class UpdateTask implements Runnable {

	private IndexedConnector connector;
	
	public UpdateTask(IndexedConnector connectr) {
		connector = connectr;
	}
	
	@Override
	public void run() {
		if (Pith.isLogging) {
			Thread.currentThread().setName(connector.getCategory() + ":: UPDATE START");
		}
		
		if (connector == null) {
			return;
		}
		
		ArrayList<ConnectorRecord> updateRecords = null;
		try {
			updateRecords = connector.resolveIncrementalDifferenceUpdate();	
		} catch (Exception e) {
			Pith.handleException(e);
		}

		if (updateRecords == null) {
			updateRecords = new ArrayList<ConnectorRecord>(0);
		}
		
		int addCount = 0;

		if (connector == null) {
			return;
		}
		
		for (ConnectorRecord cr : updateRecords) {
			if (cr.isToBeAdded) {
				++addCount;
			}
		}

		/*
		if (connector.hasPendingSearch) {
			
			connector.hasPendingIncrementalUpdate = true;
			Connector.doDelayedRunnableUpdateTask(new UpdateTask(connector), 100);
			return;
		} */


		if (addCount > 200) {
			connector.restartTwoPhaseIndexing();
		}

		if (updateRecords != null && !Thread.currentThread().isInterrupted()) {
			
			if (updateRecords.size() > 0) {
				connector.rectifyIndexRecords(updateRecords);
				connector.processSearch(null);
			}
		}
		
		
		if (Pith.isLogging) {
			Thread.currentThread().setName(connector.getCategory() + ":: UPDATE FINISHED");
		}
		
	}
}
