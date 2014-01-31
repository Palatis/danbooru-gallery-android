////////////////////////////////////////////////////////////////////////////////
// ByakuGallery is an open source Android library that allows the visualization
//     of large images with gesture capabilities.
//     This lib is based on AOSP Camera2.
//     Copyright 2013 Diego Carlos Lima
//
//     Licensed under the Apache License, Version 2.0 (the "License");
//     you may not use this file except in compliance with the License.
//     You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//     Unless required by applicable law or agreed to in writing, software
//     distributed under the License is distributed on an "AS IS" BASIS,
//     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//     See the License for the specific language governing permissions and
//     limitations under the License.
////////////////////////////////////////////////////////////////////////////////

package com.diegocarloslima.byakugallery;

import android.content.Context;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class TouchGestureDetector {

    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleGestureDetector;

    public TouchGestureDetector(Context context, OnTouchGestureListener listener) {
        mGestureDetector = new GestureDetector(context, listener);
        mGestureDetector.setOnDoubleTapListener(listener);
        mScaleGestureDetector = new ScaleGestureDetector(context, listener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            mScaleGestureDetector.setQuickScaleEnabled(false);
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = mScaleGestureDetector.onTouchEvent(event);
        if(!mScaleGestureDetector.isInProgress()) {
            ret |= mGestureDetector.onTouchEvent(event);
        }
        return ret;
    }

    public static abstract class OnTouchGestureListener implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {}

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {}

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {}
    }
}