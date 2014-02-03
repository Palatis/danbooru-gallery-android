////////////////////////////////////////////////////////////////////////////////
// Danbooru Gallery Android - an danbooru-style imageboard browser
//     Copyright (C) 2014  Victor Tseng
//
//     This program is free software: you can redistribute it and/or modify
//     it under the terms of the GNU General Public License as published by
//     the Free Software Foundation, either version 3 of the License, or
//     (at your option) any later version.
//
//     This program is distributed in the hope that it will be useful,
//     but WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//     GNU General Public License for more details.
//
//     You should have received a copy of the GNU General Public License
//     along with this program. If not, see <http://www.gnu.org/licenses/>
////////////////////////////////////////////////////////////////////////////////

package tw.idv.palatis.danboorugallery;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.File;

import tw.idv.palatis.danboorugallery.database.DanbooruGalleryDatabase;
import tw.idv.palatis.danboorugallery.database.PostTagsLinkTable;
import tw.idv.palatis.danboorugallery.database.PostsTable;
import tw.idv.palatis.danboorugallery.database.TagsTable;
import tw.idv.palatis.danboorugallery.picasso.Picasso;
import tw.idv.palatis.danboorugallery.siteapi.SiteAPI;
import tw.idv.palatis.danboorugallery.util.SiteSession;

import static tw.idv.palatis.danboorugallery.BuildConfig.DEBUG;

public class DanbooruGalleryApplication
    extends Application
    implements Thread.UncaughtExceptionHandler
{
    private static final String TAG = "DanbooruGalleryApplication";

    // FIXME: shouldn't there be a way to determine this during runtime?
    public static final int MAXIMUM_TEXTURE_SIZE = 2048;

    @Override
    public void onCreate()
    {
        super.onCreate();
        DanbooruGallerySettings.init(this);
        DanbooruGalleryDatabase.init(this);
        Picasso.init(this);
        NetworkChangeReceiver.init(this);
        SiteAPI.init(this);
        SiteSession.init();

        if (!DEBUG)
            Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        Picasso.onLowMemory();
        SQLiteDatabase.releaseMemory();
    }

    @Override
    public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        Picasso.onTrimMemory(level);
        SQLiteDatabase.releaseMemory();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable)
    {
        if (!DEBUG)
        {
            StringBuilder report = new StringBuilder();

            report.append("------------ Version ----------\n");
            report.append("Package: ").append(BuildConfig.PACKAGE_NAME).append("\n");
            report.append("Version Name: ").append(BuildConfig.VERSION_NAME).append("\n");
            report.append("Version Code: ").append(BuildConfig.VERSION_CODE).append("\n");
            report.append("Build Type: ").append(BuildConfig.BUILD_TYPE).append("\n");
            report.append("Flavor: ").append(BuildConfig.FLAVOR).append("\n");

            StackTraceElement[] stackTrace = throwable.getStackTrace();
            report.append("---------- Exception ----------\n");
            report.append(throwable.toString()).append("\n\n");
            for (StackTraceElement stack : stackTrace)
                report.append(stack.toString()).append("\n");

            // If the exception was thrown in a background thread inside
            // AsyncTask, then the actual exception can be found with getCause
            Throwable cause = throwable.getCause();
            if (cause != null)
            {
                report.append("------------ Cause ------------\n");
                report.append(cause.toString()).append("\n\n");
                stackTrace = cause.getStackTrace();
                for (StackTraceElement stack : stackTrace)
                    report.append("    ").append(stack.toString()).append("\n");
            }

            report.append("------------ Device -----------\n");
            report.append("Brand: ").append(Build.BRAND).append("\n");
            report.append("Device: ").append(Build.DEVICE).append("\n");
            report.append("Model: ").append(Build.MODEL).append("\n");
            report.append("Id: ").append(Build.ID).append("\n");
            report.append("Product: ").append(Build.PRODUCT).append("\n");

            report.append("----------- Firmware ----------\n");
            report.append("SDK: ").append(Build.VERSION.SDK_INT).append("\n");
            report.append("Release: ").append(Build.VERSION.RELEASE).append("\n");
            report.append("Incremental: ").append(Build.VERSION.INCREMENTAL).append("\n");

            report.append("------ Extra Information ------\n");

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.fromParts("mailto", "palatis@gmail.com", null));
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "palatis@gmail.com" });
            intent.putExtra(Intent.EXTRA_SUBJECT, "Crash Report: " + BuildConfig.PACKAGE_NAME + " " + BuildConfig.VERSION_CODE);
            intent.putExtra(Intent.EXTRA_TEXT, report.toString());
            startActivity(intent);

            // If you don't kill the VM here the app goes into limbo
            System.exit(-1);
        }
    }

    public static interface OnCacheClearedCallback
    {
        public void onCacheCleared();
    }

    public static void clearCacheWithThread(final Activity activity, final OnCacheClearedCallback callback)
    {
        new Thread()
        {
            private boolean recursiveDelete(File file)
            {
                if (file.isFile())
                {
                    if (file.delete())
                        return true;
                    Log.d(TAG, "Delete " + file.getAbsolutePath() + " failed!");
                    return false;
                }

                boolean result = true;
                File[] files = file.listFiles();
                if (files != null)
                    for (File subfile : files)
                        if (subfile != null)
                            result &= recursiveDelete(subfile);
                return result;
            }

            @Override
            public void run()
            {
                recursiveDelete(Picasso.getCacheDir());
                Picasso.onLowMemory();
                PostsTable.deleteAllPosts();
                TagsTable.deleteAllTags();
                PostTagsLinkTable.deleteAllPostTagsLink();

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        callback.onCacheCleared();
                    }
                });
            }
        }.start();
    }
}
