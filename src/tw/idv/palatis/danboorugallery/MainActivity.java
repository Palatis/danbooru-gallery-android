package tw.idv.palatis.danboorugallery;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Hosts;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.utils.DanbooruUncaughtExceptionHandler;
import tw.idv.palatis.danboorugallery.utils.LazyImageAdapter;
import tw.idv.palatis.danboorugallery.utils.LazyPostFetcher;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView.ScaleType;

public class MainActivity extends Activity
{
	LazyImageAdapter adapter;
	LazyPostFetcher fetcher;
	SharedPreferences preferences;

	List<Post> posts;
	Hosts hosts;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.d(D.LOGTAG, "onCreate()");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// register a global exception handler here so we don't just quit...
		Thread.setDefaultUncaughtExceptionHandler( new DanbooruUncaughtExceptionHandler(this) );

		GalleryItemDisplayer.setActivity(this);
		preferences = getSharedPreferences("DanbooruGallery", MODE_PRIVATE);
		loadPreferences();

		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int gallery_item_size = Math.min(display.getWidth() / 3, display.getHeight() / 3);
		int numcols = display.getWidth() / gallery_item_size;
		if ( numcols < ((double)display.getWidth() / gallery_item_size) )
			++numcols;
		gallery_item_size = display.getWidth() / numcols - 6; // padding

		ConfigurationEnclosure conf = (ConfigurationEnclosure) getLastNonConfigurationInstance();
		if ( conf != null )
		{
			posts = conf.posts;
			fetcher = new LazyPostFetcher(conf.url_enclosure);
		}
		else
		{
			posts = new ArrayList<Post>();
			fetcher = new LazyPostFetcher();
		}

		GridView grid = (GridView)findViewById( R.id.gallery_grid );
		grid.setNumColumns(numcols);

		adapter = new LazyImageAdapter( this, posts, gallery_item_size );
		grid.setAdapter(adapter);
		grid.setOnScrollListener(
			new GalleryOnScrollListener(
				fetcher,
				adapter,
				Toast.makeText(this, R.string.main_loading_next_page, Toast.LENGTH_SHORT),
				preferences.getInt("page_limit", 16) / 4
			)
		);
		grid.setOnItemClickListener(new GalleryOnItemClickListener(posts, this));
	}

	public void loadPreferences()
	{
		Log.d(D.LOGTAG, "loadPreferences()");

		Editor prefeditor = preferences.edit();

		if ( !preferences.contains("preference_version") )
			prefeditor.putInt("preference_version", D.PREFERENCE_VERSION );

		if ( !preferences.contains("serialized_hosts") )
		{
			hosts.add( "Danbooru", "http://danbooru.donmai.us/" );
			hosts.add( "Danbooru (mirror)", "http://hijiribe.donmai.us/" );
			hosts.add( "Konachan", "http://konachan.com/" );
			hosts.add( "Sankaku Complex (Chan)", "http://chan.sankakucomplex.com/" );
			hosts.add( "Sankaku Complex (Idol)", "http://idol.sankakucomplex.com/" );
			prefeditor.putString("serialized_hosts", hosts.toCSV() );
		}

		if ( !preferences.contains("selected_host") )
			prefeditor.putInt("selected_host", 0);

		if ( !preferences.contains("page_limit") )
			prefeditor.putInt("page_limit", 16);

		if ( !preferences.contains("rating") )
			prefeditor.putString("rating", "s");

		prefeditor.apply();
	}

	@Override
	public void onStart()
	{
		Log.d(D.LOGTAG, "onStart(): selected_host = " + preferences.getInt("selected_host", 0) + ", page_limit = " + preferences.getInt("page_limit", 16) + ", rating = " + preferences.getString("rating", "s"));
		try
		{
			hosts = Hosts.fromCSV( preferences.getString("serialized_hosts", "") );
		}
		catch (IOException e)
		{
			hosts = new Hosts();
		}

		String host[] = hosts.get( preferences.getInt("selected_host", 0) );
		String url = "http://konachan.com/";
		if ( host != null )
			url = host[ Hosts.HOST_URL ];

		boolean reset = false;
		reset |= fetcher.setUrl( url + D.URL_POST );
		reset |= fetcher.setRating( preferences.getString("rating", "s") );
		reset |= fetcher.setPageLimit( preferences.getInt("page_limit", 16) );

		if ( reset )
		{
			Log.d(D.LOGTAG, "something changed, clear posts, stop lazyadapter, restart fetcher.");
			fetcher.setPage(1);
			fetcher.cancel();
			posts.clear();
			adapter.cancelAll();
			adapter.notifyDataSetChanged();
			fetcher.fetchNextPage(adapter);
		}

		super.onStart();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Builder builder = null;
		switch(item.getItemId())
		{
		case R.id.main_menu_goto_page:
		{
			builder = new AlertDialog.Builder( this );
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			builder.setView(input);
			builder.setTitle( R.string.main_goto_page_dialog_title );
			builder.setPositiveButton( android.R.string.ok,
				new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if ( fetcher.setPage(Integer.parseInt("0" + input.getText().toString())) )
						{
							fetcher.cancel();
							posts.clear();
							adapter.notifyDataSetChanged();
						}
					}
				}

			);
			builder.setNegativeButton( android.R.string.cancel, null );
			builder.create().show();
		}
		break;
		case R.id.main_menu_goto_tags:
		{
			builder = new AlertDialog.Builder( this );
			final EditText input = new EditText(this);
			builder.setView(input);
			builder.setTitle( R.string.main_goto_tags_dialog_title );
			builder.setPositiveButton( android.R.string.ok,
				new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if ( fetcher.setTags(input.getText().toString()) )
						{
							fetcher.cancel();
							fetcher.setPage(1);
							posts.clear();
							adapter.notifyDataSetChanged();
							fetcher.fetchNextPage(adapter);
						}
					}
				}
			);
			builder.setNegativeButton( android.R.string.cancel, null );
			builder.create().show();
		}
		break;
		case R.id.main_menu_refresh:
			fetcher.cancel();
			fetcher.setPage(1);
			posts.clear();
			adapter.notifyDataSetChanged();
			fetcher.fetchNextPage(adapter);
		break;
		case R.id.main_menu_preferences:
			startActivity(new Intent(this, DanbooruGalleryPreferenceActivity.class));
		break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onDestroy()
	{
		fetcher.cancel();
		adapter.onDestroy();
		super.onDestroy();
	}

	private class ConfigurationEnclosure
	{
		public List<Post> posts;
		public LazyPostFetcher.URLEnclosure url_enclosure;

		public ConfigurationEnclosure( List<Post> p, LazyPostFetcher.URLEnclosure e )
		{
			posts = p;
			url_enclosure = e;
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new ConfigurationEnclosure( posts, fetcher.getURLEnclosure() );
	}

	private class GalleryOnItemClickListener implements OnItemClickListener
	{
		List<Post> posts;
		Activity activity;

		public GalleryOnItemClickListener( List<Post> p, Activity a )
		{
			posts = p;
			activity = a;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			Intent intent = new Intent(activity, ViewImageActivity.class);
			intent.putExtra("post.preview_url", posts.get(position).preview_url);
			intent.putExtra("post.file_url", posts.get(position).file_url);
			intent.putExtra("post.author", posts.get(position).author);
			intent.putExtra("post.tags", posts.get(position).tags);
			intent.putExtra("post.width", posts.get(position).width);
			intent.putExtra("post.height", posts.get(position).height);

			if ( posts.get(position).created_at != null )
				intent.putExtra("post.created_at", posts.get(position).created_at.getTime());

			activity.startActivity(intent);
			activity.overridePendingTransition(R.anim.zoom_up, R.anim.zoom_exit);
		}
	}

	private class GalleryOnScrollListener implements OnScrollListener
	{
		LazyPostFetcher fetcher;
		LazyImageAdapter adapter;
		Toast toast_loading;
		int fetch_threshold;

		public GalleryOnScrollListener( LazyPostFetcher f, LazyImageAdapter a, Toast t, int l )
		{
			fetcher = f;
			adapter = a;
			toast_loading = t;
			fetch_threshold = l;
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if ( firstVisibleItem + visibleItemCount > totalItemCount - fetch_threshold )
			{
				toast_loading.show();
				fetcher.fetchNextPage(adapter);
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState)
		{
			// do nothing
		}
	}

	static public class GalleryItemDisplayer implements Runnable
	{
		private static Activity activity;

		ImageView image;
		Bitmap bitmap;

		static public void setActivity( Activity a )
		{
			activity = a;
		}

		public void display( ImageView v, Bitmap b )
		{
			image = v;
			bitmap = b;
			activity.runOnUiThread(this);
		}

		@Override
		public void run()
		{
			if ( bitmap != null )
			{
				image.setImageBitmap(bitmap);
				image.setScaleType(ScaleType.CENTER_CROP);
			}
		}
	}
}