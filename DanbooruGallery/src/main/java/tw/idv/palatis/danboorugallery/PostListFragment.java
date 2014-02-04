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
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.SearchView;
import android.widget.TextView;

import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

import java.io.File;

import tw.idv.palatis.danboorugallery.android.content.CustomTaskLoader;
import tw.idv.palatis.danboorugallery.android.widget.PopupMenu;
import tw.idv.palatis.danboorugallery.database.HostsTable;
import tw.idv.palatis.danboorugallery.database.PostTagsView;
import tw.idv.palatis.danboorugallery.database.PostsTable;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;
import tw.idv.palatis.danboorugallery.picasso.Picasso;
import tw.idv.palatis.danboorugallery.siteapi.SiteAPI;
import tw.idv.palatis.danboorugallery.util.PostListAdapter;
import tw.idv.palatis.danboorugallery.util.SiteSession;
import tw.idv.palatis.danboorugallery.util.TagCursor;
import tw.idv.palatis.danboorugallery.util.TagSearchCursorAdapter;

public class PostListFragment
    extends Fragment
    implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener
{
    private static final String TAG = "PostListFragment";

    private static final String STATE_FIRST_VISIBLE_POSITION = "first_visible_position";

    private Callbacks mCallbacks = null;

    private int mShortAnimTime = 0;
    private PostListAdapter mPostListAdapter = null;
    private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener =
        new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences preferences, String key)
            {
                if (key.equals(DanbooruGallerySettings.KEY_PREF_STICKY_GRID_HEADER))
                    mGridView.setAreHeadersSticky(DanbooruGallerySettings.getStickyGridHeader());

                if (key.equals(DanbooruGallerySettings.KEY_PREF_COLUMNS_PORTRAIT) ||
                    key.equals(DanbooruGallerySettings.KEY_PREF_COLUMNS_LANDSCAPE))
                {
                    int numColumns = mCallbacks.getPreferredNumColumns();
                    if (numColumns == mGridView.getNumColumns())
                        return;

                    final int firstPosition = mGridView.getFirstVisiblePosition();
                    final int lastPosition = mGridView.getLastVisiblePosition();
                    final int midPosition = (firstPosition + lastPosition) / 2;
                    mGridView.setNumColumns(numColumns);
                    mGridView.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mGridView.requestFocusFromTouch();
                            mGridView.setSelection(lastPosition);
                            mGridView.requestFocusFromTouch();
                            mGridView.setSelection(firstPosition);
                            if (mGridView.getLastVisiblePosition() >= midPosition && midPosition >= mGridView.getFirstVisiblePosition())
                                return;
                            mGridView.post(this);
                        }
                    });
                }
            }
        };

    // LoaderManager.LoaderCallbacks<Cursor>
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle)
    {
        switch (id)
        {
            case R.id.loader_search_tags:
                return new CustomTaskLoader<Cursor>(getActivity().getApplicationContext()) {
                    @Override
                    public Cursor runTaskInBackground(CancellationSignal signal)
                    {
                        return SiteSession.searchTags(signal);
                    }

                    @Override
                    public void cleanUp(Cursor oldCursor)
                    {
                        if (!oldCursor.isClosed())
                            oldCursor.close();
                    }
                };
            case R.id.loader_post_ids:
                return new CustomTaskLoader<Cursor>(getActivity().getApplicationContext()) {
                    @Override
                    public Cursor runTaskInBackground(CancellationSignal signal)
                    {
                        return SiteSession.getAllPostsCursor(PostListAdapter.POST_COLUMNS);
                    }

                    @Override
                    public void cleanUp(Cursor oldCursor)
                    {
                        if (!oldCursor.isClosed())
                            oldCursor.close();
                    }
                };
        }
        throw new IllegalArgumentException("No such loader (id = " + id + ")");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
    {
        int id = loader.getId();
        switch (id)
        {
            case R.id.loader_search_tags:
                mSearchView.getSuggestionsAdapter().swapCursor(cursor);
                return;
            case R.id.loader_post_ids:
                mPostListAdapter.swapCursor(cursor);
                return;
        }
        throw new IllegalArgumentException("No such loader (id = " + id + ")");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        int id = loader.getId();
        switch (id)
        {
            case R.id.loader_search_tags:
                if (mSearchView != null && mSearchView.getSuggestionsAdapter() != null)
                    mSearchView.getSuggestionsAdapter().swapCursor(null);
                return;
            case R.id.loader_post_ids:
                if (mPostListAdapter != null)
                    mPostListAdapter.swapCursor(null);
                return;
        }
        throw new IllegalArgumentException("No such loader (id = " + id + ")");
    }

    @Override
    public void onScrollStateChanged(AbsListView listView, int state)
    {
    }

    private long mOldPostCreatedAt = -1;
    @Override
    public void onScroll(AbsListView listView, int firstVisibleItem, int visibleItemCount, int totalItemCount)
    {
        int last = firstVisibleItem + visibleItemCount - 1;
        while (last >= 0 && !(mGridView.getItemAtPosition(last) instanceof Cursor))
            --last;
        Cursor cursor = (Cursor) mGridView.getItemAtPosition(last);
        if (cursor != null && cursor.getCount() != 0)
        {
            long created_at = cursor.getLong(PostListAdapter.INDEX_POST_CREATED_AT);
            if (mOldPostCreatedAt != created_at)
            {
                SiteSession.fetchPosts(created_at, false, mPostLoadingCallback);
                mOldPostCreatedAt = created_at;
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
    {
        final int post_db_id = (int) mPostListAdapter.getItemId(position);
        Cursor post_cursor = PostsTable.getPostCursorById(post_db_id);
        Cursor tags_cursor = PostTagsView.getTagNamesCursorForPostDatabaseId(post_db_id);
        post_cursor.moveToFirst();
        Host host = SiteSession.getHostById(post_cursor.getInt(PostsTable.INDEX_POST_HOST_ID));
        final Post post = Post.fromCursor(host, post_cursor, tags_cursor);
        post_cursor.close();
        tags_cursor.close();

        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.setForceShowIcon(true);
        popup.getMenuInflater().inflate(R.menu.menu_post_list_fragment_longclick, popup.getMenu());
        popup.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                int id = item.getItemId();
                switch (id)
                {
                    case R.id.menu_post_detail_browser:
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(post.getWebUrl()));
                        startActivity(intent);
                        break;
                    case R.id.menu_post_detail_tags:
                        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                        adb.setTitle(getResources().getString(R.string.dialog_tags_title, post.post_id));
                        adb.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_activated_1, android.R.id.text1, post.tags),
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int position)
                                {
                                    SiteSession.submitFilterTags(post.tags[position]);
                                }
                            });
                        adb.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int position) { }
                        });
                        adb.create().show();
                        break;
                    case R.id.menu_post_detail_download:
                        File destination = post.host.getAPI().getDownloadFile(post.host, post);

                        if (destination.getParentFile().exists() || destination.getParentFile().mkdirs())
                        {
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(post.file_url));
                            request.allowScanningByMediaScanner();
                            request.addRequestHeader("Referer", post.getReferer());
                            // HACK: fake user agent
                            request.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setTitle(post.host.getAPI().getDownloadTitle(post.host, post));
                            request.setDescription(post.host.getAPI().getDownloadDescription(post.host, post));
                            request.setDestinationUri(Uri.fromFile(destination));
                            DownloadManager downloader = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                            downloader.enqueue(request);
                        }
                        break;
                }
                return true;
            }
        });
        popup.setOnDismissListener(new android.widget.PopupMenu.OnDismissListener()
        {
            @Override
            public void onDismiss(android.widget.PopupMenu popupMenu) { }
        });
        popup.show();
        return true;
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks
    {
        public int getPreferredNumColumns();
        public boolean onSearchViewExpand();
        public boolean onSearchViewCollapse();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PostListFragment() { }

    private SiteSession.LoadingCallback mPostLoadingCallback = new SiteSession.LoadingCallback()
    {
        @Override
        public void onPreExecute()
        {
            ViewPropertyAnimator animator = mLoadingIndicatorView.animate();
            if (animator != null)
            {
                animator.setDuration(mShortAnimTime)
                    .alpha(1.0f)
                    .withLayer()
                    .withStartAction(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Activity activity = getActivity();
                            if (activity != null)
                                activity.setProgressBarIndeterminateVisibility(true);
                            mLoadingIndicatorView.setVisibility(View.VISIBLE);
                            mLoadingIndicatorView.setText(R.string.gallery_grid_loading_started);
                        }
                    })
                    .start();
            }
        }

        @Override
        public void onProgressUpdate(int progress)
        {
            try
            {
                getLoaderManager().restartLoader(R.id.loader_post_ids, null, PostListFragment.this);
            }
            catch (IllegalStateException ignored) { }
        }

        @Override
        public void onPostExecute()
        {
            ViewPropertyAnimator animator = mLoadingIndicatorView.animate();
            if (animator != null)
            {
                animator.setDuration(mShortAnimTime)
                    .alpha(0.0f)
                    .withLayer()
                    .withStartAction(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Activity activity = getActivity();
                            if (activity != null)
                                activity.setProgressBarIndeterminateVisibility(false);
                            mLoadingIndicatorView.setVisibility(View.VISIBLE);
                            mLoadingIndicatorView.setText(R.string.gallery_grid_loading_finished);
                        }
                    })
                    .withEndAction(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mLoadingIndicatorView.setVisibility(View.GONE);
                        }
                    })
                    .start();
            }
        }

        @Override
        public void onError(SiteAPI.SiteAPIException error)
        {
            try
            {
                DialogFragment dialog = new SiteAPIErrorDialogFragment(error);
                dialog.show(getFragmentManager(), "SiteAPIErrorDialog");
            }
            // cannot show dialog if the app is not in foreground.
            catch (IllegalStateException ignored) { }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mShortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        final int firstPosition = mGridView.getFirstVisiblePosition();
        final int lastPosition = mGridView.getLastVisiblePosition();
        mGridView.setNumColumns(mCallbacks.getPreferredNumColumns());
        mGridView.post(new Runnable()
        {
            @Override
            public void run()
            {
                mGridView.requestFocusFromTouch();
                mGridView.setSelection(lastPosition);
                mGridView.requestFocusFromTouch();
                mGridView.setSelection(firstPosition);
            }
        });
    }

    public void scrollGridToPosition(int position)
    {
        Cursor cursor = mPostListAdapter.getCursor();
        cursor.moveToPosition(position);
        if (cursor.isBeforeFirst())
            cursor.moveToFirst();
        if (cursor.isAfterLast())
            cursor.moveToLast();
        long target_created_at = cursor.getLong(PostListAdapter.INDEX_POST_CREATED_AT);

        // find the closest cursor to target position
        while (!(mGridView.getItemAtPosition(position) instanceof Cursor))
        {
            if (position >= mGridView.getCount())
                break;
            ++position;
        }
        cursor = (Cursor) mGridView.getItemAtPosition(position);
        long current_created_at = cursor.getLong(PostListAdapter.INDEX_POST_CREATED_AT);

        // find the real position
        int step = (target_created_at > current_created_at) ? -1 : 1;
        int real_position, limit = mGridView.getCount();
        for (real_position = position;real_position < limit;real_position += step)
        {
            Object item = mGridView.getItemAtPosition(real_position);
            if (!(item instanceof Cursor))
                continue;

            cursor = (Cursor) item;
            if (cursor.getLong(PostListAdapter.INDEX_POST_CREATED_AT) == target_created_at)
                break;
        }

        // do the actual scrolling
        if (mGridView.getFirstVisiblePosition() <= real_position && real_position <= mGridView.getLastVisiblePosition())
            return;
        mGridView.post(new ScrollRunnable(mGridView, real_position));
    }

    private static class ScrollRunnable
        implements Runnable
    {
        AbsListView mListView;
        int mPosition;

        public ScrollRunnable(AbsListView listView, int position)
        {
            mListView = listView;
            mPosition = position;
        }

        @Override
        public void run()
        {
            mListView.requestFocusFromTouch();
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    private MenuItem mSearchItem = null;
    public boolean onSearchRequested()
    {
        mSearchItem.expandActionView();
        return true;
    }

    private SearchView mSearchView;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.menu_post_list_fragment, menu);

        mSearchItem = menu.findItem(R.id.menu_post_list_search);
        // Associate searchable_tags configuration with the SearchView
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) mSearchItem.getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        mSearchView.setSuggestionsAdapter(new TagSearchCursorAdapter(getActivity(), null, false));

        mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener()
        {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem)
            {
                getLoaderManager().initLoader(R.id.loader_search_tags, null, PostListFragment.this);
                mSearchView.setQuery(SiteSession.getFilterTags(), false);
                return mCallbacks.onSearchViewExpand();
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem)
            {
                getLoaderManager().getLoader(R.id.loader_search_tags).cancelLoad();
                if (TextUtils.getTrimmedLength(mSearchView.getQuery()) == 0)
                    SiteSession.submitFilterTags("");
                return mCallbacks.onSearchViewCollapse();
            }
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                SiteSession.submitFilterTags(query);
                mSearchItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query)
            {
                SiteSession.setTagSearchPattern(query);
                getLoaderManager().restartLoader(R.id.loader_search_tags, null, PostListFragment.this);
                return false;
            }
        });

        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener()
        {
            @Override
            public boolean onSuggestionSelect(int position)
            {
                TagCursor cursor = (TagCursor) mSearchView.getSuggestionsAdapter().getItem(position);
                Tag tag = cursor.getTag();
                String query = mSearchView.getQuery().toString();
                mSearchView.setQuery(query.substring(0, query.lastIndexOf(" ") + 1) + tag.name, false);
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position)
            {
                TagCursor cursor = (TagCursor) mSearchView.getSuggestionsAdapter().getItem(position);
                Tag tag = cursor.getTag();

                String query = mSearchView.getQuery().toString();
                int space_idx = query.lastIndexOf(" ");
                if (space_idx != -1)
                {
                    query = query.substring(0, space_idx + 1) + tag.name;
                    mSearchView.setQuery(query, false);
                } else
                    mSearchView.setQuery(tag.name, true);

                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        switch (id)
        {
            case R.id.menu_post_list_search:
                break;
            case R.id.menu_post_list_debug:
                Picasso.getSnapshot().dump();
                Picasso.getPreviewSnapshot().dump();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private DataSetObserver mHostsObserver;
    private DataSetObserver mPostsObserver;
    private TextView mLoadingIndicatorView;
    private StickyGridHeadersGridView mGridView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_post_list, container, false);

        DanbooruGallerySettings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        mPostListAdapter = new PostListAdapter(getActivity());
        mLoadingIndicatorView = (TextView) rootView.findViewById(R.id.gallery_grid_loading_indicator);
        mGridView = (StickyGridHeadersGridView) rootView.findViewById(R.id.gallery_grid);
        mGridView.setAreHeadersSticky(DanbooruGallerySettings.getStickyGridHeader());
        mGridView.setChoiceMode(GridView.CHOICE_MODE_NONE);
        mGridView.setNumColumns(mCallbacks.getPreferredNumColumns());
        mGridView.setAdapter(mPostListAdapter);
        mGridView.setOnScrollListener(this);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);

        mHostsObserver = new DataSetObserver()
        {
            @Override
            public void onChanged()
            {
                getLoaderManager().restartLoader(R.id.loader_post_ids, null, PostListFragment.this);
                getLoaderManager().restartLoader(R.id.loader_search_tags, null, PostListFragment.this);
                SiteSession.fetchPosts(0, true, mPostLoadingCallback);
                super.onChanged();
            }

            @Override
            public void onInvalidated()
            {
                super.onInvalidated();
                CursorAdapter adapter = mSearchView.getSuggestionsAdapter();
                if (adapter != null)
                    adapter.swapCursor(null);
            }
        };
        mPostsObserver = new DataSetObserver()
        {
            @Override
            public void onChanged()
            {
                super.onChanged();
                getLoaderManager().restartLoader(R.id.loader_post_ids, null, PostListFragment.this);
            }

            @Override
            public void onInvalidated()
            {
                super.onInvalidated();
                getLoaderManager().restartLoader(R.id.loader_post_ids, null, PostListFragment.this);
            }
        };

        HostsTable.registerDataSetObserver(mHostsObserver);
        PostsTable.registerDataSetObserver(mPostsObserver);

        getLoaderManager().initLoader(R.id.loader_post_ids, null, this);
        SiteSession.fetchPosts(0, true, mPostLoadingCallback);

        return rootView;
    }

    @Override
    public void onDestroyView()
    {
        PostsTable.unregisterDataSetObserver(mPostsObserver);
        HostsTable.unregisterDataSetObserver(mHostsObserver);
        DanbooruGallerySettings.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

        super.onDestroyView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Intent intent = new Intent(getActivity(), PostDetailActivity.class);
        intent.putExtra("post_position", position); // FIXME: hard coded key
        startActivity(intent);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null)
            mGridView.smoothScrollToPosition(savedInstanceState.getInt(STATE_FIRST_VISIBLE_POSITION));
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
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_FIRST_VISIBLE_POSITION, mGridView.getFirstVisiblePosition());
    }
}
