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
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import tw.idv.palatis.danboorugallery.defines.D;
import android.content.Context;

public class FileCache
{
	private static FileCache	instance	= null;

	public static final FileCache getInstance()
	{
		return instance;
	}

	public static void prepare(Context context)
	{
		if (instance == null)
			instance = new FileCache( context );
	}

	private Map < String, WeakReference < File >>	mCacheFiles	= Collections.synchronizedMap( new WeakHashMap < String, WeakReference < File >>() );
	private File									mCunkDirs[]	= null;

	private FileCache(Context context)
	{
		// Find the directory to save cached images
		File cachedir;
		if (android.os.Environment.getExternalStorageState().equals( android.os.Environment.MEDIA_MOUNTED ))
			cachedir = new File( android.os.Environment.getExternalStorageDirectory(), D.CACHEDIR );
		else
			cachedir = context.getCacheDir();

		mCunkDirs = new File[100];
		for (int i = 0; i < 100; ++i)
		{
			mCunkDirs[i] = new File( cachedir, String.valueOf( i ) );
			if ( !mCunkDirs[i].exists())
				mCunkDirs[i].mkdirs();
		}
	}

	public File getFile(String url)
	{
		File file = null;
		if ( mCacheFiles.containsKey( url ))
			file = mCacheFiles.get( url ).get();

		if (file == null)
		{
			file = new File( mCunkDirs[Math.abs( url.hashCode() % 100 )], String.valueOf( url.hashCode() ) );
			mCacheFiles.put( url, new WeakReference < File >( file ) );
		}

		synchronized (file)
		{
			return file;
		}
	}

	public void clear()
	{
		for (File chunkdir : mCunkDirs)
		{
			File[] files = chunkdir.listFiles();
			for (File f : files)
				f.delete();
		}
	}
}