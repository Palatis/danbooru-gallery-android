package tw.idv.palatis.danboorugallery.utils;

/*
 * This file is part of Danbooru Gallery
 *
 * Copyright 2011
 *   - Victor Tseng <palatis@gmail.com>
 *
 * Danbooru Gallery is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Danbooru Gallery is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Danbooru Gallery.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.PriorityBlockingQueue;

import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.defines.D;
import android.graphics.Bitmap;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageLoader
{
	private BitmapMemCache		mMemCache;
	private FileCache			mFileCache;
	private PhotosLoaderWeb		mWebLoader;
	private PhotosLoaderDisk	mDiskLoader;

	public ImageLoader()
	{
		mWebLoader = new PhotosLoaderWeb();
		mWebLoader.start();

		// Make the background thread low priority. This way it will not affect
		// the UI performance
		mDiskLoader = new PhotosLoaderDisk();
		mDiskLoader.setPriority( Thread.currentThread().getPriority() - 1 );
		mDiskLoader.start();

		mFileCache = FileCache.getInstance();
		mMemCache = BitmapMemCache.getInstance();
	}

	final int	stub_id	= R.drawable.icon;

	public void DisplayImage(String url, ImageView image)
	{
		// This ImageView may be used for other images before. So there may be
		// some old tasks in the queue. We need to discard them.
		mDiskLoader.discard( image );
		mWebLoader.discard( image );

		Bitmap bitmap = getBitmapCache( url );
		if (image == null)
		{
			if (bitmap == null)
				mDiskLoader.queuePhoto( new PhotoToLoad( url, null ) );
			return;
		}

		if (bitmap != null)
		{
			image.setImageBitmap( bitmap );
			image.setScaleType( ScaleType.CENTER_CROP );
			image.clearAnimation();
			image.setTag( null );
		}
		else
			mDiskLoader.queuePhoto( new PhotoToLoad( url, image ) );
	}

	public void onLowMemory()
	{
		mMemCache.clear();
	}

	public void cancelAll()
	{
		mDiskLoader.cancelAll();
		mWebLoader.cancelAll();
	}

	// from Memory cache
	private Bitmap getBitmapCache(String url)
	{
		return mMemCache.get( url );
	}

	// from SD cache
	private Bitmap getBitmapDisk(String url)
	{

		Bitmap bitmap = D.getBitmapFromFile( mFileCache.getFile( url ) );
		if (bitmap != null)
			mMemCache.put( url, bitmap );
		return bitmap;
	}

	// from web
	private Bitmap getBitmapWeb(String url)
	{
		try
		{
			InputStream is = new URL( url ).openStream();
			OutputStream os = new FileOutputStream( mFileCache.getFile( url ) );
			D.CopyStream( is, os );
			os.close();

			Bitmap bitmap = D.getBitmapFromFile( mFileCache.getFile( url ) );
			if (bitmap != null)
				mMemCache.put( url, bitmap );
			mMemCache.put( url, bitmap );
			return bitmap;
		}
		catch (Exception ex)
		{
			D.Log.wtf( ex, "image %s download failed!", url );
		}
		return null;
	}

	public void stopThread()
	{
		mDiskLoader.interrupt();
		mWebLoader.interrupt();
	}

	// Task for the queue
	private class PhotoToLoad
		implements Comparable < PhotoToLoad >
	{
		public long			mTimestamp;
		public String		mUrl;
		public ImageView	mImage;

		public PhotoToLoad(String u, ImageView i)
		{
			mUrl = u;
			mImage = i;
			mTimestamp = System.currentTimeMillis();
		}

		@Override
		public int compareTo(PhotoToLoad another)
		{
			// ones with a ImageView associated is with higher priority
			if (mImage == null && another.mImage != null)
				return 1;
			else if (mImage != null && another.mImage == null)
				return -1;

			// if both have a view or both don't have a view, the one added earlier has higher priority
			long diff = mTimestamp - another.mTimestamp;
			if (diff > 0)
				return 1;
			if (diff < 0)
				return -1;
			return 0;
		}

		@SuppressWarnings("unused")
		public boolean equals(PhotoToLoad another)
		{
			return mImage == another.mImage;
		}
	}

	private abstract class PhotosLoaderBase
		extends Thread
	{
		protected PriorityBlockingQueue < PhotoToLoad >	tasks	= new PriorityBlockingQueue < PhotoToLoad >();

		public void discard(ImageView image)
		{
			if (image == null)
				return;

			for (PhotoToLoad task : tasks)
				if (task.mImage == image)
					tasks.remove( task );
		}

		public void discardAllNoImage()
		{
			for (PhotoToLoad task : tasks)
				if (task.mImage == null)
					tasks.remove( task );
		}

		public void discardImagesWithView()
		{
			for (PhotoToLoad task : tasks)
				if (task.mImage != null)
					tasks.remove( task );
		}

		public void queuePhoto(PhotoToLoad task)
		{
			tasks.put( task );
		}

		protected PhotoToLoad pollNextTask() throws InterruptedException
		{
			return tasks.take();
		}

		public void cancelAll()
		{
			tasks.clear();
		}

		@Override
		abstract public void run();
	}

	private class PhotosLoaderDisk
		extends PhotosLoaderBase
	{
		@Override
		public void run()
		{
			try
			{
				while (true)
				{
					PhotoToLoad task = pollNextTask();

					if (task.mImage != null)
					{
						Bitmap bitmap = getBitmapDisk( task.mUrl );
						if (bitmap == null)
						{
							File file = mFileCache.getFile( task.mUrl );
							if (file.exists())
							{
								// a problem occurred, the file is there but unable to load.
								// delete it before we queue it to the webloader for a reload.
								D.Log.d( "the file %s is there but unable to load, delete it before hand to webloader.", task.mUrl );
								file.delete();
								// D.Log.d( "the file %s is there but unable to load, queue it..", task.mUrl );
								// task.mTimestamp = System.currentTimeMillis();
								// queuePhoto( task );
							}
							else
								mWebLoader.queuePhoto( task );
							continue;
						}

						task.mImage.post( new GalleryItemDisplayer( task.mUrl, task.mImage, bitmap, ScaleType.CENTER_CROP, true ) );
					}
					// if no image, just check existence, don't load.
					else if ( !mFileCache.getFile( task.mUrl ).exists())
						mWebLoader.queuePhoto( task );

					if (Thread.interrupted())
						break;
				}
			}
			catch (InterruptedException e)
			{
				// allow thread to exit
			}
		}
	}

	private class PhotosLoaderWeb
		extends PhotosLoaderBase
	{
		@Override
		public void run()
		{
			try
			{
				while (true)
				{
					PhotoToLoad task = pollNextTask();

					if (mFileCache.getFile( task.mUrl ).exists())
					{
						if (task.mImage != null)
						{
							D.Log.d( "the file %s is there already, hand back to mDiskLoader.", task.mUrl );
							mDiskLoader.queuePhoto( task );
						}
						else
							D.Log.d( "the file %s is there already and there's no view, skip.", task.mUrl );

						continue;
					}

					Bitmap bitmap = getBitmapWeb( task.mUrl );
					if (bitmap == null)
					{
						// download problem, put this task to the end of the queue and try again later.
						task.mTimestamp = System.currentTimeMillis();
						queuePhoto( task );
						continue;
					}

					if (task.mImage != null)
						task.mImage.post( new GalleryItemDisplayer( task.mUrl, task.mImage, bitmap, ScaleType.CENTER_CROP, true ) );
					else
						D.Log.d( "fetched image without a view: %s", task.mUrl );

					if (Thread.interrupted())
						break;
				}
			}
			catch (InterruptedException e)
			{
				// allow thread to exit
			}
		}
	}

	public void cancelAllPrefetch()
	{
		mDiskLoader.discardAllNoImage();
		mWebLoader.discardAllNoImage();
	}

	public void discardImagesWithView()
	{
		mDiskLoader.discardImagesWithView();
		mWebLoader.discardImagesWithView();
	}

	private static class GalleryItemDisplayer
		implements Runnable
	{
		final String	image_url;
		final ImageView	image;
		final Bitmap	bitmap;
		final ScaleType	scale_type;
		final boolean	do_animation;

		public GalleryItemDisplayer(String url, ImageView v, Bitmap b, ScaleType t, boolean anim)
		{
			image_url = url;
			image = v;
			bitmap = b;
			scale_type = t;
			do_animation = anim;
		}

		@Override
		public void run()
		{
			if (image.getTag() == null)
				return;

			String url = (String) image.getTag();
			if (url.equals( image_url ))
			{
				image.setImageBitmap( bitmap );
				image.setScaleType( scale_type );

				if (do_animation)
				{
					Animation anim = image.getAnimation();
					if (anim != null)
					{
						if (anim.hasEnded())
						{
							anim.reset();
							image.startAnimation( anim );
						}
					}
					else
						image.startAnimation( AnimationUtils.loadAnimation( image.getContext(), android.R.anim.fade_in ) );
				}

				image.setTag( null );
			}
		}
	}
}