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

import java.io.InputStream;
import java.util.HashMap;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.srch2.MainActivity.ContextSingelton;
import com.srch2.Pith;
import com.srch2.R;
import com.srch2.data.SearchCategory;
import com.srch2.data.connector.indexed.indices.Contacts;
import com.srch2.instantsearch.QueryResult;
import com.srch2.viewable.result.DefaultResourceCache.RowButtonType;

public class ContactsViewableResult extends ViewableResult {
	private String primaryTelephone;
	private String emailAddress;
	
	public ContactsViewableResult(String searchinput, QueryResult qr) {
		super(searchinput, SearchCategory.Contacts, qr.getPrimaryKey(), qr.getInMemoryRecordData().split(delim, 3)[0], null, 
				DefaultResourceCache.getHighlightedText(qr.getInMemoryRecordData().split(delim, 3)[0], qr), DefaultResourceCache.getHighlightedText(null, qr));
		//setMetaContent(qr.getPrimaryKey(), qr.getInMemoryRecordData().split(delim, 3)[1], qr.getInMemoryRecordData().split(delim, 3)[2]);
		setMetaContent(qr.getPrimaryKey());
		
	}
	
	public ContactsViewableResult(String searchinput, String iconkey, String title, String subtitle, String htitle, String hsubtitle, String encodedmetacontent) {
		super(searchinput, SearchCategory.Contacts, iconkey, title, subtitle, htitle, hsubtitle);
		
		//String metaVals[] = encodedmetacontent.split(ViewableResult.delim, 3);
		//setMetaContent(metaVals[0]);
		setMetaContent(encodedmetacontent);
	}

	public ContactsViewableResult(String searchinput, QueryResult qr, String[] inMemoryRecordData) {
		super(searchinput, SearchCategory.Contacts, qr.getPrimaryKey(), inMemoryRecordData[0], null, 
				DefaultResourceCache.getHighlightedText(inMemoryRecordData[0], qr), DefaultResourceCache.getHighlightedText(null, qr));
		//setMetaContent(qr.getPrimaryKey(), inMemoryRecordData[1], inMemoryRecordData[2]);
		setMetaContent(qr.getPrimaryKey());
	}
	
	
	
	

	
	@Override
	public void setMetaContent(String... args) {
		try {
			primaryLookupKey = args[0];
		} catch (NullPointerException npe) {
			Pith.handleException(npe);
			primaryLookupKey = null;
		}
		
		rowButtonMap = new SparseArray<Bitmap>();

		
		ContextSingelton cs = ContextSingelton.getInstance();
		if (cs == null || primaryLookupKey == null) {
			return;
		} else {
			
			try {
				String phone = Contacts.getContactPhoneNumber(cs.context.get(), primaryLookupKey);
				if (phone != null && !phone.equals(Contacts.FLAG_NO_TELEPHONE)) {
					isSwippable = true;
					primaryTelephone = phone;
					rowButtonMap.put(R.id.iv_row_special_button_1, DefaultResourceCache.getRowButtonActionIcon(RowButtonType.ContactActionCall));
					rowButtonMap.put(R.id.iv_row_special_button_2, DefaultResourceCache.getRowButtonActionIcon(RowButtonType.ContactActionMessage));
				}
				
				String email = Contacts.getContactEmail(cs.context.get(), primaryLookupKey);
				if (email != null && !email.equals(Contacts.FLAG_NO_EMAIL)) {
					emailAddress = email;
					rowButtonMap.put(R.id.iv_row_special_button_3, DefaultResourceCache.getRowButtonActionIcon(RowButtonType.ContactActionEmail));
				}
			} catch (Exception e) {
				Pith.handleException(e);
			}
		}
		
		
		
	
		
		
		
		
		
		/*
		if (args[1] != null && !args[1].equals(Contacts.FLAG_NO_TELEPHONE)) {
			isSwippable = true;
			primaryTelephone = args[1];
			rowButtonMap.put(R.id.iv_row_special_button_1, DefaultResourceCache.getRowButtonActionIcon(RowButtonType.ContactActionCall));
			rowButtonMap.put(R.id.iv_row_special_button_2, DefaultResourceCache.getRowButtonActionIcon(RowButtonType.ContactActionMessage));
		}
		if (args[2] != null && !args[2].equals(Contacts.FLAG_NO_EMAIL)) {
			emailAddress = args[2];
			rowButtonMap.put(R.id.iv_row_special_button_3, DefaultResourceCache.getRowButtonActionIcon(RowButtonType.ContactActionEmail));
		} */
	}
	
	@Override
	protected String getEncodedMetaContentString() {
		//return primaryLookupKey + ViewableResult.delim + primaryTelephone + ViewableResult.delim + emailAddress;
		return primaryLookupKey;
		
	}

	@Override
	public Bitmap getIcon(Context context) {
		Bitmap b = null;
		Uri uri = null;
		InputStream input = null;
		try {
			uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(iconKey));
			/* ContactsContract.Contacts.openContactPhotoInputStream non-deterministically time consuming */
			input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
			b = BitmapFactory.decodeStream(input);
		} catch (Exception e) {
			Pith.handleException(e);
			b = icon;
		}
		return b;
	}
	
	@Override
	public void onClick(Context context) {
		super.onClick(context);
		
		if (rawTitle.equals(SearchCategory.getTitleIfInMemoryRecordDataCorrupted(searchCategory))) {
			Toast.makeText(context, "That's my name -- \n don't wear it out!", Toast.LENGTH_LONG).show();
			return;
		}
		
		boolean success = true;
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, primaryLookupKey));
			context.startActivity(intent); 
		} catch (Exception e) {
			Pith.reportExceptionSilently(e);
			success = false;
		}

		
		if (!success) {
			Toast.makeText(context, "Sorry, Android seems to be having trouble handling this request!", Toast.LENGTH_LONG).show();
		} 
	}	
	
	@Override
	public void onSwipeLeft(Context context) { 
		super.onSwipeLeft(context);
		
		boolean success = true;
		Intent intent = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			try {
				Pith.d("SMS!!!", "testing testing testing");
				intent = new Intent(Intent.ACTION_SENDTO);
				intent.setData(Uri.parse("smsto:" + Uri.encode(primaryLookupKey)));
				context.startActivity(intent); 
			} catch (Exception e) {
				Pith.reportExceptionSilently(e);
				success = false;
			}
		} else {
			try {
				intent = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, primaryLookupKey));
				context.startActivity(intent); 
			} catch (Exception e) {
				Pith.reportExceptionSilently(e);
				success = false;
			}
		}
		

		
		if (!success) {
			Toast.makeText(context, "Sorry, Android seems to be having trouble handling this request!", Toast.LENGTH_LONG).show();
		} 
	};
	
	@Override
	public void onSwipeRight(Context context) { 
		super.onSwipeRight(context);
		
		boolean success = true;
		try {
			context.startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + primaryTelephone)));
		} catch (Exception e) {
			success = false;
		}
		
		if (!success) {
			Toast.makeText(context, "Sorry, Android seems to be having trouble handling this request!", Toast.LENGTH_LONG).show();
		}
	};
	
	@Override
	public void onRowButtonClick(Context context, int whichVieIdWasClicked) {
		super.onRowButtonClick(context, whichVieIdWasClicked);
		
		boolean success = true;
		Intent i = null;
		
		switch (whichVieIdWasClicked) {
			case R.id.iv_row_special_button_3:
				try {
					i = new Intent(Intent.ACTION_SEND);
					i.setType("text/html");
					i.putExtra(Intent.EXTRA_EMAIL, new String[] { emailAddress });
					context.startActivity(i);
				} catch (Exception e) {
					success = false;
				}
				break;
			case R.id.iv_row_special_button_1:
				try {
					i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + primaryTelephone)); 
					context.startActivity(i);
				} catch (Exception e) {
					success = false;
				}			
				break;
			case R.id.iv_row_special_button_2:
				
				
				

	
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
					try {
						Pith.d("SMS!!!", "testing testing testing");
						i = new Intent(Intent.ACTION_SENDTO);
						i.setData(Uri.parse("smsto:" + Uri.encode(primaryLookupKey)));
						context.startActivity(i); 
					} catch (Exception e) {
						Pith.reportExceptionSilently(e);
						success = false;
					}
				} else {
					try {
						i = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, primaryLookupKey));
						context.startActivity(i); 
					} catch (Exception e) {
						Pith.reportExceptionSilently(e);
						success = false;
					}
				}

				break;
		}
		
		if (!success) {
			Toast.makeText(context, "Sorry, Android seems to be having trouble handling this request!", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o.getClass() == ContactsViewableResult.class) {
			ContactsViewableResult cvr = (ContactsViewableResult) o;
			if (super.equals(cvr)) {
				if (cvr.primaryLookupKey.equals(primaryLookupKey)) {
					if (cvr.primaryTelephone.equals(primaryTelephone)) {
						if (cvr.emailAddress.equals(emailAddress)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
