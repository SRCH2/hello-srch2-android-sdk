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

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.srch2.data.SearchCategory;
import com.srch2.data.SearchCategoryDisplayOption;
import com.srch2.viewable.ViewableResultsBuffer;

public class DraggableItemGridView extends GridView {
	
	public interface OnUserPreferencesChangedObserver {	
		public void userChangedIndexCatagoryOrder(int sourceIndex, int targetIndex);	
		public void userChangedIfIndexCatagoryDisplayed(SearchCategory whichCategory, boolean isDisplayed);	
	}
	
    private boolean webShouldBeEnabled = false;
	private boolean hasInternetAvailability = true;
	
	private Context context;
	private ImageButton toggleMenuButton;
	
	private OnUserPreferencesChangedObserver userPreferencesChangedObserver;
	
	private Adapter adapter;
	
    private Rect clippingBoundsOfGridView;

	private boolean dragEventIsHappenning = false; 
	private int indexOfSelectedDragItem = -1;        
	private int downX, downY;
    
	private BitmapDrawable bitmapOfDraggingItem;
	private Rect sourceBoundsOfDraggingItem;
	private Rect currentBoundsOfDraggingItem;


	
    private OnItemClickListener itemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int pos, long id) {
			toggleSpecificIndex(pos);
		}

	};
	
    private OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> arg0, View v, int pos, long id) {
			indexOfSelectedDragItem = pos;  
			dragEventIsHappenning = true;
			bitmapOfDraggingItem = getBitmapDrawableOfIndexItemToDrag(v);
			adapter.notifyDataSetInvalidated();
			return true;
		}
    };
   
    private Object userInteractionLock = new Object();
    
    private OnTouchListener touchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (clippingBoundsOfGridView == null) { clippingBoundsOfGridView = new Rect(getLeft(), getTop(), getRight(), getBottom()); }
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:	
					downX = (int) event.getX();
					downY = (int) event.getY();
					return false;
				case MotionEvent.ACTION_MOVE:
					if (dragEventIsHappenning) {
						synchronized (userInteractionLock) {
							if (!clippingBoundsOfGridView.contains((int) (getLeft() + event.getX()), (int) (getTop() + event.getY()))) {
								resetDraggingEvent();
								invalidate();
								break;
							} else {
								int dx = (int) event.getX() - currentBoundsOfDraggingItem.centerX();
								int dy = (int) event.getY() - currentBoundsOfDraggingItem.centerY();
								
								currentBoundsOfDraggingItem.offset(dx, dy);
								bitmapOfDraggingItem.setBounds(currentBoundsOfDraggingItem);
								invalidate();
							}
						}
					}	
					return true;
				case MotionEvent.ACTION_UP:
					if (dragEventIsHappenning) {
						synchronized (userInteractionLock) {
							int selectedItemIndexToSwapInto = pointToPosition((int) event.getX(), (int) event.getY());
					    	if (selectedItemIndexToSwapInto != -1 && selectedItemIndexToSwapInto != indexOfSelectedDragItem ) {
					    		updateSortingOrder(selectedItemIndexToSwapInto);
					    	} else {
						    	resetDraggingEvent();
					    	}
						}
					}
					return false;
			}
			return false;
		}
	};            

	public DraggableItemGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setupConstructors(context);
	}
	
	public DraggableItemGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupConstructors(context);
	}
	
	public DraggableItemGridView(Context context) {
		super(context);
		setupConstructors(context);
	}
	
	private void setupConstructors(Context context) {
		this.context = context;
	}
	
	public void setToggleMenuButtonReference(ImageButton menuButton) {
		toggleMenuButton = menuButton;
	}
	
	public void initialize(ArrayList<SearchCategoryDisplayOption> savedStateToInitialize, ViewableResultsBuffer adapterOrOtherObjectToNotifyIfPreferencesChange) {
		try {
			userPreferencesChangedObserver = (OnUserPreferencesChangedObserver) (adapterOrOtherObjectToNotifyIfPreferencesChange);
		} catch (ClassCastException e) {
			throw new ClassCastException(adapterOrOtherObjectToNotifyIfPreferencesChange.toString() + " must implement OnUserPreferencesChangedObserver");
		}

		adapter = new Adapter(savedStateToInitialize);
		
		for (SearchCategoryDisplayOption io : adapter.sortableAdapterList) {
			if (io.category == SearchCategory.Web) {
				webShouldBeEnabled = io.isDisplayed;
			}
		}
		
		setAdapter(adapter);
		setOnItemClickListener(itemClickListener);
		setOnItemLongClickListener(itemLongClickListener);
		setOnTouchListener(touchListener);
	}
	
	
	
	
	
	
	private void sizeUp() {
		
		
		
		
		
		
		
		
		clippingBoundsOfGridView = null;
		
		int viewableWidth = context.getResources().getDisplayMetrics().widthPixels;
		int viewableHeight = context.getResources().getDisplayMetrics().heightPixels;
		boolean isPortrait = viewableHeight > viewableWidth;
		
		int singleColWidth = (int) context.getResources().getDimension(R.dimen.draggableGridView_maximumCellSize);
		
		int numberOfCols = (int) (viewableWidth / singleColWidth);
		
		int numCatagories = adapter.getCount();
		
		
		int totalCells = 8;
		float idealMax = context.getResources().getDimension(R.dimen.draggableGridView_maximumCellSize);
		float idealMin = context.getResources().getDimension(R.dimen.draggableGridView_minimumCellSize);
		
		float fudgeFactor = isPortrait ? .98f : .99f;
		float sourceWidth = (float) (viewableWidth * fudgeFactor);
		
		float singleRowIdealMaxTotalWidth = idealMax * totalCells;
		float singleRowIdealMinTotalWidth = idealMin * totalCells;
		int newCellSize = (int) idealMin;
		
		

		float scaleFactor = 1;
		if (sourceWidth > singleRowIdealMinTotalWidth) {
			numberOfCols = 8;
			scaleFactor = (sourceWidth / singleRowIdealMaxTotalWidth);
			scaleFactor = scaleFactor > 1 ? 1 : scaleFactor;
			newCellSize = (int) (scaleFactor * idealMax);
		} else {
			numberOfCols = 4;
			scaleFactor = (sourceWidth / (singleRowIdealMaxTotalWidth / 2));
			scaleFactor = scaleFactor > 1 ? 1 : scaleFactor;
			newCellSize = (int) (scaleFactor * idealMax);
		}
		

		
		int totalColumnWidth = 0;
		
		if (numCatagories < numberOfCols) {
			totalColumnWidth = singleColWidth * numCatagories;
		} else {
			totalColumnWidth = singleColWidth * numberOfCols;
		}
		
		
	
		int horizontalSpacing = (viewableWidth - totalColumnWidth) / 2;
		
		
		
		setNumColumns(numberOfCols);
		setColumnWidth(newCellSize);
		
		
		
		
		
		this.getLayoutParams().width = newCellSize * numberOfCols;
		
		
		
		
		setStretchMode(GridView.NO_STRETCH);
		
		
		
		
		

		// centers the cells of the gridview
		//LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) getLayoutParams();
		//lp.leftMargin = lp.rightMargin = (int) horizontalSpacing;
		adapter.cellSquareLength = newCellSize; 
		//(int) context.getResources().getDimension(R.dimen.draggableGridView_maximumCellSize);
		
		
		
		
		
		
		
		
		
		
		
		
		
	}

	
	
	
	
	
	
	
	
	
	
	public void onPause() {
		toggleVisibility(false);
	}
	
	public void onConfigurationChange() {
		sizeUp();
	}
	
	public void toggleVisibility() {
		if (getVisibility() != View.VISIBLE) {
			toggleVisibility(true);
		} else {
			toggleVisibility(false);
		}
	}
	
	public void toggleVisibility(boolean makeVisible) {
		if (makeVisible) {
			toggleMenuButton.setImageResource(R.drawable.arrow_up);
			sizeUp();
			setVisibility(View.VISIBLE);
			resetDraggingEvent();
		} else {
			toggleMenuButton.setImageResource(R.drawable.arrow_icon);
			setVisibility(View.GONE);
		}
	} 
	
	private void updateSortingOrder(int positionOfIndexToReinsertInto) {
		SearchCategoryDisplayOption adapterToReposition = adapter.getItem(indexOfSelectedDragItem);
		adapter.sortableAdapterList.remove(indexOfSelectedDragItem);
		adapter.sortableAdapterList.add(positionOfIndexToReinsertInto, adapterToReposition);
    	userPreferencesChangedObserver.userChangedIndexCatagoryOrder(indexOfSelectedDragItem, positionOfIndexToReinsertInto);
    	resetDraggingEvent();
    }

	
    private void toggleSpecificIndex(int index) {
    	SearchCategoryDisplayOption adapterToToggleVisibility = adapter.getItem(index);
    	if (adapterToToggleVisibility.category == SearchCategory.Web){
    		webShouldBeEnabled = webShouldBeEnabled ? false : true;
    		boolean userisTogglingWebDisplay = ( adapterToToggleVisibility.isDisplayed ) ? false : true;
    		if (hasInternetAvailability) {
  				adapterToToggleVisibility.isDisplayed = userisTogglingWebDisplay;
    	    	adapter.notifyDataSetChanged();
    	      	userPreferencesChangedObserver.userChangedIfIndexCatagoryDisplayed(adapterToToggleVisibility.category, adapterToToggleVisibility.isDisplayed);
    		} else {
    			if (webShouldBeEnabled) {
    				showInternetConnectivityChangedToast(false);
    			} else {
    				showInternetConnectivityChangedToast(true);
    			}
    		}
    	} else {
    		adapterToToggleVisibility.isDisplayed = ( adapterToToggleVisibility.isDisplayed ) ? false : true;
        	adapter.notifyDataSetChanged();
          	userPreferencesChangedObserver.userChangedIfIndexCatagoryDisplayed(adapterToToggleVisibility.category, adapterToToggleVisibility.isDisplayed);
    	}
    }

	public void toggleWebEnabled(boolean isInternetAvailable) {
		synchronized (userInteractionLock) {
			hasInternetAvailability = hasInternetAvailability != isInternetAvailable ? isInternetAvailable : hasInternetAvailability;
			final int length = adapter.sortableAdapterList.size();
			for (int i = 0; i < length; i++) {
				SearchCategoryDisplayOption io = adapter.sortableAdapterList.get(i);
				if (io.category == SearchCategory.Web) {
					if (hasInternetAvailability) {
						io.isDisplayed = webShouldBeEnabled;
				   	    adapter.notifyDataSetChanged();
			    	    userPreferencesChangedObserver.userChangedIfIndexCatagoryDisplayed(io.category, io.isDisplayed);
					} else {
						if (webShouldBeEnabled) {
							showInternetConnectivityChangedToast(true);
						}
						io.isDisplayed = false;
				   	    adapter.notifyDataSetChanged();
			    	    userPreferencesChangedObserver.userChangedIfIndexCatagoryDisplayed(io.category, io.isDisplayed);
					}
				}
			} 
		}
	}
    
	
	private Toast internetIsOffToast, willResumeWebToast;
    
	private void showInternetConnectivityChangedToast(boolean internetHasStopped) {
		if (internetHasStopped) {
			if (internetIsOffToast == null) {
				internetIsOffToast = Toast.makeText(context, "Web search paused while no\ninternet connection is to be found." , Toast.LENGTH_LONG);
				internetIsOffToast.show();
			} else {
				internetIsOffToast.cancel();
				internetIsOffToast = null;
				internetIsOffToast = Toast.makeText(context, "Web search paused while no\ninternet connection is to be found." , Toast.LENGTH_LONG);
				internetIsOffToast.show();
			}
		} else {
			if (willResumeWebToast == null) {
				willResumeWebToast = Toast.makeText(context, "When internet becomes available,\nweb search will be resumed." , Toast.LENGTH_LONG);
				willResumeWebToast.show();
			} else {
				willResumeWebToast.cancel();
				willResumeWebToast = null;
				willResumeWebToast = Toast.makeText(context, "When internet becomes available,\nweb search will be resumed." , Toast.LENGTH_LONG);
				willResumeWebToast.show();
			}
		}
	}
	
	
	
	
	
	
	
    
	/// pass in object instead not view directly so you can do only icon trace
	private BitmapDrawable getBitmapDrawableOfIndexItemToDrag(View selectedItemView) {
		Bitmap bitmap = getBitmapOfSelectedIndexItem(selectedItemView);
		BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
		int t = selectedItemView.getTop();
		int l = selectedItemView.getLeft();
		int h = selectedItemView.getHeight();
		int w = selectedItemView.getWidth();
		
		sourceBoundsOfDraggingItem = new Rect(l, t, l + w, t + h);
		currentBoundsOfDraggingItem = new Rect(sourceBoundsOfDraggingItem);
		currentBoundsOfDraggingItem.offset(downX - sourceBoundsOfDraggingItem.centerX(), 
				downY - sourceBoundsOfDraggingItem.centerY());
		drawable.setBounds(currentBoundsOfDraggingItem);
		
		return drawable;
	}
	
	private Bitmap getBitmapOfSelectedIndexItem(View selectedItemView) {
		Bitmap bitmap = Bitmap.createBitmap(selectedItemView.getWidth(), selectedItemView.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		selectedItemView.draw(c);
		return bitmap;
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		
		if (bitmapOfDraggingItem != null) {
			bitmapOfDraggingItem.draw(canvas);
		}
	}

    private void resetDraggingEvent() {
    	dragEventIsHappenning = false;
    	indexOfSelectedDragItem = downX = downY = -1;
    	bitmapOfDraggingItem = null;
    	adapter.notifyDataSetChanged();
    }
 
	private class Adapter extends BaseAdapter {
		ArrayList<SearchCategoryDisplayOption> sortableAdapterList;
    	public int cellSquareLength = 1;
    	public Adapter(ArrayList<SearchCategoryDisplayOption> adaptList) {
    		sortableAdapterList = adaptList;
    		for (SearchCategoryDisplayOption io : sortableAdapterList) {
    			if (io.category == SearchCategory.Web) {
    				webShouldBeEnabled = io.isDisplayed;
    			}
    		}
    	}

		@Override
		public int getCount() {
			return sortableAdapterList.size();
		}

		@Override
		public SearchCategoryDisplayOption getItem(int position) {
			return sortableAdapterList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView iv = new ImageView(context);
			iv.setLayoutParams(new GridView.LayoutParams(cellSquareLength, cellSquareLength));
			
			
			Drawable d;
			
			if (!getItem(position).isDisplayed) {
				d = context.getResources().getDrawable(getItem(position).disableIconResourceId);
			} else {
				d = context.getResources().getDrawable(getItem(position).enabledIconResourceId);
			}
			
			d.setAlpha(255);
		
			
			if (position == indexOfSelectedDragItem) {
				if (getItem(position).isDisplayed) {
					d = context.getResources().getDrawable(getItem(position).enabledBorderlessResourceId);
				} else {
					d = context.getResources().getDrawable(getItem(position).disabledBorderlessResourceId);
				}
				d.setAlpha(155);
			}	
			
			iv.setImageDrawable(d);
			
			return iv;
		}
    }
	
	public float convertPixelsToDp(float px){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return dp;
	}
	
	public float convertDpToPixels(float dipValue) {
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}
}
