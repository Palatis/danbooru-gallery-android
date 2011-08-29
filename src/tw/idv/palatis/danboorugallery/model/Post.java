package tw.idv.palatis.danboorugallery.model;

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

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

public class Post
{
	public String	preview_url;
	public String	file_url;
	public String	author;
	public String	tags;
	public Date		created_at;
	public int		width;
	public int		height;

	public Post()
	{
	}

	public Post(JSONObject json_post)
	{
		preview_url = json_post.optString( "preview_url" );
		file_url = json_post.optString( "file_url" );

		// if the preview_url is a relative url, add the host part from file_url to it.
		Uri puri = Uri.parse( preview_url );
		if (puri.getHost() == null)
		{
			Uri furi = Uri.parse( file_url );
			preview_url = furi.getScheme() + "://" + furi.getHost() + "/" + preview_url;
		}

		author = json_post.optString( "author" );
		tags = json_post.optString( "tags" );
		width = json_post.optInt( "width" );
		height = json_post.optInt( "height" );

		// date is a little tricky, we might get 2 formats...
		try
		{
			// it might just be an long value
			created_at = new Date( json_post.getLong( "created_at" ) * 1000 );
		}
		catch (JSONException ex)
		{
			try
			{
				// or it might be a JSONObject with json_class == "Time"
				JSONObject json_created_at = json_post.getJSONObject( "created_at" );
				if (json_created_at.getString( "json_class" ).equals( "Time" ))
					created_at = new Date( json_created_at.getLong( "s" ) * 1000 + json_created_at.getLong( "n" ) / 1000000 );
			}
			catch (JSONException ex2)
			{
				// it's okay to not having a date...
			}
		}
	}

	public Post(String pUrl, String fUrl, String athr, String t, Date ct, int w, int h)
	{
		preview_url = pUrl;
		file_url = fUrl;
		author = athr;
		tags = t;
		created_at = ct;
		width = w;
		height = h;
	}
}
