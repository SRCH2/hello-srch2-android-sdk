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
package com.srch2.data.incremental.observer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.srch2.Pith;
import com.srch2.data.connector.indexed.IndexedConnector;

public class BroadcastRecieverObserver extends IncrementalObserver {

	IntentFilter contentChangedIntentFilter;
	
	public BroadcastRecieverObserver(Context contxt, Object dataContentListenerHandle, IndexedConnector indexConnector) {
		super(contxt, indexConnector);
		try {
			contentChangedIntentFilter = (IntentFilter) dataContentListenerHandle;
		} catch (ClassCastException cce) {
			contentChangedIntentFilter = null;
			Pith.reportExceptionSilently(cce);
		}
	}

	@Override
	protected void registerObserver() {
		if (!isRegistered) {
			isRegistered = true;
			context.registerReceiver(reciever, contentChangedIntentFilter);
		}
	}

	@Override
	protected void unregisterObserver() {
		if (isRegistered) {
			isRegistered = false;
			context.unregisterReceiver(reciever);
		}
	}
	
	private final BroadcastReceiver reciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			onChange();
		}
	};
}
