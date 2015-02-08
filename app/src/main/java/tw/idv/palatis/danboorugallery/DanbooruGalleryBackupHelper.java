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

package tw.idv.palatis.danboorugallery;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

import tw.idv.palatis.danboorugallery.database.DanbooruGalleryDatabase;
import tw.idv.palatis.danboorugallery.database.HostsTable;
import tw.idv.palatis.danboorugallery.database.PostTagsLinkTable;
import tw.idv.palatis.danboorugallery.database.PostsTable;

/**
 * Created by 其威 on 2014/2/1.
 */
public class DanbooruGalleryBackupHelper
    extends BackupAgentHelper
{
    private static final String KEY_SHARED_PREFS = "prefs";
    private static final String KEY_DEFAULT_SHARED_PREFS = BuildConfig.APPLICATION_ID + "_preferences";

    private static final String KEY_DATABASE = "database";
    private static final String KEY_DATABASE_FILE = "../databases/" + DanbooruGalleryDatabase.DATABASE_NAME;

    public void onCreate()
    {
        super.onCreate();

        addHelper(KEY_SHARED_PREFS, new SharedPreferencesBackupHelper(this, KEY_DEFAULT_SHARED_PREFS));
        addHelper(KEY_DATABASE, new FileBackupHelper(this, KEY_DATABASE_FILE));
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
        throws IOException
    {
        super.onRestore(data, appVersionCode, newState);

        HostsTable.backupRestored();
        PostsTable.backupRestored();
        PostTagsLinkTable.backupRestored();
    }
}