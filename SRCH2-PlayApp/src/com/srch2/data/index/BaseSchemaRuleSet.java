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

public class BaseSchemaRuleSet {
	
	
	/** Enables dynamic per record boost to be set. */
	protected static final String SCHEMA_BOOST_ENABLE_STATEMENT = "doc_boost";
	
	public static final int defaultSearchAttributeBoost = 1;

	/** The primary key title of a record. */
	protected static final String attribute_recordPrimaryKeyName = "_id";
	
	/** The primary text displayed to the user, ideally matching the user's search input text verbatim: <br>
	 *  If the search result is from contacts, this would the person's name as it appears in the user's contacts app. <br>
	 *  If the search result is from images, this would the name displayed to the user when using an app file browser. <br>
	 *  If the search result is from music, this would be some form of the song, artist and album names as they would appear in an app music player. <br>
	 *  _pmt.equals("_primaryMatchingText") 
	 *  */
	public static final String attribute_recordPrimarySearchAttribute = "_pmt";
	public static final String attribute_recordSecondarySearchAttribute1 = "_snt1";
	public static final String attribute_recordSecondarySearchAttribute2 = "_snt2";
	public static final String attribute_recordSecondarySearchAttribute3 = "_snt3";
	public static final String attribute_recordSecondarySearchAttribute4 = "_snt4";
}
