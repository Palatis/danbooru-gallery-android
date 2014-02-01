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

import android.database.sqlite.SQLiteDatabase;

public class PostTagsLinkTable
{
    public static final String TABLE_NAME = "post_tags_link";
    public static final String MAIN_TABLE_NAME = DanbooruGalleryDatabase.MAIN_DATABASE_NAME + ".post_tags_link";
    public static final String MEMORY_TABLE_NAME = DanbooruGalleryDatabase.MEMORY_DATABASE_NAME + ".post_tags_link";
    public static final String KEY_LINK_DATABASE_ID = "_id";
    public static final String KEY_POST_DATABASE_ID = "post_id";
    public static final String KEY_TAG_HASHCODE = "tag_hashcode";

    private static SQLiteDatabase sDatabase;

    public static void init(SQLiteDatabase database)
    {
        sDatabase = database;
    }

    public static void backupRestored()
    {
        deleteAllPostTagsLink();
    }

    public static int deletePostTagsLinkWithPostIds(String[] ids)
    {
        if (ids.length == 0)
            return 0;

        StringBuilder selection = new StringBuilder(
            KEY_POST_DATABASE_ID.length() +
            6 + // " IN (".length()
            ids.length * 2 +
            1 // ")".length()
        );
        selection
            .append(KEY_POST_DATABASE_ID)
            .append(" IN (");
        for (String id : ids)
            selection.append("?,");
        selection.setLength(selection.length() - 1); // delete
        selection.append(")");
        return sDatabase.delete(
            MAIN_TABLE_NAME,
            selection.toString(), ids
        );
    }

    public static void deleteAllPostTagsLink()
    {
        sDatabase.delete(MAIN_TABLE_NAME, null, null);
    }
}
