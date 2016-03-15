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

import java.util.*;

import android.util.Log;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.data.ThreadPool.ThreadTaskType;
import com.srch2.data.connector.indexed.IndexedConnector;
import com.srch2.data.connector.indexed.SaveTask;
import com.srch2.data.incremental.IncrementalDatabase;

public class SerializeIncrementalStateTask implements Runnable {

	private ConnectorOperator connectorOperator;
	private IncrementalDatabase db;

	public SerializeIncrementalStateTask(ConnectorOperator conop, IncrementalDatabase database) {
		connectorOperator = conop;
		db = database;
	}

	@Override
	public void run() {
        HashMap<String, HashSet<String>> serializedIncrementalMemoryMap = new HashMap<String, HashSet<String>>();

        ArrayList<IndexedConnector> indexedConnectors = new ArrayList<IndexedConnector>();

        final Set<SearchCategory> categoryKeySet = connectorOperator.connectors.keySet();
        for (SearchCategory cat : categoryKeySet) {
            if (Connector.isIndexedConnector(connectorOperator.connectors.get(cat).getCategory())) {
                IndexedConnector ic = (IndexedConnector) connectorOperator.connectors.get(cat);
                indexedConnectors.add(ic);
            }
        }

        for (IndexedConnector ic : indexedConnectors) {
            if (ic.indexAndSnapshotSerializable()) { // LET ALL connectors be serializable, then set to false and avoid previous for loop

                HashSet<String> incrementalStateTarget = new HashSet<String>();

                HashSet<String> incrementalStateSource = null;


                SearchCategory category = ic.getCategory();
                while (Connector.getAccessLock(category).getAndSet(true)) {
                    try {
                        incrementalStateSource = ic.getLatestIncrementalSnapShot();
                        for (String s : incrementalStateSource) {
                            incrementalStateTarget.add(new String(s));
                        }
                    } catch (Exception e) {
                        Pith.handleException(e);
                    } finally {
                        Connector.getAccessLock(category).set(false);
                    }
                }
                serializedIncrementalMemoryMap.put(ic.getCategory().name(), incrementalStateTarget); //     getConnectorIncrementalMemory().getlatestIncrementalSnapShot());
            }
        }

        db.serializeIncrementalState(serializedIncrementalMemoryMap);

        //align so that save task is performed after data is deep copied above
        for (IndexedConnector ic : indexedConnectors) {


            if (ic.indexAndSnapshotSerializable()) {
                Connector.doRunnableTask(new SaveTask(ic), ThreadTaskType.Close);
            } else {
                ic.setIsReadyToBeDisposed();
            }


        }
    }
}

