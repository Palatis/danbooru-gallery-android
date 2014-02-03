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

package tw.idv.palatis.danboorugallery.siteapi;

import android.content.Context;
import android.database.Cursor;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

class DummyAPI
    extends SiteAPI
{
    // API_ID should be persistent, else users with other versions
    // of the app may not find your API.
    public static final int API_ID = 0xffffffff;
    // This one doesn't have to be persistent
    public static final String API_NAME = "Dummy API";

    // Call this function in SiteAPI will register your API.
    public static void init()
    {
        // SiteAPI.registerSiteAPI(new DummyAPI());
    }

    @Override
    public int getApiId()
    {
        return API_ID;
    }

    @Override
    public String getName()
    {
        return API_NAME;
    }

    @Override
    public String hashSecret(String login, String password)
    {
        return "";
    }

    @Override
    public List<Post> fetchPosts(Host host, int startFrom, String[] tags)
    {
        return Collections.emptyList();
    }

    @Override
    public List<Tag> searchTags(Host host, String match_pattern)
    {
        return Collections.emptyList();
    }

    @Override
    public Post getPostFromCursor(Host host, Cursor post_cursor, Cursor tags_cursor)
    {
        return new DummyPost(host);
    }

    private static class DummyPost
        extends Post
    {
        protected DummyPost(Host host)
        {
            super(host, -1, -1, -1, new Date(0), new Date(0), -1, "", "", "", new String[0], "");
        }

        @Override
        public String getReferer()
        {
            return "";
        }

        @Override
        public String getDownloadFilename()
        {
            return "";
        }

        @Override
        public String describeContent(Context context)
        {
            return "";
        }

        @Override
        public String getExtras()
        {
            return "";
        }

        @Override
        public String getWebUrl()
        {
            return "";
        }
    }
}