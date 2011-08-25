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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.utils.FileCache;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

public class ViewImageActivity extends Activity {
	FileCache filecache;
	Post post;
	AsyncImageLoader loader;
	Bitmap bitmap;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_image);

		filecache = new FileCache( this.getApplicationContext() );
		post = new Post();

		Intent intent = getIntent();
		post.preview_url = intent.getStringExtra("post.preview_url");
		post.file_url = intent.getStringExtra("post.file_url");
		post.author = intent.getStringExtra("post.author");
		post.tags = intent.getStringExtra("post.tags");
		post.width = intent.getIntExtra("post.width", 0);
		post.height = intent.getIntExtra("post.height", 0);

		if ( intent.hasExtra("post.created_at") )
			post.created_at = new Date(intent.getLongExtra("post.created_at", 0));

		ImageView image = (ImageView)findViewById( R.id.view_image_image );
		image.setImageBitmap( D.getBitmapFromFile( filecache.getFile(post.preview_url), -1 ) );

		SlidingDrawer infodrawer = (SlidingDrawer)findViewById( R.id.view_image_drawer );
		TextView infopane = (TextView)findViewById( R.id.view_image_pic_info );
		infopane.setText(
			String.format(
				getText( R.string.view_image_pic_info ).toString(),
				post.tags, post.width, post.height, post.author,
				post.created_at == null ? "" : post.created_at.toLocaleString()
			)
		);
		infopane.setOnClickListener(new InfoPaneOnClickListener( infodrawer ));

		File file = filecache.getFile(post.file_url);
		if ( file.exists() )
		{
			bitmap = D.getBitmapFromFile(file, -1);
			image.setImageBitmap( bitmap );
		}
		else
		{
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setMax(1);

			loader = new AsyncImageLoader(image, dialog, file);
			dialog.setOnCancelListener(new ProgressOnCancelListener(this, loader));

			loader.execute(post.file_url);
		}
	}

	@Override
	public void onDestroy()
	{
		if ( loader != null )
			if ( loader.getStatus() == Status.RUNNING )
				loader.cancel(true);

		if ( bitmap != null )
			bitmap.recycle();

		super.onDestroy();
	}

	private class InfoPaneOnClickListener implements OnClickListener
	{
		SlidingDrawer drawer;

		public InfoPaneOnClickListener( SlidingDrawer d )
		{
			drawer = d;
		}

		@Override
		public void onClick(View v) {
			drawer.animateClose();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_image_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.view_image_menu_refresh:
				File file = filecache.getFile(post.file_url);
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				dialog.setMax(1);

				ImageView image = (ImageView)findViewById( R.id.view_image_image );
				loader = new AsyncImageLoader(image, dialog, file);
				dialog.setOnCancelListener(new ProgressOnCancelListener(this, loader));

				loader.execute(post.file_url);
				break;
			case R.id.view_image_menu_save:
				// FIXME: do this in the background, don't block the UI thread.
				try {
					File cachefile = filecache.getFile(post.file_url);
					File outfile = getOutputFile(new URL(post.file_url));

					outfile.delete();

					InputStream in = new FileInputStream(cachefile);
					OutputStream out = new FileOutputStream(outfile);

					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) > 0)
					{
						out.write(buf, 0, len);
					}
					in.close();
					out.close();

					// TODO: localize me
					Toast.makeText(this, outfile.getPath() + " saved.", Toast.LENGTH_SHORT).show();

					// FIXME: maybe we just have to inform the media scanner to scan
					//		our folder in MainActivity::onStop()?
					sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + outfile.getParent())));
				}
				catch (Exception e)
				{
					// TODO: localize me
					Toast.makeText(this, "Unable to save file!", Toast.LENGTH_SHORT).show();
				}
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public File getOutputFile(URL url)
	{
		File downloaddir;
		// Find the directory to save cached images
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
			downloaddir = new File(android.os.Environment.getExternalStorageDirectory(), D.SAVEDIR);
		else
			return null;

		if(!downloaddir.exists())
			downloaddir.mkdirs();

		String filename = url.getPath();
		filename = filename.substring(filename.lastIndexOf('/'));
		if (filename == "")
			filename = url.toExternalForm().replace('/', '_');

		return new File(downloaddir, filename);
	}

	private class ProgressOnCancelListener implements OnCancelListener
	{
		Activity activity;
		AsyncImageLoader loader;

		public ProgressOnCancelListener(Activity a, AsyncImageLoader l)
		{
			activity = a;
			loader = l;
		}

		@Override
		public void onCancel(DialogInterface dialog)
		{
			activity.finish();
			overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
			loader.cancel(true);
		}
	}

	@Override
	public void onBackPressed() {
		finish();
		overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
		super.onBackPressed();
	}

	private class AsyncImageLoader extends AsyncTask<String, Integer, Integer>
	{
		private static final int PROGRESS_SETMAX		= 0x01;
		private static final int PROGRESS_SETPROGRESS	= 0x02;
		private static final int PROGRESS_INCREMENTBY	= 0x03;
		private static final int PROGRESS_SETMESSAGE	= 0x04;

		private static final int RESULT_SUCCESS			= 0x00;
		private static final int RESULT_FAILED			= 0x01;
		private static final int RESULT_CANCELLED		= 0x02;

		ImageView image;
		ProgressDialog dialog;
		File file;

		public AsyncImageLoader( ImageView i, ProgressDialog d, File f )
		{
			image = i;
			dialog = d;
			file = f;
		}

		@Override
		protected Integer doInBackground(String... params)
		{
			if (params.length != 1)
				return RESULT_FAILED;

			URL url;
			try {
				url = new URL(params[0]);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				publishProgress(PROGRESS_SETMAX, conn.getContentLength());

				if (isCancelled())
					return RESULT_CANCELED;

				InputStream input = conn.getInputStream();
				OutputStream output = new FileOutputStream(file);

				byte[] bytes = new byte[1024];
				for (;;) {
					int count = input.read(bytes, 0, 1024);
					if (count == -1)
						break;

					publishProgress(PROGRESS_INCREMENTBY, count);

					if (isCancelled())
					{
						output.close();
						file.delete();
						return RESULT_CANCELLED;
					}

					output.write(bytes, 0, count);
				}
				output.close();
			} catch (MalformedURLException e) {
				return RESULT_FAILED;
			} catch (IOException e) {
				return RESULT_FAILED;
			}
			return RESULT_SUCCESS;
		}

		@Override
		public void onPreExecute()
		{
			dialog.show();
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			switch (result)
			{
			case RESULT_SUCCESS:
				bitmap = D.getBitmapFromFile( file, -1 );
				image.setImageBitmap( bitmap );
				dialog.dismiss();
				break;
			case RESULT_FAILED:
				// FIXME: translation
				Toast.makeText(image.getContext(), "Download failed.", Toast.LENGTH_SHORT).show();
				dialog.dismiss();
				break;
			case RESULT_CANCELLED:
				// FIXME: translation
				Toast.makeText(image.getContext(), "User canceled.", Toast.LENGTH_SHORT).show();
				dialog.cancel();
				break;
			default:
				Log.e(D.LOGTAG, "AsyncImageLoader::onPostExecute(): Unknown result: " + result);
				dialog.dismiss();
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values)
		{
			if (values.length != 2)
			{
				Log.e(D.LOGTAG, "AsyncImageLoader::onProgressUpdate(): Invalid argument!");
				return;
			}

			switch (values[0])
			{
			case PROGRESS_SETMAX:
				dialog.setMax(values[1]);
				break;
			case PROGRESS_SETPROGRESS:
				dialog.setProgress(values[1]);
				break;
			case PROGRESS_INCREMENTBY:
				dialog.incrementProgressBy(values[1]);
				break;
			case PROGRESS_SETMESSAGE:
				dialog.setMessage("");
				break;
			default:
				Log.e(D.LOGTAG, "AsyncImageLoader::onProgressUpdate(): Unknown action: " + values[0]);
			}
		}
	}
}