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

import tw.idv.palatis.danboorugallery.defines.D;
import android.content.Context;

public class FileCache
{
	private File	chunkdirs[]	= null;

	public FileCache(Context context)
	{
		// Find the directory to save cached images
		File cachedir;
		if (android.os.Environment.getExternalStorageState().equals( android.os.Environment.MEDIA_MOUNTED ))
			cachedir = new File( android.os.Environment.getExternalStorageDirectory(), D.CACHEDIR );
		else
			cachedir = context.getCacheDir();

		chunkdirs = new File[100];
		for (int i = 0; i < 100; ++i)
		{
			chunkdirs[i] = new File( cachedir, String.valueOf( i ) );
			if ( !chunkdirs[i].exists())
				chunkdirs[i].mkdirs();
		}
	}

	public File getFile(String url)
	{
		return new File( chunkdirs[Math.abs( url.hashCode() % 100 )], String.valueOf( url.hashCode() ) );
	}

	public void clear()
	{
		for (File chunkdir : chunkdirs)
		{
			File[] files = chunkdir.listFiles();
			for (File f : files)
				f.delete();
		}
	}
}