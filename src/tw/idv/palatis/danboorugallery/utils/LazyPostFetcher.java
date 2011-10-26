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

import java.util.ArrayList;
import java.util.List;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.siteapi.SiteAPI;
import android.os.AsyncTask;

public class LazyPostFetcher
{
	private AsyncPostFetcher	fetcher		= null;
	private URLEnclosure		enclosure	= null;
	private SiteAPI			site_api	= null;
	boolean						reached_end	= false;

	public LazyPostFetcher()
	{
		enclosure = new URLEnclosure();
	}

	public LazyPostFetcher(URLEnclosure e)
	{
		enclosure = e;
	}

	public boolean setSiteAPI(SiteAPI sapi)
	{
		if (site_api == null)
		{
			site_api = sapi;
			return false;
		}

		boolean result = true;
		result &= site_api.getClass().getName().equals( sapi.getClass().getName() );
		result &= site_api.getSiteUrl().equals( sapi.getSiteUrl() );
		result &= site_api.getApi() == sapi.getApi();
		site_api = sapi;
		if ( !result)
			reached_end = false;
		return !result;
	}

	public int getPage()
	{
		return enclosure.page;
	}

	public boolean setPage(int page)
	{
		page = (page < 1) ? 1 : page;
		boolean result = (enclosure.page == page);
		enclosure.page = page;
		if ( !result)
			reached_end = false;
		return !result;
	}

	public boolean setTags(String tags)
	{
		boolean result = enclosure.tags.equals( tags );
		enclosure.tags = tags;
		if ( !result)
			reached_end = false;
		return !result;
	}

	public boolean setPageLimit(int limit)
	{
		boolean result = (enclosure.limit == limit);
		enclosure.limit = limit;
		if ( !result)
			reached_end = false;
		return !result;
	}

	public boolean setRating(String rating)
	{
		boolean result = enclosure.rating.equals( rating );
		enclosure.rating = rating;
		if ( !result)
			reached_end = false;
		return !result;
	}

	public URLEnclosure getURLEnclosure()
	{
		return enclosure;
	}

	public void fetchNextPage(LazyImageAdapter adapter)
	{
		if (fetcher == null)
		{
			fetcher = new AsyncPostFetcher( adapter, this );
			fetcher.execute( enclosure );
		}
	}

	public void cancel()
	{
		if (fetcher != null)
		{
			fetcher.cancel( true );
			fetcher = null;
		}
	}

	public boolean hasMorePost()
	{
		return !reached_end;
	}

	public void noMorePosts()
	{
		reached_end = true;
	}

	public class URLEnclosure
	{
		public String	tags;
		public int		page;
		public int		limit;
		public String	rating;

		public URLEnclosure()
		{
			this( 1, 16, "", "" );
		}

		public URLEnclosure(int p, int l, String t, String r)
		{
			tags = t;
			page = p;
			limit = l;
			rating = r;
		}
	}

	private static class AsyncPostFetcher
		extends AsyncTask < URLEnclosure, Integer, Integer >
	{
		LazyImageAdapter	adapter;
		LazyPostFetcher		fetcher;

		AsyncPostFetcher(LazyImageAdapter a, LazyPostFetcher f)
		{
			adapter = a;
			fetcher = f;
		}

		@Override
		protected Integer doInBackground(URLEnclosure... params)
		{
			int fetched_posts_count = 0;
			int skipped_posts_count = 0;

			for (URLEnclosure enclosure : params)
				while (fetched_posts_count < enclosure.limit)
				{
					if (isCancelled())
						break;

					if (fetcher.site_api == null)
						break;

					List < Post > posts = fetcher.site_api.fetchPostsIndex( enclosure.page, enclosure.tags, enclosure.limit );
					if (posts == null)
						continue;
					List < Post > filtered = new ArrayList < Post >( posts.size() );
					for (Post post : posts)
					{
						if (enclosure.rating.contains( post.rating ))
							filtered.add( post );
						else
							++skipped_posts_count;

						if (isCancelled())
							break;
					}

					if (posts.size() == 0)
					{
						fetcher.noMorePosts();
						break;
					}

					if (isCancelled())
						break;

					adapter.addPosts( filtered );
					publishProgress();
					fetched_posts_count += filtered.size();

					D.Log.d( "AsyncPostFetcher::doInBackground(): fetched + skipped / total: %d + %d / %d", fetched_posts_count, skipped_posts_count, fetched_posts_count + skipped_posts_count );
					++fetcher.enclosure.page;
				}

			return fetched_posts_count;
		}

		@Override
		protected void onProgressUpdate(Integer... values)
		{
			adapter.notifyDataSetChanged();
		}

		@Override
		protected void onCancelled()
		{
			fetcher.fetcher = null;
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			fetcher.fetcher = null;
			adapter.notifyDataSetChanged();
		}
	}
}