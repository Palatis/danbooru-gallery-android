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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.utils.BitmapMemCache;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

public class D
{
	public static final String	CACHEDIR				= "DanbooruGallery/.cache";
	public static final String	SAVEDIR					= "DanbooruGallery";

	public static final String	ERRORLOG_DIRECTORY		= "DanbooruGallery";
	public static final String	ERRORLOG_PREFIX			= "Error";
	public static final String	ERRORLOG_SUFFIX			= ".txt";

	public static final String	SHAREDPREFERENCES_NAME	= "DanbooruGallery";
	public static final int		PREFERENCE_VERSION		= 1;

	public static void makeToastOnUiThread(Activity activity, int resourceId, int length)
	{
		activity.runOnUiThread( new Runnable()
		{
			Activity	activity;
			int			resourceId;
			int			duration;

			public Runnable initialize(Activity a, int r, int d)
			{
				activity = a;
				resourceId = r;
				duration = d;
				return this;
			}

			@Override
			public void run()
			{
				Toast.makeText( activity, resourceId, duration ).show();
			}
		}.initialize( activity, resourceId, length ) );
	}

	private static final String	LOGTAG	= "DanbooruGallery";

	public static class Log
	{
		public static void d(String str)
		{
			android.util.Log.d( LOGTAG, str );
		}

		public static void d(String format, Object... params)
		{
			android.util.Log.d( LOGTAG, String.format( format, params ) );
		}

		public static void d(Throwable ex, String format, Object... params)
		{
			android.util.Log.d( LOGTAG, String.format( format, params ), ex );
		}

		public static void e(String str)
		{
			android.util.Log.e( LOGTAG, str );
		}

		public static void e(String format, Object... params)
		{
			android.util.Log.e( format, format );
		}

		public static void e(Throwable ex, String format, Object... params)
		{
			android.util.Log.e( LOGTAG, String.format( format, params ), ex );
		}

		public static void i(String str)
		{
			android.util.Log.i( LOGTAG, str );
		}

		public static void i(String format, Object... params)
		{
			android.util.Log.i( LOGTAG, String.format( format, params ) );
		}

		public static void i(Throwable ex, String format, Object... params)
		{
			android.util.Log.i( LOGTAG, String.format( format, params ), ex );
		}

		public static void v(String str)
		{
			android.util.Log.v( LOGTAG, str );
		}

		public static void v(String format, Object... params)
		{
			android.util.Log.v( LOGTAG, String.format( format, params ) );
		}

		public static void v(Throwable ex, String format, Object... params)
		{
			android.util.Log.v( LOGTAG, String.format( format, params ), ex );
		}

		public static void w(String str)
		{
			android.util.Log.w( LOGTAG, str );
		}

		public static void w(Throwable ex)
		{
			android.util.Log.w( LOGTAG, ex );
		}

		public static void w(String format, Object... parmas)
		{
			android.util.Log.w( LOGTAG, String.format( format, parmas ) );
		}

		public static void w(Throwable ex, String format, Object... params)
		{
			android.util.Log.w( LOGTAG, String.format( format, params ), ex );
		}

		public static void wtf(String str)
		{
			android.util.Log.wtf( LOGTAG, str );
		}

		public static void wtf(Throwable ex)
		{
			android.util.Log.wtf( LOGTAG, ex );
		}

		public static void wtf(String format, Object... parmas)
		{
			android.util.Log.wtf( LOGTAG, String.format( format, parmas ) );
		}

		public static void wtf(Throwable ex, String format, Object... params)
		{
			android.util.Log.wtf( LOGTAG, String.format( format, params ), ex );
		}
	}

	public static void makeToastOnUiThread(Activity activity, String message, int length)
	{
		activity.runOnUiThread( new Runnable()
		{
			Activity	activity;
			String		message;
			int			duration;

			public Runnable initialize(Activity a, String m, int d)
			{
				activity = a;
				message = m;
				duration = d;
				return this;
			}

			@Override
			public void run()
			{
				Toast.makeText( activity, message, duration ).show();
			}
		}.initialize( activity, message, length ) );
	}

	private static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels)
	{
		int initialSize = computeInitialSampleSize( options, minSideLength, maxNumOfPixels );

		int roundedSize;
		if (initialSize <= 8)
		{
			roundedSize = 1;
			while (roundedSize < initialSize)
				roundedSize <<= 1;
		}
		else
			roundedSize = (initialSize + 7) / 8 * 8;

		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels)
	{
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil( Math.sqrt( w * h / maxNumOfPixels ) );
		int upperBound = (minSideLength == -1) ? 128 : (int) Math.min( Math.floor( w / minSideLength ), Math.floor( h / minSideLength ) );

		if (upperBound < lowerBound)
			return lowerBound;

		if ((maxNumOfPixels == -1) && (minSideLength == -1))
			return 1;
		else if (minSideLength == -1)
			return lowerBound;
		else
			return upperBound;
	}

	public static void CopyStream(InputStream is, OutputStream os)
	{
		final int buffer_size = 1024;

		try
		{
			byte[] bytes = new byte[buffer_size];
			for (;;)
			{
				int count = is.read( bytes, 0, buffer_size );
				if (count == -1)
					break;
				os.write( bytes, 0, count );
			}
		}
		catch (Exception ex)
		{

		}
	}

	public static Bitmap getBitmapFromFile(File file)
	{
		try
		{
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inPreferredConfig = Bitmap.Config.RGB_565;

			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeFile( file.getAbsolutePath(), opt );

			opt.inSampleSize = computeSampleSize( opt, -1, 2048 * 2048 );
			opt.inJustDecodeBounds = false;
			opt.inPurgeable = true;
			opt.inInputShareable = true;

			return BitmapFactory.decodeFile( file.getAbsolutePath(), opt );
		}
		catch (OutOfMemoryError ex)
		{
			Log.d( "decode failed, OutOfMemory occured." );
			BitmapMemCache.getInstance().clear();
		}
		return null;
	}

	public static JSONArray JSONArrayFromHosts(List < Host > hosts)
	{
		JSONArray array = new JSONArray();
		for (Host host : hosts)
			array.put( host.toJSONObject() );
		return array;
	}

	public static List < Host > HostsFromJSONArray(JSONArray array)
	{
		int length = array.length();
		List < Host > hosts = new ArrayList < Host >( length );
		for (int i = 0; i < length; ++i)
			try
			{
				hosts.add( new Host( array.getJSONObject( i ) ) );
			}
			catch (JSONException ex)
			{
				Log.wtf( ex );
			}
		return hosts;
	}
}