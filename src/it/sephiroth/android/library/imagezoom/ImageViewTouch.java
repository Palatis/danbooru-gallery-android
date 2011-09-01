package it.sephiroth.android.library.imagezoom;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;

public class ImageViewTouch
	extends ImageViewTouchBase
{
	static final float				MIN_ZOOM	= 0.7f;
	static final float				MAX_ZOOM	= 2.5f;
	protected ScaleGestureDetector	mScaleDetector;
	protected GestureDetector		mGestureDetector;
	protected int					mTouchSlop;
	protected float					mCurrentScaleFactor;
	protected float					mScaleFactor;
	protected GestureListener		mGestureListener;
	protected ScaleListener			mScaleListener;
	protected OnClickListener		mOnClickListener;

	public OnClickListener getOnClickListener()
	{
		return mOnClickListener;
	}

	@Override
	public void setOnClickListener(OnClickListener listener)
	{
		mOnClickListener = listener;
	}

	public ImageViewTouch(Context context, AttributeSet attrs)
	{
		super( context, attrs );
	}

	@Override
	protected void init()
	{
		super.init();
		mTouchSlop = ViewConfiguration.getTouchSlop();
		mGestureListener = new GestureListener();
		mScaleListener = new ScaleListener();

		mScaleDetector = new ScaleGestureDetector( getContext(), mScaleListener );
		mGestureDetector = new GestureDetector( getContext(), mGestureListener, null, true );
		mCurrentScaleFactor = 1f;
	}

	@Override
	public void setImageBitmapReset(Bitmap bitmap, boolean reset)
	{
		super.setImageBitmapReset( bitmap, reset );
		mScaleFactor = getMaxZoom() / 2.0f;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		mScaleDetector.onTouchEvent( event );
		if ( !mScaleDetector.isInProgress())
			mGestureDetector.onTouchEvent( event );

		int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK)
		{
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			cancelScroll();
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			if (getScale() < 1f)
				zoomTo( 1f, 500 );
			if (getScale() > getMaxZoom())
				zoomTo( getMaxZoom(), 500 );
			center( true, true, 500 );
			break;
		}
		return true;
	}

	@Override
	protected void onZoom(float scale)
	{
		super.onZoom( scale );
		if ( !mScaleDetector.isInProgress())
			mCurrentScaleFactor = scale;
	}

	protected float onDoubleTapPost(float scale, float maxZoom)
	{
		return (scale >= mScaleFactor) ? 1f : scale + mScaleFactor;
	}

	class GestureListener
		extends GestureDetector.SimpleOnGestureListener
	{
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e)
		{
			if ( mOnClickListener != null )
			{
				mOnClickListener.onClick( ImageViewTouch.this );
				return true;
			}
			return super.onSingleTapConfirmed( e );
		}

		@Override
		public boolean onDoubleTap(MotionEvent e)
		{
			float scale = getScale();
			float targetScale = scale;
			targetScale = onDoubleTapPost( scale, getMaxZoom() );
			targetScale = Math.min( getMaxZoom(), Math.max( targetScale, MIN_ZOOM ) );
			mCurrentScaleFactor = targetScale;
			zoomTo( targetScale, e.getX(), e.getY(), 500 );
			invalidate();
			return super.onDoubleTap( e );
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
		{
			if (e1 == null || e2 == null)
				return false;
			if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1)
				return false;
			if (mScaleDetector.isInProgress())
				return false;

			scrollBy( -distanceX, -distanceY );
			invalidate();
			return super.onScroll( e1, e2, distanceX, distanceY );
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
		{
			if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1)
				return false;
			if (mScaleDetector.isInProgress())
				return false;

			scrollBy( velocityX / 3, velocityY / 3, 500 );
			invalidate();
			return super.onFling( e1, e2, velocityX, velocityY );
		}
	}

	class ScaleListener
		extends ScaleGestureDetector.SimpleOnScaleGestureListener
	{
		@Override
		public boolean onScale(ScaleGestureDetector detector)
		{
			mCurrentScaleFactor = Math.min( getMaxZoom() * MAX_ZOOM, Math.max( mCurrentScaleFactor * detector.getScaleFactor(), MIN_ZOOM ) );
			zoomTo( mCurrentScaleFactor, detector.getFocusX(), detector.getFocusY() );
			invalidate();
			return true;
		}
	}
}