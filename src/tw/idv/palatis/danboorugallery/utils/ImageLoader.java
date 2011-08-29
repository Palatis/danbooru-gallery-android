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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.WeakHashMap;

import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.MainActivity.GalleryItemDisplayer;
import tw.idv.palatis.danboorugallery.defines.D;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageLoader
{
	private BitmapMemCache				mMemCache;
	private FileCache					mFileCache;
	private Map < ImageView, String >	mImageViews	= Collections.synchronizedMap( new WeakHashMap < ImageView, String >() );
	private PhotosLoaderWeb				mWebLoader;
	private PhotosLoaderDisk			mDiskLoader;

	public ImageLoader(Context context)
	{
		// Make the background thread low priority. This way it will not affect
		// the UI performance
		mWebLoader = new PhotosLoaderWeb();
		mWebLoader.setPriority( Thread.MIN_PRIORITY );
		mWebLoader.start();

		mDiskLoader = new PhotosLoaderDisk();
		mDiskLoader.setPriority( Thread.MIN_PRIORITY );
		mDiskLoader.start();

		mFileCache = new FileCache( context );
		mMemCache = BitmapMemCache.getInstance();
	}

	final int	stub_id	= R.drawable.icon;

	public void DisplayImage(String url, ImageView image)
	{
		// This ImageView may be used for other images before. So there may be
		// some old tasks in the queue. We need to discard them.
		mDiskLoader.discard( image );
		mWebLoader.discard( image );

		mImageViews.put( image, url );

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
		{
			mDiskLoader.queuePhoto( new PhotoToLoad( url, image ) );
			image.setImageResource( stub_id );
			image.setTag( this );
		}
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
			URL imageUrl = new URL( url );
			HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
			InputStream is = conn.getInputStream();
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
			Log.d( D.LOGTAG, "image " + url + " download failed!" );
			Log.d( D.LOGTAG, ex.getMessage() );
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
				return -1;
			if (diff < 0)
				return 1;
			return 0;
		}
	}

	private abstract class PhotosLoaderBase
		extends Thread
	{
		protected PriorityQueue < PhotoToLoad >	tasks	= new PriorityQueue < PhotoToLoad >();

		public void discard(ImageView image)
		{
			if (image == null)
				return;

			synchronized (tasks)
			{
				for (PhotoToLoad task : tasks)
					if (task.mImage == image)
						tasks.remove( task );
			}
		}

		public void queuePhoto(PhotoToLoad task)
		{
			synchronized (tasks)
			{
				tasks.offer( task );
				tasks.notifyAll();
			}
		}

		public void cancelAll()
		{
			synchronized (tasks)
			{
				tasks.clear();
				tasks.notifyAll();
			}
		}

		protected boolean hasMoreTask()
		{
			return !tasks.isEmpty();
		}

		protected PhotoToLoad pollNextTask()
		{
			synchronized (tasks)
			{
				return tasks.poll();
			}
		}

		protected void waitForTasks() throws InterruptedException
		{
			synchronized (tasks)
			{
				tasks.wait();
			}
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
					// thread waits until there are any images to load in the queue
					if ( !hasMoreTask())
						waitForTasks();

					if (hasMoreTask())
					{
						PhotoToLoad task = pollNextTask();

						Bitmap bitmap = getBitmapDisk( task.mUrl );
						if (bitmap == null)
						{
							mWebLoader.queuePhoto( task );
							continue;
						}

						if (task.mImage != null)
						{
							// check if we still want the bitmap
							String tag = mImageViews.get( task.mImage );
							if (tag != null && tag.equals( task.mUrl ))
								new GalleryItemDisplayer( task.mImage, bitmap, ScaleType.CENTER_CROP, true ).display();
						}
					}
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
					// thread waits until there are any images to load in the queue
					if ( !hasMoreTask())
						waitForTasks();

					if (hasMoreTask())
					{
						PhotoToLoad task = pollNextTask();

						Bitmap bitmap = getBitmapWeb( task.mUrl );
						if (bitmap == null)
						{
							// download problem, put this task to the end of the queue and try again later.
							queuePhoto( task );
							continue;
						}

						if (task.mImage != null)
						{
							// check if we still want the bitmap
							String tag = mImageViews.get( task.mImage );
							if (tag != null && tag.equals( task.mUrl ))
								new GalleryItemDisplayer( task.mImage, bitmap, ScaleType.CENTER_CROP, true ).display();
						}
					}
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
}