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
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Environment;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

import com.squareup.okhttp.OkHttpClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import tw.idv.palatis.danboorugallery.DanbooruGallerySettings;
import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

@SuppressWarnings("unused")
public abstract class SiteAPI
{
    private static final String TAG = "SiteAPI";

    /**
     * An ID to identify the API used, should not be changed across app versions.
     * @return  the API identity.
     */
    public abstract int getApiId();

    /**
     * The name of the API, can be changed in the future.
     * @return  The name of the API
     */
    public abstract String getName();

    /**
     * Used to hash {@code login} and {@code password} into auth string
     * @param login       login name
     * @param password    password
     * @return            hashed secret
     */
    public abstract String hashSecret(String login, String password);

    /**
     * fetch a bunch of posts
     * @param host         the host
     * @param startFrom    the number of post to skip
     * @param tags         filtered tags
     * @return             a list of posts
     */
    public abstract List<Post> fetchPosts(Host host, int startFrom, String tags) throws SiteAPIException;

    /**
     * search for tags
     * @param host             the host
     * @param match_pattern    the match pattern
     * @return                 a list of tags
     */
    public abstract List<Tag> searchTags(Host host, String match_pattern) throws SiteAPIException;

    /**
     * Construct a post from cursors, because only the underlying API knows how to construct
     * the post.
     * You should allow null {@code tags_cursor}, because sometimes tags are not needed (such
     * as in the post list view) to speed up the process.
     *
     * @param host           the host this post is from
     * @param post_cursor    the cursor containing post data
     * @param tags_cursor    the cursor for tags
     * @return               the constructed post
     */
    public abstract Post getPostFromCursor(Host host, Cursor post_cursor, Cursor tags_cursor);

    public static final int PAGE_LIMIT_TYPE_STRICT = 0x01;
    public static final int PAGE_LIMIT_TYPE_RELAXED = 0x02;

    // some default settings, might need tuning.
    private static final int PAGE_LIMIT_STRICT = 20;
    private static final int PAGE_LIMIT_RELAXED = 40;

    private static int[] sPageLimits;
    private static List<SiteAPI> sRegisteredAPIs = new ArrayList<>();
    private static SparseArray<SiteAPI> sRegisteredAPIMap = new SparseArray<>();
    private static final SiteAPI sDummyAPI = new DummyAPI();

    // TODO:
    //    i'm looking for a method to automatically discover available APIs,
    //    so it might be possible to use APIs from another apk?
    //    was thinking about ContentProvider, but it might just be way too overkill...
    //    for now, just call init() for each class.
    static
    {
        // if you're creating new APIs, you should add your API here.
        DanbooruAPI.init();
        DanbooruLegacyAPI.init();
        MoebooruAPI.init();
        GelbooruAPI.init();
    }

    public static void registerSiteAPI(SiteAPI api)
    {
        sRegisteredAPIs.add(api);
        sRegisteredAPIMap.put(api.getApiId(), api);
    }

    public static void unregisterSiteAPI(SiteAPI siteapi)
    {
        sRegisteredAPIs.remove(siteapi);
        sRegisteredAPIMap.remove(siteapi.getApiId());
    }

    public static SiteAPI findAPIById(int id)
    {
        return sRegisteredAPIMap.get(id, sDummyAPI);
    }

    private static OkHttpClient sOkHttpClient;

    protected static HttpURLConnection openConnection(URL url)
    {
        HttpURLConnection connection = sOkHttpClient.open(url);
        // FIXME: fake user-agent
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
        return connection;
    }

    public static void init(Context context)
    {
        Resources resources = context.getResources();
        sPageLimits = resources.getIntArray(R.array.api_page_limit_array);

        sOkHttpClient = new OkHttpClient();
        // don't use the global SSL context
        try
        {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            sOkHttpClient.setSslSocketFactory(sslContext.getSocketFactory());
        }
        catch (GeneralSecurityException ignored)
        {
            // The system has no TLS. Just give up.
        }
    }

    /**
     * Get the title to show the {@link android.app.DownloadManager}
     * @param host    the host this post is from
     * @param post    the post itself
     * @return        the title
     */
    public String getDownloadTitle(Host host, Post post)
    {
        return String.format(
                "%1$s - %2$d",
                host.name, post.post_id
        );
    }

    /**
     * Get the description to show the {@link android.app.DownloadManager}
     * @param host    the host this post is from
     * @param post    the post itself
     * @return        the description
     */
    public String getDownloadDescription(Host host, Post post)
    {
        return TextUtils.join(" ", post.tags);
    }

    /**
     * Get the {@link File} that {@link android.app.DownloadManager} will save the file
     * @param host    the host this post is from
     * @param post    the post itself
     * @return        the {@link File} to be saved
     */
    public File getDownloadFile(Host host, Post post)
    {
        // truncate the download file name to 127 characters
        String filename = post.getDownloadFilename();
        int extpos = filename.lastIndexOf('.');

        String filename_ext = filename.substring(extpos);
        filename = filename.substring(0, extpos);

        if (filename.length() >= 127 - filename_ext.length())
            filename = filename.substring(0, 127 - filename_ext.length());
        filename += filename_ext;

        return new File(
            Environment.getExternalStorageDirectory(),
            String.format("%1$s/%2$s/%3$s",
                DanbooruGallerySettings.SAVEDIR,
                host.getDownloadFolder(),
                filename
            )
        );
    }

    public int getPageLimit(int type)
    {
        switch (type)
        {
            case PAGE_LIMIT_TYPE_RELAXED: return PAGE_LIMIT_RELAXED;
            case PAGE_LIMIT_TYPE_STRICT: return PAGE_LIMIT_STRICT;
        }
        throw new IllegalArgumentException("Unknown page limit type for " + type + ".");
    }

    public int[] getPageLimits(int type)
    {
        switch (type)
        {
            case PAGE_LIMIT_TYPE_RELAXED:
            case PAGE_LIMIT_TYPE_STRICT:
                return sPageLimits;
        }
        throw new IllegalArgumentException("Unknown page limit type for " + type + ".");
    }

    public static SiteAPI getDummyAPI()
    {
        return sDummyAPI;
    }

    public static abstract class PageLimitAdapter
            implements ListAdapter, SpinnerAdapter
    {
        private int[] mPageLimits;

        public PageLimitAdapter(SiteAPI api, int type)
        {
            mPageLimits = api.getPageLimits(type);
        }

        @Override
        public View getDropDownView(int position, View view, ViewGroup parent)
        {
            return getView(position, view, parent);
        }

        @Override
        public boolean areAllItemsEnabled()
        {
            return true;
        }

        @Override
        public boolean isEnabled(int i)
        {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver dataSetObserver) { }

        @Override
        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) { }

        @Override
        public int getCount()
        {
            return mPageLimits.length;
        }

        @Override
        public Integer getItem(int position)
        {
            return mPageLimits[position];
        }

        @Override
        public long getItemId(int position)
        {
            return mPageLimits[position];
        }

        @Override
        public boolean hasStableIds()
        {
            return true;
        }

        @Override
        public int getItemViewType(int i)
        {
            return 0;
        }

        @Override
        public int getViewTypeCount()
        {
            return 1;
        }

        @Override
        public boolean isEmpty()
        {
            return mPageLimits.length == 0;
        }

        public int indexOf(int value)
        {
            for (int i = mPageLimits.length - 1;i >= 0;--i)
                if (value >= mPageLimits[i])
                    return i;
            return 0;
        }
    }

    public static abstract class SiteAPIListAdapter
        implements ListAdapter, SpinnerAdapter
    {
        @Override
        public boolean areAllItemsEnabled()
        {
            return true;
        }

        @Override
        public boolean isEnabled(int position)
        {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) { }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) { }

        @Override
        public int getCount()
        {
            return sRegisteredAPIs.size();
        }

        @Override
        public SiteAPI getItem(int position)
        {
            return sRegisteredAPIs.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return sRegisteredAPIs.get(position).hashCode();
        }

        @Override
        public boolean hasStableIds()
        {
            return true;
        }

        @Override
        public View getDropDownView(int position, View view, ViewGroup viewGroup)
        {
            return getView(position, view, viewGroup);
        }

        @Override
        public int getItemViewType(int position)
        {
            return 0;
        }

        @Override
        public int getViewTypeCount()
        {
            return 1;
        }

        @Override
        public boolean isEmpty()
        {
            return sRegisteredAPIs.isEmpty();
        }

        public int indexOf(SiteAPI siteapi)
        {
            return sRegisteredAPIs.indexOf(siteapi);
        }
    }

    public static class SiteAPIException
        extends IOException
    {
        public SiteAPIException()
        {
        }

        public SiteAPIException(String message)
        {
            super(message);
        }

        public SiteAPIException(String message, Throwable cause)
        {
            super(message, cause);
        }

        public SiteAPIException(Throwable cause)
        {
            super(cause);
        }

        public SiteAPIException(SiteAPI api, HttpURLConnection connection, Throwable cause)
        {
            super(cause);
            mSiteAPI = api;
            if (connection != null)
            {
                mUrl = connection.getURL().toString();
                try
                {
                    Reader input = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
                    Writer output = new StringWriter();
                    char buffer[] = new char[1024];
                    for (int count = input.read(buffer);count > 0;count = input.read(buffer))
                        output.write(buffer, 0, count);

                    mBody = output.toString();
                }
                catch (IOException ignored) { }
            }
        }

        protected SiteAPI mSiteAPI;
        protected String mUrl;
        protected String mBody;

        @Override
        public String getMessage()
        {
            return mSiteAPI.getName() + " [" + mUrl + "]: " + mBody;
        }

        public SiteAPI getSiteAPI()
        {
            return mSiteAPI;
        }

        public String getUrl()
        {
            return mUrl;
        }

        public String getBody()
        {
            return mBody;
        }
    }
}
