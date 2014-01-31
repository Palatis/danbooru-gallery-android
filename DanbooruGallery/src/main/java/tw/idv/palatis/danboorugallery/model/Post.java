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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.Arrays;
import java.util.Date;

import tw.idv.palatis.danboorugallery.database.DanbooruGalleryDatabase;

/**
 * These are standard required information for the infrastructure to show the posts,
 * all extended informations should go to the subclass.
 */
public abstract class Post
{
    private static final String TAG = "Post";

    public static final String TABLE_NAME = "posts";
    public static final String MAIN_TABLE_NAME = DanbooruGalleryDatabase.MAIN_DATABASE_NAME + ".posts";
    public static final String MEMORY_TABLE_NAME = DanbooruGalleryDatabase.MEMORY_DATABASE_NAME + ".posts";
    public static final String KEY_POST_DATABASE_ID = "_id";
    public static final String KEY_POST_HOST_ID = "host_id";
    public static final String KEY_POST_EXTRA_INFO = "extras";

    public static final String KEY_POST_ID = "post_id";
    public static final String KEY_POST_CREATED_AT = "created_at";
    public static final String KEY_POST_UPDATED_AT = "updated_at";
    public static final String KEY_POST_FILE_SIZE = "file_size";
    public static final String KEY_POST_IMAGE_WIDTH = "image_width";
    public static final String KEY_POST_IMAGE_HEIGHT = "image_height";
    public static final String KEY_POST_FILE_URL = "file_url";
    public static final String KEY_POST_LARGE_FILE_URL = "large_file_url";
    public static final String KEY_POST_PREVIEW_FILE_URL = "preview_file_url";
    public static final String KEY_POST_RATING = "rating";

    public Host host;                       // the host associated with this post
    public int post_id;                     // post id from upstream
    public int image_width, image_height;   // image resolutions
    public Date created_at, updated_at;     // date, used for sorting and filtering
    public int file_size;                   // file size
    public String file_url;                 // urls
    public String file_url_large;           //
    public String file_url_preview;         //
    public String[] tags;                   // tags
    public String rating;                   // rating, can be "s" for Safe, "q" for Questionable, or "e" for Explicit.

    public abstract String getReferer();
    public abstract String getDownloadFilename();
    public abstract String describeContent(Context context);

    public abstract String getExtras();                 // return all extra informations in a string

    protected Post(Host host, int post_id, int image_width, int image_height,
                   Date created_at, Date updated_at,
                   int file_size, String file_url, String file_url_large, String file_url_preview,
                   String[] tags, String rating)
    {
        this.host = host;
        this.post_id = post_id;
        this.image_width = image_width;
        this.image_height = image_height;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.file_size = file_size;
        this.file_url = file_url;
        this.file_url_large = file_url_large;
        this.file_url_preview = file_url_preview;
        this.tags = tags;
        this.rating = rating;

        Arrays.sort(this.tags);
    }

    public static Post fromCursor(Host host, Cursor post_cursor, Cursor tags_cursor)
    {
        return host.getAPI().getPostFromCursor(host, post_cursor, tags_cursor);
    }

    public void putToContentValues(ContentValues values)
    {
        values.put(KEY_POST_HOST_ID, host.id);
        values.put(KEY_POST_ID, post_id);
        values.put(KEY_POST_CREATED_AT, created_at.getTime());
        values.put(KEY_POST_UPDATED_AT, updated_at.getTime());
        values.put(KEY_POST_FILE_SIZE, file_size);
        values.put(KEY_POST_IMAGE_WIDTH, image_width);
        values.put(KEY_POST_IMAGE_HEIGHT, image_height);
        values.put(KEY_POST_FILE_URL, file_url);
        values.put(KEY_POST_LARGE_FILE_URL, file_url_large);
        values.put(KEY_POST_PREVIEW_FILE_URL, file_url_preview);
        values.put(KEY_POST_RATING, rating);
        values.put(KEY_POST_EXTRA_INFO, getExtras());
    }
}
