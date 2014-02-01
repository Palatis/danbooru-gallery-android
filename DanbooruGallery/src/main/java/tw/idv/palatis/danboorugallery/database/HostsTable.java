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

import tw.idv.palatis.danboorugallery.android.database.PriorityDataSetObservable;
import tw.idv.palatis.danboorugallery.model.Host;

public class HostsTable
{
    private static SQLiteStatement sGetHostsCountStatement;
    private static SQLiteStatement sHasHostsStatement;
    private static SQLiteDatabase sDatabase;
    private static PriorityDataSetObservable sDataSetObservable = new PriorityDataSetObservable();

    public static void init(SQLiteDatabase database)
    {
        sDatabase = database;
        sGetHostsCountStatement = sDatabase.compileStatement(
            "SELECT COUNT() FROM " + Host.MAIN_TABLE_NAME + ";"
        );
        sHasHostsStatement = sDatabase.compileStatement(
            "SELECT COUNT() FROM " + Host.MAIN_TABLE_NAME + " LIMIT 1;"
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
        sDataSetObservable.notifyChanged();
    }

    public static final int INDEX_HOST_DATABASE_ID = 0;
    public static final int INDEX_HOST_ENABLED = 1;
    public static final int INDEX_HOST_NAME = 2;
    public static final int INDEX_HOST_URL = 3;
    public static final int INDEX_HOST_API = 4;
    public static final int INDEX_HOST_LOGIN = 5;
    public static final int INDEX_HOST_PASSWORD = 6;
    public static final int INDEX_HOST_PAGE_LIMIT_STRICT = 7;
    public static final int INDEX_HOST_PAGE_LIMIT_RELAXED = 8;
    private static final String[] sHostsColumnNames = new String[] {
        Host.KEY_HOST_DATABASE_ID, Host.KEY_HOST_ENABLED,
        Host.KEY_HOST_NAME, Host.KEY_HOST_URL, Host.KEY_HOST_API,
        Host.KEY_HOST_LOGIN, Host.KEY_HOST_PASSWORD,
        Host.KEY_HOST_PAGE_LIMIT_STRICT, Host.KEY_HOST_PAGE_LIMIT_RELAXED,
    };
    public static Cursor getAllHostsCursor()
    {
        return sDatabase.query(
            Host.MAIN_TABLE_NAME,
            sHostsColumnNames,
            null, null,
            null, null, Host.KEY_HOST_DATABASE_ID, null
        );
    }

    public static Cursor getHostCursorById(int id)
    {
        return sDatabase.query(
            Host.MAIN_TABLE_NAME, sHostsColumnNames,
            Host.KEY_HOST_DATABASE_ID + " == ?", new String[] { Integer.toString(id) },
            null, null, null, "1"
        );
    }

    /**
     * Add or replace the host into the database
     * @param host
     *      if host.id > 0, the host will be replaced.
     *      otherwise a new host is added.
     */
    public static void addOrUpdateHost(Host host)
    {
        if (host.name.trim().isEmpty())
            throw new IllegalArgumentException("Host name cannot be empty.");

        ContentValues values = new ContentValues();
        if (host.id > 0)
            values.put(Host.KEY_HOST_DATABASE_ID, host.id);
        values.put(Host.KEY_HOST_NAME, host.name);
        values.put(Host.KEY_HOST_ENABLED, host.enabled);
        values.put(Host.KEY_HOST_URL, host.url);
        values.put(Host.KEY_HOST_API, host.getAPI().getApiId());
        values.put(Host.KEY_HOST_LOGIN, host.getLogin());
        values.put(Host.KEY_HOST_PASSWORD, host.getPassword());
        values.put(Host.KEY_HOST_PAGE_LIMIT_STRICT, host.pageLimitStrict);
        values.put(Host.KEY_HOST_PAGE_LIMIT_RELAXED, host.pageLimitRelaxed);

        sDatabase.insertWithOnConflict(Host.MAIN_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        sDataSetObservable.notifyChanged();
    }

    public static boolean removeHost(Host host)
    {
        int deleted = sDatabase.delete(
            Host.MAIN_TABLE_NAME,
            Host.KEY_HOST_DATABASE_ID + " == ?",
            new String[] { Integer.toString(host.id) }
        );

        if (deleted != 0)
        {
            if (hasHost())
                sDataSetObservable.notifyChanged();
            else
                sDataSetObservable.notifyInvalidated();
            return true;
        }
        return false;
    }

    /**
     * Test if there are any hosts
     * @return  true if there are more than one hosts, false otherwise.
     */
    public static boolean hasHost()
    {
        return sHasHostsStatement.simpleQueryForLong() != 0;
    }

    /**
     * Get the total number of hosts in the database
     * @return  the total number of hosts
     */
    public static int getHostsCount()
    {
        return (int)sGetHostsCountStatement.simpleQueryForLong();
    }
}
