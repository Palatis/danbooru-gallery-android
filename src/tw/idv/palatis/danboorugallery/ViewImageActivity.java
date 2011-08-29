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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Hosts;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.utils.FileCache;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView.ScaleType;

public class ViewImageActivity
	extends Activity
{
	FileCache			filecache;
	Post				post;
	AsyncImageLoader	loader;
	ImageViewTouch		image;

	String				host[];
	String				page_tags;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.view_image );

		filecache = new FileCache( getApplicationContext() );
		post = new Post();

		Intent intent = getIntent();
		post.preview_url = intent.getStringExtra( "post.preview_url" );
		post.file_url = intent.getStringExtra( "post.file_url" );
		post.author = intent.getStringExtra( "post.author" );
		post.tags = intent.getStringExtra( "post.tags" );
		post.width = intent.getIntExtra( "post.width", 0 );
		post.height = intent.getIntExtra( "post.height", 0 );

		host = new String[2];
		host[Hosts.HOST_NAME] = intent.getStringExtra( "host_name" );
		host[Hosts.HOST_URL] = intent.getStringExtra( "host_url" );

		if (intent.hasExtra( "post.created_at" ))
			post.created_at = new Date( intent.getLongExtra( "post.created_at", 0 ) );

		page_tags = intent.getStringExtra( "page_tags" );

		image = (ImageViewTouch) findViewById( R.id.view_image_image );

		ConfigurationEnclosure enclosure = (ConfigurationEnclosure) getLastNonConfigurationInstance();
		Bitmap bitmap = null;
		if ( enclosure != null )
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
			file = filecache.getFile( post.file_url );
			if (file.exists())
				bitmap = D.getBitmapFromFile( file );
		}

		if (bitmap == null)
		{
			ProgressDialog dialog = new NoSearchProgressDialog( this );
			dialog.setTitle( String.format( getString( R.string.view_image_progress_title ), post.width, post.height ) );
			dialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
			dialog.setOnCancelListener( new ProgressOnCancelListener() );
			dialog.show();

			if (loader == null)
			{
				loader = new AsyncImageLoader( this, image, dialog, file );
				loader.execute( post.file_url );
				dialog.setMax( 1 );
			}
			else
			{
				dialog.setMax( loader.dialog.getMax() );
				dialog.setProgress( loader.dialog.getProgress() );
				loader.reattach( this, image, dialog );
			}
		}

		if (bitmap == null)
		{
			bitmap = D.getBitmapFromFile( filecache.getFile( post.preview_url ) );
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
				bitmap.recycle();
		}

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
		if ( loader != null && loader.getStatus() != Status.RUNNING )
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
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.view_image_menu, menu );
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.view_image_menu_info:
			Builder builder = new AlertDialog.Builder( this );
			builder.setTitle( R.string.view_image_menu_info );
			builder.setMessage( String.format( getString( R.string.view_image_pic_info ), post.width, post.height, post.author, post.created_at == null ? "" : post.created_at.toLocaleString() ) );
			ListView listview = new ListView( ViewImageActivity.this );
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
					text.setPadding( 4, 4, 4, 4 );
					return text;
				}
			} );
			listview.setOnItemClickListener( new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView < ? > parent, View view, int position, long id)
				{
					TextView text = (TextView) view;
					Intent intent = new Intent( Intent.ACTION_SEARCH );
					intent.putExtra( SearchManager.QUERY, text.getText() );
					setResult( RESULT_OK, intent );
					finish();
				}
			} );
			builder.setView( listview );
			builder.setPositiveButton( android.R.string.ok, null );
			builder.create().show();
			break;
		case R.id.view_image_menu_refresh:
			File file = filecache.getFile( post.file_url );
			ProgressDialog dialog = new NoSearchProgressDialog( this );
			dialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
			dialog.setMax( 1 );

			ImageViewTouch image = (ImageViewTouch) findViewById( R.id.view_image_image );
			loader = new AsyncImageLoader( this, image, dialog, file );
			dialog.setOnCancelListener( new ProgressOnCancelListener() );

			loader.execute( post.file_url );
			break;
		case R.id.view_image_menu_save:
			// FIXME: do this in the background, don't block the UI thread.
			// maybe we shall just do a ln?
			try
			{
				File cachefile = filecache.getFile( post.file_url );
				File outfile = getOutputFile( new URL( post.file_url ) );

				outfile.delete();

				D.CopyStream( new FileInputStream( cachefile ), new FileOutputStream( outfile ) );

				Toast.makeText( this, String.format( getString( R.string.view_image_file_saved ), outfile.getPath() ), Toast.LENGTH_SHORT ).show();

				MediaScannerConnection.scanFile( this, new String[] {
					outfile.getAbsolutePath()
				}, null, null );
			}
			catch (Exception e)
			{
				Log.d( D.LOGTAG, Log.getStackTraceString( e ) );
				Toast.makeText( this, R.string.view_image_file_save_failed, Toast.LENGTH_SHORT ).show();
			}
			break;
		case R.id.view_image_menu_share:
			Intent intent = new Intent( Intent.ACTION_SEND );
			intent.setType( "text/plain" );
			intent.putExtra( android.content.Intent.EXTRA_TEXT, post.file_url );
			startActivity( Intent.createChooser( intent, getText( R.string.view_image_menu_share_chooser_title ) ) );
			break;
		default:
			return super.onOptionsItemSelected( item );
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

		downloaddir = new File( downloaddir, host[Hosts.HOST_NAME] );
		if ( !downloaddir.exists())
			downloaddir.mkdirs();

		String filename = url.getPath();
		filename = filename.substring( filename.lastIndexOf( '/' ) );
		if (filename == "")
			filename = URLEncoder.encode( url.toExternalForm() );

		return new File( downloaddir, URLDecoder.decode( filename ) );
	}

	private class ProgressOnCancelListener
		implements OnCancelListener
	{
		@Override
		public void onCancel(DialogInterface dialog)
		{
			finish();
			overridePendingTransition( R.anim.zoom_enter, R.anim.zoom_exit );
			loader.cancel( true );
		}
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
		private static final int	PROGRESS_SETMESSAGE		= 0x04;

		private static final int	RESULT_SUCCESS			= 0x00;
		private static final int	RESULT_FAILED			= 0x01;
		private static final int	RESULT_CANCELLED		= 0x02;

		ViewImageActivity			activity;
		ImageViewTouch				image;
		ProgressDialog				dialog;
		File						file;

		public AsyncImageLoader(ViewImageActivity a, ImageViewTouch i, ProgressDialog p, File f)
		{
			activity = a;
			image = i;
			dialog = p;
			file = f;
		}

		public void reattach(ViewImageActivity a, ImageViewTouch i, ProgressDialog p)
		{
			activity = a;
			image = i;
			dialog = p;
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
				image.setImageBitmapReset( bitmap, true );
				image.setScaleType( ScaleType.MATRIX );
				dialog.dismiss();
				break;
			case RESULT_FAILED:
				Toast.makeText( image.getContext(), R.string.view_image_download_failed, Toast.LENGTH_SHORT ).show();
				dialog.dismiss();
				break;
			case RESULT_CANCELLED:
				Toast.makeText( image.getContext(), R.string.view_image_user_canceled, Toast.LENGTH_SHORT ).show();
				dialog.cancel();
				break;
			}

			activity.loader = null;
		}

		@Override
		protected void onProgressUpdate(Integer... values)
		{
			switch (values[0])
			{
			case PROGRESS_SETMAX:
				dialog.setMax( values[1] );
				break;
			case PROGRESS_SETPROGRESS:
				dialog.setProgress( values[1] );
				break;
			case PROGRESS_INCREMENTBY:
				dialog.incrementProgressBy( values[1] );
				break;
			case PROGRESS_SETMESSAGE:
				dialog.setMessage( "" );
				break;
			}
		}
	}

	private class NoSearchProgressDialog
		extends ProgressDialog
	{
		public NoSearchProgressDialog(Context context)
		{
			super( context );
		}

		public NoSearchProgressDialog(Context context, int theme)
		{
			super( context, theme );
		}

		@Override
		public boolean onSearchRequested()
		{
			return false;
		}
	}
}