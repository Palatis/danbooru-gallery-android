////////////////////////////////////////////////////////////////////////////////
//Danbooru Gallery Android - an danbooru-style imageboard browser
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

import android.app.Fragment;
import android.app.FragmentManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;

import tw.idv.palatis.danboorugallery.PostDetailFragment;
import tw.idv.palatis.danboorugallery.database.PostsTable;
import tw.idv.palatis.danboorugallery.model.Post;

public class PostDetailPagerAdapter
    extends FragmentStatePagerAdapter
{
    private Cursor mCursor;

    public PostDetailPagerAdapter(FragmentManager fm, Cursor cursor)
    {
        super(fm);
        mCursor = cursor;
    }

    @Override
    public Fragment getItem(int position)
    {
        mCursor.moveToPosition(position);

        Bundle arguments = new Bundle();
        arguments.putInt(Post.KEY_POST_ID, mCursor.getInt(PostsTable.INDEX_POST_DATABASE_ID));

        Fragment fragment = new PostDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public int getCount()
    {
        if (mCursor != null)
            return mCursor.getCount();
        return 0;
    }

    public Cursor swapCursor(Cursor cursor)
    {
        Cursor oldCursor = mCursor;
        mCursor = cursor;
        notifyDataSetChanged();
        return oldCursor;
    }

    public void changeCursor(Cursor cursor)
    {
        if (mCursor != null && !mCursor.isClosed())
            mCursor.close();
        mCursor = cursor;
        notifyDataSetChanged();
    }

    public Cursor getCursor(int position)
    {
        if (mCursor != null)
            mCursor.moveToPosition(position);
        return mCursor;
    }
}
