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

import java.util.Map;
import java.util.WeakHashMap;

import android.graphics.Bitmap;

public class BitmapMemCache
{
	private static BitmapMemCache instance = new BitmapMemCache();

	public static BitmapMemCache getInstance()
	{
		return instance;
	}

	private Map< String, Bitmap > cache;

	private BitmapMemCache()
	{
		cache = new WeakHashMap< String, Bitmap >();
	}

	public Bitmap get(String key)
	{
		if (cache.containsKey(key))
			return cache.get(key);
		return null;
	}

	public void put( String key, Bitmap bitmap )
	{
		synchronized(cache)
		{
			cache.put( key, bitmap );
		}
	}

	public void clear()
	{
		for (Map.Entry<String, Bitmap> entry : cache.entrySet())
			entry.getValue().recycle();
		cache.clear();
	}
}