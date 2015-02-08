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

package tw.idv.palatis.danboorugallery.android.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PopupMenu
    extends android.widget.PopupMenu
{
    /**
     * {@inheritDoc}
     */
    public PopupMenu(Context context, View anchor)
    {
        super(context, anchor);
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public PopupMenu(Context context, View anchor, int gravity)
    {
        super(context, anchor, gravity);
    }

    private static Field sMPopupField = null;
    private static Method sSetForceShowIconMethod = null;
    private static Field sMForceShowIcon = null;

    public void setForceShowIcon(boolean showIcon)
    {
        try
        {
            if (sMPopupField == null)
            {
                sMPopupField = getClass().getSuperclass().getDeclaredField("mPopup");
                sMPopupField.setAccessible(true);
            }
            if (sMPopupField != null && sSetForceShowIconMethod == null)
                sSetForceShowIconMethod = sMPopupField.getType().getMethod("setForceShowIcon", boolean.class);

            sSetForceShowIconMethod.invoke(sMPopupField.get(this), showIcon);
        }
        catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException ignored) { }
    }

    public boolean getForceShowIcon()
    {
        try
        {
            if (sMPopupField == null)
            {
                sMPopupField = getClass().getSuperclass().getDeclaredField("mPopup");
                sMPopupField.setAccessible(true);
            }
            if (sMPopupField != null && sMForceShowIcon == null)
            {
                sMForceShowIcon = sMPopupField.getType().getDeclaredField("mForceShowIcon");
                sMForceShowIcon.setAccessible(true);
            }

            return sMForceShowIcon.getBoolean(sMPopupField.get(this));
        }
        catch (IllegalAccessException | NoSuchFieldException ignored) { }

        return false;
    }
}
