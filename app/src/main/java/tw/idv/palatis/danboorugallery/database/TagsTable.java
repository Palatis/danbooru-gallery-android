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
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import tw.idv.palatis.danboorugallery.model.Tag;

/**
 * Created by 其威 on 2014/1/28.
 */
public class TagsTable
{
    private static SQLiteDatabase sDatabase;
    private static DataSetObservable sDataSetObservable = new DataSetObservable();

    private static SQLiteStatement sIncreaseTagSearchCountStatement;
    private static SQLiteStatement sGetTagSearchCountStatement;

    public static void init(SQLiteDatabase database)
    {
        sDatabase = database;
        sIncreaseTagSearchCountStatement = sDatabase.compileStatement(
            "UPDATE OR IGNORE " + Tag.MAIN_TABLE_NAME + " " +
            "SET " + Tag.KEY_TAG_SEARCH_COUNT + " = " + Tag.KEY_TAG_SEARCH_COUNT + " + 1 " +
            "WHERE " + Tag.KEY_TAG_HASHCODE + " == ?;"
        );
        sGetTagSearchCountStatement = sDatabase.compileStatement(
            "SELECT " + Tag.KEY_TAG_SEARCH_COUNT + " " +
            "FROM " + Tag.MAIN_TABLE_NAME + " " +
            "WHERE " + Tag.KEY_TAG_HASHCODE + " == ? " +
            "LIMIT 1;"
        );
    }

    public static int increaseTagsSearchCount(String[] tags)
    {
        int n = 0;
        sDatabase.beginTransactionNonExclusive();
        try
        {
            for (String tag : tags)
            {
                if (TextUtils.isEmpty(tag))
                    continue;

                // try insert, ignore on fail (tag already exists)
                ContentValues values = new ContentValues(2);
                values.put(Tag.KEY_TAG_HASHCODE, tag.hashCode());
                values.put(Tag.KEY_TAG_NAME, tag);
                sDatabase.insertWithOnConflict(Tag.MAIN_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);

                // increase the search_count by 1
                sIncreaseTagSearchCountStatement.clearBindings();
                sIncreaseTagSearchCountStatement.bindLong(1, tag.hashCode());
                n += sIncreaseTagSearchCountStatement.executeUpdateDelete();
            }
            sDatabase.setTransactionSuccessful();
        }
        finally
        {
            sDatabase.endTransaction();
        }
        return n;
    }

    public static int getTagSearchCount(Tag tag)
    {
        try
        {
            sGetTagSearchCountStatement.clearBindings();
            sGetTagSearchCountStatement.bindLong(1, tag.name.hashCode());
            return (int) sGetTagSearchCountStatement.simpleQueryForLong();
        }
        catch (SQLiteDoneException ignored) { }

        return 0; // return 0 when tag doesn't exists in the database.
    }

    public static void registerDataSetObserver(DataSetObserver observer)
    {
        sDataSetObservable.registerObserver(observer);
    }

    public static void unregisterDataSetObserver(DataSetObserver observer)
    {
        sDataSetObservable.unregisterObserver(observer);
    }

    public static void deleteAllTags()
    {
        sDatabase.delete(Tag.MAIN_TABLE_NAME, null, null);
    }
}
