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

import com.srch2.R;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class TutorialDialogFragment extends DialogFragment {

	public static final String SHARED_PREFERENCES_TUTORIAL_KEY = "tutorial";
	public static final String TUTORIAL_DIALOG_FRAGMENT_NAME = "tutorial";
	public static final String TUTORIAL_DIALOG_FRAGMENT_NAME_BOOLEAN_KEY = "tutorialb";
	
	public TutorialDialogFragment() { }
	
	private TutorialPagingAdapter autoPagingAdapter;
	
	public static boolean onLoadTutorial(Context context) {
	   	SharedPreferences prefs = ((Activity) context).getPreferences(Context.MODE_PRIVATE);
		int savedtutorialPageNumber = prefs.getInt(TutorialDialogFragment.SHARED_PREFERENCES_TUTORIAL_KEY, 0);
		
		if (savedtutorialPageNumber != -1) {
			Fragment td = ((Activity) context).getFragmentManager().findFragmentByTag(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME);
    		if (td != null) {
    			((TutorialDialogFragment) td).dismiss();
    		}
      		FragmentTransaction ft = ((Activity) context).getFragmentManager().beginTransaction();
    		Fragment tda = ((Activity) context).getFragmentManager().findFragmentByTag(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME);
    		if (tda != null) {
    			ft.remove(tda);
    		}
    	
    		TutorialDialogFragment tdf = new TutorialDialogFragment();
    		Bundle b = new Bundle();
    		b.putInt(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME, savedtutorialPageNumber);
    		b.putBoolean(TUTORIAL_DIALOG_FRAGMENT_NAME_BOOLEAN_KEY, false);

    		tdf.setArguments(b);
    		tdf.show(ft, TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME);
    		return true;
		} else {
			return false;
		}
	}
	
	public static void onLoadTutorial(Context context, boolean override) {
		Fragment td = ((Activity) context).getFragmentManager().findFragmentByTag(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME);
    	if (td != null) {
    		((TutorialDialogFragment) td).dismiss();
    	}
      	FragmentTransaction ft = ((Activity) context).getFragmentManager().beginTransaction();
    	Fragment tda = ((Activity) context).getFragmentManager().findFragmentByTag(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME);
    	if (tda != null) {
    		ft.remove(tda);
    	}
    
    	
    	TutorialDialogFragment tdf = new TutorialDialogFragment();
   
    	Bundle b = new Bundle();
    	b.putInt(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME, 0);
		b.putBoolean(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME_BOOLEAN_KEY, true);
		tdf.setArguments(b);
		
    	tdf.show(ft, TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		
		getDialog().setCancelable(true);
		getDialog().setCanceledOnTouchOutside(false);
		View v = inflater.inflate(R.layout.tutorial_fragment_dialog, container, false);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		

		
		int currentPage = 0;
		boolean isUserOpened = false;
		if (getArguments() != null) {
			currentPage = getArguments().getInt(TUTORIAL_DIALOG_FRAGMENT_NAME);
			isUserOpened = getArguments().getBoolean(TUTORIAL_DIALOG_FRAGMENT_NAME_BOOLEAN_KEY);
		}
		
		isOver = false;

		int windowWidth = getActivity().getResources().getDisplayMetrics().widthPixels;
		int windowHeight = getActivity().getResources().getDisplayMetrics().heightPixels;
		
		int targetWidth = Math.min((int) (windowWidth * .9), (int) convertDpToPixels(80 * 8));
		int targetHeight = 0;
		
		if (windowWidth >= windowHeight) {
			targetHeight = (int) (windowHeight * .9);
		} else {
			targetHeight = (int) (windowHeight * .8);
		}
		
		getView().getLayoutParams().width = targetWidth;
		getView().getLayoutParams().height = targetHeight;
		
		autoPagingAdapter = new TutorialPagingAdapter(this, targetWidth, targetHeight);

		autoPagingAdapter.isUserOpened = isUserOpened;
		
		getDialog().setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
					autoPagingAdapter.advancePage();
					return true;
				}
		        return false;
			} 
		});
		
		autoPagingAdapter.startTutorial(currentPage);
	}
	


	protected static void setTutorialNormallyOn(Context c) {
		SharedPreferences prefs = ((Activity) c).getPreferences(Context.MODE_PRIVATE);
	   	SharedPreferences.Editor editor = prefs.edit();
	   	editor.putInt(SHARED_PREFERENCES_TUTORIAL_KEY, 0);
	   	editor.commit();
	}
	
	protected static void setTutorialNormallyOff(Context c) {
		SharedPreferences prefs = ((Activity) c).getPreferences(Context.MODE_PRIVATE);
	   	SharedPreferences.Editor editor = prefs.edit();
	   	editor.putInt(SHARED_PREFERENCES_TUTORIAL_KEY, -1);
	   	editor.commit();
	}
	
	@Override
	public void onPause() {
		super.onPause();

		if (autoPagingAdapter != null) {
			//autoPagingAdapter.interuptAndStopAutoPaging();
			if (!isOver && !autoPagingAdapter.isUserOpened) {
				setTutorialNormallyOn(getActivity());
			} 
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		//if (autoPagingAdapter != null && !TutorialPagingAdapter.isAutoPaging) {
		//	autoPagingAdapter.restartAutoTrigger();
		//}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		
		
		int windowWidth = getActivity().getResources().getDisplayMetrics().widthPixels;
		int windowHeight = getActivity().getResources().getDisplayMetrics().heightPixels;
		
		int targetWidth = Math.min((int) (windowWidth * .9), (int) convertDpToPixels(80 * 8));
		int targetHeight = 0;
		
		if (windowWidth >= windowHeight) {
			targetHeight = (int) (windowHeight * .9);
		} else {
			targetHeight = (int) (windowHeight * .8);
		}
		
		getView().getLayoutParams().width = targetWidth;
		getView().getLayoutParams().height = targetHeight;
		
		
		
		autoPagingAdapter.resize(this, targetWidth, targetHeight);
		
	}
	/*
		//autoPagingAdapter.interuptAndStopAutoPaging(); 
		int currentPage = TutorialPagingAdapter.currentPageIndex;
		boolean isUserOpened = autoPagingAdapter.isUserOpened;

		
		
  
		Fragment tda = getActivity().getFragmentManager().findFragmentByTag(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME);
		if (tda != null) {
			getFragmentManager().beginTransaction().remove(tda).commitAllowingStateLoss();
			tda.
		}
		 
		
		
		
		FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
		
		TutorialDialogFragment tdf = new TutorialDialogFragment();
		Bundle b = new Bundle();
		b.putInt(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME, currentPage);
		b.putBoolean(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME_BOOLEAN_KEY, isUserOpened);
		
		tdf.setArguments(b);
		tdf.show(ft, TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME);		*/
//	}

	private static boolean isOver = false;
	

	
	
	public static void OnTutorialFinished(Activity a) {
		isOver = true;
		setTutorialNormallyOff(a);
		
  	//	FragmentTransaction ft =  a.getFragmentManager().beginTransaction();
		Fragment tda =  a.getFragmentManager().findFragmentByTag(TutorialDialogFragment.TUTORIAL_DIALOG_FRAGMENT_NAME);
		if (tda != null) {
			((DialogFragment) tda).dismiss();
		}
//		ft.commit();
		
	}
	


	public float convertPixelsToDp(float px){
	    Resources resources = getActivity().getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return dp;
	}
	
	public float convertDpToPixels(float dipValue) {
	    DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
	    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}


}
