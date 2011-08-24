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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.defines.D;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Looper;
import android.util.Log;

public class DanbooruUncaughtExceptionHandler implements UncaughtExceptionHandler
{
	final Activity activity;

	public DanbooruUncaughtExceptionHandler( Activity a )
	{
		activity = a;
	}

	@Override
	public void uncaughtException(final Thread thread, final Throwable ex)
	{
		Log.e(D.LOGTAG, Log.getStackTraceString(ex));
		if ( ex.getCause() != null )
			Log.e(D.LOGTAG, Log.getStackTraceString(ex.getCause()));

		File logdir;
		File logfile = null;
		// Find the directory to save cached images
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
		{
			logdir = new File(android.os.Environment.getExternalStorageDirectory(), D.ERRORLOG_DIRECTORY);
			if(!logdir.exists())
				logdir.mkdirs();

				try {
				logfile = File.createTempFile(D.ERRORLOG_PREFIX, D.ERRORLOG_SUFFIX, logdir);
				BufferedWriter out = new BufferedWriter( new FileWriter( logfile ) );

				out.write(activity.getText(R.string.preferences_about_version_description).toString());

				out.write(Log.getStackTraceString(ex));
				if ( ex.getCause() != null )
					out.write(Log.getStackTraceString(ex.getCause()));

				out.close();
			} catch (IOException e) {
				// failed, does nothing.
				e.printStackTrace();
			}
		}

		new Thread()
		{
			String logfile;

			public Thread initialize( File l )
			{
				logfile = ( l != null ) ? l.getAbsolutePath() : "<unable to save error log>";
				return this;
			}

			@Override
			public void run() {
				Looper.prepare();
				Builder builder = new AlertDialog.Builder(activity);
				builder.setTitle( R.string.error_dialog_title );
				builder.setIcon( android.R.drawable.ic_delete );
				builder.setMessage(
					String.format(
						activity.getText( R.string.error_dialog_message ).toString(),
						logfile,
						activity.getText( R.string.preferences_about_author ).toString()
					)
				);
				builder.setPositiveButton(
					android.R.string.ok,
					new OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							// we crashed, force the application to exit.
							System.exit(1);
						}
					}
				);
				builder.create().show();
				Looper.loop();
			}
		}.initialize(logfile).start();
	}
}