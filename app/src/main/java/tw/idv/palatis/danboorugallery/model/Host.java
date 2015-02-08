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

import android.database.Cursor;

import tw.idv.palatis.danboorugallery.database.DanbooruGalleryDatabase;
import tw.idv.palatis.danboorugallery.siteapi2.SiteAPI;

public class Host {
	private static final String TAG = "Host";

	public static final String TABLE_NAME = "hosts";
	public static final String MAIN_TABLE_NAME = DanbooruGalleryDatabase.MAIN_DATABASE_NAME + ".hosts";
	public static final String MEMORY_TABLE_NAME = DanbooruGalleryDatabase.MEMORY_DATABASE_NAME + ".hosts";
	public static final String KEY_HOST_DATABASE_ID = "_id";
	public static final String KEY_HOST_ENABLED = "host_enabled";
	public static final String KEY_HOST_NAME = "host_name";
	public static final String KEY_HOST_URL = "host_url";
	public static final String KEY_HOST_LOGIN = "host_login";
	public static final String KEY_HOST_PASSWORD = "host_password";
	public static final String KEY_HOST_API = "host_api";
	public static final String KEY_HOST_PAGE_LIMIT_STRICT = "page_limit_strict";
	public static final String KEY_HOST_PAGE_LIMIT_RELAXED = "page_limit_relaxed";

	public static final int INVALID_ID = -1;

	public int id;
	public String name;
	public String url;
	public int pageLimitStrict;
	public int pageLimitRelaxed;
	public boolean enabled;
	private String mLogin;
	private String mPassword;
	private SiteAPI mAPI;

	public String getLogin() {
		return mLogin;
	}

	public void setLogin(String login) {
		mLogin = login;
	}

	public String getPassword() {
		return mPassword;
	}

	public void setPassword(String password) {
		mPassword = password;
	}

	public SiteAPI getAPI() {
		return mAPI;
	}

	public void setAPI(SiteAPI api) {
		mAPI = api;
	}

	public void setAPI(int apiId) {
		setAPI(SiteAPI.findAPIById(apiId));
	}

	public String getDownloadFolder() {
		return name + ((mLogin != null) ? " - " + mLogin : "");
	}

	public int getPageLimit(int type) {
		if (type == SiteAPI.PAGE_LIMIT_TYPE_RELAXED)
			return pageLimitRelaxed;
		return pageLimitStrict;
	}

	public Host(int id, boolean enabled, String name, String url, String login, String password, int apiId, int pageLimitStrict, int pageLimitRelaxed) {
		this(id, enabled, name, url, login, password, SiteAPI.findAPIById(apiId), pageLimitStrict, pageLimitRelaxed);
	}

	public Host(int id, boolean enabled, String name, String url, String login, String password, SiteAPI api, int pageLimitStrict, int pageLimitRelaxed) {
		this.id = id;
		this.enabled = enabled;
		this.name = name;
		this.url = url;
		this.pageLimitRelaxed = pageLimitRelaxed;
		this.pageLimitStrict = pageLimitStrict;
		mLogin = login;
		mPassword = password;
		mAPI = api;
	}

	public static Host getFromCursor(Cursor cursor) {
		return new Host(
				cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOST_DATABASE_ID)),
				cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOST_ENABLED)) != 0,
				cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOST_NAME)),
				cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOST_URL)),
				cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOST_LOGIN)),
				cursor.getString(cursor.getColumnIndexOrThrow(KEY_HOST_PASSWORD)),
				cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOST_API)),
				cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOST_PAGE_LIMIT_STRICT)),
				cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HOST_PAGE_LIMIT_RELAXED))
		);
	}

	@Override
	public String toString() {
		return String.format("%s: %d, %s (%s), %s, %d, %d", getClass().getName(), id, name, url, mAPI.toString(), pageLimitStrict, pageLimitRelaxed);
	}
}
