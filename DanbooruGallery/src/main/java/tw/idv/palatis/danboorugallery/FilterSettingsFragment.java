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

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class FilterSettingsFragment
    extends PreferenceFragment
{
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
//
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
//
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
//
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

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_filters);

        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_FILTER_WIDTH), 0);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_FILTER_HEIGHT), 0);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_FILTER_RATING_SAFE), true);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_FILTER_RATING_QUESTIONABLE), false);
        bindPreferenceSummaryToValue(findPreference(DanbooruGallerySettings.KEY_PREF_FILTER_RATING_EXPLICIT), false);
    }
}
