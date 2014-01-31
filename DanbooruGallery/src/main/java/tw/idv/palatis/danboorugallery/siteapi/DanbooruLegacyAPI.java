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
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import tw.idv.palatis.danboorugallery.DanbooruGallerySettings;
import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.database.PostTagsView;
import tw.idv.palatis.danboorugallery.database.PostsTable;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

/**
 * Created by 其威 on 2014/1/25.
 */
public class DanbooruLegacyAPI
    extends SiteAPI
{
    private static final String TAG = "DanbooruLegacyAPI";

    public static final int API_ID = 0xdab00001;
    public static final String API_NAME = "Danbooru Legacy (JSON)";

    public static void init()
    {
        SiteAPI.registerSiteAPI(new DanbooruLegacyAPI());
    }

    private DanbooruLegacyAPI()
    {
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
        return Base64.encodeToString((login + ":" + password).getBytes(), Base64.DEFAULT);
    }

    // 1: url, 2: page #, 3: tags, 4: limit
    private static final String URL_POSTS_FORMAT = "%1$s/post/index.json?page=%2$d&tags=%3$s&limit=%4$d";
    // 1: url, 2: match_pattern
    private static final String URL_TAGS_FORMAT = "%1$s/tag/index.json?name=%2$s&order=count";
    private static final int _BUFFER_SIZE = 8192;

    @Override
    public List<Post> fetchPosts(Host host, int startFrom, String tags)
        throws SiteAPIException
    {
        HttpURLConnection connection = null;
        try
        {
            int limit = host.getPageLimit(DanbooruGallerySettings.getBandwidthUsageType());
            int page = startFrom / limit + 1;

            String url = String.format(URL_POSTS_FORMAT, host.url, page, URLEncoder.encode(tags, "UTF-8"), limit);
            Log.v(TAG, String.format("URL: %s", url));
            connection = SiteAPI.openConnection(new URL(url));
            if (!host.getLogin().isEmpty())
                connection.setRequestProperty("Authorization", "Basic " + host.getSecret());
            Reader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            Writer output = new StringWriter();

            char buffer[] = new char[_BUFFER_SIZE];
            for (int count = input.read(buffer);count > 0;count = input.read(buffer))
                output.write(buffer, 0, count);

            JSONArray json_posts = new JSONArray(output.toString());
            int len = json_posts.length();
            List<Post> posts = new ArrayList<>(len);
            for (int j = 0;j < len; ++j)
                posts.add(parseJSONObjectToPost(host, json_posts.getJSONObject(j)));

            return posts;
        }
        catch (IOException ex)
        {
            throw new SiteAPIException(this, connection, ex);
        }
        catch (JSONException | ParseException ex)
        {
            throw new SiteAPIException(ex);
        }
        finally
        {
            if (connection != null)
                connection.disconnect();
        }
    }

    @Override
    public List<Tag> searchTags(Host host, String pattern)
    {
        if (pattern.length() == 0)
            pattern = "*";
        else if (!pattern.contains("*"))
            pattern = "*" + pattern + "*";

        try
        {
            String url = String.format(URL_TAGS_FORMAT, host.url, URLEncoder.encode(pattern, "UTF-8"));
            Log.v(TAG, String.format("URL: %s", url));
            HttpURLConnection connection = SiteAPI.openConnection(new URL(url));
            if (!host.getLogin().isEmpty())
                connection.setRequestProperty("Authorization", "Basic " + host.getSecret());
            Reader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            Writer output = new StringWriter();

            char buffer[] = new char[_BUFFER_SIZE];
            for (int count = input.read(buffer);count > 0;count = input.read(buffer))
                output.write(buffer, 0, count);

            JSONArray json_tags = new JSONArray(output.toString());
            int len = json_tags.length();
            List<Tag> tags = new ArrayList<>(len);
            for (int i = 0;i < len;++i)
                tags.add(parseJSONObjectToTag(json_tags.getJSONObject(i)));

            return tags;
        }
        catch (UnknownHostException ex)
        {
            Log.v(TAG, "Connection problem?", ex);
        }
        catch (IOException | JSONException ex)
        {
            ex.printStackTrace();
        }

        return Collections.emptyList();
    }

    public Tag parseJSONObjectToTag(JSONObject json) throws JSONException
    {
        return new DanbooruLegacyTag(
            json.getInt(DanbooruLegacyTag.KEY_TAG_ID),
            json.getString(DanbooruLegacyTag.KEY_TAG_NAME),
            json.getInt(DanbooruLegacyTag.KEY_TAG_POST_COUNT)
        );
    }

    @Override
    public Post getPostFromCursor(Host host, Cursor post_cursor, Cursor tags_cursor)
    {
        String[] tags;
        if (tags_cursor != null)
        {
            tags = new String[tags_cursor.getCount()];
            while (tags_cursor.moveToNext())
                tags[tags_cursor.getPosition()] = tags_cursor.getString(PostTagsView.INDEX_KEY_POST_TAG_TAG_NAME);
        }
        else
            tags = new String[0];
        return new DanbooruLegacyPost(
            host,
            post_cursor.getInt(PostsTable.INDEX_POST_POST_ID),
            post_cursor.getInt(PostsTable.INDEX_POST_IMAGE_WIDTH),
            post_cursor.getInt(PostsTable.INDEX_POST_IMAGE_HEIGHT),
            new Date(post_cursor.getLong(PostsTable.INDEX_POST_CREATED_AT)),
            new Date(post_cursor.getLong(PostsTable.INDEX_POST_UPDATED_AT)),
            post_cursor.getInt(PostsTable.INDEX_POST_FILE_SIZE),
            post_cursor.getString(PostsTable.INDEX_POST_FILE_URL),
            post_cursor.getString(PostsTable.INDEX_POST_LARGE_FILE_URL),
            post_cursor.getString(PostsTable.INDEX_POST_PREVIEW_FILE_URL),
            tags,
            post_cursor.getString(PostsTable.INDEX_POST_RATING),
            post_cursor.getString(PostsTable.INDEX_POST_EXTRA_INFO)
        );
    }

    public static Post parseJSONObjectToPost(Host host, JSONObject json)
        throws JSONException, ParseException
    {
        if (json == null)
            return null;

        String file_url = json.getString(DanbooruLegacyPost.KEY_POST_FILE_URL);
        String file_url_large = json.getString(DanbooruLegacyPost.KEY_POST_LARGE_FILE_URL);
        String file_url_preview = json.getString(DanbooruLegacyPost.KEY_POST_PREVIEW_FILE_URL);
        if (!file_url.startsWith("http"))
            file_url = host.url + file_url;
        if (!file_url_large.startsWith("http"))
            file_url_large = host.url + file_url_large;
        if (!file_url_preview.startsWith("http"))
            file_url_preview = host.url + file_url_preview;

        JSONObject json_date = json.getJSONObject(DanbooruLegacyPost.KEY_POST_CREATED_AT);
        Date date = new Date(json_date.getLong("s") * 1000 + json_date.getLong("n") / 1000000);
        return new DanbooruLegacyPost(
            host,
            json.getInt(DanbooruLegacyPost.KEY_POST_ID),
            json.getInt(DanbooruLegacyPost.KEY_POST_IMAGE_WIDTH),
            json.getInt(DanbooruLegacyPost.KEY_POST_IMAGE_HEIGHT),
            date,
            date,
            json.getInt(DanbooruLegacyPost.KEY_POST_FILE_SIZE),
            file_url,
            file_url_large,
            file_url_preview,
            TextUtils.split(json.getString(DanbooruLegacyPost.KEY_POST_TAG_STRING), " "),
            json.getString(DanbooruLegacyPost.KEY_POST_RATING),
            json.getInt(DanbooruLegacyPost.KEY_POST_UPLOADER_ID),
            json.getString(DanbooruLegacyPost.KEY_POST_UPLOADER_NAME),
            json.getString(DanbooruLegacyPost.KEY_POST_MD5),
            json.getInt(DanbooruLegacyPost.KEY_POST_SCORE)
        );
    }

    private static class DanbooruLegacyTag extends Tag
    {
        public static final String KEY_TAG_ID = "id";                   // "id":11582,
        public static final String KEY_TAG_NAME = "name";               // "name":";]",
        public static final String KEY_TAG_POST_COUNT = "count";        // "count":1,

        public DanbooruLegacyTag(int id, String name, int post_count)
        {
            super(id, name, post_count);
        }
    }

    private static class DanbooruLegacyPost extends Post
    {
        public static final String KEY_POST_ID = "id";                                      // "id":498574,
        public static final String KEY_POST_CREATED_AT = "created_at";                      // "created_at":{"json_class":"Time","n":197131000,"s":1390583690}},
        public static final String KEY_POST_FILE_SIZE = "file_size";                        // "file_size":315909,
        public static final String KEY_POST_IMAGE_WIDTH = "width";                          // "width":800,
        public static final String KEY_POST_IMAGE_HEIGHT = "height";                        // "height":1199,
        public static final String KEY_POST_FILE_URL = "file_url";                          // "file_url":"http://behoimi.org/data/f2/a7/f2a75a684a008091125523cf632cb00e.jpg",
        public static final String KEY_POST_LARGE_FILE_URL = "sample_url";                  // "sample_url":"http://behoimi.org/data/f2/a7/f2a75a684a008091125523cf632cb00e.jpg",
        public static final String KEY_POST_PREVIEW_FILE_URL = "preview_url";               // "preview_url":"http://behoimi.org/data/preview/f2/a7/f2a75a684a008091125523cf632cb00e.jpg",
        public static final String KEY_POST_TAG_STRING = "tags";                            // "tags":"bikini blonde_hair blouse collar cosplay hanna_of_mysteria nakko open_clothes shingeki_no_bahamut straw_hat swimsuit twintails",
        public static final String KEY_POST_RATING = "rating";                              // "rating":"s",

        public static final String KEY_POST_MD5 = "md5";                                        // "md5":"f2a75a684a008091125523cf632cb00e",
        public static final String KEY_POST_UPLOADER_ID = "creator_id";                         // "creator_id":1,
        public static final String KEY_POST_UPLOADER_NAME = "author";                           // "author":"nil!",
        public static final String KEY_POST_SCORE = "score";                                    // "score":2,

        public static final String KEY_POST_FILE_EXT = "file_ext";

        public int uploader_id;
        public String uploader_name;
        public String md5;
        public String file_ext;
        public int score;

        protected DanbooruLegacyPost(// superclass
                                     Host host, int post_id, int image_width, int image_height,
                                     Date created_at, Date updated_at,
                                     int file_size, String file_url, String file_url_large, String file_url_preview,
                                     String[] tags, String rating,
                                     // this class
                                     int uploader_id, String uploader_name, String md5, int score)
        {
            super(host, post_id, image_width, image_height, created_at, updated_at,
                file_size, file_url, file_url_large, file_url_preview,
                tags, rating);

            this.uploader_id = uploader_id;
            this.uploader_name = uploader_name;
            this.md5 = md5;
            this.score = score;

            this.file_ext = file_url.substring(file_url.lastIndexOf(".") + 1, file_url.length());
        }

        protected DanbooruLegacyPost(// superclass
                                     Host host, int post_id, int image_width, int image_height,
                                     Date created_at, Date updated_at,
                                     int file_size, String file_url, String file_url_large, String file_url_preview,
                                     String[] tags, String rating,
                                     // this class
                                     String extras)
        {
            super(host, post_id, image_width, image_height, created_at, updated_at,
                file_size, file_url, file_url_large, file_url_preview,
                tags, rating);

            try
            {
                JSONObject json = new JSONObject(extras);
                try { md5 = json.getString(KEY_POST_MD5); } catch (JSONException ignored) { }
                try { file_ext = json.getString(KEY_POST_FILE_EXT); } catch (JSONException ignored) { }
                try { uploader_id = json.getInt(KEY_POST_UPLOADER_ID); } catch (JSONException ignored) { }
                try { uploader_name = json.getString(KEY_POST_UPLOADER_NAME); } catch (JSONException ignored) { }
                try { score = json.getInt(KEY_POST_SCORE); } catch (JSONException ignored) { }
            }
            catch (JSONException ignored) { }
        }

        @Override
        public String getReferer()
        {
            return host.url + "/post/show/" + post_id + "/";
        }

        @Override
        public String getDownloadFilename()
        {
            Log.d(TAG, String.format("%1$d - %2$s.%3$s", post_id, TextUtils.join(" ", tags), file_ext));
            return String.format("%1$d - %2$s.%3$s", post_id, TextUtils.join(" ", tags), file_ext);
        }

        @Override
        public String describeContent(Context context)
        {
            return context.getResources().getString(R.string.api_danboorulegacy_post_description,
                host.name, host.url, host.getAPI().getName(),
                post_id, image_width, image_height, created_at.toString(), updated_at.toString(),
                rating, uploader_id, md5, file_ext, score);
        }

        @Override
        public String getExtras()
        {
            JSONObject json = new JSONObject();
            try { json.put(KEY_POST_MD5, md5); } catch (JSONException ignored) { }
            try { json.put(KEY_POST_FILE_EXT, file_ext); } catch (JSONException ignored) { }
            try { json.put(KEY_POST_UPLOADER_ID, uploader_id); } catch (JSONException ignored) { }
            try { json.put(KEY_POST_UPLOADER_NAME, uploader_name); } catch (JSONException ignored) { }
            try { json.put(KEY_POST_SCORE, score); } catch (JSONException ignored) { }
            return json.toString();
        }
    }
}
