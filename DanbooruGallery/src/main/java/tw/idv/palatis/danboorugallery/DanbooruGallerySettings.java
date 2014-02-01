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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import tw.idv.palatis.danboorugallery.siteapi.SiteAPI;

public class DanbooruGallerySettings
{
    private static final String TAG = "DanbooruGallerySettings";

    public static final String KEY_PREF_FILTER_WIDTH = "pref_filter_width";
    public static final String KEY_PREF_FILTER_HEIGHT = "pref_filter_height";
    public static final String KEY_PREF_FILTER_RATING_SAFE = "pref_filter_rating_safe";
    public static final String KEY_PREF_FILTER_RATING_QUESTIONABLE = "pref_filter_rating_questionable";
    public static final String KEY_PREF_FILTER_RATING_EXPLICIT = "pref_filter_rating_explicit";

    public static final String KEY_PREF_COLUMNS_PORTRAIT = "pref_columns_portrait";
    public static final String KEY_PREF_COLUMNS_LANDSCAPE = "pref_columns_landscape";
    public static final String KEY_PREF_STICKY_GRID_HEADER = "pref_sticky_grid_header";
    public static final String KEY_PREF_SHOW_POST_ID = "pref_show_post_id";
    public static final String KEY_PREF_SHOW_IMAGE_RESOLUTION = "pref_show_image_resolution";
    public static final String KEY_PREF_STRICT_BANDWIDTH_USAGE = "pref_strict_bandwidth_usage";
    public static final String KEY_PREF_AGGRESSIVE_PREFETCH_PREVIEW = "pref_aggressive_prefetch_preview";
    public static final String KEY_PREF_ASYNC_IMAGE_LOADER_INDICATOR = "pref_async_image_loader_indicator";
    public static final String KEY_PREF_CLEAR_CACHE = "pref_clear_cache";
    public static final String KEY_PREF_COPYRIGHT = "pref_copyright";
    public static final String KEY_PREF_TRANSLATOR = "pref_translator";
    public static final String KEY_PREF_WEBSITE = "pref_website";
    public static final String KEY_PREF_VERSION = "pref_version";

    public static final String SAVEDIR = "DanbooruGallery";

    static SharedPreferences sSharedPreferences;

    public static void init(Context context)
    {
        sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener)
    {
        sSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener)
    {
        sSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static boolean getShowAsyncImageLoaderIndicator()
    {
        return sSharedPreferences.getBoolean(KEY_PREF_ASYNC_IMAGE_LOADER_INDICATOR, false);
    }

    public static int getColumnsPortrait()
    {
        return sSharedPreferences.getInt(KEY_PREF_COLUMNS_PORTRAIT, 3);
    }

    public static int getColumnsLandscape()
    {
        return sSharedPreferences.getInt(KEY_PREF_COLUMNS_LANDSCAPE, 5);
    }

    public static boolean getStickyGridHeader()
    {
        return sSharedPreferences.getBoolean(KEY_PREF_STICKY_GRID_HEADER, true);
    }

    public static boolean getShowPostId()
    {
        return sSharedPreferences.getBoolean(KEY_PREF_SHOW_POST_ID, true);
    }

    public static boolean getShowImageResolution()
    {
        return sSharedPreferences.getBoolean(KEY_PREF_SHOW_IMAGE_RESOLUTION, true);
    }

    public static int getBandwidthUsageType()
    {
        return sSharedPreferences.getBoolean(KEY_PREF_STRICT_BANDWIDTH_USAGE, true)
            ? SiteAPI.PAGE_LIMIT_TYPE_STRICT
            : SiteAPI.PAGE_LIMIT_TYPE_RELAXED;
    }

    public static boolean getAggressivePrefetchPreview()
    {
        return sSharedPreferences.getBoolean(KEY_PREF_AGGRESSIVE_PREFETCH_PREVIEW, false);
    }

    public static int getFilterImageWidth()
    {
        return sSharedPreferences.getInt(KEY_PREF_FILTER_WIDTH, 0);
    }

    public static int getFilterImageHeight()
    {
        return sSharedPreferences.getInt(KEY_PREF_FILTER_HEIGHT, 0);
    }

    public static boolean getFilterRatingSafe()
    {
        return sSharedPreferences.getBoolean(KEY_PREF_FILTER_RATING_SAFE, true);
    }

    public static boolean getFilterRatingQuestionable()
    {
        return sSharedPreferences.getBoolean(KEY_PREF_FILTER_RATING_QUESTIONABLE, false);
    }

    public static boolean getFilterRatingExplicit()
    {
        return sSharedPreferences.getBoolean(KEY_PREF_FILTER_RATING_EXPLICIT, false);
    }
}
