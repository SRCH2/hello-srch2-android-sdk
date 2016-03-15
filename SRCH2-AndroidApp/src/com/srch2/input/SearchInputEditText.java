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
package com.srch2.input;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.srch2.R;

public class SearchInputEditText extends EditText implements TextWatcher {

	
	//private Handler textInputPipeLineHandler;
	
    private Drawable clearButton = null;
   // private String lastQueryInputText = "";
	private SearchInputObserver onSearchInputtedObserver;
    
	//private SearchInputPipelineTask pipelineTask;
	
	
	public SearchInputEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); setupConstructors(context);
	}
	public SearchInputEditText(Context context, AttributeSet attrs) {
		super(context, attrs); setupConstructors(context);
	}
	public SearchInputEditText(Context context) {
		super(context); setupConstructors(context);
	}
	
    private void setupConstructors(Context context) {
    	//textInputPipeLineHandler = new Handler();
    	setClearButton(context.getResources().getDrawable(R.drawable.clear_input_icon));
    	inputBuffer.setLength(0);
    }
	
    public void setSearchInputObserver(Activity a) {
    	onSearchInputtedObserver = (SearchInputObserver) a;
    }
    
    
    

	@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { 
		inputBuffer.setLength(0);
		inputBuffer.append(s);	
	}
	
    
	
	private StringBuilder inputBuffer = new StringBuilder();
	
	@Override
	public void afterTextChanged(Editable s) {
		final String userInput = s.toString().trim();
		
		if (inputBuffer.toString().trim().equals(userInput)) {
			return;
		}
		
		onSearchInputtedObserver.onNewSearchInput(userInput);
    	
        if (s.length() > 0) {
            this.setCompoundDrawablesWithIntrinsicBounds(null, null, clearButton, null);
        } else {
            this.setCompoundDrawables(null, null, null, null);
        }
		
		
		/* Previous code to handle throttling of input generating garbage to search on - no longer necessary with thread pooling
		if (pipelineTask != null) {
			textInputPipeLineHandler.removeCallbacks(pipelineTask);
		}
    	
        if (userInput.length() > 0) {
        	pipelineTask = new SearchInputPipelineTask(userInput, onSearchInputtedObserver);
        	textInputPipeLineHandler.postDelayed(pipelineTask, 16);
        } else {
        	onSearchInputtedObserver.onNewSearchInput("");
        }
        */
	}
	
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {   }
    
    private void setClearButton(Drawable clearButton) {
        this.clearButton = clearButton;
        final SearchInputEditText _this = this;
        this.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				onSearchInputtedObserver.onSearchInputHasFocus();
                if (_this.getCompoundDrawables()[2] == null) {
                    return false;
                }
                if (event.getAction() != MotionEvent.ACTION_UP) {
                    return false;
                }
                if (event.getX() > _this.getWidth() - _this.getPaddingRight() - _this.clearButton.getIntrinsicWidth()) {
                    _this.setText("");
                    _this.setCompoundDrawables(null, null, null, null);
                }
                return false;
			}
        });
        this.addTextChangedListener(this);
    }
    
    public boolean hasRealInput() {
    	return getText().toString().trim().length() > 0;
    }
}
