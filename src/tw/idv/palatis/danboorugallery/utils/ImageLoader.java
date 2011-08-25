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

import tw.idv.palatis.danboorugallery.MainActivity;
import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.defines.D;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

public class ImageLoader
{
	private BitmapMemCache memCache;
	private FileCache fileCache;
	private Map<ImageView, String> imageViews = Collections
			.synchronizedMap(new WeakHashMap<ImageView, String>());
	private PhotosLoader loader;

	public ImageLoader( Context context ) {
		// Make the background thread low priority. This way it will not affect
		// the UI performance
		loader = new PhotosLoader();
		loader.setPriority(Thread.MIN_PRIORITY);
		loader.start();

		fileCache = new FileCache(context);
		memCache = BitmapMemCache.getInstance();
	}

	// final int stub_id=R.drawable.stub;
	final int stub_id = R.drawable.icon;

	public void DisplayImage(String url, Activity activity, ImageView image)
	{
		// This ImageView may be used for other images before. So there may be
		// some old tasks in the queue. We need to discard them.
		loader.discard(image);
		imageViews.put(image, url);

		Bitmap bitmap = getBitmapCache( url );
		if (bitmap != null)
		{
			MainActivity.GalleryItemDisplayer displayer = new MainActivity.GalleryItemDisplayer();
			displayer.display(image, bitmap);
			return;
		}

		queuePhoto( url, activity, image );
		image.setImageResource(stub_id);
	}

	public void cancelAll()
	{
		synchronized(loader.photosToLoad)
		{
			loader.photosToLoad.clear();
			loader.photosToLoad.notifyAll();
		}
	}

	private void queuePhoto(String url, Activity activity, ImageView imageView)
	{
		PhotoToLoad p = new PhotoToLoad(url, imageView);
		synchronized (loader.photosToLoad)
		{
			loader.photosToLoad.push(p);
			loader.photosToLoad.notifyAll();
		}
	}

	private void CopyStream(InputStream is, OutputStream os) {
		final int buffer_size = 1024;

		try {
			byte[] bytes = new byte[buffer_size];
			for (;;) {
				int count = is.read(bytes, 0, buffer_size);
				if (count == -1)
					break;
				os.write(bytes, 0, count);
			}
		}
		catch (Exception ex)
		{

		}
	}

	private Bitmap getBitmapCache( String url )
	{
		try
		{
			// from Memory cache
			Bitmap bitmap = memCache.get(url);
			if ( bitmap != null )
				return bitmap;

			// from SD cache
			bitmap = D.getBitmapFromFile(fileCache.getFile(url));
			if ( bitmap != null )
				memCache.put(url, bitmap);

			return bitmap;
		}
		catch (Exception ex)
		{
			// fail OK, we fetch the picture from web...
		}
		return null;
	}

	private Bitmap getBitmapWeb(String url)
	{
		try {
			// from web
			URL imageUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
			InputStream is = conn.getInputStream();
			OutputStream os = new FileOutputStream(fileCache.getFile(url));
			CopyStream(is, os);
			os.close();

			return D.getBitmapFromFile(fileCache.getFile(url));
		} catch (Exception ex) {
			Log.d(D.LOGTAG, "image " + url + " download failed!");
			Log.d(D.LOGTAG, ex.getMessage());
			ex.printStackTrace();
		}
		return null;
	}

	// Task for the queue
	private class PhotoToLoad {
		public String url;
		public ImageView imageView;

		public PhotoToLoad(String u, ImageView i)
		{
			url = u;
			imageView = i;
		}
	}

	public void stopThread()
	{
		loader.interrupt();
	}

	private class PhotosLoader extends Thread
	{
		private Stack<PhotoToLoad> photosToLoad = new Stack<PhotoToLoad>();

		public void discard(ImageView image)
		{
			synchronized(photosToLoad)
			{
				for (int j = 0; j < photosToLoad.size();)
					if (photosToLoad.get(j).imageView == image)
						photosToLoad.remove(j);
					else
						++j;
			}
		}

		@Override
		public void run() {
			try {
				while (true) {
					// thread waits until there are any images to load in the queue
					if (photosToLoad.empty())
						synchronized (photosToLoad)
						{
							photosToLoad.wait();
						}

					if (!photosToLoad.empty())
					{
						PhotoToLoad photoToLoad;
						synchronized (photosToLoad)
						{
							photoToLoad = photosToLoad.pop();
						}

						Bitmap bmp = getBitmapWeb(photoToLoad.url);

						// check if we still want the bitmap
						String tag = imageViews.get(photoToLoad.imageView);
						if (tag != null && tag.equals(photoToLoad.url))
						{
							MainActivity.GalleryItemDisplayer displayer = new MainActivity.GalleryItemDisplayer();
							memCache.put(photoToLoad.url, bmp);
							displayer.display(photoToLoad.imageView, bmp);
						}
					}
					if (Thread.interrupted())
						break;
				}
			} catch (InterruptedException e) {
				// allow thread to exit
			}
		}
	}
}