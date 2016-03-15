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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.srch2.R;
import com.srch2.viewable.list.TouchController;
import com.srch2.viewable.result.ViewableResult;

import com.srch2.Pith;

public class ViewableResultsAdapter extends BaseAdapter implements OnTouchListener, OnClickListener {

	private ArrayList<ViewableResult> viewableResults;
	private LayoutInflater linflater;
	
	private ViewableResultFocusObserver viewableResultFocusObserver;
	private int actionEvent;

	private boolean hasNoInputYet = true;
	
	
	private static class GooeyHandler extends Handler {
		WeakReference<ViewableResultsAdapter> thisVra;
		
		public GooeyHandler(ViewableResultsAdapter vra) {
			thisVra = new WeakReference<ViewableResultsAdapter>(vra);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == LOAD_QUICK_SCREEN) {
				ArrayList<ViewableResult> newResults = null;
				
				try {
					newResults = (ArrayList<ViewableResult>) msg.obj;
				} catch (ClassCastException cce) {
					Pith.handleException(cce);
					newResults = new ArrayList<ViewableResult>(0);
				}
				
				thisVra.get().onNewViewableResultsToPublish(newResults, 10);
			} else {
				super.handleMessage(msg);
			}
		}	
	}
	
	public void sendQuickLoadMessageUpdate(ArrayList<ViewableResult> vras) {
		if (gooeyHandler != null) {
			gooeyHandler.sendMessage(gooeyHandler.obtainMessage(LOAD_QUICK_SCREEN, vras));			
		}
	}
	
	private static final int LOAD_QUICK_SCREEN = 100303;
	private GooeyHandler gooeyHandler;
	
	public ViewableResultsAdapter(Context contxt) { 
		viewableResultFocusObserver = (ViewableResultFocusObserver) contxt;
		linflater = (LayoutInflater) contxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		viewableResults = new ArrayList<ViewableResult>(30);
		maximumSwipeThreshold = convertDpToPixels(contxt, 100);
		
		rowTouchController = new TouchController(contxt);
		gooeyHandler = new GooeyHandler(this);
	}
	
	public void onNewViewableResultsToPublish(ArrayList<ViewableResult> newResults) {
		hasNoInputYet = false;
		viewableResults = newResults;
		notifyDataSetChanged();
	}
	
	

	public void onNewViewableResultsToPublish(ArrayList<ViewableResult> newResults, int i) {
		if (hasNoInputYet) {
			viewableResults = newResults;
			notifyDataSetChanged();
		}
	}
	
	@Override
	public int getCount() {
		return viewableResults.size();
	}

	@Override
	public ViewableResult getItem(int position) {
		return viewableResults.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	private static final int[] specialButtonIds = { R.id.iv_row_special_button_1, R.id.iv_row_special_button_2, R.id.iv_row_special_button_3 };
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		final ViewableResult thisViewableResult = getItem(position);
		
		ViewHolder viewHolder = null;
		if (convertView != null) {
			viewHolder = (ViewHolder) convertView.getTag(R.id.tag_viewholder);
		} else {
			convertView = linflater.inflate(R.layout.row_viewable_result, parent, false);

			RelativeLayout rowRoot = (RelativeLayout) convertView.findViewById(R.id.rl_row_root);

			View indicator = (View) convertView.findViewById(R.id.v_row_colorCodeIndicator);
			TextView tv = (TextView) convertView.findViewById(R.id.tv_row_title);
			ImageView icon = (ImageView) convertView.findViewById(R.id.iv_row_icon);

			ImageView callSpecialButton = (ImageView) convertView.findViewById(R.id.iv_row_special_button_1);
			ImageView smsSpecialButton = (ImageView) convertView.findViewById(R.id.iv_row_special_button_2);
			ImageView emailSpecialButton = (ImageView) convertView.findViewById(R.id.iv_row_special_button_3);
			
			ImageView leftHint = (ImageView) convertView.findViewById(R.id.iv_row_left_hint);
			ImageView rightHint = (ImageView) convertView.findViewById(R.id.iv_row_right_hint);
		
			HashMap<Integer, ImageView> specialButtons = new HashMap<Integer, ImageView>(3);
			specialButtons.put(R.id.iv_row_special_button_1, callSpecialButton);
			specialButtons.put(R.id.iv_row_special_button_2, smsSpecialButton);
			specialButtons.put(R.id.iv_row_special_button_3, emailSpecialButton);

			viewHolder = new ViewHolder();
			viewHolder.rowRoot = rowRoot;
			viewHolder.indicator = indicator;
			viewHolder.icon = icon;
			viewHolder.title = tv;
			viewHolder.specialFunctionButtons = specialButtons;
			viewHolder.leftHint = leftHint;
			viewHolder.rightHint = rightHint;
			
			convertView.setTag(R.id.tag_viewholder, viewHolder);
		}
		viewHolder.leftHint.setVisibility(View.GONE);
		viewHolder.rightHint.setVisibility(View.GONE);
		
		
		viewHolder.rowRoot.setTag(R.id.tag_viewable_result, thisViewableResult);
		viewHolder.rowRoot.setOnTouchListener(rowTouchController);
		viewHolder.rowRoot.setTag(R.id.tag_row_content, convertView.findViewById(R.id.ll_row_content_container));
		
		
		for (ImageView specialButton : viewHolder.specialFunctionButtons.values()) {
			specialButton.setVisibility(View.GONE);
		}
		
		if (thisViewableResult.hasRowButtons()) {
			for (Integer specialButtonViewId : specialButtonIds) {
				ImageView specialButton;
				for (int i = 0; i < 3; i++) {
					
					
					int sparseKey = thisViewableResult.rowButtonMap.keyAt(i);
					
		
					
					
					if (sparseKey == specialButtonViewId) {
						
						if (i == 0) {
							viewHolder.leftHint.setVisibility(View.VISIBLE);
							viewHolder.leftHint.setImageBitmap(thisViewableResult.rowButtonMap.get(sparseKey));
							
						} else if (i == 1) {
							viewHolder.rightHint.setVisibility(View.VISIBLE);
							viewHolder.rightHint.setImageBitmap(thisViewableResult.rowButtonMap.get(sparseKey));
							
						}
						
						specialButton = viewHolder.specialFunctionButtons.get(specialButtonViewId);
						specialButton.setVisibility(View.VISIBLE);
						specialButton.setTag(R.id.tag_viewable_result, thisViewableResult);
						specialButton.setOnClickListener(this);
					}
				}
			}
		}


		if (!thisViewableResult.highlightedTitle.equals(ViewableResult.INVALID_DISPLAY_TEXT)) {
			viewHolder.title.setText(Html.fromHtml(thisViewableResult.highlightedTitle));
		} else {
			viewHolder.title.setText(thisViewableResult.rawTitle);
		}
		
	
		viewHolder.indicator.setBackgroundColor(thisViewableResult.indicatorColor);
		viewHolder.icon.setImageBitmap(thisViewableResult.icon);

		if (thisViewableResult.iconKey != null) {
			Bitmap bicon = refluxIconCache(thisViewableResult, viewHolder.icon);
			if (bicon != null) {
				viewHolder.icon.setImageBitmap(bicon);
			}
		} 
        
		return convertView;
	}
	
	
	
	
	
	
	
	private TouchController rowTouchController;
	
	
	
	
	
	
	private RowIconCache rowIconCache = new RowIconCache();

	
	public Bitmap refluxIconCache(ViewableResult vr, ImageView iv) {
		Bitmap k = null;
		if (rowIconCache.containsKey(vr.getIconCacheKey())) {
			k = rowIconCache.get(vr.getIconCacheKey());
		} else {
			lazyLoadRowIcon(vr, iv);
		}
		return k;
	}

	private void lazyLoadRowIcon(ViewableResult vr, ImageView imageView) {
		if (cancelLazyLoadIconTask(vr, imageView)) {
			imageView.setTag(vr.icon);
			final IconLazyLoader ill = new IconLazyLoader(imageView, rowIconCache);
			final AsyncIcon ai = new AsyncIcon(imageView.getResources(), vr.icon, ill);
			imageView.setImageDrawable(ai);
			ill.execute(vr);
		} 
	}
	
	private static class AsyncIcon extends BitmapDrawable {
		private final WeakReference<IconLazyLoader> illWeakReference;
		
		public AsyncIcon(Resources res, Bitmap b, IconLazyLoader ill) {
			super(res, b);
			illWeakReference = new WeakReference<IconLazyLoader>(ill);
		}
		
		public IconLazyLoader getIconLazyLoaderTask() {
			return illWeakReference.get();
		}
	}
	
	private static boolean cancelLazyLoadIconTask(ViewableResult vrr, ImageView iv) {
		final IconLazyLoader ill = getLazyLoadIconTask(iv);
		
		if (ill != null && ill.vr != null) {
			final ViewableResult vr = ill.vr.get();
			if (vr != null && !vrr.equals(vr)) {
				ill.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}
	
	protected static IconLazyLoader getLazyLoadIconTask(ImageView iv) {
		if (iv != null) {
			final Drawable d = iv.getDrawable();
			if (d instanceof AsyncIcon) {
				final AsyncIcon asi = (AsyncIcon) d;
				return asi.getIconLazyLoaderTask();
			} 
		}
		return null;
	}
	
	
	
	
	
	
	
	
	
	private static class ViewHolder {
		RelativeLayout rowRoot;
		View indicator;
		TextView title;
		ImageView icon;
		HashMap<Integer, ImageView> specialFunctionButtons;
		ImageView leftHint, rightHint;
	}

	public float convertDpToPixels(Context context, float dipValue) {
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}
	
	float downX;
	float maximumSwipeThreshold;
	float swipeDisplacement;
	
	private void invalidateForSwipe(View content, float currentSwipeDisplacement) {
		content.setTranslationX(currentSwipeDisplacement);
		swipeDisplacement = currentSwipeDisplacement;
	}
	
	private void restoreOriginalState(View rowContent) {
		downX = 0;
		swipeDisplacement = 0;
		rowContent.setTranslationX(0);
	}
	
	private boolean isSwipeEventTriggered(View v) {
		if ((maximumSwipeThreshold - Math.abs(swipeDisplacement)) < 10) {
			if (swipeDisplacement < 0) {
				((ViewableResult) v.getTag(R.id.tag_viewable_result)).onSwipeLeft(v.getContext());
			} else {
				((ViewableResult) v.getTag(R.id.tag_viewable_result)).onSwipeRight(v.getContext());
			}
			return true;
		} else {
			return false;
		}
	}
	
	private void isClickEventTriggered(View v) {
		if (Math.abs(swipeDisplacement) < 10) {
			((ViewableResult) v.getTag(R.id.tag_viewable_result)).onClick(v.getContext());
		}
	}
	
	private int lastSwipeViewId = -10;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		final int actionEvent = event.getAction();
		final ViewableResult thisViewableResult = (ViewableResult) v.getTag(R.id.tag_viewable_result);

		if (thisViewableResult.isSwippable) {
			if (v.getId() != lastSwipeViewId) {
				restoreOriginalState((View) v.getTag(R.id.tag_row_content));
				lastSwipeViewId = v.getId();
			}
			
			try {
				Thread.currentThread().sleep(10);
			} catch (InterruptedException e) {
				Pith.handleException(e);
			}
			
			switch (actionEvent) {
				case MotionEvent.ACTION_DOWN:
					downX = event.getX();
					return true;
				case MotionEvent.ACTION_MOVE:
					final float x = event.getX();
					final float y = event.getY();
					
					if (y < (v.getBottom() - v.getY()) && y > (v.getTop() - v.getY())) {
						float currentDisplacement = x - downX;
						if (Math.abs(currentDisplacement) > maximumSwipeThreshold) {
							final int sign = currentDisplacement > 0 ? 1 : -1;
							currentDisplacement = maximumSwipeThreshold * sign;
						}
						invalidateForSwipe((View) v.getTag(R.id.tag_row_content), currentDisplacement);
						return true;
					}
					return false;
				case MotionEvent.ACTION_UP:
					if (!isSwipeEventTriggered(v)) {
						isClickEventTriggered(v);
					}
					restoreOriginalState((View) v.getTag(R.id.tag_row_content));
					viewableResultFocusObserver.onViewableResultHasFocus();
					return true;
				case MotionEvent.ACTION_CANCEL:
					viewableResultFocusObserver.onViewableResultHasFocus();
					restoreOriginalState((View) v.getTag(R.id.tag_row_content));
					return true;
			}
		} else {
			switch (actionEvent) {
				case MotionEvent.ACTION_DOWN:
					return true;
				case MotionEvent.ACTION_UP:
					ViewableResult vr = ((ViewableResult) v.getTag(R.id.tag_viewable_result));
					
					vr.onClick(v.getContext());
				
					viewableResultFocusObserver.onViewableResultHasFocus();
					return true;
			}
		}

		return false;
	}

	@Override
	public void onClick(View v) {
		ViewableResult vr = (ViewableResult) v.getTag(R.id.tag_viewable_result);
		vr.onRowButtonClick(v.getContext(), v.getId());
	}
}
