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
package com.srch2.tutorial;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import com.srch2.R;

import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;

public class TutorialPagingAdapter extends PagerAdapter implements OnPageChangeListener {

	private TutorialFinishedObserver hostingDialogFragment;
	private int dialogWidth = 0;
	private int dialogHeight = 0;
	
	public boolean isUserOpened = false;
	
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

	private Context context;
	
	private static ArrayList<View> pages = null;
	
//	private AutoFrameTrigger autoFrameTrigger = null;
//	protected volatile static boolean isAutoPaging = false;
//	protected final static int default_frame_duration_ms = 3500;
//	protected static int frame_duration_ms;
	
	protected ViewPager tutorialPager;
	protected static int currentPageIndex = 0;

	private HashMap<ImageView, Bitmap> pages_cache = new HashMap<ImageView, Bitmap>();

	private static boolean isPortrait = true;

	private static final int[] page_layouts = new int[] { 
											R.layout.tutorial_page_blank, 
											R.layout.tutorial_page_welcome, 
											R.layout.tutorial_page_click_here_to_select_catagories, 
											R.layout.tutorial_page_featuring_menu, 
											R.layout.tutorial_page_rearrange, 
											R.layout.tutorial_page_deselecting, 
											R.layout.tutorial_page_contact_swipe, 
											R.layout.tutorial_page_out 
														};
	
	public void setFrameDuration(boolean speedUp) {
		
		
		/*
		if (speedUp && frame_duration_ms != 250) {
			frame_duration_ms = 250;
			autoFrameTrigger.changeFrameRate();
		} */
	} 
	
	public void restartAutoTrigger() {
		/*
		frame_duration_ms = default_frame_duration_ms;
		autoFrameTrigger.startAutoPaging();
		*/
	}

	public TutorialPagingAdapter(DialogFragment df, int w, int h) {
		isPortrait = h > w;
		
		dialogWidth = w;
		dialogHeight = h;
		
		context = df.getActivity();
		hostingDialogFragment = (TutorialFinishedObserver) df.getActivity();
	
		pages = new ArrayList<View>(page_layouts.length);
		
		final LayoutInflater li = (LayoutInflater) df.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for (int i = 0; i < page_layouts.length; i++) {
			View v = li.inflate(page_layouts[i], null);
			v.setTag(i);
			pages.add(v);
		}
		
		tutorialPager = (ViewPager) df.getView().findViewById(R.id.vp_tutorial);
//		tutorialPager.setOnTouchListener(this);
		tutorialPager.setOnPageChangeListener(this);
		tutorialPager.setAdapter(this);
	}
	
	public void resize(DialogFragment df, int w, int h) {
		int cp = currentPageIndex;
		tutorialPager.setAdapter(new TutorialPagingAdapter(df, w, h));
		startTutorial(cp);
	}
	
	public void startTutorial(int pageToStartOn) {
		if (pageToStartOn < 1) {
			pageToStartOn = 1;
		}
		currentPageIndex = pageToStartOn;
		tutorialPager.setCurrentItem(currentPageIndex);
		if (pageToStartOn == 1) {
			startTutorial(true);
		} else {
			startTutorial(false);
		}
	}
	
	private void startTutorial(boolean startFromBeginning) {
		tutorialPager.setEnabled(true);
//		frame_duration_ms = default_frame_duration_ms;
		if (startFromBeginning) {
			tutorialPager.setEnabled(false);
			currentPageIndex = 1;
			isFirstTime = true;
		}
//		autoFrameTrigger = new AutoFrameTrigger(this, true);
	}


	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		container.removeView(pages.get(position));
	}

	@Override 
	public int getItemPosition(Object object) {
		final int ip = pages.indexOf(object);
		return ip == -1 ? POSITION_NONE : ip;
	}

	public static class Pages {
		public static final int WELCOME = 1;
		public static final int CLICK_HERE = 2;
		public static final int FEATURING_OPEN_MENU = 3;
		public static final int REARRANGE = 4;
		public static final int DESELECT = 5;
		public static final int SWIPE = 6;
		public static final int OUT = 7;
	}
	
	private final static int[][] menuIconIds = new int[][] {
			{
				R.drawable.menu_icon_contacts_red, 
				R.drawable.menu_icon_sms_red, 
				R.drawable.menu_icon_calendar_red, 
				R.drawable.menu_icon_installed_apps_red, 
				R.drawable.menu_icon_music_red, 
				R.drawable.menu_icon_video_red, 
				R.drawable.menu_icon_images_red, 
				R.drawable.menu_icon_web_search_red },
			{
					R.drawable.menu_icon_contacts_grey, 
					R.drawable.menu_icon_sms_grey,  
					R.drawable.menu_icon_calendar_grey, 
					R.drawable.menu_icon_installed_apps_grey,  
					R.drawable.menu_icon_music_grey,  
					R.drawable.menu_icon_video_grey, 
					R.drawable.menu_icon_images_grey,  
					R.drawable.menu_icon_web_search_grey },
	}; 
	
	private final static int[] menuFeaturingIconIds = new int[]
		
		{
			R.drawable.menu_icon_contacts_red, 
			R.drawable.menu_icon_sms_red, 
			R.drawable.menu_icon_calendar_red, 
			R.drawable.menu_icon_installed_apps_red, 
			R.drawable.menu_icon_music_red, 
			R.drawable.menu_icon_video_red, 
			R.drawable.menu_icon_images_red, 
			R.drawable.menu_icon_web_search_red 
		};

	private final static int[] menuRearrganeIconIds = new int[] 
		{
			R.drawable.menu_icon_contacts_red, 
			R.drawable.menu_icon_sms_red, 
			R.drawable.menu_icon_calendar_red, 
			R.drawable.menu_icon_installed_apps_red, 
			R.drawable.menu_icon_web_search_red,
			R.drawable.menu_icon_video_red, 
			R.drawable.menu_icon_images_red, 
			R.drawable.menu_icon_music_red, 
		};
		
	private final static int[] menuDeselectIconIds = new int[] 
		{
			R.drawable.menu_icon_contacts_red, 
			R.drawable.menu_icon_sms_grey, 
			R.drawable.menu_icon_calendar_grey, 
			R.drawable.menu_icon_installed_apps_red, 
			R.drawable.menu_icon_web_search_red, 
			R.drawable.menu_icon_video_grey, 
			R.drawable.menu_icon_images_red, 
			R.drawable.menu_icon_music_red 
		};
	
	boolean isFirstTime = true;
	
	protected void advancePage() {
		if (currentPageIndex < Pages.OUT) {
			tutorialPager.setCurrentItem(currentPageIndex + 1);
		}
	}
	
	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		View v = pages.get(position);
		
		if (v.getParent() != null) {
			LayoutInflater le = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View vv = le.inflate(page_layouts[position], null);
			vv.setTag(position);
			pages.set(position, vv);
			v = null;
			v = vv;
		}
		
		final int pageIndex = (Integer) v.getTag();
		
		if (pageIndex == Pages.WELCOME) {
			ImageView welcomeIV = (ImageView) v.findViewById(R.id.iv_tutorial_welcome);

			int boxSize = dialogWidth > dialogHeight ? dialogHeight : dialogHeight;
			boxSize *= .7;
			
			welcomeIV.getLayoutParams().width = boxSize;
			welcomeIV.getLayoutParams().height = boxSize; 
			
			Bitmap b = getSubSampledBitmap(context, R.drawable.srch2_logo_flat, dialogWidth, dialogHeight);
			
			addBitmapToCache(welcomeIV, b);
			welcomeIV.setImageBitmap(b);
			
			if (!isUserOpened) {
				welcomeIV.setAnimation(new FadeAlphaAnimation(1));
			} else {
				LinearLayout welcomeLL = (LinearLayout) v.findViewById(R.id.ll_welcome_to_srch2);
				welcomeLL.setVisibility(View.VISIBLE);
			}
	
			if (!isPortrait) {
				ImageView swipeHint = (ImageView) v.findViewById(R.id.iv_tutorial_swipe_hint);
				((RelativeLayout.LayoutParams) swipeHint.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				((RelativeLayout.LayoutParams) swipeHint.getLayoutParams()).rightMargin = (int) convertDpToPixels(20);
			}
			
		} else if (pageIndex == Pages.FEATURING_OPEN_MENU || pageIndex == Pages.DESELECT || pageIndex == Pages.REARRANGE) {
			int[] icons = null;
			switch (pageIndex) {
				case Pages.FEATURING_OPEN_MENU:
					icons = menuFeaturingIconIds;
					
					LinearLayout featuringCategoriesContainer = null;
					
					if (isPortrait) {
						featuringCategoriesContainer = (LinearLayout) v.findViewById(R.id.ll_tutorial_featuring_categories_container_portrait);
						featuringCategoriesContainer.setVisibility(View.VISIBLE);
					} else {
						featuringCategoriesContainer = (LinearLayout) v.findViewById(R.id.ll_tutorial_featuring_categories_container_landscape);
						featuringCategoriesContainer.setVisibility(View.VISIBLE);
					}
						
					break;
				case Pages.REARRANGE:
					icons = menuRearrganeIconIds;

					break;
				case Pages.DESELECT:
					icons = menuDeselectIconIds;
					break;
			}

			LinearLayout r = (LinearLayout) v.findViewById(R.id.ll_tutorial_category_menu);
		
			int totalCells = 8;
			float idealMax = context.getResources().getDimension(R.dimen.draggableGridView_maximumCellSize);
			float idealMin = context.getResources().getDimension(R.dimen.draggableGridView_minimumCellSize);
			
			float fudgeFactor = isPortrait ? .98f : .99f;
			float sourceWidth = (float) (dialogWidth * fudgeFactor);
			
			float singleRowIdealMaxTotalWidth = idealMax * totalCells;
			float singleRowIdealMinTotalWidth = idealMin * totalCells;
			int newCellSize = (int) idealMin;
			
			
			
			boolean needsTwoRows = false;
			float scaleFactor = 1;
			if (sourceWidth > singleRowIdealMinTotalWidth) {
				scaleFactor = (sourceWidth / singleRowIdealMaxTotalWidth);
				scaleFactor = scaleFactor > 1 ? 1 : scaleFactor;
				newCellSize = (int) (scaleFactor * idealMax);
			} else {
				needsTwoRows = true;
				scaleFactor = (sourceWidth / (singleRowIdealMaxTotalWidth / 2));
				scaleFactor = scaleFactor > 1 ? 1 : scaleFactor;
				newCellSize = (int) (scaleFactor * idealMax);
			}
			
			if (!needsTwoRows) {
				r.getLayoutParams().height = newCellSize;

				LinearLayout.LayoutParams lps = null;
				for (int i = 0; i < 8; i++) {
					ImageView iv = new ImageView(context);
					lps = new LayoutParams(newCellSize, newCellSize);
					iv.setImageResource(icons[i]);
					iv.setScaleType(ScaleType.FIT_CENTER);
					iv.setEnabled(false);
					r.addView(iv, lps);
				}
			} else {
				r.getLayoutParams().height = newCellSize * 2;
				r.setOrientation(LinearLayout.VERTICAL);

				LinearLayout.LayoutParams lps;
				for (int i = 0; i < 2; ++i) {
					LinearLayout ll = new LinearLayout(context);
					LinearLayout.LayoutParams llps = new LayoutParams(LayoutParams.WRAP_CONTENT, newCellSize);
					ll.setOrientation(LinearLayout.HORIZONTAL);
				
					for (int j = 0; j < 4; ++j) { 
						ImageView iv = new ImageView(context);
						lps = new LayoutParams(newCellSize, newCellSize);
						iv.setImageResource(icons[(i * 4) + j]);
						iv.setScaleType(ScaleType.FIT_CENTER);
						iv.setEnabled(false);
						ll.addView(iv, lps);
					}
					
					r.addView(ll, llps);
				}
			}
			
			if (pageIndex == Pages.REARRANGE) {
				
				ImageView arrowRearrangeIV = (ImageView) v.findViewById(R.id.iv_tutorial_rearrange_arrow);
			
				if (needsTwoRows) {
					View sv = v.findViewById(R.id.v_tutorial_arrow_rearrange_spacer);
					sv.setVisibility(View.GONE);
					arrowRearrangeIV.setScaleType(ScaleType.CENTER_INSIDE);
				} 
			}
		} else if (pageIndex == Pages.SWIPE) {
			EditText et = (EditText) v.findViewById(R.id.et_search_input);
			et.setText(Html.fromHtml("marty <u>mc</u>"));
	
			TextView tv = (TextView) v.findViewById(R.id.tv_row_title);
			tv.setText(Html.fromHtml("<font color='red'><b>Marty Mc</b></font>Fly"));
		} else if (pageIndex == Pages.OUT) {
			ImageView iv = (ImageView) v.findViewById(R.id.iv_tutorial_out);
			
			int boxSize = dialogWidth > dialogHeight ? dialogHeight : dialogHeight;
			boxSize *= .7;
			
			iv.getLayoutParams().width = boxSize;
			iv.getLayoutParams().height = boxSize; 
			
			Bitmap b = getSubSampledBitmap(iv.getContext(), R.drawable.srch2_logo_full, dialogWidth, dialogHeight);
			iv.setImageBitmap(b);

			addBitmapToCache(iv, b);
		}
		container.addView(v);
		return v;
	}
	
	private class DelayedStartAnimationRunnable implements Runnable {
		private WeakReference<ImageView> ivRef;
		
		public DelayedStartAnimationRunnable(ImageView iv) {
			ivRef = new WeakReference<ImageView>(iv);
		}
		
		@Override
		public void run() {
			ImageView iv = ivRef.get();
			if (iv != null) {
				if (!isUserOpened) {
					ivRef.get().startAnimation(new FadeAlphaAnimation(1, 0));
				} else {
					ivRef.get().startAnimation(new FadeAlphaAnimation(1, 0, 400));
				}
			}
		}
	}
	
	private Bitmap getSubSampledBitmap(Context context, int resourceId, int targetWidth, int targetHeight) {
	    BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;  
	    BitmapFactory.decodeResource(context.getResources(), resourceId, options);
	    int sampleSize = 1;
	    int inMemorySize_argb8888 = (options.outHeight * options.outWidth * 8) / 1;
	    
	    int maxInMemorySize = 4000000;
	    if (context.getResources().getDisplayMetrics().densityDpi >= DisplayMetrics.DENSITY_HIGH) {
	    	maxInMemorySize = 10000000;
	    }
	    while (inMemorySize_argb8888 > maxInMemorySize) {
	    	inMemorySize_argb8888 = (options.outHeight * options.outWidth * 8) / ++sampleSize;	    	
	    }
	    options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = false; 

	    options.inSampleSize = sampleSize;
	    return BitmapFactory.decodeResource(context.getResources(), resourceId, options);
	}
	
	@Override
	public int getCount() {
		return pages.size();
	}

	@Override
	public boolean isViewFromObject(View v, Object o) {
		return v == o;
	}
	
	/*
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (isAutoPaging) {
			interuptAndStopAutoPaging();
			return true; 
		} else {
			if (currentPageIndex > 0 && currentPageIndex < 8) {
				v.removeCallbacks(DelayedRestartAutoPagingRunnable);
				v.postDelayed(DelayedRestartAutoPagingRunnable, 2000);
			}
		}
		return false;
	}
	private Runnable DelayedRestartAutoPagingRunnable = new Runnable() {
		@Override
		public void run() {
			startAutoPaging();
		}
	};
	*/
	
	
	
	@Override public void onPageScrollStateChanged(int arg0) { }
	@Override public void onPageScrolled(int arg0, float arg1, int arg2) { }

	@Override
	public void onPageSelected(int pos) {
		
	
		currentPageIndex = pos;
		
		tutorialPager.setEnabled(pos > 0 && pos < 8);

		if (currentPageIndex == pages.size() - 1) {
			View v = pages.get(pos);
			ImageView iv = (ImageView) v.findViewById(R.id.iv_tutorial_out);
			if (!isUserOpened) {
				iv.postDelayed(new DelayedStartAnimationRunnable(iv), 200);
			} else {
				iv.postDelayed(new DelayedStartAnimationRunnable(iv), 50);
			}
		} else if (currentPageIndex == 0) {
			tutorialPager.setCurrentItem(1, false);
		} 
	}
	
	/*
	protected void interuptAndStopAutoPaging() {
		autoFrameTrigger.stopAutoPaging();
	}
	
	protected void startAutoPaging() {
		autoFrameTrigger.startAutoPaging();
	}

	private static class AutoFrameTrigger extends Handler {
		private WeakReference<TutorialPagingAdapter> tutorialPager;
		
		protected AutoFrameTrigger(TutorialPagingAdapter tutPager, boolean startAutoPaging) {
			tutorialPager = new WeakReference<TutorialPagingAdapter>(tutPager);
			startAutoPaging();
		}
		
		protected void startAutoPaging() {
			isAutoPaging = true;
			if (currentPageIndex == 0) {
				this.postDelayed(intervalDelay, 400);
			} else {
				this.postDelayed(intervalDelay, frame_duration_ms);
			}
		}
	
		protected void stopAutoPaging() {
			isAutoPaging = false;
			this.removeCallbacks(intervalDelay);
		}
		
		protected void changeFrameRate() {
			stopAutoPaging();
			startAutoPaging();
		}
		
		protected void onNextFrame() {
			if (isAutoPaging) {
				++currentPageIndex;
				tutorialPager.get().tutorialPager.setCurrentItem(currentPageIndex);
				if (currentPageIndex == pages.size()) {
					isAutoPaging = false;
					stopAutoPaging();
				} 
			} 
		}

		private Runnable intervalDelay = new Runnable() {
			@Override
			public void run() {
				postDelayed(intervalDelay, frame_duration_ms);
				onNextFrame();
			}
		};
		
	}
	*/
	
	private class FadeAlphaAnimation extends AlphaAnimation {
		public FadeAlphaAnimation(int which) {
			super(0, 1);
			setDuration(650);

			setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationEnd(Animation arg0) {
					View v = pages.get(1);
					LinearLayout welcomeLL = (LinearLayout) v.findViewById(R.id.ll_welcome_to_srch2);
					welcomeLL.setVisibility(View.VISIBLE);
				}
  
				@Override
				public void onAnimationRepeat(Animation animation) { }

				@Override
				public void onAnimationStart(Animation animation) {	}	
			});
		}
		
		public FadeAlphaAnimation(int a, int b) {
			super(1, 0);
			
			setDuration(2000);
			setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationEnd(Animation arg0) {
					View v = pages.get(pages.size() - 1);
					ImageView ivb = (ImageView) v.findViewById(R.id.iv_tutorial_out);
					
					Bitmap ivbit = ivb.getDrawingCache();
					if (ivbit != null) {
						ivbit.recycle();
					}
					ivb.setImageResource(android.R.color.transparent);
				
					TextView tv = (TextView) v.findViewById(R.id.tv_tutorial_out);
					tv.setVisibility(View.VISIBLE);
					tv.postDelayed(r, 1500);
				}

				@Override
				public void onAnimationRepeat(Animation animation) { }

				@Override
				public void onAnimationStart(Animation animation) { }	
			});
		}
		
		public FadeAlphaAnimation(int a, int b, int duration) {
			super(1, 0);
			
			setDuration(duration);
			setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationEnd(Animation arg0) {
					View v = pages.get(pages.size() - 1);
					ImageView ivb = (ImageView) v.findViewById(R.id.iv_tutorial_out);
					
					Bitmap ivbit = ivb.getDrawingCache();
					if (ivbit != null) {
						ivbit.recycle();
					}
					ivb.setImageResource(android.R.color.transparent);
				
					TextView tv = (TextView) v.findViewById(R.id.tv_tutorial_out);
					tv.setVisibility(View.VISIBLE);
					tv.postDelayed(r, 1500);
				}

				@Override
				public void onAnimationRepeat(Animation animation) { }

				@Override
				public void onAnimationStart(Animation animation) { }	
			});
		}
	}
	
	public void addBitmapToCache(ImageView key, Bitmap b) {
		if (pages_cache.containsKey(key)) {
			Bitmap pb = pages_cache.get(key);
			if (pb != null) {
				pb.recycle();
			}
		}
		pages_cache.put(key, b);
	}
	
	public void clearBitmapCache(ImageView key) {
		if (pages_cache.containsKey(key)) {
			Bitmap pb = pages_cache.get(key);
			if (pb != null) {
				pb.recycle();
			}
		}
	}
	
	public void clearAllBitmapCache() {
		for (ImageView i : pages_cache.keySet()) {
			
			Bitmap pb = pages_cache.get(i);
			i.setImageResource(android.R.color.transparent);

			if (pb != null) {
				pb.recycle();
			}
		}
	}
	
	private Runnable r = new Runnable() {
		@Override
		public void run() {
			onTutorialFinished();
		}
	};
	
	protected void onTutorialFinished() {
		clearAllBitmapCache();
		for (View v : pages) {
			v = null;
		}
		System.gc();
		hostingDialogFragment.onTutorialFinished(isUserOpened);
	}
}
