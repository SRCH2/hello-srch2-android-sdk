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


import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.srch2.R;
import com.srch2.viewable.result.ViewableResult;

public class Row extends RelativeLayout {
	public Row(Context context) { super(context); setupConstructors(context); }
	public Row(Context context, AttributeSet attrs) { super(context, attrs); setupConstructors(context); }
	public Row(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); setupConstructors(context); }
	
	public RelativeLayout rowRoot;
	public LinearLayout rowContent;
	
	public View indicator;
	
	public ImageView icon;
	
	public TextView title;
	public TextView subTitle;

	public final static int rowButtonCount = 3;
	public SparseArray<ImageView> rowButtons;
	
	public ImageView leftHint;
	public ImageView rightHint;
	
	private void setupConstructors(Context context) {
		final LayoutInflater linflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		linflater.inflate(R.layout.row_viewable_result, this, true);
	
		rowRoot = (RelativeLayout) findViewById(R.id.rl_row_root);
		rowContent = (LinearLayout) findViewById(R.id.ll_row_content_container);
		
		indicator = findViewById(R.id.v_row_colorCodeIndicator);
		icon = (ImageView) findViewById(R.id.iv_row_icon);
		title = (TextView) findViewById(R.id.tv_row_title);
	
		rowButtons = new SparseArray<ImageView>(rowButtonCount);
		rowButtons.put(R.id.iv_row_special_button_1, (ImageView) findViewById(R.id.iv_row_special_button_1));
		rowButtons.put(R.id.iv_row_special_button_2, (ImageView) findViewById(R.id.iv_row_special_button_2));
		rowButtons.put(R.id.iv_row_special_button_3, (ImageView) findViewById(R.id.iv_row_special_button_3));
		
		leftHint = (ImageView) findViewById(R.id.iv_row_left_hint);
		rightHint = (ImageView) findViewById(R.id.iv_row_right_hint);
	}
	
	public void setTouchControllers(TouchController tc) { 
		rowRoot.setOnTouchListener(tc);
		for (int i = 0; i < rowButtonCount; i++) {
			rowButtons.get(rowButtons.keyAt(i)).setOnClickListener(tc);
		}
	}
	
	public void bindViewableResult(ViewableResult vr) {
		setTag(R.id.tag_viewable_result, vr);
		
		indicator.setBackgroundColor(vr.indicatorColor);
		
		icon.setImageBitmap(vr.icon);
		
		if (vr.hasHighlightableText()) {
			title.setText(Html.fromHtml(vr.highlightedTitle));
		} else {
			title.setText(vr.rawTitle);
		}

		if (vr.hasRowButtons()) {
			for (int i = 0; i < rowButtonCount; i++) {
				final int sparseKey = rowButtons.keyAt(i);
				ImageView rb = rowButtons.get(sparseKey);
				rb.setVisibility(View.GONE);
				for (int j = 0; j < rowButtonCount; j++) {
					final int otherSparseKey = vr.rowButtonMap.keyAt(j);
					if (otherSparseKey == sparseKey) {
						if (j == 0) {
							leftHint.setImageBitmap(vr.rowButtonMap.get(sparseKey));
						} else if (j == 1) {
							rightHint.setImageBitmap(vr.rowButtonMap.get(sparseKey));
						}
						
						rb.setVisibility(View.VISIBLE);
						rb.setTag(R.id.tag_viewable_result, vr);
						rb.setImageBitmap(vr.rowButtonMap.get(sparseKey));
						break;
					}
				}
			}
		} 
		
		rowRoot.setTag(R.id.tag_viewable_result, vr);
		rowRoot.setTag(R.id.tag_row_content, rowContent);
	}
}
