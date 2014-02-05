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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import tw.idv.palatis.danboorugallery.android.content.CustomTaskLoader;
import tw.idv.palatis.danboorugallery.database.PostsTable;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.util.PostDetailPagerAdapter;
import tw.idv.palatis.danboorugallery.util.SiteSession;
import tw.idv.palatis.danboorugallery.util.SystemUiHider;
import tw.idv.palatis.danboorugallery.util.UiHider;

public class PostDetailActivity
    extends Activity
    implements
        PostDetailFragment.Callbacks,
        LoaderManager.LoaderCallbacks<Cursor>,
        ViewPager.OnPageChangeListener
{
    public static final String TAG = "PostDetailActivity";

    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private UiHider mUiHider;
    private ViewPager mViewPager;
    private LinearLayout mControlsView;
    private PostDetailPagerAdapter mPagerAdapter;
    private TextView mInfoText;
    private ImageButton mPlayPauseButton;

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
    {
    }

    @Override
    public void onPageSelected(int position)
    {
        mPosition = position;
        Cursor post_cursor = mPagerAdapter.getCursor(mPosition);
        if (post_cursor != null && post_cursor.getCount() != 0)
        {
            Host host = SiteSession.getHostById(post_cursor.getInt(PostsTable.INDEX_POST_HOST_ID));
            Post post = Post.fromCursor(host, post_cursor, null);
            mInfoText.setText(post.describeContent(this));

            // FIXME: hard coded page limit
            boolean forced = (position < 10) || (position > post_cursor.getCount() - 10);
            long created_at = post_cursor.getLong(PostsTable.INDEX_POST_CREATED_AT);
            SiteSession.fetchPosts(created_at, forced, null);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state)
    {
    }

    private Runnable mNextPageRunnable = new Runnable() {
        @Override
        public void run()
        {
            int next = mViewPager.getCurrentItem() + 1;
            if (next >= mViewPager.getAdapter().getCount())
                next = 0;
            int nextnext = next + 1;
            if (nextnext >= mViewPager.getAdapter().getCount())
                nextnext = 0;
            mViewPager.getAdapter().instantiateItem(mViewPager, nextnext);
            mViewPager.setCurrentItem(next, next != 0);
            mViewPager.postDelayed(this, DanbooruGallerySettings.getAutoplayDelay());
        }
    };

    private DataSetObserver mPostsObserver = new DataSetObserver()
    {
        @Override
        public void onChanged()
        {
            super.onChanged();
        }

        @Override
        public void onInvalidated()
        {
            super.onInvalidated();
            mPagerAdapter.swapCursor(null);
        }
    };

    private int mPosition;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle)
    {
        return new CustomTaskLoader<Cursor>(getApplicationContext())
        {
            @Override
            public Cursor runTaskInBackground(CancellationSignal signal)
            {
                return SiteSession.getAllPostsCursor(PostsTable.POST_ALL_COLUMNS);
            }

            @Override
            public void cleanUp(Cursor oldCursor) { }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
    {
        if (mPosition != -1)
        {
            // tries to find the position in the new cursor
            Cursor oldCursor = mPagerAdapter.getCursor(mPosition);
            if (oldCursor != null)
            {
                int post_id = oldCursor.getInt(PostsTable.INDEX_POST_POST_ID);
                cursor.moveToPosition(-1);
                while (cursor.moveToNext())
                    if (cursor.getInt(PostsTable.INDEX_POST_POST_ID) == post_id)
                    {
                        mPosition = cursor.getPosition();
                        break;
                    }
            }
        }

        mPagerAdapter.swapCursor(cursor);
        mViewPager.setCurrentItem(mPosition, false);
        cursor.moveToPosition(mPosition);
        Host host = SiteSession.getHostById(cursor.getInt(PostsTable.INDEX_POST_HOST_ID));
        Post post = Post.fromCursor(host, cursor, null);
        mInfoText.setText(post.describeContent(PostDetailActivity.this));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        mPagerAdapter.swapCursor(null);
    }

    private boolean mIsAutoplaying = false;

    @Override
    protected void onPause()
    {
        super.onPause();

        if (mIsAutoplaying)
        {
            mViewPager.removeCallbacks(mNextPageRunnable);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        setupActionBar();

        mViewPager = (ViewPager) findViewById(R.id.post_detail_pager);
        mControlsView = (LinearLayout) findViewById(R.id.post_detail_content_controls);
        mInfoText = (TextView) findViewById(R.id.post_detail_info);
        mPlayPauseButton = (ImageButton) findViewById(R.id.post_detail_button_autoplay);

        mPlayPauseButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (mIsAutoplaying = !mIsAutoplaying)
                {
                    mPlayPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                    mViewPager.postDelayed(mNextPageRunnable, DanbooruGallerySettings.getAutoplayDelay());
                    mUiHider.hide();
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else
                {
                    mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);
                    mViewPager.removeCallbacks(mNextPageRunnable);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
        mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);

        PostsTable.registerDataSetObserver(mPostsObserver);

        // FIXME: hard coded key
        if (savedInstanceState == null)
            mPosition = getIntent().getIntExtra("post_position", -1);
        else
            mPosition = savedInstanceState.getInt("post_position", -1);

        mPagerAdapter = new PostDetailPagerAdapter(getFragmentManager(), null);
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setPageMargin(16); // TODO: i'm lazy to calculate dp here...

        mViewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                if (mIsAutoplaying)
                {
                    mViewPager.removeCallbacks(mNextPageRunnable);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);
                }
                return false;
            }
        });

        mUiHider = new SystemUiHider(getWindow().getDecorView(), AUTO_HIDE_DELAY_MILLIS,
            new UiHider.OnVisibilityChangeListener()
            {
                // Cached values.
                int mShortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

                @Override
                @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                public void onVisibilityChange(boolean visible)
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
                    {
                        // If the ViewPropertyAnimator API is available
                        // (Honeycomb MR2 and later), use it to animate the
                        // in-layout UI controls at the bottom of the
                        // screen.
                        mControlsView.animate()
                            .alpha(visible ? 1.0f : 0.0f)
                            .setDuration(mShortAnimTime);
                    } else {
                        // If the ViewPropertyAnimator APIs aren't
                        // available, simply show or hide the in-layout UI
                        // controls.
                        mControlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                    }

                    if (visible)
                        getActionBar().show();
                    else
                        getActionBar().hide();
                }
            });

        getLoaderManager().initLoader(R.id.loader_post_ids, null, this);
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        mUiHider.show();
        mUiHider.delayedHide(AUTO_HIDE_DELAY_MILLIS);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().addOnMenuVisibilityListener(new ActionBar.OnMenuVisibilityListener()
            {
                @Override
                public void onMenuVisibilityChanged(boolean visible)
                {
                    if (visible)
                    {
                        mUiHider.setAutoHideDelay(UiHider.AUTO_HIDE_DELAY_DISABLED);
                        mUiHider.show();
                    } else
                    {
                        mUiHider.setAutoHideDelay(AUTO_HIDE_DELAY_MILLIS);
                        mUiHider.delayedHide();
                    }
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt("post_position", mViewPager.getCurrentItem());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        mViewPager.setCurrentItem(savedInstanceState.getInt("post_position", 0), false);
    }

    @Override
    public void onBackPressed()
    {
        // super.onBackPressed();
        // This ID represents the Home or Up button. In the case of this
        // activity, the Up button is shown. Use NavUtils to allow users
        // to navigate up one level in the application structure. For
        // more details, see the Navigation pattern on Android Design:
        //
        // http://developer.android.com/design/patterns/navigation.html#up-vs-back
        //
        Intent intent = new Intent(this, PostListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("post_position", mViewPager.getCurrentItem());
        navigateUpTo(intent);
    }

    @Override
    public void onImageClick()
    {
        mUiHider.toggle();
        if (mIsAutoplaying)
        {
            mViewPager.removeCallbacks(mNextPageRunnable);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }
}
