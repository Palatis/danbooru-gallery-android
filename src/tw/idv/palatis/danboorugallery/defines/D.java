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

import tw.idv.palatis.danboorugallery.utils.BitmapMemCache;
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

	public static Bitmap getBitmapFromFile( File file, int itemSize )
	{
		boolean retry = true;
		while ( true )
			try
			{
				BitmapFactory.Options opt = new BitmapFactory.Options();
				if ( itemSize > 0 )
				{
					opt.inJustDecodeBounds = true;
					BitmapFactory.decodeFile( file.getAbsolutePath(), opt );

					int width = opt.outWidth;
					int height = opt.outHeight;

					opt = new BitmapFactory.Options();
					opt.inSampleSize = (int) Math.floor( Math.min((double)width / itemSize, (double)height / itemSize) );
				}
				opt.inTempStorage = new byte[32*1024];
				opt.inPreferredConfig = Bitmap.Config.RGB_565;

				// decodeFile() gets OutOfMemoryError very often, try decodeFileDescriptor()
				// return BitmapFactory.decodeFile( file.getAbsolutePath(), opt );
				FileInputStream input = new FileInputStream(file);
				return BitmapFactory.decodeFileDescriptor( input.getFD(), null, opt );
			}
			catch ( OutOfMemoryError ex )
			{
				BitmapMemCache.getInstance().clear();
				Log.d(D.LOGTAG, "decode failed, OutOfMemory occured.");

				if ( retry == false )
					return null;

				retry = false;
			}
			catch (FileNotFoundException ex)
			{
				// ok for file not found, get from web anyway.
				return null;
			}
			catch (IOException ex)
			{
				// ok for io exception, get from web anyway.
				return null;
			}
	}
}