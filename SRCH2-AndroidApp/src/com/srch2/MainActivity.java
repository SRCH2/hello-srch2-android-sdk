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

import java.lang.ref.WeakReference;

import org.acra.ACRA;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.srch2.InternetConnectivity.InternetConnectivityChangedObserver;
import com.srch2.data.connector.ConnectorOperator;
import com.srch2.data.index.Index;
import com.srch2.input.SearchInputEditText;
import com.srch2.input.SearchInputObserver;
import com.srch2.tutorial.TutorialDialogFragment;
import com.srch2.tutorial.TutorialFinishedObserver;
import com.srch2.viewable.ViewableResultFocusObserver;


public class MainActivity extends Activity 
							implements SearchInputObserver, ViewableResultFocusObserver, InternetConnectivityChangedObserver, TutorialFinishedObserver {

	
	private SearchInputEditText searchInputEditText;
	private ListView viewableResultsListView;
	
	private ConnectorOperator connectorOperator;
	
	private InternetConnectivity internetConnectivityActuator;
	
	private DraggableItemGridView draggableItemGridViewMenu;
	private ImageButton toggleMenuButton;
	
	
	ExitRunnable exitRunnable;
	private class ExitRunnable implements Runnable {
		private int countDown = 10;
		
		private WeakReference<TextView> countDownTextView;
		
		public ExitRunnable(TextView cdTv) {
			countDownTextView = new WeakReference<TextView>(cdTv);
		}
		
		@Override
		public void run() {
			--countDown;
			
			if (countDown == 0) {
				finish();
			} else {
				if (countDownTextView.get() != null) {
					countDownTextView.get().setText("Exiting application in " + countDown + " seconds...");
					countDownTextView.get().postDelayed(exitRunnable, 1000);
				}
				
			}
			
			
		}
		
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!Index.initializeSRCH2NativeLibrary(getFilesDir().getAbsolutePath())) {
        	setContentView(R.layout.activity_main_quick_exit);
        	
        	TextView countDownTextView = (TextView) findViewById(R.id.tv_exit_countdown);
        	exitRunnable = new ExitRunnable(countDownTextView);
        	
        	countDownTextView.postDelayed(exitRunnable, 1000);
        	return;
        }
        
       
        
        
        ContextSingelton.init(this);
        
        /*
        setContentView(R.layout.static_list_test);
 
        ArrayList<ViewableResult> vrs = new ArrayList<ViewableResult>(20);
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "jingoistic"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "sat"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "vine"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "gonothecal"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "institutes"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "unpersonified"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "contrapositive"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "stayer"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "barnacle"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "unswayable"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "feebleness"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "polyamine"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "overgrow"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "arteriotomy"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "nonratability"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "ungenerating"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "eleutherois"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "microsporangium"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "configurationist"));
        vrs.add(new MonteCarloViewableResult(SearchCategory.getRandom(), "blaspheming"));
        
        StaticListView slv = (StaticListView) findViewById(R.id.slv_results);
        ((StaticListView) slv).updateVisibleResults(vrs);
        */
        if (!TutorialDialogFragment.onLoadTutorial(this)) {
        	
        }

        
       
        
        internetConnectivityActuator = new InternetConnectivity(this);
      
        setContentView(R.layout.activity_main);
        setOnHideSoftInputListenerForAllUIViews(findViewById(R.id.ll_main_activity_rootview));
    
        searchInputEditText = (SearchInputEditText) findViewById(R.id.et_search_input);
        searchInputEditText.setSearchInputObserver(this);

        viewableResultsListView = (ListView) findViewById(R.id.ll);
        viewableResultsListView.setOnTouchListener(hideSoftInputListener);
     
        draggableItemGridViewMenu = (DraggableItemGridView) findViewById(R.id.digv_menu);
        toggleMenuButton = (ImageButton) findViewById(R.id.ib_menu);
        draggableItemGridViewMenu.setToggleMenuButtonReference(toggleMenuButton);
        toggleMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				draggableItemGridViewMenu.toggleVisibility();
			} 
		});
        
        ImageView logo = (ImageView) findViewById(R.id.iv_logo);
        logo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TutorialDialogFragment.onLoadTutorial(v.getContext(), true);
			} 
		});
        
        connectorOperator = new ConnectorOperator(this, viewableResultsListView, draggableItemGridViewMenu);
     
        
        connectorOperator.onCreate();

        
    }

	@Override
	protected void onResume() {
		super.onResume();
		
		
		
		
		
		
		
		
		if (internetConnectivityActuator != null) {
			internetConnectivityActuator.registerInternetConnectivityListener();
		}
		
		
		
		if (connectorOperator != null) {
			connectorOperator.onResume();
		}

		
	
	
	}
	
	public boolean checkIfSearchInputShouldOpenSoftKeyboard() {
		boolean success;
		
		if (!searchInputEditText.hasRealInput()) {
			searchInputEditText.requestFocus();
			searchInputEditText.postDelayed(new Runnable() {
                  @Override
                  public void run() {
              		try {
                        InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                        keyboard.showSoftInput(searchInputEditText, 0);
            		} catch (NullPointerException npe) {
            			Pith.handleException(npe);
            		}
                  }
              },200);
		}
		
		return true;
	}
	
	@Override
	protected void onPostResume() {
		super.onPostResume();
		
		if (connectorOperator != null) {
			checkIfSearchInputShouldOpenSoftKeyboard();
		}

	}

	@Override
	public void onNewSearchInput(String newInput) {
		if (newInput.length() == 0) {
			if (!searchInputEditText.hasFocus()) {
				searchInputEditText.requestFocus();
			}
		} 
		connectorOperator.onNewSearchInput(newInput);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		if (connectorOperator != null) {
			draggableItemGridViewMenu.onConfigurationChange();
			if (searchInputEditText.hasRealInput() && viewableResultsListView.getChildCount() > 0) {
				hideSoftKeyboard();
			} else {
				checkIfSearchInputShouldOpenSoftKeyboard();
			}
		}

	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		if (internetConnectivityActuator != null) {
			internetConnectivityActuator.unregisterInternetConnectivityListener();
		}
		
		if (connectorOperator != null) {
			draggableItemGridViewMenu.onPause();
			connectorOperator.onPause();
		}
		

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (connectorOperator != null) {
			
			connectorOperator.onDestroy();
			
	        ContextSingelton.dispose();
		}

	}
	
	public void setOnHideSoftInputListenerForAllUIViews(View view) {
	    if(!(view instanceof SearchInputEditText)) {
	        view.setOnTouchListener(hideSoftInputListener);
	    }
	    if (view instanceof ViewGroup) {
	        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
	            View innerView = ((ViewGroup) view).getChildAt(i);
	            setOnHideSoftInputListenerForAllUIViews(innerView);
	        }
	    }
	}

	private OnTouchListener hideSoftInputListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (v.getId() == R.id.et_search_input) {
				draggableItemGridViewMenu.toggleVisibility();
			}
			if (viewableResultsListView.getChildCount() == 0) {
				return false;
			} else {
				
				if (v.getId() == R.id.ll) {
					
					hideSoftKeyboard();
				
					
				}
				//viewableResultsListView.requestFocus();


			}
			return false;
		}
	};
	
	private void hideSoftKeyboard() {
		try {
		    InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(Activity.INPUT_METHOD_SERVICE);
		    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		} catch (NullPointerException npe) {
			Pith.handleException(npe);
		}
	}

	@Override
	public void onViewableResultHasFocus() {
		viewableResultsListView.requestFocus();
		draggableItemGridViewMenu.toggleVisibility(false);
		hideSoftKeyboard();
	}
	
	@Override
	public void onBackPressed() {
		
		
		if (connectorOperator != null) {
		    if (searchInputEditText.hasRealInput()) {
		    	searchInputEditText.setText("");
		    } else {
		    	super.onBackPressed();
		    }
		}

	}



	@Override
	public void onSearchInputHasFocus() {
		draggableItemGridViewMenu.toggleVisibility(false);
	}



	@Override
	public void onInternetConnectivityChanged(boolean isAvailable) {
		draggableItemGridViewMenu.toggleWebEnabled(isAvailable);
		connectorOperator.toggleInternetConnectivityIsAvailable(isAvailable);
	}

	@Override
	public void onTutorialFinished(boolean isUserOpened) {		
		TutorialDialogFragment.OnTutorialFinished(this);

		if (!isUserOpened) {
			Toast t = Toast.makeText(this, "Press the SRCH2 logo in the upper left\n corner to see this tutorial again.", Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER, 0, -(int) convertDpToPixels(40)); 
			t.show();
		}

		checkIfSearchInputShouldOpenSoftKeyboard();
	}
	
	public float convertDpToPixels(float dipValue) {
	    DisplayMetrics metrics = this.getResources().getDisplayMetrics();
	    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}
	
	
	
	public static class ContextSingelton {
		
		private static ContextSingelton instance;
		public WeakReference<Context> context;
		
		private ContextSingelton(Context cntxt) {
			context = new WeakReference<Context>(cntxt);
		}
		
		public static void init(Context cntxt) {
			if (instance == null) {
				instance = new ContextSingelton(cntxt);
			}
		}
		
		public static ContextSingelton getInstance() {
			return instance;
		}
		
		public static void dispose() {
			if (instance != null) {
				instance.context.clear();
				instance = null;
			}
		}
	}
}
