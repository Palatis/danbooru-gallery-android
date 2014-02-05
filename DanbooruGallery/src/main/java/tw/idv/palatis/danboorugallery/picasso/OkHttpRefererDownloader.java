/*
* Copyright (C) 2013 Square, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package tw.idv.palatis.danboorugallery.picasso;

import android.net.Uri;
import android.text.TextUtils;

import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.Downloader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

/** A {@link Downloader} which uses OkHttp to download images. */
public class OkHttpRefererDownloader implements Downloader
{
    private static final String TAG = "OkHttpRefererDownloader";

    static final String RESPONSE_SOURCE_ANDROID = "X-Android-Response-Source";
    static final String RESPONSE_SOURCE_OKHTTP = "OkHttp-Response-Source";

    static final int DEFAULT_READ_TIMEOUT = 20 * 1000; // 20s
    static final int DEFAULT_CONNECT_TIMEOUT = 15 * 1000; // 15s

    protected final OkHttpClient client;

    /**
     * Create new downloader that uses OkHttp. This will install an image cache into your application
     * cache directory.
     *
     * @param cacheDir The directory in which the cache should be stored
     * @param maxSize The size limit for the cache.
     */
    public OkHttpRefererDownloader(final File cacheDir, final long maxSize) {
        client = new OkHttpClient();

        // don't use the global SSL context
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
        } catch (GeneralSecurityException e) {
            throw new AssertionError(); // The system has no TLS. Just give up.
        }
        client.setSslSocketFactory(sslContext.getSocketFactory());

        try {
            client.setResponseCache(new HttpResponseCache(cacheDir, maxSize));
        } catch (IOException ignored) {
        }
    }

    protected HttpURLConnection openConnection(Uri uri) throws IOException
    {
        String[] url = TextUtils.split(uri.toString(), "\\|");
        HttpURLConnection connection = client.open(new URL(url[0]));
        if (url.length > 1)
            connection.setRequestProperty("Referer", url[1]);
        // HACK: fake user agent
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
        connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
        return connection;
    }

    @Override public Response load(Uri uri, boolean localCacheOnly) throws IOException {
        HttpURLConnection connection = openConnection(uri);
        connection.setUseCaches(true);
        if (localCacheOnly) {
            connection.setRequestProperty("Cache-Control", "only-if-cached,max-age=" + Integer.MAX_VALUE);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode >= 300)
        {
            connection.disconnect();
            throw new ResponseException(responseCode + " " + connection.getResponseMessage());
        }

        String responseSource = connection.getHeaderField(RESPONSE_SOURCE_OKHTTP);
        if (responseSource == null) {
            responseSource = connection.getHeaderField(RESPONSE_SOURCE_ANDROID);
        }
        boolean fromCache = parseResponseSourceHeader(responseSource);

        return new Response(connection.getInputStream(), fromCache);
    }

    /** Thrown for non-2XX responses. */
    class ResponseException extends IOException {
        public ResponseException(String message) {
            super(message);
        }
    }

    /** Returns {@code true} if header indicates the response body was loaded from the disk cache. */
    static boolean parseResponseSourceHeader(String header) {
        if (header == null) {
            return false;
        }
        String[] parts = header.split(" ", 2);
        if ("CACHE".equals(parts[0])) {
            return true;
        }
        if (parts.length == 1) {
            return false;
        }
        try {
            return "CONDITIONAL_CACHE".equals(parts[0]) && Integer.parseInt(parts[1]) == 304;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}