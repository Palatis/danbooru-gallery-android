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

    // "related_tags":"touhou 300 1girl 193 solo 180 hat 125 short_hair 117 long_hair 88 red_eyes 85 blush 80 multiple_girls 75 highres 74 ribbon 62 bow 61 wings 60 smile 59 blonde_hair 57 2girls 56 breasts 48 dress 47 translated 47 bad_id 47 blue_eyes 46 open_mouth 45 hair_bow 44 animal_ears 42 blue_hair 39",
    // "related_tags_updated_at":"2014-01-22T01:12:34-05:00",
    public static final String KEY_TAG_ID = "id";                   // "id":29,
//    public static final String KEY_TAG_CATEGORY = "category";       // "category":3,
//    public static final String KEY_TAG_IS_LOCKED = "is_locked";     // "is_locked":false,
    public static final String KEY_TAG_NAME = "name";               // "name":"touhou",
    public static final String KEY_TAG_POST_COUNT = "post_count";   // "post_count":376077,
    public static final String KEY_TAG_SEARCH_COUNT = "search_count";
//    public static final String KEY_TAG_CREATED_AT = "created_at";   // "created":"2013-02-27T22:33:43-05:00",
//    public static final String KEY_TAG_UPDATED_AT = "updated_at";   // "updated":"2014-01-22T01:12:34-05:00"

    public int id;
//    public int category;
//    public boolean is_locked;
    public String name;
    public int post_count;
    public int search_count;
//    public Date created;
//    public Date updated;
    public List<Host> hosts = new ArrayList<>();

    public Tag(int id,
//               int category, boolean is_locked,
               String name, int post_count
//        , Date created, Date updated
    )
    {
        init(id,
//            category, is_locked,
            name, post_count
//            , created, updated
        );
    }

    private void init(int id,
//                      int category, boolean is_locked,
                      String name, int post_count
//        , Date created, Date updated
    )
    {
        this.id = id;
//        this.category = category;
//        this.is_locked = is_locked;
        this.name = name;
        this.post_count = post_count;
//        this.created = created;
//        this.updated = updated;
    }

    @Override
    public String toString()
    {
        return String.format("Tag: %d, %s, %d", id, name, post_count);
    }
}
