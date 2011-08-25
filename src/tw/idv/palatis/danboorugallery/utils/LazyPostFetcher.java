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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Post;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.widget.Toast;

public class LazyPostFetcher
{
	private AsyncPostFetcher fetcher = null;
	public URLEnclosure enclosure = null;

	public LazyPostFetcher()
	{
		enclosure = new URLEnclosure();
	}

	public LazyPostFetcher( URLEnclosure e )
	{
		enclosure = e;
	}

	public boolean setUrl(String url)
	{
		Log.e(D.LOGTAG, "setting url to " + url);
		boolean result = enclosure.url_format.equals(url);
		enclosure.url_format = url;
		return !result;
	}

	public boolean setPage(int page)
	{
		page = (page < 1) ? 1 : page;
		boolean result = (enclosure.page == page);
		enclosure.page = page;
		return !result;
	}

	public boolean setTags(String tags)
	{
		boolean result = enclosure.tags.equals(tags);
		enclosure.tags = tags;
		return !result;
	}

	public boolean setPageLimit(int limit)
	{
		boolean result = (enclosure.limit == limit);
		enclosure.limit = limit;
		return !result;
	}

	public boolean setRating(String rating)
	{
		boolean result = enclosure.rating.equals(rating);
		enclosure.rating = rating;
		return !result;
	}

	public URLEnclosure getURLEnclosure()
	{
		return enclosure;
	}

	public void fetchNextPage( LazyImageAdapter adapter )
	{
		if ( fetcher != null && fetcher.getStatus() == Status.RUNNING )
			return;
	
		fetcher = new AsyncPostFetcher();
		fetcher.setAdapter(adapter);
		fetcher.execute( enclosure );
	}

	public void cancel()
	{
		if ( fetcher != null && fetcher.getStatus() == Status.RUNNING )
		{
			fetcher.cancel(true);
			fetcher = null;
		}
	}

	public class URLEnclosure
	{
		public String url_format;
		public String tags;
		public int page;
		public int limit;
		public String rating;

		public URLEnclosure()
		{
			url_format = "";
			tags = "";
			page = 1;
			limit = 16;
			rating = "";
		}

		public URLEnclosure( String f, int p, int l, String t, String r )
		{
			url_format = f;
			tags = t;
			page = p;
			limit = l;
			rating = r;
		}
	}

	private class AsyncPostFetcher extends AsyncTask<URLEnclosure, Integer, Integer>
	{
		LazyImageAdapter adapter;

		public void setAdapter( LazyImageAdapter a )
		{
			adapter = a;
		}

		@Override
		protected Integer doInBackground(URLEnclosure... params)
		{
			char buffer[] = new char[8192];
			int fetched_posts_count = 0;
			int skipped_posts_count = 0;

			try
			{
				for (URLEnclosure enclosure: params)
				{
					while ( fetched_posts_count < enclosure.limit )
					{
						URL url = new URL(String.format(enclosure.url_format, enclosure.page, enclosure.tags, enclosure.limit));

						Log.v(D.LOGTAG, "fetching " + url.toString() + " (" + fetched_posts_count + " fetched, " + skipped_posts_count + " skipped)");

						if (isCancelled())
							return fetched_posts_count;

						Reader input = new BufferedReader( new InputStreamReader( url.openStream(), "UTF-8" ) );
						Writer output = new StringWriter();

						int count = 0;
						while ((count = input.read(buffer)) > 0)
						{
							output.write(buffer, 0, count);
							if (isCancelled())
								return fetched_posts_count;
						}

						try
						{
							ArrayList<Post> posts = new ArrayList<Post>();
							JSONArray json_posts = new JSONArray( output.toString() );
							int len = json_posts.length();
							posts.ensureCapacity(len);
							for (int j = 0;j < len; ++j)
							{
								JSONObject json_post = json_posts.getJSONObject(j);

								try
								{
									if ( enclosure.rating.indexOf( json_post.getString("rating") ) == -1 )
									{
										++skipped_posts_count;
										continue;
									}
								}
								catch (JSONException ex)
								{
								}

								Post post = new Post( json_post );

								posts.add(post);
								++fetched_posts_count;

								if (isCancelled())
									return fetched_posts_count;
							}
							adapter.addPosts(posts);
						}
						catch (JSONException ex)
						{
							D.makeToastOnUiThread(adapter.getActivity(), ex.getMessage(), Toast.LENGTH_LONG);
						}

						if (isCancelled())
							return fetched_posts_count;

						++enclosure.page;
					}
				}
			}
			catch (IOException ex)
			{
				adapter.getActivity().runOnUiThread(
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
					}.initialize(adapter.getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_LONG)
				);
			}

			return fetched_posts_count;
		}
	}
}