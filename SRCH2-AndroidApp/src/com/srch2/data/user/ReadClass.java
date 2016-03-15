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
package com.srch2.data.user;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;

import android.content.Context;
import android.util.Log;

import com.srch2.Pith;
import com.srch2.viewable.ViewableResultsAdapter;
import com.srch2.viewable.result.ViewableResult;

public class ReadClass implements Runnable {

	private WeakReference<Context> contextC;
	private WeakReference<ViewableResultsAdapter> vRRA;
	
	public ReadClass(Context c, ViewableResultsAdapter vra) {
		contextC = new WeakReference<Context>(c);
		vRRA = new WeakReference<ViewableResultsAdapter>(vra);
	}
	
	@Override
	public void run() {
		if (contextC.get() != null) {
	
			HashSet<ViewableResultStatistic> v = null;
			try {
				UserCacheDatabase ucd = new UserCacheDatabase(contextC.get());
				v = ucd.getAll();
			} catch (Exception e) {
				Pith.reportExceptionSilently(e);
			}
		
			if (v != null) {
				UserCache.getUserCache().set(v);
				
				ArrayList<ViewableResult> mostFrequentResults = new ArrayList<ViewableResult>(34);
				for (ViewableResultStatistic vrss : v) {
					mostFrequentResults.add(ViewableResult.fromSerialString(vrss.searchCategory, vrss.serialString));
				}
						
				if (!Thread.currentThread().isInterrupted() && vRRA.get() != null && mostFrequentResults.size() > 0) {
					vRRA.get().sendQuickLoadMessageUpdate(mostFrequentResults);
				}
			}
		}
	}
}

