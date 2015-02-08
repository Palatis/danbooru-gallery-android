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

import android.content.Context;
import android.net.Uri;

import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.ResponseBody;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.OkHttpDownloader;

import java.io.File;
import java.io.IOException;

/**
 * A {@link Downloader} which uses OkHttp to download images.
 */
public class OkHttpRefererDownloader extends OkHttpDownloader {
	private static final String TAG = "OkHttpRefererDownloader";

	public OkHttpRefererDownloader(Context context) {
		super(context);
	}

	public OkHttpRefererDownloader(File cacheDir) {
		super(cacheDir);
	}

	public OkHttpRefererDownloader(Context context, long maxSize) {
		super(context, maxSize);
	}

	public OkHttpRefererDownloader(File cacheDir, long maxSize) {
		super(cacheDir, maxSize);
	}

	public OkHttpRefererDownloader(OkHttpClient client) {
		super(client);
	}

	private String mReferer = null;

	public void setReferer(String referer) {
		mReferer = referer;
	}

	@Override
	public Response load(Uri uri, int networkPolicy) throws IOException {
		CacheControl cacheControl = null;
		if (networkPolicy != 0) {
			if (NetworkPolicy.isOfflineOnly(networkPolicy)) {
				cacheControl = CacheControl.FORCE_CACHE;
			} else {
				CacheControl.Builder builder = new CacheControl.Builder();
				if (!NetworkPolicy.shouldReadFromDiskCache(networkPolicy)) {
					builder.noCache();
				}
				if (!NetworkPolicy.shouldWriteToDiskCache(networkPolicy)) {
					builder.noStore();
				}
				cacheControl = builder.build();
			}
		}

		Request.Builder builder = new Request.Builder().url(uri.toString());
		if (cacheControl != null) {
			builder.cacheControl(cacheControl);
		}
		if (mReferer != null)
			builder.addHeader("Referer", mReferer);
		// HACK: fake user-agent
		builder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");

		com.squareup.okhttp.Response response = getClient().newCall(builder.build()).execute();
		int responseCode = response.code();
		if (responseCode >= 300) {
			response.body().close();
			throw new Downloader.ResponseException(responseCode + " " + response.message(), networkPolicy, responseCode);
		}

		boolean fromCache = response.cacheResponse() != null;

		ResponseBody responseBody = response.body();
		return new Response(responseBody.byteStream(), fromCache, responseBody.contentLength());
	}
}