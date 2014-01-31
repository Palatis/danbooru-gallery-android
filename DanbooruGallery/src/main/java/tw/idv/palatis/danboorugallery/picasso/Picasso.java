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

package tw.idv.palatis.danboorugallery.picasso;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Cache;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.StatsSnapshot;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

import tw.idv.palatis.danboorugallery.DanbooruGallerySettings;
import tw.idv.palatis.danboorugallery.R;

import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

public class Picasso
{
    private static final String TAG = "Local Picasso";

    private static final String PICASSO_CACHE = ".picasso-cache";
    private static final int MIN_DISK_CACHE_SIZE = 4 * 1024 * 1024; // 4MB

    private static Downloader sDownloader = null;
    private static Cache sMemCache = null;
    private static com.squareup.picasso.Picasso sInstancePreview = null;
    private static com.squareup.picasso.Picasso sInstance = null;

    private static SharedPreferences.OnSharedPreferenceChangeListener sOnSharedPreferenceChangeListener =
        new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
                sInstance.setDebugging(DanbooruGallerySettings.getShowAsyncImageLoaderIndicator());
            }
        };

    public static void init(Context context)
    {
        if (sInstance != null)
            sInstance.shutdown();

        File cache = _createDefaultCacheDir(context);
        sDownloader = new OkHttpRefererDownloader(cache, calculateDiskCacheSize(cache));
        sMemCache = new LruCache(_calculateMemoryCacheSize(context));

        sInstancePreview = new com.squareup.picasso.Picasso.Builder(context)
            .memoryCache(sMemCache)
            .downloader(sDownloader)
            .executor(Executors.newFixedThreadPool(4))
            .debugging(DanbooruGallerySettings.getShowAsyncImageLoaderIndicator())
            .build();

        sInstance = new com.squareup.picasso.Picasso.Builder(context)
            .memoryCache(sMemCache)
            .downloader(sDownloader)
            .executor(Executors.newFixedThreadPool(2))
            .debugging(DanbooruGallerySettings.getShowAsyncImageLoaderIndicator())
            .build();

        DanbooruGallerySettings.registerOnSharedPreferenceChangeListener(sOnSharedPreferenceChangeListener);
    }

    public static com.squareup.picasso.Picasso with()
    {
        return sInstance;
    }

    public static com.squareup.picasso.Picasso withPreview()
    {
        return sInstancePreview;
    }

    public static Downloader getDownloader()
    {
        return sDownloader;
    }

    public static StatsSnapshot getSnapshot()
    {
        return sInstance.getSnapshot();
    }

    public static StatsSnapshot getPreviewSnapshot()
    {
        return sInstancePreview.getSnapshot();
    }

    public static long calculateDiskCacheSize(File dir)
    {
        long size = MIN_DISK_CACHE_SIZE;

        try
        {
            // Target 5% of the total space.
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            size = statFs.getTotalBytes() / 20;
        }
        catch (IllegalArgumentException ignored) { }

        // Bound inside min/max size for disk cache.
        return Math.max(size, MIN_DISK_CACHE_SIZE);
    }

    private static int _calculateMemoryCacheSize(Context context)
    {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && SDK_INT >= HONEYCOMB)
            memoryClass = ActivityManagerHoneycomb.getLargeMemoryClass(am);

        // Target ~25% of the available heap.
        return 1024 * 1024 * memoryClass / 4;
    }

    public static File getCacheDir()
    {
        return new File(Environment.getExternalStorageDirectory(), DanbooruGallerySettings.SAVEDIR + "/" + PICASSO_CACHE);
    }

    private static File _createDefaultCacheDir(Context context)
    {
        File cache = getCacheDir();
        if (!cache.exists())
            cache.mkdirs();

        try
        {
            File nomedia = new File(cache, ".nomedia");
            if (!nomedia.exists())
                if (!nomedia.createNewFile())
                {
                    Toast.makeText(context, R.string.error_nomedia_create_failed, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "creating .nomedia failed without exception.");
                }
        }
        catch (IOException ex)
        {
            Toast.makeText(context, R.string.error_nomedia_create_failed, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "creating .nomedia failed.", ex);
        }

        return cache;
    }

    public static void onLowMemory()
    {
        if (sMemCache != null)
            sMemCache.clear();
    }

    public static void onTrimMemory(int level)
    {
        onLowMemory();
    }

    @TargetApi(HONEYCOMB)
    private static class ActivityManagerHoneycomb {
        static int getLargeMemoryClass(ActivityManager activityManager) {
            return activityManager.getLargeMemoryClass();
        }
    }
}
