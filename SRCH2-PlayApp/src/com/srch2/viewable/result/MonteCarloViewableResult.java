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
package com.srch2.viewable.result;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.Toast;

import com.srch2.R;
import com.srch2.data.SearchCategory;

public class MonteCarloViewableResult extends ViewableResult {

	public MonteCarloViewableResult(SearchCategory scategory, String title) {
		super(scategory, title);
	}

	@Override
	protected String getEncodedMetaContentString() {
		return "";
	}

	@Override
	public void setMetaContent(String... args) { }

	@Override
	public Bitmap getIcon(Context context) {
		return icon;
	}

	@Override
	public void onClick(Context context) {
		super.onClick(context);
		Toast.makeText(context, "One click, two click, single click, double click!", Toast.LENGTH_LONG).show();
	}	
	
	@Override
	public void onSwipeLeft(Context context) { 
		super.onSwipeLeft(context);
		Toast.makeText(context, "Congratulations, you know your left from your right!", Toast.LENGTH_LONG).show();
	};
	
	@Override
	public void onSwipeRight(Context context) { 
		super.onSwipeRight(context);
		Toast.makeText(context, "Congratulations, you know your right from your left!", Toast.LENGTH_LONG).show();
	};
	
	@Override
	public void onRowButtonClick(Context context, int whichVieIdWasClicked) {
		super.onRowButtonClick(context, whichVieIdWasClicked);
		Toast.makeText(context, "Your ability to click a button \n is unmatched in all of history \n o' great clicker of the row button.", Toast.LENGTH_LONG).show();
	}
}
