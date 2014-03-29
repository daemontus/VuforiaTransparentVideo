/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

package com.qualcomm.vuforia.samples.VideoPlayback.ui.SampleAppMenu;

import android.animation.Animator;
import android.animation.ValueAnimator;


public class SampleAppMenuAnimator extends ValueAnimator implements
    ValueAnimator.AnimatorUpdateListener, ValueAnimator.AnimatorListener
{
    
    private static long MENU_ANIMATION_DURATION = 300;
    private SampleAppMenu mSampleAppMenu;
    private float mMaxX;
    private float mEndX;
    
    
    public SampleAppMenuAnimator(SampleAppMenu menu)
    {
        mSampleAppMenu = menu;
        setDuration(MENU_ANIMATION_DURATION);
        addUpdateListener(this);
        addListener(this);
    }
    
    
    @Override
    public void onAnimationUpdate(ValueAnimator animation)
    {
        Float f = (Float) animation.getAnimatedValue();
        mSampleAppMenu.setAnimationX(f.floatValue());
    }
    
    
    @Override
    public void onAnimationCancel(Animator animation)
    {
    }
    
    
    @Override
    public void onAnimationEnd(Animator animation)
    {
        mSampleAppMenu.setDockMenu(mEndX == mMaxX);
        if (mEndX == 0)
            mSampleAppMenu.hide();
    }
    
    
    @Override
    public void onAnimationRepeat(Animator animation)
    {
    }
    
    
    @Override
    public void onAnimationStart(Animator animation)
    {
    }
    
    
    public void setStartEndX(float start, float end)
    {
        mEndX = end;
        setFloatValues(start, end);
        setDuration((int) (MENU_ANIMATION_DURATION * (Math.abs(end - start) / mMaxX)));
    }
    
    
    public void setMaxX(float maxX)
    {
        mMaxX = maxX;
    }
    
}
