package tw.idv.palatis.danboorugallery.defines;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

public class D
{
	public static final String LOGTAG = "DanbooruGallery";
	public static final String CACHEDIR = "DanbooruGallery/cache";
	public static final String SAVEDIR = "DanbooruGallery";

	public static final String ERRORLOG_DIRECTORY = "DanbooruGallery";
	public static final String ERRORLOG_PREFIX = "Error";
	public static final String ERRORLOG_SUFFIX = ".txt";

	public static final int PREFERENCE_VERSION = 1;

	public static final String URL_POST = "/post/index.json?page=%1$s&tags=%2$s&limit=%3$s";
	// public static final String URL_TAGS = "";
	// public static final String URL_SEARCH = "";

	public static void makeToastOnUiThread( Activity activity, String message, int length )
	{
		activity.runOnUiThread(
			new Runnable() {
				Activity activity;
				String message;
				int duration;

				public Runnable initialize( Activity a, String m, int d )
				{
					activity = a;
					message = m;
					duration = d;
					return this;
				}

				@Override
				public void run() {
					Toast.makeText(activity, message, duration).show();
				}
			}.initialize(activity, message, length)
		);
	}

	private static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels)
	{
	    int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

	    int roundedSize;
	    if (initialSize <= 8)
	    {
	        roundedSize = 1;
	        while (roundedSize < initialSize)
				roundedSize <<= 1;
	    } else
			roundedSize = (initialSize + 7) / 8 * 8;

	    return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels)
	{
	    double w = options.outWidth;
	    double h = options.outHeight;

	    int lowerBound = (maxNumOfPixels == -1) ? 1 :
	            (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
	    int upperBound = (minSideLength == -1) ? 128 :
	            (int) Math.min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));

	    if (upperBound < lowerBound)
	        return lowerBound;

	    if ((maxNumOfPixels == -1) && (minSideLength == -1))
	        return 1;
	    else if (minSideLength == -1)
	        return lowerBound;
	    else
	        return upperBound;
	}


	public static Bitmap getBitmapFromFile( File file )
	{
		try
		{
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inPreferredConfig = Bitmap.Config.RGB_565;

			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeFile( file.getAbsolutePath(), opt );

			opt.inSampleSize = computeSampleSize(opt, -1, 2048 * 2048);
			opt.inJustDecodeBounds = false;

			// decodeFile() gets OutOfMemoryError very often, try decodeFileDescriptor()
			// return BitmapFactory.decodeFile( file.getAbsolutePath(), opt );
			FileInputStream input = new FileInputStream(file);
			return BitmapFactory.decodeFileDescriptor( input.getFD(), null, opt );
		}
		catch (OutOfMemoryError ex)
		{
			Log.d(D.LOGTAG, "decode failed, OutOfMemory occured, try free");
		}
		catch (FileNotFoundException ex)
		{
			// ok for file not found, get from web anyway.
		}
		catch (IOException ex)
		{
			// ok for io exception, get from web anyway.
		}
		return null;
	}
}