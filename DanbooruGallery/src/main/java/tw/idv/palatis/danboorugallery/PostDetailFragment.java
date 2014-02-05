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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;

import com.diegocarloslima.byakugallery.TileBitmapDrawable;
import com.diegocarloslima.byakugallery.TouchImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Downloader;

import java.io.File;
import java.io.IOException;

import tw.idv.palatis.danboorugallery.android.content.CustomTaskLoader;
import tw.idv.palatis.danboorugallery.database.PostTagsView;
import tw.idv.palatis.danboorugallery.database.PostsTable;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.picasso.Picasso;
import tw.idv.palatis.danboorugallery.util.SiteSession;

public class PostDetailFragment
    extends Fragment implements LoaderManager.LoaderCallbacks<Post>
{
    private static final String TAG = "PostDetailFragment";

    private int mPostId;
    private Post mPost;
    private ProgressBar mProgressBar;
    private TouchImageView mImageView;
    private ImageView mDownloadErrorIndicator;
    private Callbacks mCallbacks;
    private ImageView mPreviewImageView;

    public PostDetailFragment()
    {
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(Post.KEY_POST_ID, mPostId);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null)
            mPostId = savedInstanceState.getInt(Post.KEY_POST_ID);
    }

    @Override
    public Loader<Post> onCreateLoader(int id, Bundle bundle)
    {
        return new CustomTaskLoader<Post>(getActivity().getApplicationContext()) {
            @Override
            public Post runTaskInBackground(CancellationSignal signal)
            {
                try
                {
                    Cursor post_cursor = PostsTable.getPostCursorById(mPostId);
                    post_cursor.moveToFirst();
                    Cursor tags_cursor = PostTagsView.getTagNamesCursorForPostDatabaseId(mPostId);
                    Host host = SiteSession.getHostById(post_cursor.getInt(PostsTable.INDEX_POST_HOST_ID));
                    return Post.fromCursor(host, post_cursor, tags_cursor);
                }
                catch (Exception ex)
                {
                    Log.d(TAG, "Loading post " + mPostId + " failed.", ex);
                }
                return null;
            }

            @Override
            public void cleanUp(Post oldPost) { }
        };
    }

    @Override
    public void onLoadFinished(Loader<Post> loader, Post post)
    {
        mPost = post;
        if (mPost != null)
        {
            mPreviewImageView.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);

            // this is tricky.
            // 1.a. we try load the preview into the imageview, this should be done really quick
            //      since we might already have it in the cache.
            //      even if we have to download it from the internet, it should still be faster
            //      than the full-size image.
            // 1.b. we load the full-size image at the same time, this may take some time.
            // 2. after the real full size image loaded, we have to make sure it really loads.
            // 2.a. the image is within texture size limit, nothing to be done.
            // 2.b. the image is too big that OpenGLRenderer refuse to upload it to a texture,
            //      we have to let TileBitmapDrawable to do the work for us.
            //
            // This is tricky, I hope someday Picasso will use something like TileBitmapDrawable
            // to load the image, so this whole part can be "simplified".

            // load the preview into the preview imageview
            Picasso.withPreview(getActivity().getApplicationContext())
                .load(mPost.file_url_preview)
                .noFade()
                .into(mPreviewImageView);

            // FIXME: fake the referer
            final String file_url_with_referer =
                (DanbooruGallerySettings.getDownloadFullsize() ? mPost.file_url : mPost.file_url_large) +
                "|" + mPost.getReferer();

            // load the actual image to the imageview
            Picasso.with(getActivity().getApplicationContext())
                .load(file_url_with_referer)
                .noFade()
                .into(mImageView, new Callback()
                {
                    @Override
                    public void onSuccess()
                    {
                        Drawable drawable = mImageView.getDrawable();
                        if (drawable != null &&
                            drawable.getIntrinsicWidth() <= DanbooruGalleryApplication.MAXIMUM_TEXTURE_SIZE &&
                            drawable.getIntrinsicHeight() <= DanbooruGalleryApplication.MAXIMUM_TEXTURE_SIZE)
                        {
                            // ok, we're fine to display the image.
                            mProgressBar.setVisibility(View.GONE);
                            mPreviewImageView.setVisibility(View.GONE);
                            // release the drawable as we don't want it anymore.
                            mPreviewImageView.setImageDrawable(null);
                            return;
                        }

                        Log.v(TAG, "Bitmap might be too big to be uploaded to a texture, try the alternative method.");
                        try
                        {
                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                            {
                                final Downloader.Response response = Picasso.getDownloader(getActivity().getApplicationContext()).load(Uri.parse(file_url_with_referer), true);

                                TileBitmapDrawable.attachTileBitmapDrawable(mImageView, response.getInputStream(), null, new TileBitmapDrawable.OnInitializeListener()
                                {
                                    @Override
                                    public void onStartInitialization() { }

                                    @Override
                                    public void onEndInitialization()
                                    {
                                        mProgressBar.setVisibility(View.GONE);
                                        mPreviewImageView.setVisibility(View.GONE);
                                        // release the drawable as we don't want it anymore.
                                        mPreviewImageView.setImageDrawable(null);
                                    }
                                });
                            }
                        }
                        catch (IOException ex)
                        {
                            Log.e(TAG, "Downloader thrown an exception?", ex);
                            mProgressBar.setVisibility(View.GONE);
                            mDownloadErrorIndicator.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError()
                    {
                        Log.d(TAG, "wtf! download failed?");
                        mProgressBar.setVisibility(View.GONE);
                        mDownloadErrorIndicator.setVisibility(View.VISIBLE);
                    }
                });
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onLoaderReset(Loader<Post> loader)
    {
        mPreviewImageView.setImageDrawable(null);
        mPreviewImageView.setVisibility(View.GONE);

        // set visible to allow user click
        mImageView.setVisibility(View.VISIBLE);
        mImageView.setImageDrawable(null);

        mProgressBar.setVisibility(View.GONE);
        mDownloadErrorIndicator.setVisibility(View.VISIBLE);

        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks))
            throw new IllegalStateException("Activity must implement fragment's callbacks.");

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach()
    {
        mCallbacks = null;
        super.onDetach();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        Bundle bundle = savedInstanceState;
        if (bundle == null)
            bundle = getArguments();
        if (bundle != null)
        {
            mPostId = bundle.getInt(Post.KEY_POST_ID);
            getLoaderManager().initLoader(R.id.loader_post, null, this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private ShareActionProvider mShareActionProvider = null;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        if (mPost != null)
        {
            inflater.inflate(R.menu.menu_post_detail_fragment, menu);

            mShareActionProvider = (ShareActionProvider) menu.findItem(R.id.menu_post_detail_share).getActionProvider();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Intent.EXTRA_SUBJECT, mPost.getDownloadFilename());
            intent.putExtra(Intent.EXTRA_TEXT, mPost.getWebUrl());
            mShareActionProvider.setShareIntent(intent);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        switch (id)
        {
            // case R.id.menu_post_detail_share: // handled by ShareActionProvider
            case R.id.menu_post_detail_browser:
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(mPost.getWebUrl()));
                startActivity(intent);
                return true;
            }
            case R.id.menu_post_detail_tags:
            {
                AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                adb.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_activated_1, android.R.id.text1, mPost.tags),
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int position)
                        {
                            Intent intent = new Intent(getActivity(), PostListActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.setAction(Intent.ACTION_SEARCH);
                            intent.putExtra("tag", mPost.tags[position]);
                            getActivity().navigateUpTo(intent);
                            Log.d(TAG, "onClick(): " + position);
                        }
                    });
                adb.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position) { }
                });
                adb.create().show();
                return true;
            }
            case R.id.menu_post_detail_download:
            {
                File destination = mPost.host.getAPI().getDownloadFile(mPost.host, mPost);

                if (destination.getParentFile().exists() || destination.getParentFile().mkdirs())
                {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mPost.file_url));
                    request.allowScanningByMediaScanner();
                    request.addRequestHeader("Referer", mPost.getReferer());
                    // HACK: fake user agent
                    request.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setTitle(mPost.host.getAPI().getDownloadTitle(mPost.host, mPost));
                    request.setDescription(mPost.host.getAPI().getDownloadDescription(mPost.host, mPost));
                    request.setDestinationUri(Uri.fromFile(destination));
                    DownloadManager downloader = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                    downloader.enqueue(request);
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_post_detail, container, false);
        mPreviewImageView = (ImageView) rootView.findViewById(R.id.post_detail_image_preview);
        mImageView = (TouchImageView) rootView.findViewById(R.id.post_detail_image);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.post_detail_progress);
        mDownloadErrorIndicator = (ImageView) rootView.findViewById(R.id.post_detail_download_error_indicator);

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                mCallbacks.onImageClick();
            }
        });

        return rootView;
    }

    public static interface Callbacks
    {
        public void onImageClick();
    }
}
