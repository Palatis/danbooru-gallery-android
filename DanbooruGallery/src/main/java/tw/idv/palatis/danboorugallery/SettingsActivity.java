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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import java.io.File;
import java.util.List;

import tw.idv.palatis.danboorugallery.database.PostsTable;
import tw.idv.palatis.danboorugallery.picasso.Picasso;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity
    extends PreferenceActivity
{
    private static final String TAG = "SettingsActivity";

    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // TODO: If Settings has multiple levels, Up should navigate up
            // that hierarchy.
            Intent intent = new Intent(this, PostListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            navigateUpTo(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final String KEY_CACHE_SIZE_CALCULATED = "cache_size_calculated";
    private static final String KEY_CACHE_SIZE = "cache_size";
    private static final String KEY_CACHE_SIZE_MAX = "cache_size_max";
    private static final String KEY_MEM_CACHE_SIZE = "mem_cache_size";
    private static final String KEY_MEM_CACHE_SIZE_MAX = "mem_cache_size_max";
    private static final String KEY_POST_COUNT = "post_count";
    private boolean mCacheSizeCalculated = false;
    private double mCacheSize = -1;
    private double mMaxCacheSize = -1;
    private double mMemCacheSize = -1;
    private double mMaxMemCacheSize = -1;
    private int mPostCount = -1;

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CACHE_SIZE_CALCULATED, mCacheSizeCalculated);
        outState.putDouble(KEY_CACHE_SIZE, mCacheSize);
        outState.putDouble(KEY_CACHE_SIZE_MAX, mMaxCacheSize);
        outState.putDouble(KEY_MEM_CACHE_SIZE, mMemCacheSize);
        outState.putDouble(KEY_MEM_CACHE_SIZE_MAX, mMaxMemCacheSize);
        outState.putInt(KEY_POST_COUNT, mPostCount);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();

        if (savedInstanceState != null)
        {
            mCacheSizeCalculated = savedInstanceState.getBoolean(KEY_CACHE_SIZE_CALCULATED, false);
            mCacheSize = savedInstanceState.getDouble(KEY_CACHE_SIZE, -1);
            mMaxCacheSize = savedInstanceState.getDouble(KEY_CACHE_SIZE_MAX, -1);
            mMemCacheSize = savedInstanceState.getDouble(KEY_MEM_CACHE_SIZE, -1);
            mMaxMemCacheSize = savedInstanceState.getDouble(KEY_MEM_CACHE_SIZE_MAX, -1);
            mPostCount = savedInstanceState.getInt(KEY_POST_COUNT, -1);
        }
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen()
    {
        if (!isSimplePreferences(this))
            return;

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.
        PreferenceCategory fakeHeader;

        // Add 'screen layout' preferences
        addPreferencesFromResource(R.xml.pref_screen_layout);

        // Add 'detail view' preference, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_detail_view);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_detail);

        // Add 'network policy' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_network_policy);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_network_policy);

        // Add 'debug' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_advanced);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_advanced);

        // Add 'about' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_about);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_about);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_COLUMNS_PORTRAIT), 3);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_COLUMNS_LANDSCAPE), 5);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_STICKY_GRID_HEADER), true);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_SHOW_POST_ID), true);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_SHOW_IMAGE_RESOLUTION), true);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_DOWNLOAD_FULLSIZE), false);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_AUTOPLAY_DELAY), 5000);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_STRICT_BANDWIDTH_USAGE), true);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_AGGRESSIVE_PREFETCH_PREVIEW), false);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_ASYNC_IMAGE_LOADER_INDICATOR), false);

        Preference preference;

        preference = findPreference(DanbooruGallerySettings.KEY_PREF_CLEAR_CACHE);
        assert preference != null;
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference)
            {
                setProgressBarIndeterminate(true);
                setProgressBarIndeterminateVisibility(true);
                preference.setEnabled(false);
                DanbooruGalleryApplication.clearCacheWithThread(SettingsActivity.this, new DanbooruGalleryApplication.OnCacheClearedCallback()
                {
                    @Override
                    public void onCacheCleared()
                    {
                        mCacheSizeCalculated = false;
                        new Thread(new CalculateDiskUsageRunnable(preference, new CalculateDiskUsageRunnable.Callbacks()
                        {
                            @Override
                            public void onCalculationDone(double disk_size, double disk_size_max, double mem_size, double mem_size_max, int post_count)
                            {
                                mCacheSizeCalculated = true;
                                mCacheSize = disk_size;
                                mMaxCacheSize = disk_size_max;
                                mMemCacheSize = mem_size;
                                mMaxMemCacheSize = mem_size_max;
                                mPostCount = post_count;
                                setProgressBarIndeterminateVisibility(false);
                            }
                        },
                            getResources().getString(R.string.pref_description_clear_cache_calculating),
                            getResources().getString(R.string.pref_description_clear_cache_done)
                        )).start();

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                preference.setEnabled(true);
                            }
                        });
                    }
                });
                return true;
            }
        });

        if (mCacheSizeCalculated)
        {
            preference.setSummary(getResources().getString(R.string.pref_description_clear_cache_done,
                mCacheSize, mMaxCacheSize, mMemCacheSize, mMaxMemCacheSize, mPostCount));
        }
        else
        {
            new Thread(new CalculateDiskUsageRunnable(preference, new CalculateDiskUsageRunnable.Callbacks()
                {
                    @Override
                    public void onCalculationDone(double disk_size, double disk_size_max, double mem_size, double mem_size_max, int post_count)
                    {
                        mCacheSizeCalculated = true;
                        mCacheSize = disk_size;
                        mMaxCacheSize = disk_size_max;
                        mMemCacheSize = mem_size;
                        mMaxMemCacheSize = mem_size_max;
                        mPostCount = post_count;
                    }
                },
                getResources().getString(R.string.pref_description_clear_cache_calculating),
                getResources().getString(R.string.pref_description_clear_cache_done)
            )).start();
        }

        if (getResources().getString(R.string.pref_title_translator).trim().isEmpty())
        {
            preference = findPreference(DanbooruGallerySettings.KEY_PREF_TRANSLATOR);
            getPreferenceScreen().removePreference(preference);

        }

        preference = findPreference(DanbooruGallerySettings.KEY_PREF_VERSION);
        try
        {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            preference.setTitle(getResources().getString(R.string.pref_title_version, pinfo.versionCode, pinfo.versionName));
            preference.setSummary(getResources().getString(R.string.pref_description_version, pinfo.versionCode, pinfo.versionName));
        }
        catch (PackageManager.NameNotFoundException ignored)
        {
            preference.setTitle(getResources().getString(R.string.pref_title_version, 99999999, "????????"));
            preference.setSummary(getResources().getString(R.string.pref_description_version, 99999999, "????????"));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane()
    {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context)
    {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context)
    {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /** {@inheritDoc} */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target)
    {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
        new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value)
            {
                if (preference instanceof ListPreference)
                {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(value.toString());

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                        index >= 0
                            ? listPreference.getEntries()[index]
                            : null);
                }
                return true;
            }
        };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference, int defaultValue)
    {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        try
        {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getInt(preference.getKey(), defaultValue));
        }
        catch (ClassCastException ignored)
        {
            PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                .edit().remove(preference.getKey()).commit();
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getInt(preference.getKey(), defaultValue));
        }
    }

    private static void bindPreferenceSummaryToValue(Preference preference, boolean defaultValue)
    {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        try
        {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getBoolean(preference.getKey(), defaultValue));
        }
        catch (ClassCastException ignored)
        {
            PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                .edit().remove(preference.getKey()).commit();
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getBoolean(preference.getKey(), defaultValue));
        }
    }

//    private static void bindPreferenceSummaryToValue(Preference preference, float defaultValue)
//    {
//        // Set the listener to watch for value changes.
//        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
//
//        // Trigger the listener immediately with the preference's
//        // current value.
//        try
//        {
//            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                    .getDefaultSharedPreferences(preference.getContext())
//                    .getFloat(preference.getKey(), defaultValue));
//        }
//        catch (ClassCastException ignored)
//        {
//            PreferenceManager.getDefaultSharedPreferences(preference.getContext())
//                .edit().remove(preference.getKey()).commit();
//            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                    .getDefaultSharedPreferences(preference.getContext())
//                    .getFloat(preference.getKey(), defaultValue));
//        }
//    }

//    private static void bindPreferenceSummaryToValue(Preference preference, long defaultValue)
//    {
//        // Set the listener to watch for value changes.
//        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
//
//        // Trigger the listener immediately with the preference's
//        // current value.
//        try
//        {
//            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                    .getDefaultSharedPreferences(preference.getContext())
//                    .getLong(preference.getKey(), defaultValue));
//        }
//        catch (ClassCastException ignored)
//        {
//            PreferenceManager.getDefaultSharedPreferences(preference.getContext())
//                .edit().remove(preference.getKey()).commit();
//            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                    .getDefaultSharedPreferences(preference.getContext())
//                    .getLong(preference.getKey(), defaultValue));
//        }
//    }

//    private static void bindPreferenceSummaryToValue(Preference preference, String defaultValue)
//    {
//        // Set the listener to watch for value changes.
//        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
//
//        // Trigger the listener immediately with the preference's
//        // current value.
//        try
//        {
//            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                    .getDefaultSharedPreferences(preference.getContext())
//                    .getString(preference.getKey(), defaultValue));
//        }
//        catch (ClassCastException ignored)
//        {
//            PreferenceManager.getDefaultSharedPreferences(preference.getContext())
//                .edit().remove(preference.getKey()).commit();
//            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                    .getDefaultSharedPreferences(preference.getContext())
//                    .getString(preference.getKey(), defaultValue));
//        }
//    }

//    private static void bindPreferenceSummaryToValue(Preference preference, Set<String> defaultValue)
//    {
//        // Set the listener to watch for value changes.
//        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
//
//        // Trigger the listener immediately with the preference's
//        // current value.
//        try
//        {
//            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                    .getDefaultSharedPreferences(preference.getContext())
//                    .getStringSet(preference.getKey(), defaultValue));
//        }
//        catch (ClassCastException ignored)
//        {
//            PreferenceManager.getDefaultSharedPreferences(preference.getContext())
//                .edit().remove(preference.getKey()).commit();
//            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                    .getDefaultSharedPreferences(preference.getContext())
//                    .getStringSet(preference.getKey(), defaultValue));
//        }
//    }

    /**
     * This fragment shows screen layout preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ScreenLayoutPreferenceFragment
        extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_screen_layout);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_COLUMNS_PORTRAIT), 3);
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_COLUMNS_LANDSCAPE), 5);
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_STICKY_GRID_HEADER), true);
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_SHOW_POST_ID), true);
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_SHOW_IMAGE_RESOLUTION), true);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DetailViewPreferenceFragment
        extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_screen_layout);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_DOWNLOAD_FULLSIZE), false);
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_AUTOPLAY_DELAY), 5000);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NetworkPolicyPreferenceFragment
        extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_network_policy);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_STRICT_BANDWIDTH_USAGE), true);
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_AGGRESSIVE_PREFETCH_PREVIEW), false);
        }
    }

    /**
     * This fragment shows debug preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AdvancedPreferenceFragment extends PreferenceFragment
    {
        private boolean mCacheSizeCalculated = false;
        private double mCacheSize = -1;
        private double mMaxCacheSize = -1;
        private double mMemCacheSize = -1;
        private double mMaxMemCacheSize = -1;
        private int mPostCount = -1;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_advanced);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_ASYNC_IMAGE_LOADER_INDICATOR), false);
            bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_CLEAR_CACHE), false);

            if (savedInstanceState != null)
            {
                mCacheSizeCalculated = savedInstanceState.getBoolean(KEY_CACHE_SIZE_CALCULATED, false);
                mCacheSize = savedInstanceState.getDouble(KEY_CACHE_SIZE, -1);
                mMaxCacheSize = savedInstanceState.getDouble(KEY_CACHE_SIZE_MAX, -1);
            }

            Preference preference = findPreference(DanbooruGallerySettings.KEY_PREF_CLEAR_CACHE);
            assert preference != null;
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference)
                {
                    getActivity().setProgressBarIndeterminate(true);
                    getActivity().setProgressBarIndeterminateVisibility(true);
                    DanbooruGalleryApplication.clearCacheWithThread(getActivity(), new DanbooruGalleryApplication.OnCacheClearedCallback() {
                        @Override
                        public void onCacheCleared()
                        {
                            mCacheSizeCalculated = false;
                            new Thread(new CalculateDiskUsageRunnable(preference, new CalculateDiskUsageRunnable.Callbacks()
                            {
                                @Override
                                public void onCalculationDone(double disk_size, double disk_size_max, double mem_size, double mem_size_max, int post_count)
                                {
                                    mCacheSizeCalculated = true;
                                    mCacheSize = disk_size;
                                    mMaxCacheSize = disk_size_max;
                                    mMemCacheSize = mem_size;
                                    mMaxMemCacheSize = mem_size_max;
                                    mPostCount = post_count;
                                    getActivity().setProgressBarIndeterminateVisibility(false);
                                }
                            },
                                getResources().getString(R.string.pref_description_clear_cache_calculating),
                                getResources().getString(R.string.pref_description_clear_cache_done)
                            )).start();
                        }
                    });
                    return true;
                }
            });

            if (mCacheSizeCalculated)
            {
                preference.setSummary(getResources().getString(R.string.pref_description_clear_cache_done,
                    mCacheSize, mMaxCacheSize, mMemCacheSize, mMaxMemCacheSize, mPostCount));
            }
            else
            {
                new Thread(new CalculateDiskUsageRunnable(preference, new CalculateDiskUsageRunnable.Callbacks()
                {
                    @Override
                    public void onCalculationDone(double disk_size, double disk_size_max, double mem_size, double mem_size_max, int post_count)
                    {
                        mCacheSizeCalculated = true;
                        mCacheSize = disk_size;
                        mMaxCacheSize = disk_size_max;
                        mMemCacheSize = mem_size;
                        mMaxMemCacheSize = mem_size_max;
                        mPostCount = post_count;
                    }
                },
                    getResources().getString(R.string.pref_description_clear_cache_calculating),
                    getResources().getString(R.string.pref_description_clear_cache_done)
                )).start();
            }
        }
    }

    /**
     * This fragment shows about preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AboutPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_about);

            Preference preference;

            if (getResources().getString(R.string.pref_title_translator).trim().isEmpty())
            {
                preference = findPreference(DanbooruGallerySettings.KEY_PREF_TRANSLATOR);
                getPreferenceScreen().removePreference(preference);
            }

            preference = findPreference(DanbooruGallerySettings.KEY_PREF_VERSION);
            try
            {
                PackageInfo pinfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                preference.setTitle(getResources().getString(R.string.pref_title_version, pinfo.versionCode, pinfo.versionName));
                preference.setSummary(getResources().getString(R.string.pref_description_version, pinfo.versionCode, pinfo.versionName));
            }
            catch (PackageManager.NameNotFoundException ignored)
            {
                preference.setTitle(getResources().getString(R.string.pref_title_version, 99999999, "????????"));
                preference.setSummary(getResources().getString(R.string.pref_description_version, 99999999, "????????"));
            }
        }
    }

    private static class CalculateDiskUsageRunnable
        implements Runnable
    {
        public interface Callbacks
        {
            public void onCalculationDone(double disk_size, double disk_size_max, double mem_size, double mem_size_max, int post_count);
        }

        private double disk_size = 0;
        private double disk_size_max = 0;
        private double mem_size = 0;
        private double mem_size_max = 0;
        private int post_count = 0;
        private String mMessage;
        private String mMessageDone;

        private Callbacks mCallbacks;
        private Preference mPreference;
        private Handler mHandler = new Handler();

        public CalculateDiskUsageRunnable(Preference preference, Callbacks callbacks, String message, String messageDone)
        {
            mPreference = preference;
            mCallbacks = callbacks;
            mMessage = message;
            mMessageDone = messageDone;
        }

        long accumulateDiskSizeRecursive(File file)
        {
            if (!file.exists())
            {
                synchronized (mUpdateRunnable)
                {
                    mHandler.removeCallbacks(mUpdateRunnable);
                    mHandler.post(mUpdateRunnable);
                }
                return 0;
            }

            if (file.isFile())
            {
                disk_size = file.length() / 1024.0 / 1024.0;
                synchronized (mUpdateRunnable)
                {
                    mHandler.removeCallbacks(mUpdateRunnable);
                    mHandler.post(mUpdateRunnable);
                }
                return file.length();
            }

            if (file.isDirectory())
            {
                long ret = file.length();
                File[] children = file.listFiles();
                if (children != null)
                    for (File child : children)
                    {
                        ret += accumulateDiskSizeRecursive(child);
                        disk_size = ret / 1024.0 / 1024.0;
                        synchronized (mUpdateRunnable)
                        {
                            mHandler.removeCallbacks(mUpdateRunnable);
                            mHandler.post(mUpdateRunnable);
                        }
                    }

                return ret;
            }

            throw new IllegalArgumentException("The file " + file.getAbsolutePath() + " is not a file nor a directory.");
        }

        private final Runnable mUpdateRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    mPreference.setSummary(String.format(
                        mMessage, disk_size, disk_size_max, mem_size, mem_size_max, post_count));
                }
                catch (IllegalStateException ignored) { }
            }
        };

        @Override
        public void run()
        {
            mem_size = Picasso.getMemCache().size() / 1024.0 / 1024.0;
            mem_size_max = Picasso.getMemCache().maxSize() / 1024.0 / 1024.0;
            post_count = PostsTable.getPostCount();
            mHandler.post(mUpdateRunnable);

            File cache = Picasso.getCacheDir();
            disk_size_max = Picasso.calculateDiskCacheSize(cache) / 1024.0 / 1024.0;
            mHandler.removeCallbacks(mUpdateRunnable);
            mHandler.post(mUpdateRunnable);
            disk_size = accumulateDiskSizeRecursive(cache) / 1024.0 / 1024.0;

            mMessage = mMessageDone;

            mHandler.removeCallbacks(mUpdateRunnable);
            mHandler.post(mUpdateRunnable);

            mCallbacks.onCalculationDone(disk_size, disk_size_max, mem_size, mem_size_max, post_count);
        }
    }
}
