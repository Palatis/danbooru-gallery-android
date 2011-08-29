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

import java.util.List;

import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.model.Post;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class LazyImageAdapter
	extends BaseAdapter
{
	final LayoutInflater	inflater;

	Activity				activity;
	List < Post >			posts;
	ImageLoader				loader;
	int						item_size;

	public LazyImageAdapter(Activity a, List < Post > p, int sz)
	{
		activity = a;
		posts = p;
		inflater = activity.getLayoutInflater();
		loader = new ImageLoader( activity.getApplicationContext() );
		item_size = sz;
	}

	public void cancelAll()
	{
		loader.cancelAll();
	}

	public Activity getActivity()
	{
		return activity;
	}

	public void addPosts(List < Post > commit)
	{
		if (commit.isEmpty())
			return;

		posts.addAll( commit );

		// add the posts for aggressive preview image pre-fetching
		for (Post post : commit )
			loader.DisplayImage( post.preview_url, null );

		activity.runOnUiThread( new Runnable()
		{
			@Override
			public void run()
			{
				LazyImageAdapter.this.notifyDataSetChanged();
			}
		} );
	}

	@Override
	public int getCount()
	{
		return posts.size();
	}

	@Override
	public Object getItem(int position)
	{
		return posts.get( position );
	}

	@Override
	public long getItemId(int position)
	{
		return posts.get( position ).hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ImageView image = null;
		if (convertView != null)
		{
			image = (ImageView) convertView;
			image.setScaleType( ScaleType.CENTER );
		}

		if (image == null)
		{
			image = (ImageView) inflater.inflate( R.layout.gallery_image, null );
			image.setLayoutParams( new GridView.LayoutParams( item_size, item_size ) );
		}

		loader.DisplayImage( posts.get( position ).preview_url, image );

		return image;
	}

	public void onDestroy()
	{
		loader.stopThread();
	}

	public void onLowMemory()
	{
		loader.onLowMemory();
	}
}