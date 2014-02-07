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
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import tw.idv.palatis.danboorugallery.DanbooruGallerySettings;
import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.database.PostTagsView;
import tw.idv.palatis.danboorugallery.database.PostsTable;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;
import tw.idv.palatis.danboorugallery.util.ParseUtils;

public class ShimmieAPI
    extends SiteAPI
{
    private static final String TAG = "ShimmieAPI";

    public static final int API_ID = 0x561331e2;
    public static final String API_NAME = "Shimmie 2 (Danbooru XML)";

    // this is used to format the created attribute in XML
    // it is here because Android frees Locale.ENGLISH when formatter destroyed,
    // resulting reloading of locale data every time which is SLOW.
    private static final DateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

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

    public static void init()
    {
        SiteAPI.registerSiteAPI(new ShimmieAPI());
    }

    private ShimmieAPI() { }

    @Override
    public String hashSecret(String login, String password)
    {
        return "";
    }

    // 1: url, 2: page #, 3: tags, 4: limit
    private static final String URL_POSTS_FORMAT = "%1$s/api/danbooru/post/index.xml?page=%2$d&tags=%3$s&limit=%4$d";
    // 1: url, 2: match_pattern
//    private static final String URL_TAGS_FORMAT = "%1$s/api/danbooru/find_tags?tags=%2$s";
    // 1: url, 2: post_id
    private static final String URL_POST_WEB = "%1$s/post/view/%2$s";

    @Override
    public List<Post> fetchPosts(Host host, int startFrom, String[] tags) throws SiteAPIException
    {
        HttpURLConnection connection = null;
        try
        {
            int limit = host.getPageLimit(DanbooruGallerySettings.getBandwidthUsageType());
            int page = startFrom / limit + 1;

            String strtags = URLEncoder.encode(TextUtils.join(" ", tags), "UTF-8");
            if (TextUtils.isEmpty(strtags))
                strtags = "*";

            String url = String.format(URL_POSTS_FORMAT, host.url, page, strtags, limit);
            Log.v(TAG, String.format("URL: %s", url));
            connection = SiteAPI.openConnection(new URL(url));
            if (!host.getLogin().isEmpty())
                connection.setRequestProperty("Authorization", "Basic " + host.getSecret());

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(connection.getInputStream()));
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName( "post" );

            int length = nodes.getLength();
            List<Post> posts = new ArrayList<>(length);
            for (int j = 0; j < length; ++j)
                posts.add(parseXMLElementToPost(host, (Element)nodes.item(j)));

            return posts;
        }
        catch (ParserConfigurationException | SAXException | IOException ex)
        {
            throw new SiteAPIException(this, connection, ex);
        }
        finally
        {
            if (connection != null)
                connection.disconnect();
        }
    }

    @Override
    public List<Tag> searchTags(Host host, String match_pattern) throws SiteAPIException
    {
        List<Tag> fakeTags = new ArrayList<>(1);
        fakeTags.add(new ShimmieTag(-1, "Tag search for Shimmie isn't working.", -1));
        return fakeTags;
    }

    private Post parseXMLElementToPost(Host host, Element item)
    {
        if (item == null)
            return null;

        String file_url = item.getAttribute(ShimmiePost.KEY_POST_FILE_URL);
        String file_url_preview = item.getAttribute(ShimmiePost.KEY_POST_PREVIEW_FILE_URL);
        if (!file_url.startsWith("http"))
            file_url = host.url + file_url;
        if (!file_url_preview.startsWith("http"))
            file_url_preview = host.url + file_url_preview;

        Date date;
        try
        {
            date = sDateFormat.parse(item.getAttribute(ShimmiePost.KEY_POST_CREATED_AT));
        }
        catch (ParseException e)
        {
            date = new Date(0);
        }

        return new ShimmiePost(
            host,
            ParseUtils.parseInt(item.getAttribute(ShimmiePost.KEY_POST_ID)),
            ParseUtils.parseInt(item.getAttribute(ShimmiePost.KEY_POST_IMAGE_WIDTH), -1),
            ParseUtils.parseInt(item.getAttribute(ShimmiePost.KEY_POST_IMAGE_HEIGHT), -1),
            date,
            date,
            -1, // no file size
            file_url,
            file_url,
            file_url_preview,
            TextUtils.split(item.getAttribute(ShimmiePost.KEY_POST_TAG_STRING).trim(), " "),
            "e", // item.getAttribute(ShimmiePost.KEY_POST_RATING), // rating always "u"... we don't recognize that.
            item.getAttribute(ShimmiePost.KEY_POST_MD5),
            item.getAttribute(ShimmiePost.KEY_POST_UPLOADER_NAME),
            ParseUtils.parseInt(item.getAttribute(ShimmiePost.KEY_POST_SCORE))
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
        return new ShimmiePost(
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

    private static class ShimmieTag extends Tag
    {
        public ShimmieTag(int id, String name, int post_count)
        {
            super(id, name, post_count);
        }
    }

    private static class ShimmiePost
        extends Post
    {
        public static final String KEY_POST_ID = "id";                          // id="3177"
        public static final String KEY_POST_CREATED_AT = "date";                // date="2014-01-25 06:42:34"
        public static final String KEY_POST_IMAGE_WIDTH = "width";              // width="600"
        public static final String KEY_POST_IMAGE_HEIGHT = "height";            // height="600"
        public static final String KEY_POST_FILE_URL = "file_url";              // file_url="/v2/_images/f8b624959cb6f25ec08c67491448c281/3177%20-%20bunny%20cute%20ilolled.jpg"
        public static final String KEY_POST_PREVIEW_FILE_URL = "preview_url";   // preview_url="/v2/_thumbs/f8b624959cb6f25ec08c67491448c281/thumb.jpg"
        public static final String KEY_POST_TAG_STRING = "tags";                // tags="bunny cute ilolled"

        public static final String KEY_POST_MD5 = "md5";                        // md5="f8b624959cb6f25ec08c67491448c281"
        public static final String KEY_POST_UPLOADER_NAME = "author";           // author="Shish"
        public static final String KEY_POST_SCORE = "score";                    // score="0"

        public static final String KEY_POST_FILE_EXT = "file_ext";

        public String uploader_name;
        public String md5;
        public String file_ext;
        public int score;

        protected ShimmiePost(// superclass
                              Host host, int post_id, int image_width, int image_height,
                              Date created_at, Date updated_at,
                              int file_size, String file_url, String file_url_large, String file_url_preview,
                              String[] tags, String rating,
                              // this class
                              String md5, String uploader_name, int score)
        {
            super(host, post_id, image_width, image_height, created_at, updated_at,
                file_size, file_url, file_url_large, file_url_preview,
                tags, rating);

            this.uploader_name = uploader_name;
            this.md5 = md5;
            this.score = score;

            this.file_ext = file_url.substring(file_url.lastIndexOf(".") + 1, file_url.length());
        }

        protected ShimmiePost(// superclass
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
                try { uploader_name = json.getString(KEY_POST_UPLOADER_NAME); } catch (JSONException ignored) { }
                try { score = json.getInt(KEY_POST_SCORE); } catch (JSONException ignored) { }
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
            return String.format("%1$d - %2$s.%3$s", post_id, TextUtils.join(" ", tags), file_ext);
        }

        @Override
        public String getExtras()
        {
            JSONObject json = new JSONObject();
            try { json.put(KEY_POST_MD5, md5); } catch (JSONException ignored) { }
            try { json.put(KEY_POST_FILE_EXT, file_ext); } catch (JSONException ignored) { }
            try { json.put(KEY_POST_UPLOADER_NAME, uploader_name); } catch (JSONException ignored) { }
            try { json.put(KEY_POST_SCORE, score); } catch (JSONException ignored) { }
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
            return context.getResources().getString(R.string.api_shimmie_post_description,
                host.name, host.url, host.getAPI().getName(),
                post_id, image_width, image_height, created_at.toString(), updated_at.toString(),
                rating, uploader_name, md5, file_ext, score);
        }
    }
}