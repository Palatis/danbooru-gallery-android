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

import android.app.Activity;
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
}