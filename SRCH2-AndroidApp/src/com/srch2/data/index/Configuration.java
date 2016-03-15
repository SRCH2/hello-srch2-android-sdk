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

import java.util.HashMap;
import java.util.Set;

import com.srch2.Pith;
import com.srch2.data.SearchCategory;
import com.srch2.instantsearch.Analyzer;
import com.srch2.instantsearch.CreateException;
import com.srch2.instantsearch.Schema;

public class Configuration {

	protected static final int DEFAULT_MERGE_EVERY_NSECONDS = 1;
	protected static final int DEFAULT_MERGE_EVERY_MWRITES = 1;
	protected static final int DEFAULT_ATTRIBUTE_BOOST = 1;
	protected static final String SCHEMA_BOOST_ENABLE_STATEMENT = "idf_score*doc_boost";
	
	private static String serializedIndexDirectoryPath;
	protected static void setDirectoryPathForSerializedIndexes(String directoryPath) { serializedIndexDirectoryPath = directoryPath; }

	private String serializedIndexFileName;
	protected String getSerializedIndexFilePath() { return serializedIndexDirectoryPath + serializedIndexFileName; }

	protected Analyzer analyzer;
	protected Schema schema;
	
	protected int topK;
	protected int editDistance;

	protected long cacheByteSize;
	protected int cacheEntryCount;

	private Configuration(SearchCategory indexFileNameQualifier, HashMap<String, Integer> schemaAttributes) {
		serializedIndexFileName = indexFileNameQualifier.name();
		
		topK = 4;
		editDistance = 1;
		cacheByteSize = 102400;
		cacheEntryCount = 100;
	
		schema = Schema.create(Schema.IndexType.FULL_TEXT_INDEX, Schema.PositionIndexType.NO_POSITION_INDEX, BaseSchemaRuleSet.attribute_recordPrimaryKeyName);
		schema.setScoringExpression(SCHEMA_BOOST_ENABLE_STATEMENT);
		
		final Set<String> schemaAttributeKeySet = schemaAttributes.keySet();
		for (String schemaAttribute : schemaAttributeKeySet) {
			schema.setSearchableAttribute(schemaAttribute, schemaAttributes.get(schemaAttribute));
		}
		schema.commit();
	
		try { analyzer = new Analyzer(false, false, Analyzer.AnalyzerType.STANDARD_ANALYZER, ""); } 
		catch (CreateException e) { Pith.handleException(e);; }
	}
	
	static public Configuration getSimpleConfiguration(SearchCategory forWhichSearchCategory, HashMap<String, Integer> schemaRuleSet) {	
		return new Configuration(forWhichSearchCategory, schemaRuleSet);
	}
}
