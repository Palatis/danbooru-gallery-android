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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SearchView;

/**
 * A {@link android.widget.SearchView} that doesn't clear out the search text
 * during {@link tw.idv.palatis.danboorugallery.android.widget.NoClearSearchView#onActionViewExpanded()} or
 * {@link tw.idv.palatis.danboorugallery.android.widget.NoClearSearchView#onActionViewCollapsed()}
 */
public class NoClearSearchView
    extends SearchView
{
    /**
     * {@inheritDoc}
     */
    public NoClearSearchView(Context context)
    {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    public NoClearSearchView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActionViewCollapsed()
    {
        CharSequence query = getQuery();
        super.onActionViewCollapsed();
        setQuery(query, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActionViewExpanded()
    {
        CharSequence query = getQuery();
        super.onActionViewExpanded();
        setQuery(query, false);
    }
}
