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
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import tw.idv.palatis.danboorugallery.defines.D;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

public class FileCache {
	private File cachedir;
	private Mac mac;

	public FileCache(Context context){
		// Find the directory to save cached images
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
			cachedir = new File(android.os.Environment.getExternalStorageDirectory(), D.CACHEDIR);
		else
			cachedir = context.getCacheDir();

		if(!cachedir.exists())
			cachedir.mkdirs();

		SecretKeySpec key;
		try {
			key = new SecretKeySpec(("Danbooru Gallery").getBytes("UTF-8"), "HmacSHA1");
			mac = Mac.getInstance("HmacSHA1");
			mac.init(key);
		} catch (UnsupportedEncodingException e) {

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public File getFile(String url)
	{
		String filename = null;
		try
		{
			filename = sha1(url);
			// the trailing "=" is making problem...
			filename = filename.substring(0, filename.length() - 2);
		}
		catch (Exception ex)
		{
			filename = String.valueOf(url.hashCode());
			Log.e(D.LOGTAG, url + " cannot be encoded to sha1, using " + filename);
			Log.d(D.LOGTAG, Log.getStackTraceString(ex));
		}

		return new File(cachedir, filename);
	}

	public void clear()
	{
		File[] files = cachedir.listFiles();
		for(File f:files)
			f.delete();
	}

	private String sha1(String s)
		throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException
	{
		byte[] bytes = mac.doFinal(s.getBytes("UTF-8"));
		return Base64.encodeToString(bytes, Base64.URL_SAFE);
	}
}