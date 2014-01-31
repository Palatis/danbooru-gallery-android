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

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;

import tw.idv.palatis.danboorugallery.model.Tag;

/**
 * Created by 其威 on 2014/1/28.
 */
public class TagsTable
{
    private static SQLiteDatabase sDatabase;
    private static DataSetObservable sDataSetObservable = new DataSetObservable();

    public static void init(SQLiteDatabase database)
    {
        sDatabase = database;
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
