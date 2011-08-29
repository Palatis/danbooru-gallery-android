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
import java.util.Stack;
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
	private BitmapMemCache				memCache;
	private FileCache					fileCache;
	private Map < ImageView, String >	imageViews	= Collections.synchronizedMap( new WeakHashMap < ImageView, String >() );
	private PhotosLoaderWeb				webloader;
	private PhotosLoaderDisk			diskloader;

	public ImageLoader(Context context)
	{
		// Make the background thread low priority. This way it will not affect
		// the UI performance
		webloader = new PhotosLoaderWeb();
		webloader.setPriority( Thread.MIN_PRIORITY );
		webloader.start();

		diskloader = new PhotosLoaderDisk();
		diskloader.setPriority( Thread.MIN_PRIORITY );
		diskloader.start();

		fileCache = new FileCache( context );
		memCache = BitmapMemCache.getInstance();
	}

	// final int stub_id=R.drawable.stub;
	final int	stub_id	= R.drawable.icon;

	public void DisplayImage(String url, ImageView image)
	{
		// This ImageView may be used for other images before. So there may be
		// some old tasks in the queue. We need to discard them.
		diskloader.discard( image );
		webloader.discard( image );

		imageViews.put( image, url );

		Bitmap bitmap = getBitmapCache( url );
		if (bitmap != null)
		{
			image.setImageBitmap( bitmap );
			image.setScaleType( ScaleType.CENTER_CROP );
			image.clearAnimation();
			image.setTag( null );
		}
		else
		{
			diskloader.queuePhoto( new PhotoToLoad( url, image ) );
			image.setImageResource( stub_id );
			image.setTag( this );
		}
	}

	public void onLowMemory()
	{
		memCache.clear();
	}

	public void cancelAll()
	{
		diskloader.cancelAll();
		webloader.cancelAll();
	}

	// from Memory cache
	private Bitmap getBitmapCache(String url)
	{
		return memCache.get( url );
	}

	// from SD cache
	private Bitmap getBitmapDisk(String url)
	{
		Bitmap bitmap = D.getBitmapFromFile( fileCache.getFile( url ) );
		if (bitmap != null)
			memCache.put( url, bitmap );
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
			OutputStream os = new FileOutputStream( fileCache.getFile( url ) );
			D.CopyStream( is, os );
			os.close();

			Bitmap bitmap = D.getBitmapFromFile( fileCache.getFile( url ) );
			if (bitmap != null)
				memCache.put( url, bitmap );
			memCache.put( url, bitmap );
			return bitmap;
		}
		catch (Exception ex)
		{
			Log.d( D.LOGTAG, "image " + url + " download failed!" );
			Log.d( D.LOGTAG, ex.getMessage() );
			ex.printStackTrace();
		}
		return null;
	}

	// Task for the queue
	private class PhotoToLoad
	{
		public String		url;
		public ImageView	imageView;

		public PhotoToLoad(String u, ImageView i)
		{
			url = u;
			imageView = i;
		}
	}

	public void stopThread()
	{
		diskloader.interrupt();
		webloader.interrupt();
	}

	private abstract class PhotosLoaderBase
		extends Thread
	{
		protected Stack < PhotoToLoad >	photosToLoad	= new Stack < PhotoToLoad >();

		public void discard(ImageView image)
		{
			synchronized (photosToLoad)
			{
				for (int i = 0; i < photosToLoad.size();)
					if (photosToLoad.get( i ).imageView == image)
						photosToLoad.remove( i );
					else
						++i;
			}
		}

		public void queuePhoto(PhotoToLoad photo)
		{
			synchronized (photosToLoad)
			{
				photosToLoad.push( photo );
				photosToLoad.notifyAll();
			}
		}

		public void cancelAll()
		{
			synchronized (photosToLoad)
			{
				photosToLoad.clear();
				photosToLoad.notifyAll();
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
					// thread waits until there are any images to load in the
					// queue
					if (photosToLoad.empty())
						synchronized (photosToLoad)
						{
							photosToLoad.wait();
						}

					if ( !photosToLoad.empty())
					{
						PhotoToLoad photoToLoad;
						synchronized (photosToLoad)
						{
							photoToLoad = photosToLoad.pop();
						}

						Bitmap bmp = getBitmapDisk( photoToLoad.url );
						if (bmp == null)
						{
							webloader.queuePhoto( photoToLoad );
							continue;
						}

						// check if we still want the bitmap
						String tag = imageViews.get( photoToLoad.imageView );
						if (tag != null && tag.equals( photoToLoad.url ))
							new GalleryItemDisplayer( photoToLoad.imageView, bmp, ScaleType.CENTER_CROP, true ).display();
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
					// thread waits until there are any images to load in the
					// queue
					if (photosToLoad.empty())
						synchronized (photosToLoad)
						{
							photosToLoad.wait();
						}

					if ( !photosToLoad.empty())
					{
						PhotoToLoad photoToLoad;
						synchronized (photosToLoad)
						{
							photoToLoad = photosToLoad.pop();
						}

						Bitmap bmp = getBitmapWeb( photoToLoad.url );

						if ( bmp == null )
						{
							synchronized (photosToLoad)
							{
								photosToLoad.add( 0, photoToLoad );
							}
							continue;
						}

						// check if we still want the bitmap
						String tag = imageViews.get( photoToLoad.imageView );
						if (tag != null && tag.equals( photoToLoad.url ))
							new GalleryItemDisplayer( photoToLoad.imageView, bmp, ScaleType.CENTER_CROP, true ).display();
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