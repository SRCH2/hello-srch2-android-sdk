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
package com.srch2.data.connector.web;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.srch2.InternetConnectivity;
import com.srch2.Pith;
import com.srch2.R;
import com.srch2.data.SearchCategory;
import com.srch2.data.ThreadPool.ThreadTaskType;
import com.srch2.data.connector.Connector;
import com.srch2.data.connector.indexed.SearchTask;
import com.srch2.instantsearch.QueryResult;
import com.srch2.viewable.result.ViewableResult;
import com.srch2.viewable.result.WebViewableResult;

public class WebConnector extends Connector {

	public static final String SUGGESTION_TAG_NAME = "suggestion";
	public static final String SUGGESTION_ATTRIBUTE_DATA = "data";
	public static final int MAXIMUM_SUGGESTIONS = 5;
	
	private Context context;
	
	protected static AtomicBoolean accessLock;

	public WebConnector(Context contxt) {
		super(SearchCategory.Web);
		
		context = contxt;

		accessLock = new AtomicBoolean();
		super.addAndSetAccessLock(getCategory(), accessLock);
	}

	@Override public void load(HashSet<String> serializedIncrementalMemory) { /* do nothing */ }
	@Override public void open() { /* do nothing */ }
	@Override public void save() { /* do nothing */ }
	@Override public void dispose() { /* do nothing */ }

	public static Document getDomElement(String xml) {
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			doc = db.parse(is);
		} catch (ParserConfigurationException e) {
			Pith.handleException(e);
			return null;
		} catch (SAXException e) {
			Pith.handleException(e);
			return null;
		} catch (IOException e) {
			Pith.handleException(e);
			return null;
	    }catch (NullPointerException e) {
	    	Pith.handleException(e);
			return null;
	    }
		return doc;
	}
	
	private String latestSearchInput = "";
	
	@Override
	protected void processSearch(String searchInput) {
		if (searchInput != null) {
			latestSearchInput = searchInput;
		}
		
		if (InternetConnectivity.isNetworkAvailable(context)) {
			if (super.checkIsLockAccessable(getCategory())) {
				if (searchInput != null && searchInput.length() > 0) {
					Connector.doRunnableTask(new WebSearchRunnableTask(searchInput), ThreadTaskType.Search);
				} else if (searchInput == null && latestSearchInput != null && latestSearchInput.length() > 0) {
					Connector.doRunnableTask(new WebSearchRunnableTask(latestSearchInput), ThreadTaskType.Search);
				}
			} 
		}
	}

	private class WebSearchRunnableTask implements Runnable {
		
		private String currentSearchInput = "";
		
	
		public WebSearchRunnableTask(String searchInput) {
			currentSearchInput = searchInput;
		}
		
		@Override
		public void run() {
			final String searchInput = currentSearchInput;

			ArrayList<String> suggestionResults = new ArrayList<String>();
			
			final SearchCategory category = getCategory();
			while (Connector.getAccessLock(category).getAndSet(true)) {
				try {
					String queryUri = String.format(
							"http://www.google.com/complete/search?output=toolbar&q=%s",
							Uri.encode(searchInput));

					if (InternetConnectivity.isNetworkAvailable(context)) {
						
						HttpClient httpclient = new DefaultHttpClient(); 
				        HttpResponse response;
				        String responseString = null;
				        try {
				            response = httpclient.execute(new HttpGet(queryUri));
				            StatusLine statusLine = response.getStatusLine();
				            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
				                ByteArrayOutputStream out = new ByteArrayOutputStream();
				                response.getEntity().writeTo(out);
				                out.close();
				                responseString = out.toString();
				            } else {
				                response.getEntity().getContent().close();
				                throw new IOException(statusLine.getReasonPhrase());
				            }
				        } catch (ClientProtocolException e) {
				        	Pith.handleException(e);
				        } catch (IOException e) {
				        	Pith.handleException(e);
				        }       
				  
				        Document doc = getDomElement(responseString);
				        
				        if (doc != null) {	 
				    		NodeList nodeList = doc.getElementsByTagName(SUGGESTION_TAG_NAME);
							for (int i = 0; i < nodeList.getLength() && i < MAXIMUM_SUGGESTIONS; i++) {
								Element element = (Element) nodeList.item(i);
								if (element != null) {
									String data = element.getAttribute(SUGGESTION_ATTRIBUTE_DATA);
									if (data.length() > 0 && !element.getAttribute(SUGGESTION_ATTRIBUTE_DATA).equals(searchInput)) {
										suggestionResults.add(data);
									}
								}
							}
				        	
				        }

					}
				} catch (Exception e) {
					Pith.handleException(e);
				} finally {
					Connector.getAccessLock(category).set(false);
				}
			}

			ArrayList<ViewableResult> viewableResults = new ArrayList<ViewableResult>();
			viewableResults.add(new WebViewableResult(searchInput, WebSearchType.PlayStore, searchInput));
			viewableResults.add(new WebViewableResult(searchInput, WebSearchType.Wikipedia, searchInput));
			viewableResults.add(new WebViewableResult(searchInput, WebSearchType.Google, searchInput));
			for (String suggestion : suggestionResults) {
				viewableResults.add(new WebViewableResult(searchInput, WebSearchType.Suggestion, suggestion));	
			}
			
			if (viewableResults.size() > 0 && !Thread.currentThread().isInterrupted()) {
				Connector.vrao.onNewViewableResultsAvailable(getCategory(), viewableResults);
			}
		}
	};



	@Override
	public ArrayList<ViewableResult> getViewableResults(String searchInput, ArrayList<QueryResult> queryResults) { return null; }

	/*
	public static class WebViewableResult extends ViewableResult {
		protected WebSearchType resolveType;
		protected String webQueryString;

		public WebViewableResult(SearchCategory fromWhichCategory, WebSearchType resolvType, String queriString) {
			super(fromWhichCategory, getTitle(resolvType, queriString), getRowIcon(resolvType));
			webQueryString = queriString;
			resolveType = resolvType;
			hasStaticIcon = true;
		
		}

		private static String getTitle(WebSearchType resolveType, String unprocessedQueryText) {
			return webTypeTitlePrefixMap.get(resolveType) + formatHtmlShowing(unprocessedQueryText);
		
		}
		
		private static Bitmap getRowIcon(WebSearchType resolveType) {
			return webTypeBitmapMap.get(resolveType);
		}
		
		@Override
		public void onClick(Context context) {
			Intent i = null;
			switch (resolveType) {
				case Google:
					i = new Intent(Intent.ACTION_WEB_SEARCH);
					i.putExtra(SearchManager.QUERY, webQueryString);
					context.startActivity(i);
					break;
				case PlayStore:
					final String appPackageName = webQueryString; // getPackageName() from Context or Activity object
					try {
					    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + appPackageName)));
					} catch (android.content.ActivityNotFoundException anfe) {
						context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://market.android.com/search?q=" + appPackageName)));
					}
					break;	
				case Wikipedia:
					i = new Intent(Intent.ACTION_VIEW,  Uri.parse("http://en.wikipedia.org/wiki/" + webQueryString));
					context.startActivity(i);
					break;	
				case Suggestion:
					i = new Intent(Intent.ACTION_WEB_SEARCH);
					i.putExtra(SearchManager.QUERY, webQueryString);
					context.startActivity(i);
					break;
			}
		}

		//@Override
		public String getCategoryConcatenatedPrimaryKey() {
			return SearchCategory.Web + "~" + resolveType + "~" + webQueryString;
		}
	} */
}
