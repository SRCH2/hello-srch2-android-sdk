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

import com.srch2.R;
import com.srch2.viewable.ViewableResultFocusObserver;
import com.srch2.viewable.result.DefaultResourceCache;
import com.srch2.viewable.result.ViewableResult;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

public class TouchController implements OnTouchListener, OnClickListener {
	
	private ViewableResultFocusObserver vrfObserver;
	
	private int lastSwipeViewId = -10;
	private float downX;
	private float maximumSwipeThreshold;
	private float minimumThreshold;
	private float verticalThreshold;
	private float swipeDisplacement;
	
	public TouchController(Context context) {
		maximumSwipeThreshold = DefaultResourceCache.convertDpToPixels(context, 100);
		minimumThreshold = DefaultResourceCache.convertDpToPixels(context, 10);
		verticalThreshold = DefaultResourceCache.convertDpToPixels(context, 160);
		//(ViewableResultFocusObserver vrfobsver)
		//vrfObserver = vrfobsver;
	}
	
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
		if ((maximumSwipeThreshold - Math.abs(swipeDisplacement)) < minimumThreshold) {
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
		if (Math.abs(swipeDisplacement) < minimumThreshold) {
			((ViewableResult) v.getTag(R.id.tag_viewable_result)).onClick(v.getContext());
		}
	}
	
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
				Thread.currentThread().sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			switch (actionEvent) {
				case MotionEvent.ACTION_DOWN:
					downX = event.getX();
					v.getParent().requestDisallowInterceptTouchEvent(true);
					return true;
				case MotionEvent.ACTION_MOVE:
					final float x = event.getX();
					final float y = event.getY();
					if (y < ((v.getBottom() - v.getY()) + verticalThreshold) && y >( v.getTop() - v.getY() - verticalThreshold)) {
						float currentDisplacement = x - downX;
						if (Math.abs(currentDisplacement) > maximumSwipeThreshold) {
							final int sign = currentDisplacement > 0 ? 1 : -1;
							currentDisplacement = maximumSwipeThreshold * sign;
						}
						invalidateForSwipe((View) v.getTag(R.id.tag_row_content), currentDisplacement);
					} else {
						restoreOriginalState((View) v.getTag(R.id.tag_row_content));
					}
//					vrfObserver.onViewableResultHasFocus();
					return true;
				case MotionEvent.ACTION_UP:
					if (!isSwipeEventTriggered(v)) {
						isClickEventTriggered(v);
					}
					restoreOriginalState((View) v.getTag(R.id.tag_row_content));
				//	vrfObserver.onViewableResultHasFocus();
					return true;
			//	case MotionEvent.ACTION_CANCEL:
				//	restoreOriginalState((View) v.getTag(R.id.tag_row_content));
				//	vrfObserver.onViewableResultHasFocus();
				//	return true;
			}
		} else {
			switch (actionEvent) {
				case MotionEvent.ACTION_DOWN:
					return true;
				case MotionEvent.ACTION_UP:
					ViewableResult vr = ((ViewableResult) v.getTag(R.id.tag_viewable_result));
					
					vr.onClick(v.getContext());
				
				//	vrfObserver.onViewableResultHasFocus();
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
