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
import java.util.HashMap;

import com.srch2.instantsearch.Record;

public class ConnectorRecord {
	/** Set to <b>true</b> if this meta-record is to be added to native index, <b>false</b> if to be removed. */
	public boolean isToBeAdded = true;
	
	/** Primary key from client data to be used as primary key of native index record. */
	public String primaryKey = "";
	
	/** Attribute values map for setting searchable attributes of index. */
	public HashMap<String, String> attributeValues = null;
	
	/** Value to boost native index record by. */
	public float boostValue = 1;
	
	/** Value for inMemoryRecordData of native index record. */
	public String inMemoryRecordData = null;
	
	/** Value for inMemoryRecordData of native index record. */
	public String incrementalValue = "";

	/**
	 * This constructor presumes a new native index 
	 * record will created from this data values of this meta-record.
	 */
	public ConnectorRecord() { 
		attributeValues = new HashMap<String, String>();
	}
	
	/**
	 * This constructor presumes an existing native 
	 * index record will deleted by this meta-record.
	 */
	public ConnectorRecord(String pidToBeDeleted, String incrementalValueToBeRemoved) {
		primaryKey = pidToBeDeleted;
		incrementalValue = incrementalValueToBeRemoved;
		isToBeAdded = false;
	}
	
	/**
	 * Sets the values of a native index record to be added to a native index.
	 * @param indexerRecord the native index record instance.
	 * @param connectorRecord the meta-record holding the values to be used.
	 * @return The processed native index record instance passed in.
	 */	
	public static Record getRecordAsIndexerRecord(Record indexerRecord, final ConnectorRecord connectorRecord) {
		indexerRecord.clear();
		indexerRecord.setPrimaryKey(connectorRecord.primaryKey);
		for (String attributeKey : connectorRecord.attributeValues.keySet()) {
			indexerRecord.setAttibuteValue(attributeKey, connectorRecord.attributeValues.get(attributeKey));
		}
		if (connectorRecord.boostValue != -1) {
			indexerRecord.setRecordBoost(connectorRecord.boostValue);
		}
		indexerRecord.setInMemoryData(connectorRecord.inMemoryRecordData);
		return indexerRecord;
	}

	/** Returns a verbose, multi-line String representing the values in this meta-record. */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("pk[" + primaryKey + "]\n");
		sb.append("mrd[" + inMemoryRecordData + "]\n");
		sb.append("boost[" + boostValue + "]\n");
		
		if (attributeValues != null) {
			sb.append("attrs:: ");
			for (String key : attributeValues.keySet()) {
				sb.append("[" + key + ": ");
				sb.append(attributeValues.get(key) + " ]");
				sb.append("\n");
			}
		}
		sb.append("incrementalValue[" + incrementalValue + "]");
		return sb.toString();
	}
}
