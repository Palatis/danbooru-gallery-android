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
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import tw.idv.palatis.danboorugallery.android.content.CustomTaskLoader;
import tw.idv.palatis.danboorugallery.android.widget.PopupMenu;
import tw.idv.palatis.danboorugallery.database.HostsTable;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.util.DrawerListAdapter;
import tw.idv.palatis.danboorugallery.util.SiteSession;
import tw.idv.palatis.danboorugallery.util.UiHider;

public class PostListActivity
    extends Activity
    implements PostListFragment.Callbacks
{
    public static final String TAG = "PostListActivity";

    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private UiHider mUiHider;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private RelativeLayout mDrawerLeft;
    private RelativeLayout mDrawerRight;
    private boolean mIsDoingSearch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // enable progress icon
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminate(true);

        setContentView(R.layout.activity_post_list);

        _setupActionBar();
        _setupDrawer();

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mUiHider = new UiHider(AUTO_HIDE_DELAY_MILLIS, new UiHider.OnVisibilityChangeListener() {
            @Override
            public void onVisibilityChange(boolean visible)
            {
                if (visible)
                    getActionBar().show();
                else
                    getActionBar().hide();
            }
        });

        if (savedInstanceState == null)
        {
            mPostListFragment = new PostListFragment();
            getFragmentManager()
                .beginTransaction()
                .replace(R.id.post_list_container, mPostListFragment)
                .commit();
        }
    }

    private void _setupActionBar()
    {
        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    private CursorAdapter mHostsAdapter = null;

    @Override
    protected void onNewIntent(final Intent intent)
    {
        super.onNewIntent(intent);
        if (intent.getAction() != null)
        {
            if (intent.getAction().equals(Intent.ACTION_VIEW))
            {
                mDrawerLayout.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (mPostListFragment == null)
                            return;

                        // FIXME: hard coded key
                        int position = intent.getIntExtra("post_position", -1);
                        if (position != -1)
                            mPostListFragment.scrollGridToPosition(position);
                    }
                });
            }
            else if (intent.getAction().equals(Intent.ACTION_SEARCH))
            {
                // FIXME: hard coded key
                SiteSession.submitFilterTags(intent.getStringExtra("tag"));
            }
        }
    }

    private void _setupDrawer()
    {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLeft = (RelativeLayout) findViewById(R.id.left_drawer);
        mDrawerRight = (RelativeLayout) findViewById(R.id.right_drawer);
        ListView hostsList = (ListView) findViewById(R.id.host_list);

        hostsList.setOnItemClickListener(new DrawerItemClickListener());

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close)
        {
            @Override
            public void onDrawerStateChanged(int newState)
            {
                super.onDrawerStateChanged(newState);

                if (newState == DrawerLayout.STATE_DRAGGING)
                    mUiHider.show();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
                super.onDrawerSlide(drawerView, slideOffset);
            }

            public void onDrawerClosed(View view)
            {
                if (!mIsDoingSearch &&
                    !mDrawerLayout.isDrawerVisible(mDrawerLeft) &&
                    !mDrawerLayout.isDrawerVisible(mDrawerRight))
                {
                    mUiHider.setAutoHideDelay(AUTO_HIDE_DELAY_MILLIS);
                    mUiHider.hide();
                }
            }

            public void onDrawerOpened(View drawerView)
            {
                mUiHider.setAutoHideDelay(UiHider.AUTO_HIDE_DELAY_DISABLED);
                mUiHider.show();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        HostsTable.registerDataSetObserver(new DataSetObserver()
        {
            @Override
            public void onChanged()
            {
                super.onChanged();
                getLoaderManager().restartLoader(R.id.loader_host_ids, null, mCursorLoaderCallbacks);
            }

            @Override
            public void onInvalidated()
            {
                super.onInvalidated();
                mHostsAdapter.swapCursor(null);
            }
        });
        mHostsAdapter = new CursorAdapter(this, null, false)
        {
            private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener =
                new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton button, boolean checked)
                    {
                        ViewHolder holder = (ViewHolder) button.getTag(R.id.view_tag_view_holder);
                        Host host = SiteSession.getHostById(holder.host_id);
                        host.enabled = checked;
                        HostsTable.addOrUpdateHost(host);
                    }
                };

            class ViewHolder
            {
                int host_id;
                TextView title;
                TextView summary;
                CheckBox toggle;

                public ViewHolder(View view, int host_id)
                {
                    this.host_id = host_id;
                    this.title = (TextView) view.findViewById(android.R.id.title);
                    this.summary = (TextView) view.findViewById(android.R.id.summary);
                    this.toggle = (CheckBox) view.findViewById(android.R.id.toggle);
                    this.toggle.setTag(R.id.view_tag_view_holder, this);
                }
            }

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent)
            {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.listitem_drawer_host_item, parent, false);
                view.setTag(R.id.view_tag_view_holder, new ViewHolder(view, cursor.getInt(HostsTable.INDEX_HOST_DATABASE_ID)));
                return view;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor)
            {
                Host host = SiteSession.getHostById(cursor.getInt(HostsTable.INDEX_HOST_DATABASE_ID));
                if (host == null)
                    return;

                ViewHolder holder = (ViewHolder)view.getTag(R.id.view_tag_view_holder);

                holder.host_id = host.id;
                holder.toggle.setChecked(host.enabled);
                holder.title.setText(host.name);
                holder.summary.setText(host.url);
                holder.toggle.setOnCheckedChangeListener(mOnCheckedChangeListener);
            }
        };
        DrawerListAdapter adapter = new DrawerListAdapter(this, mHostsAdapter);
        hostsList.setAdapter(adapter);
        hostsList.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        hostsList.setOnItemClickListener(new DrawerItemClickListener());
        hostsList.setOnItemLongClickListener(new DrawerItemLongClickListener());

        getLoaderManager().initLoader(R.id.loader_host_ids, null, mCursorLoaderCallbacks);
    }

    private LoaderManager.LoaderCallbacks<Cursor> mCursorLoaderCallbacks =
        new LoaderManager.LoaderCallbacks<Cursor>()
        {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle bundle)
            {
                return new CustomTaskLoader<Cursor>(getApplicationContext()) {
                    @Override
                    public Cursor runTaskInBackground(CancellationSignal signal)
                    {
                        return HostsTable.getAllHostsCursor();
                    }

                    @Override
                    public void cleanUp(Cursor oldCursor)
                    {
                        if (!oldCursor.isClosed())
                            oldCursor.close();
                    }
                };
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
            {
                mHostsAdapter.swapCursor(cursor);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader)
            {
                mHostsAdapter.swapCursor(null);
            }
        };

    private class DrawerItemLongClickListener
            implements ListView.OnItemLongClickListener
    {
        @Override
        public boolean onItemLongClick(final AdapterView<?> parent, View view, int position, long id)
        {
            Object item = parent.getAdapter().getItem(position);
            if (item instanceof Cursor)
            {
                final Cursor cursor = (Cursor)item;
                PopupMenu popup = new PopupMenu(PostListActivity.this, view);
                popup.inflate(R.menu.popupmenu_host_item);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem)
                    {
                        int host_id = cursor.getInt(HostsTable.INDEX_HOST_DATABASE_ID);
                        final Host host = SiteSession.getHostById(host_id);
                        int id = menuItem.getItemId();
                        switch (id)
                        {
                            case R.id.popupmenu_host_item_edit:
                                Intent intent = new Intent(PostListActivity.this, NewHostActivity.class);
                                intent.putExtra(Host.TABLE_NAME + Host.KEY_HOST_DATABASE_ID, host.id);
                                startActivity(intent);
                                return true;
                            case R.id.popupmenu_host_item_delete:
                                AlertDialog.Builder b = new AlertDialog.Builder(PostListActivity.this);
                                b.setTitle(R.string.dialog_delete_host_title);
                                b.setMessage(getResources().getString(
                                    R.string.dialog_delete_host_message,
                                    host.name, host.url, host.getLogin(), host.getAPI().getName()));
                                b.setIcon(android.R.drawable.ic_dialog_alert);
                                b.setNegativeButton(android.R.string.cancel, null);
                                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i)
                                    {
                                        HostsTable.removeHost(host);
                                    }
                                });
                                b.create().show();
                                return true;
                        }
                        return false;
                    }
                });
                popup.setForceShowIcon(true);
                popup.show();
                return true;
            }
            return false;
        }
    }

    private class DrawerItemClickListener
        implements ListView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            Object rawitem = parent.getAdapter().getItem(position);
            if (rawitem.getClass().equals(DrawerListAdapter.DrawerItem.class))
            {
                DrawerListAdapter.DrawerItem item = (DrawerListAdapter.DrawerItem)rawitem;
                switch (item.id)
                {
                    case R.id.action_new_host:
                        startActivity(new Intent(parent.getContext(), NewHostActivity.class));
                        break;
                }
                mDrawerLayout.closeDrawer(mDrawerLeft);
            }
            else if (rawitem instanceof Cursor)
            {
                Cursor cursor = (Cursor) rawitem;
                int host_id = cursor.getInt(HostsTable.INDEX_HOST_DATABASE_ID);
                Host host = SiteSession.getHostById(host_id);
                host.enabled = !host.enabled;
                HostsTable.addOrUpdateHost(host);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_post_list_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private PostListFragment mPostListFragment = null;

    @Override
    public boolean onSearchRequested()
    {
        return !mPostListFragment.onSearchRequested() && super.onSearchRequested();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;

        int id = item.getItemId();
        switch (id)
        {
            case R.id.menu_post_list_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_post_list_filters:
                if (mDrawerLayout.isDrawerOpen(mDrawerRight))
                    mDrawerLayout.closeDrawer(mDrawerRight);
                else
                    mDrawerLayout.openDrawer(mDrawerRight);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();

        if (HostsTable.hasHost())
        {
            // Trigger the initial hide() shortly after the activity has been
            // created, to briefly hint to the user that UI controls
            // are available.
            mUiHider.delayedHide(100);
        }
        else
        {
            Log.d(TAG, "onPostCreate(): no host, disable autohide.");
            getActionBar().show();
            mDrawerLayout.openDrawer(mDrawerLeft);
            mUiHider.setAutoHideDelay(UiHider.AUTO_HIDE_DELAY_DISABLED);
            mUiHider.show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public int getPreferredNumColumns()
    {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT
            ? DanbooruGallerySettings.getColumnsPortrait()
            : DanbooruGallerySettings.getColumnsLandscape();
    }

    @Override
    public boolean onSearchViewExpand()
    {
        mIsDoingSearch = true;
        mUiHider.setAutoHideDelay(UiHider.AUTO_HIDE_DELAY_DISABLED);
        mUiHider.show();
        mDrawerLayout.closeDrawer(mDrawerLeft);
        mDrawerLayout.closeDrawer(mDrawerRight);
        return true;
    }

    @Override
    public boolean onSearchViewCollapse()
    {
        mIsDoingSearch = false;
        mUiHider.setAutoHideDelay(AUTO_HIDE_DELAY_MILLIS);
        mUiHider.hide();
        return true;
    }
}