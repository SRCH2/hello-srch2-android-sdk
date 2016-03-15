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
package com.srch2.viewable.list;


import java.util.ArrayList;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.srch2.data.SearchCategory;
import com.srch2.viewable.result.ViewableResult;

public class StaticListView extends LinearLayout {
	public StaticListView(Context context) { super(context); setupConstructors(context); }
	public StaticListView(Context context, AttributeSet attrs) { super(context, attrs); setupConstructors(context); }
	public StaticListView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); setupConstructors(context); }
	
	private Row[] resultRows;
	private final int resultRowMaximumSize = 45;
	
	private TouchController rowTouchController;
	
	private void setupConstructors(Context context) {
		rowTouchController = new TouchController(context);
		
		resultRows = new Row[resultRowMaximumSize];
		for (int i = 0; i < resultRowMaximumSize; i++) {
			Row r = new Row(context);
			r.setVisibility(View.GONE);
			r.setTouchControllers(rowTouchController);
			addView(r);
			resultRows[i] = r;
		}
	}

	public void updateVisibleResults(ArrayList<ViewableResult> newResults) {
		
		long t = SystemClock.uptimeMillis();
		final int newResultCount = newResults.size();
		for (int i = 0; i < newResultCount; i++) {
			resultRows[i].bindViewableResult(newResults.get(i));
			resultRows[i].setVisibility(VISIBLE);
		}
		for (int i = newResultCount; i < resultRowMaximumSize - 1; i++) {
			resultRows[i].setVisibility(View.GONE);
		}
		long e = SystemClock.uptimeMillis() - t;

	}
	
	
	
}
