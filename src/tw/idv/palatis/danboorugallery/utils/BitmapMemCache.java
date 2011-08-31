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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import tw.idv.palatis.danboorugallery.defines.D;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;

public class BitmapMemCache
{
	private static BitmapMemCache	instance	= null;

	public static void prepare(Context context)
	{
		instance = new BitmapMemCache( context );
	}

	public static BitmapMemCache getInstance()
	{
		return instance;
	}

	private Map < String, Bitmap >	cache;

	private BitmapMemCache(Context context)
	{
		ActivityManager manager = (ActivityManager) context.getSystemService( Service.ACTIVITY_SERVICE );
		int cache_size = manager.getMemoryClass() * 8;
		D.Log.d( "Using %d for cache size", cache_size );
		cache = Collections.synchronizedMap( new LruCache < String, Bitmap >( cache_size ) );
	}

	public Bitmap get(String key)
	{
		return cache.get( key );
	}

	public void put(String key, Bitmap bitmap)
	{
		cache.put( key, bitmap );
	}

	public void clear()
	{
		for (Map.Entry < String, Bitmap > entry : cache.entrySet())
			entry.getValue().recycle();
		cache.clear();
	}

	@SuppressWarnings("serial")
	private class LruCache < K, V >
		extends LinkedHashMap < K, V >
	{
		private final int	maxEntries;

		public LruCache(final int maxEntries)
		{
			super( maxEntries + 1, 1.0f, true );
			this.maxEntries = maxEntries;
		}

		/**
		 * Returns <tt>true</tt> if this <code>LruCache</code> has more entries
		 * than the maximum specified when it was
		 * created.
		 * <p>
		 * This method <em>does not</em> modify the underlying <code>Map</code>;
		 * it relies on the implementation of <code>LinkedHashMap</code> to do
		 * that, but that behavior is documented in the JavaDoc for
		 * <code>LinkedHashMap</code>.
		 * </p>
		 *
		 * @param eldest
		 *            the <code>Entry</code> in question; this implementation
		 *            doesn't care what it is, since the
		 *            implementation is only dependent on the size of the cache
		 * @return <tt>true</tt> if the oldest
		 * @see java.util.LinkedHashMap#removeEldestEntry(Map.Entry)
		 */
		@Override
		protected boolean removeEldestEntry(final Map.Entry < K, V > eldest)
		{
			return super.size() > maxEntries;
		}
	}
}