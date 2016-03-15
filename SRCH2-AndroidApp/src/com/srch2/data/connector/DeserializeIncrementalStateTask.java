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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.util.Log;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.ThreadPool.ThreadTaskType;
import com.srch2.data.connector.indexed.LoadTask;
import com.srch2.data.incremental.IncrementalDatabase;

public class DeserializeIncrementalStateTask implements Runnable {

	private ConnectorOperator connectorOperator;
	
	public DeserializeIncrementalStateTask(ConnectorOperator conop) {
		connectorOperator = conop;
	}

	@Override
	public void run() {
		
		
		if (Pith.isLogging) {
			Thread.currentThread().setName("start - DESERIALIZE THREAD");
		}
		
		IncrementalDatabase db = new IncrementalDatabase(connectorOperator.context);

		HashMap<String, HashSet<String>> incrementalState = db.getSerializedIncrementalState(SearchCategory.toNamesArray());

		final Set<SearchCategory> searchCategoryNames = connectorOperator.connectors.keySet();
		for (SearchCategory categoryName : searchCategoryNames) {
			Connector.doRunnableTask(new LoadTask(connectorOperator.connectors.get(categoryName), incrementalState.get(categoryName.name())), ThreadTaskType.Open);
		}
	
	
		if (Pith.isLogging) {
			Thread.currentThread().setName("finish - DESERIALIZE THREAD");
		}
	}
}

