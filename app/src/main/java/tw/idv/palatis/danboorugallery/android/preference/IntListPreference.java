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

package tw.idv.palatis.danboorugallery.android.preference;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class IntListPreference
    extends ListPreference
{
    private static final String TAG = "IntListPreference";

    public IntListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public IntListPreference(Context context)
    {
        super(context);
    }

    @Override
    protected boolean persistString(String value)
    {
        if(value == null)
        {
            return false;
        }
        else
        {
            return persistInt(Integer.valueOf(value));
        }
    }

    @Override
    protected String getPersistedString(String defaultReturnValue)
    {
        if(getSharedPreferences().contains(getKey()))
        {
            int intValue = getPersistedInt(0);
            return String.valueOf(intValue);
        }
        else
        {
            return defaultReturnValue;
        }
    }
}