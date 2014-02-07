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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Cache;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.LruCache;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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
    private static com.squareup.picasso.Picasso sInstancePrefetch = null;
    private static com.squareup.picasso.Picasso sInstancePreview = null;
    private static com.squareup.picasso.Picasso sInstance = null;

    private static ThreadPoolExecutor sExecutorPrefetch = null;
    private static ThreadPoolExecutor sExecutorPreview = null;
    private static ThreadPoolExecutor sExecutor = null;

    private static SharedPreferences.OnSharedPreferenceChangeListener sOnSharedPreferenceChangeListener =
        new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
                boolean debugging = DanbooruGallerySettings.getShowAsyncImageLoaderIndicator();
                if (sInstancePrefetch != null)
                    sInstancePrefetch.setDebugging(debugging);
                if (sInstancePreview != null)
                    sInstancePreview.setDebugging(debugging);
                if (sInstance != null)
                    sInstance.setDebugging(debugging);
            }
        };

    public static void init(Context context)
    {
        sMemCache = new LruCache(_calculateMemoryCacheSize(context));

        sExecutorPrefetch = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        sExecutorPreview = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        sExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

        DanbooruGallerySettings.registerOnSharedPreferenceChangeListener(sOnSharedPreferenceChangeListener);
    }

    public static void adjustThreadCount(NetworkInfo info)
    {
        if (info == null || !info.isConnectedOrConnecting())
        {
            // sExecutorPrefetch.setCorePoolSize(1);
            // sExecutorPrefetch.setMaximumPoolSize(1);
            sExecutorPreview.setCorePoolSize(3);
            sExecutorPreview.setMaximumPoolSize(3);
            sExecutor.setCorePoolSize(1);
            sExecutor.setMaximumPoolSize(1);
        }
        else
        {
            switch (info.getType())
            {
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_WIMAX:
                case ConnectivityManager.TYPE_ETHERNET:
                    // sExecutorPrefetch.setCorePoolSize(1);
                    // sExecutorPrefetch.setMaximumPoolSize(1);
                    sExecutorPreview.setCorePoolSize(4);
                    sExecutorPreview.setMaximumPoolSize(4);
                    sExecutor.setCorePoolSize(2);
                    sExecutor.setMaximumPoolSize(2);
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    switch (info.getSubtype())
                    {
                        case TelephonyManager.NETWORK_TYPE_LTE: // 4G
                        case TelephonyManager.NETWORK_TYPE_HSPAP:
                        case TelephonyManager.NETWORK_TYPE_EHRPD:
                            // sExecutorPrefetch.setCorePoolSize(1);
                            // sExecutorPrefetch.setMaximumPoolSize(1);
                            sExecutorPreview.setCorePoolSize(3);
                            sExecutorPreview.setMaximumPoolSize(3);
                            sExecutor.setCorePoolSize(2);
                            sExecutor.setMaximumPoolSize(2);
                            break;
                        case TelephonyManager.NETWORK_TYPE_UMTS: // 3G
                        case TelephonyManager.NETWORK_TYPE_CDMA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        case TelephonyManager.NETWORK_TYPE_EVDO_B:
                            // sExecutorPrefetch.setCorePoolSize(1);
                            // sExecutorPrefetch.setMaximumPoolSize(1);
                            sExecutorPreview.setCorePoolSize(2);
                            sExecutorPreview.setMaximumPoolSize(2);
                            sExecutor.setCorePoolSize(1);
                            sExecutor.setMaximumPoolSize(1);
                            break;
                        case TelephonyManager.NETWORK_TYPE_GPRS: // 2G
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                            // sExecutorPrefetch.setCorePoolSize(1);
                            // sExecutorPrefetch.setMaximumPoolSize(1);
                            sExecutorPreview.setCorePoolSize(1);
                            sExecutorPreview.setMaximumPoolSize(1);
                            sExecutor.setCorePoolSize(1);
                            sExecutor.setMaximumPoolSize(1);
                            break;
                        default:
                            // sExecutorPrefetch.setCorePoolSize(1);
                            // sExecutorPrefetch.setMaximumPoolSize(1);
                            sExecutorPreview.setCorePoolSize(4);
                            sExecutorPreview.setMaximumPoolSize(4);
                            sExecutor.setCorePoolSize(2);
                            sExecutor.setMaximumPoolSize(2);
                    }
                    break;
                default:
                    // sExecutorPrefetch.setCorePoolSize(1);
                    // sExecutorPrefetch.setMaximumPoolSize(1);
                    sExecutorPreview.setCorePoolSize(4);
                    sExecutorPreview.setMaximumPoolSize(4);
                    sExecutor.setCorePoolSize(2);
                    sExecutor.setMaximumPoolSize(2);
            }
        }
    }

    public static com.squareup.picasso.Picasso with(Context context)
    {
        if (sInstance == null)
        {
            sInstance = new com.squareup.picasso.Picasso.Builder(context)
                .memoryCache(sMemCache)
                .downloader(getDownloader(context))
                .executor(sExecutor)
                .debugging(DanbooruGallerySettings.getShowAsyncImageLoaderIndicator())
                .build();
        }
        return sInstance;
    }

    public static com.squareup.picasso.Picasso withPreview(Context context)
    {
        if (sInstancePreview == null)
        {
            sInstancePreview = new com.squareup.picasso.Picasso.Builder(context)
                .memoryCache(sMemCache)
                .downloader(getDownloader(context))
                .executor(sExecutorPreview)
                .debugging(DanbooruGallerySettings.getShowAsyncImageLoaderIndicator())
                .build();
        }
        return sInstancePreview;
    }

    public static com.squareup.picasso.Picasso withPrefetch(Context context)
    {
        if (sInstancePrefetch == null)
        {
            sInstancePrefetch = new com.squareup.picasso.Picasso.Builder(context)
                .memoryCache(sMemCache)
                .downloader(getDownloader(context))
                .executor(sExecutorPrefetch)
                .debugging(DanbooruGallerySettings.getShowAsyncImageLoaderIndicator())
                .build();
        }
        return sInstancePrefetch;
    }

    public static Downloader getDownloader(Context context)
    {
        if (sDownloader == null)
        {
            File cache = _createDefaultCacheDir(context);
            sDownloader = new OkHttpRefererDownloader(cache, calculateDiskCacheSize(cache));
        }
        return sDownloader;
    }

    public static Cache getMemCache()
    {
        return sMemCache;
    }

    public static long calculateDiskCacheSize(File dir)
    {
        long size = MIN_DISK_CACHE_SIZE;

        try
        {
            // Target 5% of the total space.
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            size = statFs.getBlockCountLong() * statFs.getBlockSizeLong() / 20;
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
