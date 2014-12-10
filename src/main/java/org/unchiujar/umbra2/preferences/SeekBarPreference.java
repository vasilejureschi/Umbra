/*******************************************************************************
 * This file is part of Umbra.
 *
 *     Umbra is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Umbra is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Umbra.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     Copyright (c) 2011 Vasile Jureschi <vasile.jureschi@gmail.com>.
 *     All rights reserved. This program and the accompanying materials
 *     are made available under the terms of the GNU Public License v3.0
 *     which accompanies this distribution, and is available at
 *
 *    http://www.gnu.org/licenses/gpl-3.0.html
 *
 *     Contributors:
 *        Vasile Jureschi <vasile.jureschi@gmail.com> - initial API and implementation
 *        Yen-Liang, Shen - Simplified Chinese and Traditional Chinese translations
 ******************************************************************************/


package org.unchiujar.umbra2.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import org.unchiujar.umbra2.R;

public class SeekBarPreference extends Preference implements
        OnSeekBarChangeListener {
    private final String TAG = SeekBarPreference.class.getName();

    private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
    private static final int DEFAULT_VALUE = 50;

    private int mMaxValue = 255;
    private int mCurrentValue;
    private SeekBar mSeekBar;

    private ImageView mImage;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPreference(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPreference(context, attrs);
    }

    private void initPreference(Context context, AttributeSet attrs) {
        setValuesFromXml(attrs);
        mSeekBar = new SeekBar(context, attrs);
        mSeekBar.setMax(mMaxValue);
        mSeekBar.setOnSeekBarChangeListener(this);
        Log.d(TAG, "Fog transparency seek bar initialized." + mSeekBar);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", 255);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        LayoutInflater mInflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        return mInflater.inflate(R.layout.seek_transparency,
                parent, false);

    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        Log.d(TAG, "Binding view...");
        // move our seekbar to the new view we've been given
        ViewParent oldContainer = mSeekBar.getParent();
        ViewGroup newContainer = (ViewGroup) view
                .findViewById(R.id.seekBarPrefBarContainer);

        if (oldContainer != newContainer) {
            // remove the seekbar from the old view
            if (oldContainer != null) {
                ((ViewGroup) oldContainer).removeView(mSeekBar);
            }
            // remove the existing seekbar (there may not be one) and add
            // ours
            newContainer.removeAllViews();
            newContainer.addView(mSeekBar, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        updateView(view);
    }

    /**
     * Update a SeekBarPreference view with our current state
     *
     * @param view
     */
    protected void updateView(View view) {
        Log.d(TAG, "Updating view, transparency value is " + mCurrentValue);

        RelativeLayout layout = (RelativeLayout) view;
        mImage = (ImageView) layout.findViewById(R.id.transparency);
        mImage.setImageAlpha(mCurrentValue);
        mSeekBar.setProgress(mCurrentValue);

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        Log.d(TAG, "Progress changed...");
        // change accepted, store it
        mCurrentValue = progress;
        mImage.setImageAlpha(progress);
        persistInt(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index) {
        return ta.getInt(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

        if (restoreValue) {
            mCurrentValue = getPersistedInt(DEFAULT_VALUE);
        } else {
            mCurrentValue = (Integer) defaultValue;
            persistInt(mCurrentValue);
        }

    }

}
