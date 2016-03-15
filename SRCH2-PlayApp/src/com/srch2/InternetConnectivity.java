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
package com.srch2;

import org.acra.ACRA;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class InternetConnectivity {
	private Context context;
	
	private boolean isListeningForInternetChanges = false;
	private boolean hasInterentConnection = true;
	
	InternetConnectivityChangedObserver internetConnectivityChangedObserver;
	
	public InternetConnectivity(MainActivity contxt) {
		context = contxt;
		internetConnectivityChangedObserver = (InternetConnectivityChangedObserver) contxt;
	}

	public void registerInternetConnectivityListener() {	
		if (!isListeningForInternetChanges) {
			isListeningForInternetChanges = true;
			final IntentFilter filter = new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
			context.registerReceiver(connectivityReciever, filter);
		}
	}

	public void unregisterInternetConnectivityListener() { 
		if (isListeningForInternetChanges) {
			isListeningForInternetChanges = false;
			context.unregisterReceiver(connectivityReciever);
		}
	}
	
	private BroadcastReceiver connectivityReciever = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateInternetConnectivityAvailability(isNetworkAvailable(context));
		}
	};
	
	public final static boolean isNetworkAvailable(Context context) {
		boolean isNetwork = true;
		
		
		try {
			ConnectivityManager connec = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			
			boolean mobileIsConnected = false;
			boolean wifiIsConnected = false;
			if (connec != null) {
				NetworkInfo networkInfo = connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				
				if (networkInfo != null && networkInfo.isConnected()) {
					mobileIsConnected = true;
				} 
				
				networkInfo = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				
				if (networkInfo != null & networkInfo.isConnected()) {
					wifiIsConnected = true;
				}
			}
			
			isNetwork = mobileIsConnected || wifiIsConnected;

		} catch (Exception e) {
			Pith.handleException(e);
			isNetwork = false;
		}

	    return isNetwork;
	}
	
	private void updateInternetConnectivityAvailability(boolean isAvailable) {
		if (isAvailable != hasInterentConnection) {
			hasInterentConnection = isAvailable;
			internetConnectivityChangedObserver.onInternetConnectivityChanged(hasInterentConnection);
		} 
	}
	
	public interface InternetConnectivityChangedObserver {
		public void onInternetConnectivityChanged(boolean isAvailable);
	}
}
