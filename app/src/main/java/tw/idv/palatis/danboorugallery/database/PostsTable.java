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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tw.idv.palatis.danboorugallery.android.database.PriorityDataSetObservable;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

public class PostsTable
{
    private static final String TAG = "PostsTable";

    private static SQLiteStatement sGetMainPostsCountStatement;
    private static SQLiteDatabase sDatabase;
    private static PriorityDataSetObservable sDataSetObservable = new PriorityDataSetObservable();

    public static void init(SQLiteDatabase database)
    {
        sDatabase = database;
        sGetMainPostsCountStatement = sDatabase.compileStatement(
            "SELECT COUNT() FROM " + Post.MAIN_TABLE_NAME + ";"
        );
    }

    public static void registerDataSetObserver(DataSetObserver observer)
    {
        sDataSetObservable.registerObserver(observer);
    }

    public static void registerDataSetObserver(DataSetObserver observer, int priority)
    {
        sDataSetObservable.registerObserver(observer, priority);
    }

    public static void unregisterDataSetObserver(DataSetObserver observer)
    {
        sDataSetObservable.unregisterObserver(observer);
    }

    public static void backupRestored()
    {
        deleteAllPosts();
        rebuildTempTable(new ArrayList<Host>(), new String[0]);
    }

    // posts related
    public static final int INDEX_POST_DATABASE_ID = 0;
    public static final int INDEX_POST_HOST_ID = 1;
    public static final int INDEX_POST_POST_ID = 2;
    public static final int INDEX_POST_CREATED_AT = 3;
    public static final int INDEX_POST_UPDATED_AT = 4;
    public static final int INDEX_POST_FILE_SIZE = 5;
    public static final int INDEX_POST_IMAGE_WIDTH = 6;
    public static final int INDEX_POST_IMAGE_HEIGHT = 7;
    public static final int INDEX_POST_FILE_URL = 8;
    public static final int INDEX_POST_LARGE_FILE_URL = 9;
    public static final int INDEX_POST_PREVIEW_FILE_URL = 10;
    public static final int INDEX_POST_RATING = 11;
    public static final int INDEX_POST_EXTRA_INFO = 12;
    public static final String[] POST_ALL_COLUMNS = new String[] {
        Post.KEY_POST_DATABASE_ID, Post.KEY_POST_HOST_ID, Post.KEY_POST_ID,
        Post.KEY_POST_CREATED_AT, Post.KEY_POST_UPDATED_AT,
        Post.KEY_POST_FILE_SIZE,
        Post.KEY_POST_IMAGE_WIDTH, Post.KEY_POST_IMAGE_HEIGHT,
        Post.KEY_POST_FILE_URL, Post.KEY_POST_LARGE_FILE_URL, Post.KEY_POST_PREVIEW_FILE_URL,
        Post.KEY_POST_RATING, Post.KEY_POST_EXTRA_INFO,
    };

    public static Cursor getPostCursorById(int post_id)
    {
        return sDatabase.query(
            Post.MAIN_TABLE_NAME,
            POST_ALL_COLUMNS,
            Post.KEY_POST_DATABASE_ID + " == ?", new String[] { Integer.toString(post_id) },
            null, null, null, null
        );
    }

    public static int getPostCount()
    {
        return (int)sGetMainPostsCountStatement.simpleQueryForLong();
    }

    public static Cursor getTempPostsCursor(String[] columns, String selection, String[] selectionArgs, String order_by, String limit)
    {
        return sDatabase.query(
            Post.MEMORY_TABLE_NAME,
            columns,
            selection, selectionArgs,
            null, null, order_by, limit
        );
    }

    /**
     * add or update poses
     * @param host
     *      the host of the posts
     * @param posts
     *      the posts
     * @return
     *      number of updated posts. (updated + new = posts.size())
     */
    public static int addOrUpdatePosts(Host host, List<Post> posts)
    {
        if (posts.size() == 0)
            return 0;

        // sort the posts by id ascending, we rely on this order to associate
        // tags with the post correctly.
        Collections.sort(posts, new Comparator<Post>()
        {
            @Override
            public int compare(Post lhs, Post rhs)
            {
                return lhs.post_id - rhs.post_id;
            }
        });

        // collect post id and tags
        StringBuilder sb = new StringBuilder();
        Set<String> tags = new HashSet<>();
        for (Post post : posts)
        {
            sb.append(post.post_id).append(',');
            Collections.addAll(tags, post.tags);
        }
        sb.deleteCharAt(sb.length() - 1);

        String sql;

        int deleted = -1;
        sDatabase.beginTransactionNonExclusive();
        try
        {
            // delete existing post_tags_link
            sql =   PostTagsLinkTable.KEY_POST_DATABASE_ID + " IN (" +
                "SELECT " + Post.KEY_POST_DATABASE_ID + " FROM " + Post.MAIN_TABLE_NAME + " " +
                "WHERE " +
                    Post.KEY_POST_HOST_ID + " == " + host.id + " AND " +
                    Post.KEY_POST_ID + " IN (" + sb.toString() + ")" +
                ");";
            deleted = sDatabase.delete(PostTagsLinkTable.MAIN_TABLE_NAME, sql, null);

            // insert tags into the database
            ContentValues values = new ContentValues();
            for (String tag : tags)
            {
                values.clear();
                values.put(Tag.KEY_TAG_HASHCODE, tag.hashCode());
                values.put(Tag.KEY_TAG_NAME, tag);
                values.put(Tag.KEY_TAG_SEARCH_COUNT, 0);
                sDatabase.insertWithOnConflict(Tag.MAIN_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }

            // collect existing posts
            sql =   Post.KEY_POST_HOST_ID + " == " + host.id + " AND " +
                Post.KEY_POST_ID + " IN (" + sb.toString() + ")";
            Cursor existing = sDatabase.query(
                Post.MAIN_TABLE_NAME,
                new String[] { Post.KEY_POST_DATABASE_ID, Post.KEY_POST_ID },
                sql, null,
                null, null, null, null
            );

            // map existing post to database id
            deleted = existing.getCount();
            SparseIntArray post_map = new SparseIntArray(existing.getCount());
            while (existing.moveToNext())
                post_map.put(existing.getInt(1), existing.getInt(0));

            // insert posts into the database
            for (Post post : posts)
            {
                values.clear();
                int post_db_id = post_map.get(post.post_id, -1);
                if (post_db_id != -1)
                    values.put(Post.KEY_POST_DATABASE_ID, post_db_id);
                post.putToContentValues(values);
                sDatabase.insertWithOnConflict(Post.MAIN_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            // connect tags with posts
            // reuse this sql from previous DELETE query
            // sql =   Post.KEY_POST_HOST_ID + " == " + sHost.id + " AND " +
            //         Post.KEY_POST_ID + " IN (" + sb.toString() + ")";
            Cursor cursor = sDatabase.query(
                Post.MAIN_TABLE_NAME,
                new String[] { Post.KEY_POST_DATABASE_ID },
                sql, null,
                null, null, Post.KEY_POST_ID, Integer.toString(posts.size()));
            int n = posts.size();
            for (int i = 0;i < n;++i)
            {
                Post post = posts.get(i);
                cursor.moveToPosition(i);
                int post_id = cursor.getInt(0);
                for (int j = post.tags.length - 1;j >= 0;--j)
                {
                    values.clear();
                    values.put(PostTagsLinkTable.KEY_POST_DATABASE_ID, post_id);
                    values.put(PostTagsLinkTable.KEY_TAG_HASHCODE, post.tags[j].hashCode());
                    sDatabase.insert(PostTagsLinkTable.TABLE_NAME, null, values);
                }
            }
            sDatabase.setTransactionSuccessful();
        }
        finally
        {
            sDatabase.endTransaction();
        }

        return deleted;
    }

    // delete
    public static void deleteAllPosts()
    {
        sDatabase.beginTransactionNonExclusive();
        try
        {
            sDatabase.delete(Post.MAIN_TABLE_NAME, null, null);
            sDatabase.delete(Post.MEMORY_TABLE_NAME, null, null);
            sDatabase.delete(PostTagsLinkTable.MAIN_TABLE_NAME, null, null);
            sDatabase.delete(Tag.MAIN_TABLE_NAME, null, null);
            sDatabase.setTransactionSuccessful();
        }
        finally
        {
            sDatabase.endTransaction();
        }
        sDatabase.execSQL("VACUUM;");
        sDataSetObservable.notifyInvalidated();
    }

    public static void clearTempPostTable()
    {
        sDatabase.beginTransactionNonExclusive();
        try
        {
            sDatabase.delete(Post.MEMORY_TABLE_NAME, null, null);
            sDatabase.setTransactionSuccessful();
        }
        finally
        {
            sDatabase.endTransaction();
        }
        sDataSetObservable.notifyInvalidated();
    }

    public static void rebuildTempTable(List<Host> hosts, /* String selection, String[] selectionArgs, */ String[] tags)
    {
        List<String> args = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        builder.append("INSERT OR IGNORE INTO ").append(Post.MEMORY_TABLE_NAME);

        if (tags != null && tags.length != 0)
        {
            builder.append(" SELECT * FROM (");
            for (int i = tags.length - 1;i >= 0;--i)
            {
                builder
                    .append("SELECT * FROM ").append(Post.MAIN_TABLE_NAME)
                    .append(" WHERE ").append(Post.KEY_POST_DATABASE_ID)
                    .append(" IN (SELECT ").append(PostTagsLinkTable.KEY_POST_DATABASE_ID)
                    .append(" FROM ").append(PostTagsLinkTable.TABLE_NAME)
                    .append(" WHERE ").append(PostTagsLinkTable.KEY_TAG_HASHCODE).append(" == ?) INTERSECT ");
                args.add(Integer.toString(tags[i].hashCode()));
            }
            builder.setLength(builder.length() - 11 /* " INTERSECT ".length() */);
            builder.append(")");
        }
        else
            builder.append(" SELECT * FROM ").append(Post.MAIN_TABLE_NAME);
        builder.append(" WHERE ").append(Post.KEY_POST_HOST_ID).append(" IN ( ");
        for (Host host : hosts)
        {
            if (host.enabled)
            {
                builder.append("?,");
                args.add(Integer.toString(host.id));
            }
        }
        builder.setLength(builder.length() - 1);
        builder.append(");");

        sDatabase.beginTransactionNonExclusive();
        try
        {
            sDatabase.delete(Post.MEMORY_TABLE_NAME, null, null);
            sDatabase.execSQL(builder.toString(), args.toArray());
            sDatabase.setTransactionSuccessful();
        }
        finally
        {
            sDatabase.endTransaction();
        }

        sDataSetObservable.notifyChanged();
    }

    private static final String[] sCountColumnNames = new String[] { "COUNT()" };
    public static int getPostPosition(Host host, long post_created_at)
    {
        Cursor cursor = sDatabase.query(
            Post.MEMORY_TABLE_NAME,
            sCountColumnNames,
            Post.KEY_POST_CREATED_AT + " >= ? AND " + Post.KEY_POST_HOST_ID + " == ?",
            new String[] { Long.toString(post_created_at), Integer.toString(host.id) },
            null, null, null, null
        );
        cursor.moveToFirst();
        int pos = cursor.getInt(0);
        cursor.close();
        return pos;
    }
}
