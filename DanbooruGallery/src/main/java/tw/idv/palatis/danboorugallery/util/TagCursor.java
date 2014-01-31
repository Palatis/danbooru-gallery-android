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

import android.database.AbstractCursor;

import java.util.Collections;
import java.util.List;

import tw.idv.palatis.danboorugallery.model.Tag;

public class TagCursor
        extends AbstractCursor
{
    private static final String TAG = "TagCursor";

    private static final String[] sColumns = new String[] {
        Tag.KEY_TAG_DATABASE_ID,
        Tag.KEY_TAG_ID,
        Tag.KEY_TAG_NAME, Tag.KEY_TAG_POST_COUNT,
    };

    private List<Tag> mTags = Collections.emptyList();

    public TagCursor(List<Tag> tags)
    {
        mTags = tags;
    }

    public Tag getTag()
    {
        return mTags.get(mPos);
    }

    @Override
    public int getCount()
    {
        if (mTags != null)
            return mTags.size();
        return 0;
    }

    @Override
    public String[] getColumnNames()
    {
        return sColumns;
    }

    @Override
    public String getString(int index)
    {
        switch (index)
        {
            case 0: // Tag.KEY_TAG_DATABASE_ID
                return Integer.toString(mPos);
            case 1: // Tag.KEY_TAG_ID
                return Integer.toString(mTags.get(mPos).id);
            case 2: // Tag.KEY_TAG_NAME
                return mTags.get(mPos).name;
            case 3: // Tag.KEY_TAG_POST_COUNT
                return Integer.toString(mTags.get(mPos).post_count);
        }
        throw new IllegalArgumentException("Column " + index + " is not a String.");
    }

    @Override
    public short getShort(int index)
    {
        switch (index)
        {
            case 0: // Tag.KEY_TAG_DATABASE_ID
                if (mPos <= Short.MAX_VALUE)
                    return (short)mPos;
            case 1: // Tag.KEY_TAG_ID
            {
                int id = mTags.get(mPos).id;
                if (id <= Short.MAX_VALUE)
                    return (short)id;
            }
            // case 2: // Tag.KEY_TAG_NAME
            case 3: // Tag.KEY_TAG_POST_COUNT
            {
                int post_count = mTags.get(mPos).post_count;
                if (post_count <= Short.MAX_VALUE)
                    return (short)post_count;
            }
        }
        throw new IllegalArgumentException("Cannot convert column " + index + " to short.");
    }

    @Override
    public int getInt(int index)
    {
        switch (index)
        {
            case 0: // "_id"
                return mPos;
            case 1: // Tag.KEY_TAG_ID
                return mTags.get(mPos).id;
            // case 2: // Tag.KEY_TAG_NAME
            case 3: // Tag.KEY_TAG_POST_COUNT
                return mTags.get(mPos).post_count;
        }
        throw new IllegalArgumentException("Cannot get column " + index + " is an integer type.");
    }

    @Override
    public long getLong(int index)
    {
        switch (index)
        {
            case 0: // "_id"
                return mPos;
            case 1: // Tag.KEY_TAG_ID
                return mTags.get(mPos).id;
            // case 2: // Tag.KEY_TAG_NAME
            case 3: // Tag.KEY_TAG_POST_COUNT
                return mTags.get(mPos).post_count;
        }
        throw new IllegalArgumentException("Cannot get column " + index + " is a long type.");
    }

    @Override
    public float getFloat(int index)
    {
        try
        {
            return getLong(index);
        }
        catch (IllegalArgumentException ex)
        {
            throw new IllegalArgumentException("Cannot get column " + index + " is a float type.");
        }
    }

    @Override
    public double getDouble(int index)
    {
        try
        {
            return getLong(index);
        }
        catch (IllegalArgumentException ex)
        {
            throw new IllegalArgumentException("Cannot get column " + index + " is a double type.");
        }
    }

    @Override
    public boolean isNull(int i)
    {
        return false;
    }

    @Override
    public void close()
    {
        super.close();
        mTags = null;
    }

    @Override
    public boolean isClosed()
    {
        return mTags == null;
    }
}