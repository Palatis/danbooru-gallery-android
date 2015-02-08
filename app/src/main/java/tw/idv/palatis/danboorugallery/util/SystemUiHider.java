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

package tw.idv.palatis.danboorugallery.util;

import android.view.View;

/**
 * Created by 其威 on 2014/1/26.
 */
public class SystemUiHider
    extends UiHider
{
    private static final String TAG = "SystemUiHider";

    /**
     * Flags for {@link android.view.View#setSystemUiVisibility(int)} to use when showing the
     * system UI.
     */
    private int mShowFlags;

    /**
     * Flags for {@link android.view.View#setSystemUiVisibility(int)} to use when hiding the
     * system UI.
     */
    private int mHideFlags;

    private final View mAncharView;

    public SystemUiHider(View view, long autoHideMillis, OnVisibilityChangeListener callback)
    {
        super(autoHideMillis, callback);
        mAncharView = view;
        mShowFlags = View.SYSTEM_UI_FLAG_VISIBLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        mHideFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    }

    @Override
    public void show()
    {
        mAncharView.setSystemUiVisibility(mShowFlags);
        super.show();
    }

    @Override
    public void hide()
    {
        mAncharView.setSystemUiVisibility(mHideFlags);
        super.hide();
    }
}
