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

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.utils.BitmapMemCache;
import tw.idv.palatis.danboorugallery.utils.FileCache;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView.ScaleType;

public class ViewImageActivity
	extends Activity
{
	private static int	INFOPANE_DELAY	= 4000;

	FileCache			filecache;
	AsyncImageLoader	loader;

	ImageViewTouch		image;
	RelativeLayout		infopane;

	Post				post;
	Host				host;
	String				page_tags;
	String				image_url;

	long				lastClick;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.view_image );

		// parpare the cache if it's not there (although this shouldn't happen)
		FileCache.prepare( getApplicationContext() );
		filecache = FileCache.getInstance();

		Intent intent = getIntent();
		post = intent.getParcelableExtra( "post" );
		host = intent.getParcelableExtra( "host" );
		page_tags = intent.getStringExtra( "page_tags" );

		SharedPreferences preferences = getSharedPreferences( D.SHAREDPREFERENCES_NAME, MODE_PRIVATE );
		image_url = post.sample_url;
		if (preferences.getBoolean( "high_quality_image", false ))
			image_url = post.file_url;

		image = (ImageViewTouch) findViewById( R.id.view_image_image );
		infopane = (RelativeLayout) findViewById( R.id.view_image_infopane );
		registerForContextMenu( image );

		TextView infopane_info = (TextView) findViewById( R.id.view_image_infopane_info );
		infopane_info.setText( String.format( getString( R.string.view_image_infopane_message ), post.width, post.height, post.author, post.created_at ) );

		ImageView infopane_back = (ImageView) findViewById( R.id.view_image_infopane_back );
		infopane_back.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
				overridePendingTransition( R.anim.zoom_enter, R.anim.zoom_exit );
			}
		} );

		infopane.postDelayed( new Runnable()
		{
			@Override
			public void run()
			{
				if (System.currentTimeMillis() - lastClick < INFOPANE_DELAY)
					return;

				infopane.clearAnimation();

				Animation anim = new AlphaAnimation( 1.0f, 0.0f );
				anim.setDuration( 300 );
				infopane.startAnimation( anim );
				infopane.postDelayed( new Runnable()
				{
					@Override
					public void run()
					{
						if (System.currentTimeMillis() - lastClick < INFOPANE_DELAY + 300)
							return;

						infopane.setVisibility( View.GONE );
					}
				}, 300 );
			}
		}, INFOPANE_DELAY );
		image.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				lastClick = System.currentTimeMillis();

				if (infopane.getVisibility() != View.VISIBLE)
				{
					infopane.setVisibility( View.VISIBLE );
					Animation anim = new AlphaAnimation( 0.0f, 1.0f );
					anim.setDuration( 300 );
					infopane.startAnimation( anim );
				}
				infopane.postDelayed( new Runnable()
				{
					@Override
					public void run()
					{
						if (System.currentTimeMillis() - lastClick < INFOPANE_DELAY)
							return;

						infopane.clearAnimation();

						Animation anim = new AlphaAnimation( 1.0f, 0.0f );
						anim.setDuration( 300 );
						infopane.startAnimation( anim );
						infopane.postDelayed( new Runnable()
						{
							@Override
							public void run()
							{
								if (System.currentTimeMillis() - lastClick < INFOPANE_DELAY + 300)
									return;

								infopane.setVisibility( View.GONE );
							}
						}, 300 );
					}
				}, INFOPANE_DELAY );
			}
		} );

		ConfigurationEnclosure enclosure = (ConfigurationEnclosure) getLastNonConfigurationInstance();
		Bitmap bitmap = null;
		if (enclosure != null)
		{
			loader = enclosure.loader;
			bitmap = enclosure.bitmap;
		}
		loadImage( bitmap );

		if (savedInstanceState != null)
		{
			final float old_scale = savedInstanceState.getFloat( "image_scale" );
			final float old_center_x = savedInstanceState.getFloat( "image_center_x" );
			final float old_center_y = savedInstanceState.getFloat( "image_center_y" );
			final float old_width = savedInstanceState.getFloat( "image_width" );

			image.post( new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						if (old_scale > 0.0f)
						{
							float target_scale = old_scale / image.getBaseScale();
							image.zoomTo( Math.min( image.getMaxZoom(), Math.max( 1.0f, target_scale ) ) );
						}
						else
							image.zoomTo( 1.0f );

						PointF from_pt = image.getViewportCenter();
						RectF now_rect = image.getBitmapRect();
						image.scrollBy( from_pt.x - old_center_x * now_rect.width(), from_pt.y - old_center_y * now_rect.height() );

						image.center( true, true );

						// do some animations...
						float factor = old_width / image.getBitmapRect().width();

						Animation anim = null;
						if (Math.abs( factor - 1.0f ) < 0.05)
						{
							AnimationSet animset = new AnimationSet( false );
							anim = new ScaleAnimation( 1.0f, 1.2f, 1.0f, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f );
							anim.setInterpolator( new AccelerateInterpolator() );
							anim.setDuration( 250 );
							animset.addAnimation( anim );
							anim = new ScaleAnimation( 1.0f, 1.0f / 1.2f, 1.0f, 1.0f / 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f );
							anim.setInterpolator( new DecelerateInterpolator() );
							anim.setDuration( 250 );
							anim.setStartOffset( 250 );
							animset.addAnimation( anim );
							anim = animset;
						}
						else
						{
							anim = new ScaleAnimation( factor, 1.0f, factor, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f );
							anim.setInterpolator( new OvershootInterpolator() );
							anim.setDuration( 500 );
						}
						image.clearAnimation();
						image.startAnimation( anim );
					}
					catch (NullPointerException ex)
					{
						// message queue problem. the time we try to get the image's status
						// (getViewportCenter() or getBitmapRect()) but they're just not
						// ready yet. so post this to the message queue so it gets processed
						// later when it's ready.
						image.post( this );
					}
				}
			} );
		}
	}

	private void loadImage(Bitmap bitmap)
	{
		File file = null;
		if (bitmap == null)
		{
			file = filecache.getFile( image_url );
			if (file.exists())
				bitmap = D.getBitmapFromFile( file );
		}

		if (bitmap == null)
		{
			if (loader == null)
			{
				if (file == null)
					file = filecache.getFile( image_url );
				loader = new AsyncImageLoader( this, file );
				loader.execute( image_url );
			}
			else
				loader.reattach( this );

			BitmapMemCache memcache = BitmapMemCache.getInstance();
			bitmap = memcache.get( post.preview_url );
			if (bitmap == null)
			{
				bitmap = D.getBitmapFromFile( filecache.getFile( post.preview_url ) );
				if (bitmap != null)
					memcache.put( post.preview_url, bitmap );
			}
			image.setScaleType( ScaleType.FIT_CENTER );
		}
		image.setImageBitmapReset( bitmap, true );
	}

	@Override
	public void onDestroy()
	{
		BitmapDrawable drawable = (BitmapDrawable) image.getDrawable();
		if (drawable != null)
		{
			Bitmap bitmap = drawable.getBitmap();
			if (bitmap != null)
				if (BitmapMemCache.getInstance().get( post.preview_url ) != bitmap)
					bitmap.recycle();
		}
		image.setImageDrawable( null );
		super.onDestroy();
	}

	@Override
	public void onStart()
	{
		super.onStart();
	}

	private static class ConfigurationEnclosure
	{
		public AsyncImageLoader	loader;
		public Bitmap			bitmap;

		ConfigurationEnclosure(AsyncImageLoader loader, Bitmap bitmap)
		{
			this.loader = loader;
			this.bitmap = bitmap;
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		Bitmap bitmap = null;
		if (loader != null && loader.getStatus() != Status.RUNNING)
		{
			BitmapDrawable drawable = (BitmapDrawable) image.getDrawable();
			if (drawable != null)
			{
				bitmap = drawable.getBitmap();
				image.setImageDrawable( null );
			}
		}
		return new ConfigurationEnclosure( loader, bitmap );
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		try
		{
			PointF pt = image.getViewportCenter();
			RectF rect = image.getBitmapRect();
			if (image.getScale() == 1.0f)
				outState.putFloat( "image_scale", -1.0f );
			else
				outState.putFloat( "image_scale", image.getScale() * image.getBaseScale() );
			outState.putFloat( "image_center_x", pt.x / rect.width() );
			outState.putFloat( "image_center_y", pt.y / rect.height() );
			outState.putFloat( "image_width", rect.width() );
			outState.putFloat( "image_height", rect.height() );
		}
		catch (NullPointerException ex)
		{

		}
		super.onSaveInstanceState( outState );
	}

	@Override
	public boolean onSearchRequested()
	{
		startSearch( page_tags, true, null, false );
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		getMenuInflater().inflate( R.menu.view_image_longclick, menu );
		super.onCreateContextMenu( menu, v, menuInfo );
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		if (onMenuItemSelected( item ))
			return true;
		return super.onContextItemSelected( item );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate( R.menu.view_image_menu, menu );
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (onMenuItemSelected( item ))
			return true;
		return super.onOptionsItemSelected( item );
	}

	private boolean onMenuItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.view_image_menu_info:
		case R.id.view_menu_longclick_info:
		{
			Intent intent = new Intent( ViewImageActivity.this, ViewImageInfoActivity.class );
			intent.putExtra( "post", post );
			startActivityForResult( intent, -1 );
			break;
		}
		case R.id.view_image_menu_tags:
		case R.id.view_menu_longclick_tags:
		{
			Builder builder = new AlertDialog.Builder( this );
			builder.setTitle( R.string.view_image_tags );
			ListView listview = new ListView( this );
			listview.setAdapter( new BaseAdapter()
			{
				String	tags[]	= post.tags.split( " " );

				@Override
				public int getCount()
				{
					return tags.length;
				}

				@Override
				public Object getItem(int position)
				{
					return tags[position];
				}

				@Override
				public long getItemId(int position)
				{
					return tags[position].hashCode();
				}

				@Override
				public View getView(int position, View convertView, ViewGroup parent)
				{
					TextView text = null;
					if (convertView != null)
						text = (TextView) convertView;

					if (text == null)
						text = new TextView( ViewImageActivity.this );

					text.setText( tags[position] );
					text.setLayoutParams( new ListView.LayoutParams( ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT ) );
					text.setGravity( Gravity.CENTER_HORIZONTAL );
					text.setPadding( 8, 8, 8, 8 );
					return text;
				}
			} );
			builder.setView( listview );
			builder.setPositiveButton( android.R.string.ok, null );
			final AlertDialog dialog = builder.create();
			listview.setOnItemClickListener( new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView < ? > parent, View view, int position, long id)
				{
					TextView text = (TextView) view;
					Intent intent = new Intent( Intent.ACTION_SEARCH );
					intent.putExtra( SearchManager.QUERY, text.getText() );
					setResult( RESULT_OK, intent );
					dialog.dismiss();
					finish();
					overridePendingTransition( R.anim.zoom_enter, R.anim.zoom_exit );
				}
			} );
			dialog.show();
			break;
		}
		case R.id.view_image_menu_refresh:
			File file = filecache.getFile( image_url );
			TextView progress_message = (TextView) findViewById( R.id.view_image_progress_message );
			progress_message.setText( R.string.view_image_progress_message );

			loader = new AsyncImageLoader( this, file );
			loader.execute( image_url );
			break;
		case R.id.view_image_menu_download:
		case R.id.view_menu_longclick_download:
			String title = post.tags;
			if (host != null)
				title = String.format( "%1$s - %2$s", host.url, post.tags );

			Uri uri = Uri.parse( post.file_url );
			String filename = uri.getLastPathSegment();
			if (filename == null)
				filename = Uri.encode( uri.toString() );

			File dest = new File( android.os.Environment.getExternalStorageDirectory(), D.SAVEDIR + "/" + host.name + "/" + filename );
			if ( !dest.getParentFile().exists())
				dest.getParentFile().mkdirs();

			DownloadManager.Request request = new DownloadManager.Request( uri );
			request.setTitle( title );
			request.setDestinationUri( Uri.fromFile( dest ) );
			DownloadManager downloader = (DownloadManager) getSystemService( DOWNLOAD_SERVICE );
			downloader.enqueue( request );

			Toast.makeText( this, String.format( getString( R.string.view_image_menu_downloading ), filename ), Toast.LENGTH_SHORT ).show();
			break;
		case R.id.view_image_menu_share:
			Intent intent = new Intent( Intent.ACTION_SEND );
			intent.setType( "text/plain" );
			intent.putExtra( android.content.Intent.EXTRA_TEXT, post.file_url );
			startActivity( Intent.createChooser( intent, getText( R.string.view_image_menu_share_chooser_title ) ) );
			break;
		case R.id.view_menu_longclick_back:
			finish();
			overridePendingTransition( R.anim.zoom_enter, R.anim.zoom_exit );
			break;
		default:
			return false;
		}
		return true;
	}

	public File getOutputFile(URL url)
	{
		File downloaddir;
		// Find the directory to save cached images
		if (android.os.Environment.getExternalStorageState().equals( android.os.Environment.MEDIA_MOUNTED ))
			downloaddir = new File( android.os.Environment.getExternalStorageDirectory(), D.SAVEDIR );
		else
			return null;

		downloaddir = new File( downloaddir, host.name );
		if ( !downloaddir.exists())
			downloaddir.mkdirs();

		String filename = url.getPath();
		filename = filename.substring( filename.lastIndexOf( '/' ) );
		if (filename == "")
			filename = URLEncoder.encode( url.toExternalForm() );

		return new File( downloaddir, URLDecoder.decode( filename ) );
	}

	@Override
	public void onBackPressed()
	{
		try
		{
			if (image.getScale() != 1.0f)
			{
				image.zoomTo( 1.0f, 400 );
				return;
			}
		}
		catch (NullPointerException ex)
		{
			// no picture just quit
		}

		if (loader != null)
			if (loader.getStatus() == Status.RUNNING)
				loader.cancel( true );

		finish();
		overridePendingTransition( R.anim.zoom_enter, R.anim.zoom_exit );
	}

	private static class AsyncImageLoader
		extends AsyncTask < String, Integer, Integer >
	{
		private static final int	PROGRESS_SETMAX			= 0x01;
		private static final int	PROGRESS_SETPROGRESS	= 0x02;
		private static final int	PROGRESS_INCREMENTBY	= 0x03;

		private static final int	RESULT_SUCCESS			= 0x00;
		private static final int	RESULT_FAILED			= 0x01;
		private static final int	RESULT_CANCELLED		= 0x02;

		ViewImageActivity			activity;
		ImageViewTouch				image;
		File						file;

		RelativeLayout				progress_indicator;
		TextView					progress_message;
		ProgressBar					progress_progressbar;

		public void init(ViewImageActivity a)
		{
			activity = a;
			image = (ImageViewTouch) activity.findViewById( R.id.view_image_image );
			progress_indicator = (RelativeLayout) activity.findViewById( R.id.view_image_progress_indicator );
			progress_message = (TextView) activity.findViewById( R.id.view_image_progress_message );
			progress_progressbar = (ProgressBar) activity.findViewById( R.id.view_image_progress_progressbar );
		}

		public AsyncImageLoader(ViewImageActivity a, File f)
		{
			file = f;
			init( a );
		}

		public void reattach(ViewImageActivity a)
		{
			int old_max = progress_progressbar.getMax();
			int old_progress = progress_progressbar.getProgress();
			init( a );
			progress_progressbar.setMax( old_max );
			progress_progressbar.setProgress( old_progress );
			progress_indicator.setVisibility( View.VISIBLE );
		}

		@Override
		protected void onPreExecute()
		{
			progress_progressbar.setMax( 1 );
			progress_progressbar.setProgress( 0 );
			progress_indicator.setVisibility( View.VISIBLE );
		}

		@Override
		protected Integer doInBackground(String... params)
		{
			if (params.length != 1)
				return RESULT_FAILED;

			URL url;
			try
			{
				url = new URL( params[0] );
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				publishProgress( PROGRESS_SETMAX, conn.getContentLength() );

				if (isCancelled())
					return RESULT_CANCELED;

				InputStream input = conn.getInputStream();
				OutputStream output = new FileOutputStream( file );

				byte[] bytes = new byte[4096];
				for (;;)
				{
					int count = input.read( bytes );
					if (count == -1)
						break;

					publishProgress( PROGRESS_INCREMENTBY, count );

					if (isCancelled())
					{
						output.close();
						file.delete();
						return RESULT_CANCELLED;
					}

					output.write( bytes, 0, count );
				}
				output.close();
			}
			catch (MalformedURLException e)
			{
				return RESULT_FAILED;
			}
			catch (IOException e)
			{
				return RESULT_FAILED;
			}
			return RESULT_SUCCESS;
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			switch (result)
			{
			case RESULT_SUCCESS:
				Bitmap bitmap = D.getBitmapFromFile( file );
				if (bitmap != null)
				{
					image.setImageBitmapReset( bitmap, true );
					image.setScaleType( ScaleType.MATRIX );
				}
				else
					Toast.makeText( image.getContext(), R.string.view_image_download_failed, Toast.LENGTH_SHORT ).show();
				break;
			case RESULT_FAILED:
				Toast.makeText( image.getContext(), R.string.view_image_download_failed, Toast.LENGTH_SHORT ).show();
				break;
			case RESULT_CANCELLED:
				Toast.makeText( image.getContext(), R.string.view_image_user_canceled, Toast.LENGTH_SHORT ).show();
				break;
			}

			Animation anim = new AlphaAnimation( 1.0f, 0.0f );
			anim.setDuration( 500 );
			progress_indicator.startAnimation( anim );
			progress_indicator.postDelayed( new Runnable()
			{
				@Override
				public void run()
				{
					progress_indicator.setVisibility( View.GONE );
				}
			}, 500 );

			activity.loader = null;
		}

		@Override
		protected void onProgressUpdate(Integer... values)
		{
			switch (values[0])
			{
			case PROGRESS_SETMAX:
				progress_progressbar.setMax( values[1] );
				break;
			case PROGRESS_SETPROGRESS:
				progress_progressbar.setProgress( values[1] );
				break;
			case PROGRESS_INCREMENTBY:
				progress_progressbar.incrementProgressBy( values[1] );
				break;
			}
			progress_message.setText( String.format( "%d / %d", progress_progressbar.getProgress(), progress_progressbar.getMax() ) );
		}
	}
}