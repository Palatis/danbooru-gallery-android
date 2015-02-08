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

package tw.idv.palatis.danboorugallery.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by 其威 on 2014/1/23.
 */
public class PostTagsView
{
    public static final String VIEW_NAME = "post_tags";
    // I guess you cannot create no memory views?
    // public static final String MAIN_VIEW_NAME = DanbooruGalleryDatabaseOpenHelper.MAIN_DATABASE_NAME + ".post_tags";
    // public static final String MEMORY_VIEW_NAME = DanbooruGalleryDatabaseOpenHelper.MEMORY_DATABASE_NAME + ".post_tags";
    public static final String KEY_POST_DATABASE_ID = "post_id";
    public static final String KEY_TAG_NAME = "tag_name";

    private static SQLiteDatabase sDatabase;

    public static void init(SQLiteDatabase database)
    {
        sDatabase = database;
    }

    public static final int INDEX_KEY_POST_TAG_TAG_NAME = 0;
    public static final String[] sPostTagsColumnNames = new String[] {
        KEY_TAG_NAME,
    };

    public static Cursor getTagNamesCursorForPostDatabaseId(int post_db_id)
    {
        return sDatabase.query(
            VIEW_NAME,
            sPostTagsColumnNames,
            KEY_POST_DATABASE_ID + " == ?",
            new String[] { Integer.toString(post_db_id) },
            null, null, null, null
        );
    }
}
