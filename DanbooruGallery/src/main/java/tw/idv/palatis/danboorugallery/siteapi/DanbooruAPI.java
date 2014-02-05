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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tw.idv.palatis.danboorugallery.DanbooruGallerySettings;
import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.database.PostTagsView;
import tw.idv.palatis.danboorugallery.database.PostsTable;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

public class DanbooruAPI
    extends SiteAPI
{
    private static final String TAG = "DanbooruAPI";

    // this is used to format the created attribute in XML
    // it is here because Android frees Locale.ENGLISH when formatter destroyed,
    // resulting reloading of locale data every time which is SLOW.
    private static final DateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.ENGLISH);

    public static void init()
    {
        SiteAPI.registerSiteAPI(new DanbooruAPI());
    }

    private DanbooruAPI() { }

    public static final int API_ID = 0xdab00002;
    public static final String API_NAME = "Danbooru (JSON)";

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
    private static final String URL_POSTS_FORMAT = "%1$s/posts.json?page=%2$d&tags=%3$s&limit=%4$d";
    // 1: url, 2: tag
    private static final String URL_TAGS_FORMAT = "%1$s/tags.json?search[name_matches]=%2$s&search[order]=count&search[hide_empty]=yes";
    // 1: url, 2: post_id
    private static final String URL_POST_WEB = "%1$s/posts/%2$s";

    private static final int _BUFFER_SIZE = 8192;

    @Override
    public List<Tag> searchTags(Host host, String pattern)
        throws SiteAPIException
    {
        if (pattern.length() == 0)
            pattern = "*";
        else if (!pattern.contains("*"))
            pattern = "*" + pattern + "*";

        HttpURLConnection connection = null;
        try
        {
            String url = String.format(URL_TAGS_FORMAT, host.url, URLEncoder.encode(pattern, "UTF-8"));
            Log.v(TAG, String.format("URL: %s", url));
            connection = SiteAPI.openConnection(new URL(url));
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
        catch (IOException | JSONException ex)
        {
            throw new SiteAPIException(this, connection, ex);
        }
        finally
        {
            if (connection != null)
                connection.disconnect();
        }
    }

    public Tag parseJSONObjectToTag(JSONObject json)
        throws JSONException
    {
        return new Tag(
            json.getInt(DanbooruTag.KEY_TAG_ID),
            json.getString(DanbooruTag.KEY_TAG_NAME),
            json.getInt(DanbooruTag.KEY_TAG_POST_COUNT)
        );
    }

    @Override
    public List<Post> fetchPosts(Host host, int startFrom, String[] tags)
        throws SiteAPIException
    {
        HttpURLConnection connection = null;
        try
        {
            int limit = host.getPageLimit(DanbooruGallerySettings.getBandwidthUsageType());
            int page = startFrom / limit + 1;

            String url = String.format(URL_POSTS_FORMAT, host.url, page, URLEncoder.encode(TextUtils.join(" ", tags), "UTF-8"), limit);
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
        catch (JSONException | ParseException | IOException ex)
        {
            throw new SiteAPIException(this, connection, ex);
        }
        finally
        {
            if (connection != null)
                connection.disconnect();
        }
    }

    public static Post parseJSONObjectToPost(Host host, JSONObject json)
        throws JSONException, ParseException
    {
        if (json == null)
            return null;

        String file_url = json.getString(DanbooruPost.KEY_POST_FILE_URL);
        String file_url_large = json.getString(DanbooruPost.KEY_POST_LARGE_FILE_URL);
        String file_url_preview = json.getString(DanbooruPost.KEY_POST_PREVIEW_FILE_URL);
        if (!file_url.startsWith("http"))
            file_url = host.url + file_url;
        if (!file_url_large.startsWith("http"))
            file_url_large = host.url + file_url_large;
        if (!file_url_preview.startsWith("http"))
            file_url_preview = host.url + file_url_preview;

        int uploader_id = -1;
        String uploader_name = "";
        try { uploader_id = json.getInt(DanbooruPost.KEY_POST_UPLOADER_ID); } catch (JSONException ignored) { }
        try { uploader_name = json.getString(DanbooruPost.KEY_POST_UPLOADER_NAME); } catch (JSONException ignored) { }

        return new DanbooruPost(
            host,
            json.getInt(DanbooruPost.KEY_POST_ID),
            json.getInt(DanbooruPost.KEY_POST_IMAGE_WIDTH),
            json.getInt(DanbooruPost.KEY_POST_IMAGE_HEIGHT),
            sDateFormat.parse(json.getString(DanbooruPost.KEY_POST_CREATED_AT)),
            sDateFormat.parse(json.getString(DanbooruPost.KEY_POST_UPDATED_AT)),
            json.getInt(DanbooruPost.KEY_POST_FILE_SIZE),
            file_url,
            file_url_large,
            file_url_preview,
            TextUtils.split(json.getString(DanbooruPost.KEY_POST_TAG_STRING), " "),
            json.getString(DanbooruPost.KEY_POST_RATING),
            uploader_id,
            uploader_name,
            json.getString(DanbooruPost.KEY_POST_MD5),
            json.getString(DanbooruPost.KEY_POST_FILE_EXT),
            json.getInt(DanbooruPost.KEY_POST_SCORE),
            Math.abs(json.getInt(DanbooruPost.KEY_POST_UP_SCORE)),
            Math.abs(json.getInt(DanbooruPost.KEY_POST_DOWN_SCORE))
        );
    }

    @Override
    public Post getPostFromCursor(Host host, Cursor post_cursor, Cursor tags_cursor)
    {
        String[] tags;
        if (tags_cursor != null)
        {
            tags_cursor.moveToPosition(-1);
            tags = new String[tags_cursor.getCount()];
            while (tags_cursor.moveToNext())
                tags[tags_cursor.getPosition()] = tags_cursor.getString(PostTagsView.INDEX_KEY_POST_TAG_TAG_NAME);
        }
        else
            tags = new String[0];
        return new DanbooruPost(
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

    private static class DanbooruTag extends Tag
    {
        public DanbooruTag(int id, String name, int post_count)
        {
            super(id, name, post_count);
        }
    }

    private static class DanbooruPost
        extends Post
    {
        public static final String KEY_POST_ID = "id";                                      // "id":1595369,
        public static final String KEY_POST_CREATED_AT = "created_at";                      // "created":"2014-01-17T18:35:43-05:00",
        public static final String KEY_POST_UPDATED_AT = "updated_at";                      // "updated":"2014-01-17T18:35:43-05:00",
        public static final String KEY_POST_FILE_SIZE = "file_size";                        // "file_size":172776,
        public static final String KEY_POST_IMAGE_WIDTH = "image_width";                    // "image_width":640,
        public static final String KEY_POST_IMAGE_HEIGHT = "image_height";                  // "image_height":800,
        public static final String KEY_POST_FILE_URL = "file_url";                          // "file_url":"/data/80f21ab51c9f9c8779491aaa13eba6c8.jpg",
        public static final String KEY_POST_LARGE_FILE_URL = "large_file_url";              // "large_file_url":"/data/sample/sample-80f21ab51c9f9c8779491aaa13eba6c8.jpg",
        public static final String KEY_POST_PREVIEW_FILE_URL = "preview_file_url";          // "preview_file_url":"/ssd/data/preview/80f21ab51c9f9c8779491aaa13eba6c8.jpg",
        public static final String KEY_POST_TAG_STRING = "tag_string";                      // "tag_string":"ark_performance wooser's_hand-to-mouth_life tagme"
        public static final String KEY_POST_RATING = "rating";                              // "rating":"s",

        public static final String KEY_POST_MD5 = "md5";                                        // "md5":"80f21ab51c9f9c8779491aaa13eba6c8",
        public static final String KEY_POST_FILE_EXT = "file_ext";                              // "file_ext":"jpg",
        public static final String KEY_POST_UPLOADER_ID = "uploader_id";                        // "uploader_id":99262,
        public static final String KEY_POST_UPLOADER_NAME = "uploader_name";                    // "uploader_name":"DrLove",
        public static final String KEY_POST_SCORE = "score";                                    // "score":1,
        public static final String KEY_POST_UP_SCORE = "up_score";                              // "up_score":0,
        public static final String KEY_POST_DOWN_SCORE = "down_score";                          // "down_score":0,

        public int uploader_id;
        public String uploader_name;
        public String md5;
        public String file_ext;
        public int score, score_up, score_down;

        protected DanbooruPost(// superclass
                               Host host, int post_id, int image_width, int image_height,
                               Date created_at, Date updated_at,
                               int file_size, String file_url, String file_url_large, String file_url_preview,
                               String[] tags, String rating,
                               // this class
                               int uploader_id, String uploader_name, String md5, String file_ext,
                               int score, int score_up, int score_down)
        {
            super(host, post_id, image_width, image_height, created_at, updated_at,
                file_size, file_url, file_url_large, file_url_preview,
                tags, rating);

            this.uploader_id = uploader_id;
            this.uploader_name = uploader_name;
            this.md5 = md5;
            this.file_ext = file_ext;
            this.score = score;
            this.score_up = score_up;
            this.score_down = score_down;
        }

        protected DanbooruPost(// superclass
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
                try { score_up = json.getInt(KEY_POST_UP_SCORE); } catch (JSONException ignored) { }
                try { score_down = json.getInt(KEY_POST_DOWN_SCORE); } catch (JSONException ignored) { }
            }
            catch (JSONException ignored) { }
        }

        @Override
        public String getReferer()
        {
            return host.url + "/posts/" + post_id;
        }

        @Override
        public String getDownloadFilename()
        {
            Log.d(TAG, String.format("%1$d - %2$s.%3$s", post_id, TextUtils.join(" ", tags), file_ext));
            return String.format("%1$d - %2$s.%3$s", post_id, TextUtils.join(" ", tags), file_ext);
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
            try { json.put(KEY_POST_UP_SCORE, score_up); } catch (JSONException ignored) { }
            try { json.put(KEY_POST_DOWN_SCORE, score_down); } catch (JSONException ignored) { }
            return json.toString();
        }

        @Override
        public String getWebUrl()
        {
            return String.format(URL_POST_WEB, host.url, post_id);
        }

        @Override
        public String describeContent(Context context)
        {
            return context.getResources().getString(R.string.api_danbooru_post_description,
                host.name, host.url, host.getAPI().getName(),
                post_id, image_width, image_height, created_at.toString(), updated_at.toString(),
                rating, uploader_id, uploader_name, md5, file_ext, score, score_up, score_down);
        }
    }
}