package it.sephiroth.android.library.imagezoom;

import it.sephiroth.android.library.imagezoom.easing.Cubic;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageViewTouchBase extends ImageView
{
	public interface OnBitmapChangedListener
	{
		void onBitmapChanged( Bitmap bitmap );
	};

	protected enum Command
	{
		Center, Move, Zoom, Layout, Reset,
	};

	protected ScrollRunnable			mScrollRunnable		= new ScrollRunnable();
	protected Matrix					mBaseMatrix			= new Matrix();
	protected Matrix					mSuppMatrix			= new Matrix();
	protected Handler					mHandler			= new Handler();
	protected Runnable					mOnLayoutRunnable	= null;
	protected float						mMaxZoom;
	protected final Matrix				mDisplayMatrix		= new Matrix();
	protected final float[]				mMatrixValues		= new float[9];
	protected int						mThisWidth			= -1;
	protected int						mThisHeight			= -1;

	final protected RotateBitmap		mBitmapDisplayed	= new RotateBitmap( null, 0 );
	final protected float				MAX_ZOOM			= 2.0f;

	private OnBitmapChangedListener	mListener;

	public ImageViewTouchBase( Context context )
	{
		super( context );
		init();
	}

	public ImageViewTouchBase( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		init();
	}

	public void setOnBitmapChangedListener( OnBitmapChangedListener listener ) {
		mListener = listener;
	}

	protected void init()
	{
		setScaleType( ImageView.ScaleType.MATRIX );
	}

	public void zoomAwareScrollBy(float distanceX, float distanceY, float targetScale, float durationMs )
	{
		if ( Math.abs(distanceX) < 1 && Math.abs(distanceY) < 1 )
		{
			scrollBy( distanceX, distanceY );
			return;
		}

		mScrollRunnable.reset(distanceX, distanceY, targetScale, durationMs);
		mHandler.post( mScrollRunnable );
	}

	private class ScrollRunnable implements Runnable
	{
		float	dx			= 0;
		float	dy			= 0;
		float	old_x		= 0;
		float	old_y		= 0;
		float	dms			= 0;
		long	st			= 0;
		boolean canceled	= false;

		public void reset( float distanceX, float distanceY, float durationMs )
		{
			reset( distanceX, distanceY, getScale(), durationMs );
		}

		public void reset( float distanceX, float distanceY, float targetScale, float durationMs )
		{
			old_x = 0;
			old_y = 0;
			dx = distanceX / targetScale;
			dy = distanceY / targetScale;
			st = System.currentTimeMillis();
			dms = durationMs;
			canceled = false;
		}

		public void run()
		{
			long now = System.currentTimeMillis();
			float scale = getScale();
			float currentMs = Math.min( dms, now - st );
			float ease = Cubic.easeOut(currentMs, dms);
			float x = ease * dx;
			float y = ease * dy;
			postTranslate( (x - old_x) * scale, (y - old_y) * scale );
			old_x = x;
			old_y = y;

			if ( canceled )
				return;

			if ( currentMs < dms )
				mHandler.post( this );
			else
				center( true, true, 500 );
		}

		public void cancel()
		{
			canceled = true;
		}
	}

	public void cancelScroll()
	{
		mScrollRunnable.cancel();
	}

	public void clear()
	{
		setImageBitmapReset( null, true );
	}

	@Override
	protected void onLayout( boolean changed, int left, int top, int right, int bottom )
	{
		super.onLayout( changed, left, top, right, bottom );
		mThisWidth = right - left;
		mThisHeight = bottom - top;
		Runnable r = mOnLayoutRunnable;
		if ( r != null )
		{
			mOnLayoutRunnable = null;
			r.run();
		}
		if ( mBitmapDisplayed.getBitmap() != null )
		{
			getProperBaseMatrix( mBitmapDisplayed, mBaseMatrix );
			setImageMatrix( getImageViewMatrix() );
		}
	}

	public void setImageBitmapReset( final Bitmap bitmap, final boolean reset )
	{
		setImageRotateBitmapReset( new RotateBitmap( bitmap, 0 ), reset );
	}

	public void setImageBitmapReset( final Bitmap bitmap, final int rotation, final boolean reset )
	{
		setImageRotateBitmapReset( new RotateBitmap( bitmap, rotation ), reset );
	}

	public void setImageRotateBitmapReset( final RotateBitmap bitmap, final boolean reset )
	{
		final int viewWidth = getWidth();
		if ( viewWidth <= 0 )
		{
			mOnLayoutRunnable = new Runnable()
			{
				public void run()
				{
					setImageBitmapReset( bitmap.getBitmap(), bitmap.getRotation(), reset );
				}
			};
			return;
		}

		if ( bitmap.getBitmap() != null )
		{
			getProperBaseMatrix( bitmap, mBaseMatrix );
			setImageBitmap( bitmap.getBitmap(), bitmap.getRotation() );
		}
		else
		{
			mBaseMatrix.reset();
			setImageBitmap( null );
		}

		if ( reset )
			mSuppMatrix.reset();

		setImageMatrix( getImageViewMatrix() );
		mMaxZoom = maxZoom();

		if( mListener != null )
			mListener.onBitmapChanged( bitmap.getBitmap() );
	}

	protected float maxZoom()
	{
		if ( mBitmapDisplayed.getBitmap() == null )
			return 1F;

		float max = Math.max(
			(float)mBitmapDisplayed.getWidth() / mThisWidth,
			(float)mBitmapDisplayed.getHeight() / mThisHeight
		) * 4;
		return max;
	}

	public RotateBitmap getDisplayBitmap()
	{
		return mBitmapDisplayed;
	}

	public float getMaxZoom()
	{
		return mMaxZoom;
	}

	@Override
	public void setImageBitmap( Bitmap bitmap )
	{
		setImageBitmap( bitmap, 0 );
	}

	/**
	 * This is the ultimate method called when a new bitmap is set
	 * @param bitmap
	 * @param rotation
	 */
	protected void setImageBitmap( Bitmap bitmap, int rotation )
	{
		super.setImageBitmap( bitmap );
		Drawable d = getDrawable();
		if ( d != null )
			d.setDither( true );
		mBitmapDisplayed.setBitmap( bitmap );
		mBitmapDisplayed.setRotation( rotation );
	}

	protected Matrix getImageViewMatrix()
	{
		mDisplayMatrix.set( mBaseMatrix );
		mDisplayMatrix.postConcat( mSuppMatrix );
		return mDisplayMatrix;
	}

	/**
	 * Setup the base matrix so that the image is centered and scaled properly.
	 *
	 * @param bitmap
	 * @param matrix
	 */
	protected void getProperBaseMatrix( RotateBitmap bitmap, Matrix matrix )
	{
		float viewWidth = getWidth();
		float viewHeight = getHeight();
		float w = bitmap.getWidth();
		float h = bitmap.getHeight();
		matrix.reset();
		float widthScale = Math.min( viewWidth / w, MAX_ZOOM );
		float heightScale = Math.min( viewHeight / h, MAX_ZOOM );
		float scale = Math.min( widthScale, heightScale );
		matrix.postConcat( bitmap.getRotateMatrix() );
		matrix.postScale( scale, scale );
		matrix.postTranslate( ( viewWidth - w * scale ) / MAX_ZOOM, ( viewHeight - h * scale ) / MAX_ZOOM );
	}

	protected float getValue( Matrix matrix, int whichValue )
	{
		matrix.getValues( mMatrixValues );
		return mMatrixValues[whichValue];
	}

	public RectF getBitmapRect()
	{
		if ( mBitmapDisplayed.getBitmap() == null )
			return null;
		Matrix m = getImageViewMatrix();
		RectF rect = new RectF( 0, 0, mBitmapDisplayed.getBitmap().getWidth(), mBitmapDisplayed.getBitmap().getHeight() );
		m.mapRect( rect );
		return rect;
	}

	public float getBaseScale()
	{
		return getValue( mBaseMatrix, Matrix.MSCALE_X );
	}

	public float getScale()
	{
		return getValue( mSuppMatrix, Matrix.MSCALE_X );
	}

	public void center( boolean horizontal, boolean vertical )
	{
		if ( mBitmapDisplayed.getBitmap() == null )
			return;

		final PointF pt = getCenter( horizontal, vertical );
		if ( pt.x != 0 || pt.y != 0 )
				postTranslate( pt.x, pt.y );
	}

	public void center( boolean horizontal, boolean vertical, final float durationMs )
	{
		if ( mBitmapDisplayed.getBitmap() == null )
			return;

		final PointF pt = getCenter( horizontal, vertical );
		if ( pt.x != 0 || pt.y != 0 )
				scrollBy( pt.x, pt.y, durationMs );
	}

	public PointF getViewportCenter()
	{
		RectF rect = getBitmapRect();
		return new PointF( -rect.left + getWidth() / 2, -rect.top + getHeight() / 2 );
	}

	public PointF getCenter( boolean horizontal, boolean vertical )
	{
		if ( mBitmapDisplayed.getBitmap() == null )
			return new PointF( 0, 0 );

		RectF rect = getBitmapRect();
		float deltaX = 0, deltaY = 0;
		if ( vertical )
		{
			float height = rect.height();
			int viewHeight = getHeight();
			if ( height < viewHeight )
				deltaY = ( viewHeight - height ) / 2 - rect.top;
			else if ( rect.top > 0 )
				deltaY = -rect.top;
			else if ( rect.bottom < viewHeight )
				deltaY = getHeight() - rect.bottom;
		}
		if ( horizontal )
		{
			float width = rect.width();
			int viewWidth = getWidth();
			if ( width < viewWidth )
				deltaX = ( viewWidth - width ) / 2 - rect.left;
			else if ( rect.left > 0 )
				deltaX = -rect.left;
			else if ( rect.right < viewWidth )
				deltaX = viewWidth - rect.right;
		}
		return new PointF( deltaX, deltaY );
	}

	protected void postTranslate( float deltaX, float deltaY )
	{
		mSuppMatrix.postTranslate( deltaX, deltaY );
		setImageMatrix( getImageViewMatrix() );
	}

	protected void postScale( float scale, float centerX, float centerY )
	{
		mSuppMatrix.postScale( scale, scale, centerX, centerY );
		setImageMatrix( getImageViewMatrix() );
	}

	public void zoomTo( float scale )
	{
		float cx = getWidth() / 2F;
		float cy = getHeight() / 2F;
		zoomTo( scale, cx, cy );
	}

	public void zoomTo( float scale, float durationMs )
	{
		float cx = getWidth() / 2F;
		float cy = getHeight() / 2F;
		zoomTo( scale, cx, cy, durationMs );
	}

	public void zoomTo( float scale, float centerX, float centerY )
	{
		float oldScale = getScale();
		float deltaScale = scale / oldScale;
		postScale( deltaScale, centerX, centerY );
		onZoom( getScale() );
		center( true, true );
	}

	protected void onZoom( float scale )
	{
	}

	public void scrollBy( float x, float y )
	{
		mScrollRunnable.cancel();
		postTranslate( x, y );
	}

	public void scrollBy( float distanceX, float distanceY, final float durationMs )
	{
		if ( Math.abs(distanceX) < 1 && Math.abs(distanceY) < 1 )
		{
			scrollBy( distanceX, distanceY );
			return;
		}

		mScrollRunnable.reset(distanceX, distanceY, durationMs);
		mHandler.post( mScrollRunnable );
	}

	public void zoomTo( float scale, final float centerX, final float centerY, final float durationMs )
	{
		final long startTime = System.currentTimeMillis();
		final float oldScale = getScale();
		final float diffScale = scale - oldScale;
		mHandler.post(
			new Runnable()
			{
				public void run()
				{
					long now = System.currentTimeMillis();
					float currentMs = Math.min( durationMs, now - startTime );
					float target = oldScale + ( diffScale * Cubic.easeOut( currentMs, durationMs ) );
					zoomTo( target, centerX, centerY );
					if ( currentMs < durationMs )
						mHandler.post( this );
				}
			}
		);
	}

	public void dispose()
	{
		if ( mBitmapDisplayed.getBitmap() != null )
			if ( !mBitmapDisplayed.getBitmap().isRecycled() )
				mBitmapDisplayed.getBitmap().recycle();
		clear();
	}
}