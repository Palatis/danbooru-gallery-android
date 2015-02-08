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

package tw.idv.palatis.danboorugallery.model;

import java.util.ArrayList;
import java.util.List;

import tw.idv.palatis.danboorugallery.database.DanbooruGalleryDatabase;

public class Tag
{
    private static final String TAG = "Tag";

    public static final String TABLE_NAME = "tags";
    public static final String MAIN_TABLE_NAME = DanbooruGalleryDatabase.MAIN_DATABASE_NAME + ".tags";
    public static final String MEMORY_TABLE_NAME = DanbooruGalleryDatabase.MEMORY_DATABASE_NAME + ".tags";
    public static final String KEY_TAG_DATABASE_ID = "_id";
    public static final String KEY_TAG_HASHCODE = "hashcode";

    public static final String KEY_TAG_ID = "id";                   // "id":29,
    public static final String KEY_TAG_NAME = "name";               // "name":"touhou",
    public static final String KEY_TAG_POST_COUNT = "post_count";   // "post_count":376077,
    public static final String KEY_TAG_SEARCH_COUNT = "search_count";

    public int id;
    public String name;
    public int post_count;
    public int search_count;
    public List<Host> hosts = new ArrayList<>();

    public Tag(int id, String name, int post_count)
    {
        this.id = id;
        this.name = name;
        this.post_count = post_count;
    }

    @Override
    public String toString()
    {
        return String.format("Tag: %d, %s, %d", id, name, post_count);
    }
}
