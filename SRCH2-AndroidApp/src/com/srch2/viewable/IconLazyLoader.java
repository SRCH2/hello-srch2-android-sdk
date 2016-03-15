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
package com.srch2.viewable;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.srch2.R;
import com.srch2.viewable.result.ViewableResult;

import com.srch2.Pith;

public class IconLazyLoader extends AsyncTask<ViewableResult, Void, Bitmap> {
	
	private final WeakReference<ImageView> iv;
	public WeakReference<ViewableResult> vr;
	public WeakReference<RowIconCache> cacheMap;
	public String imageKey = null;
	
	public IconLazyLoader(ImageView ivv, RowIconCache cmap) {
		iv = new WeakReference<ImageView>(ivv);
		cacheMap = new WeakReference<RowIconCache>(cmap);
	}
	
	@Override
	protected Bitmap doInBackground(ViewableResult... params) {
		vr = new WeakReference<ViewableResult>(params[0]);
		if (vr.get().getIconCacheKey() != null) {
			imageKey = vr.get().getIconCacheKey();
		}

		Bitmap b;
		try {
			b = vr.get().getIcon(iv.get().getContext());
		} catch (NullPointerException npe) {
			Pith.handleException(npe);
			b = null;
		}
	
		return b == null ? vr.get().icon : b;
	}

	@Override
    protected void onPostExecute(Bitmap bitmap) {
		
		if (isCancelled()) {
			bitmap = null;
		}
		
        if (iv != null && bitmap != null) {
            final ImageView imageView = iv.get();
            final IconLazyLoader iller = ViewableResultsAdapter.getLazyLoadIconTask(imageView);
            if (this == iller && imageView != null) {
            	Bitmap icon = (Bitmap) imageView.getTag();
            	icon = bitmap;
            	imageView.setTag(null);
            	imageView.setTag(R.id.tag_async_icon, icon);
            	imageView.setImageBitmap(bitmap);
            	
            	if (imageKey != null) {
            		cacheMap.get().put(imageKey, bitmap);
            	}
            }
        }
    }
	
	
}
