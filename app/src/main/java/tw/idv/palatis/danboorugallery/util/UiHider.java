////////////////////////////////////////////////////////////////////////////////
// Danbooru Gallery Android - an danbooru-style imageboard browser
//     Copyright (C) 2014  Victor Tseng
//
//     This program is free software: you can redistribute it and/or modify
//     it under the terms of the GNU General Public License as published by
//     the Free Software Foundation, either version 3 of the License, or
//     (at your option) any later version.
//
//     This program is distributed in the hope that it will be useful,
//     but WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//     GNU General Public License for more details.
//
//     You should have received a copy of the GNU General Public License
//     along with this program. If not, see <http://www.gnu.org/licenses/>
////////////////////////////////////////////////////////////////////////////////

package tw.idv.palatis.danboorugallery.util;

import android.os.Handler;

public class UiHider
{
    private static final String TAG = "UiHider";

    /**
     * Giving a value greater than this will enable auto-hide.
     */
    public static final long AUTO_HIDE_DELAY_DISABLED = -1;

    private boolean mVisible = true;
    private long mAutoHideDelay;

    private Handler mHandler = new Handler();;
    private Runnable mHideRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            hide();
        }
    };
    private OnVisibilityChangeListener mOnVisibilityChangeCallback;

    public UiHider(long autoHideMillis, OnVisibilityChangeListener callback)
    {
        setOnVisibilityChangeCallback(callback);
        mAutoHideDelay = autoHideMillis;
    }

    public void setAutoHideDelay(long millis)
    {
        mAutoHideDelay = millis;
        if (mAutoHideDelay <= AUTO_HIDE_DELAY_DISABLED)
            mHandler.removeCallbacks(mHideRunnable);
    }

    public long getAutoHideDelay()
    {
        return mAutoHideDelay;
    }

    public void setOnVisibilityChangeCallback(OnVisibilityChangeListener callback)
    {
        mOnVisibilityChangeCallback = callback == null ? sDummyOnVisibilityChangeCallback : callback;
    }

    public OnVisibilityChangeListener getOnVisibilityChangeCallback()
    {
        return mOnVisibilityChangeCallback == sDummyOnVisibilityChangeCallback ? null : mOnVisibilityChangeCallback;
    }

    public boolean isVisible()
    {
        return mVisible;
    }

    public void show()
    {
        mHandler.removeCallbacks(mHideRunnable);
        mOnVisibilityChangeCallback.onVisibilityChange(mVisible = true);
        if (mAutoHideDelay > AUTO_HIDE_DELAY_DISABLED)
            delayedHide(mAutoHideDelay);
    }

    public void hide()
    {
        mOnVisibilityChangeCallback.onVisibilityChange(mVisible = false);
    }

    public void toggle()
    {
        if (mVisible = !mVisible)
            show();
        else
            hide();
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    public void delayedHide(long delayMillis)
    {
        mHandler.removeCallbacks(mHideRunnable);
        mHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void delayedHide()
    {
        delayedHide(mAutoHideDelay);
    }

    private static final OnVisibilityChangeListener sDummyOnVisibilityChangeCallback =
        new OnVisibilityChangeListener() {
            @Override
            public void onVisibilityChange(boolean visible) { }
        };

    public static interface OnVisibilityChangeListener
    {
        public void onVisibilityChange(boolean visible);
    }
}
