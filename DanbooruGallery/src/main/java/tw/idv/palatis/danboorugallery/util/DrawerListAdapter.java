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

package tw.idv.palatis.danboorugallery.util;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.WrapperListAdapter;

import java.util.ArrayList;
import java.util.List;

import tw.idv.palatis.danboorugallery.R;

public class DrawerListAdapter
    implements WrapperListAdapter
{
    private final LayoutInflater mInflater;
    private List<DrawerItem> mDrawerItems = new ArrayList<>();

    public DrawerListAdapter(Context context, ListAdapter adapter)
    {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // new hosts
        mDrawerItems.add(new DrawerItem(
                context,
                R.string.drawer_section_header_hosts, R.string.drawer_section_header_hosts_description,
                -1, -1, DrawerItem.TYPE_SECTION_HEADER));
        // list of hosts here
        mDrawerItems.add(new DrawerItem(
                context,
                R.string.drawer_normal_item_new_host, R.string.drawer_normal_item_new_host_description,
                android.R.drawable.ic_menu_add, R.id.action_new_host, DrawerItem.TYPE_NORMAL_ITEM));

        // about
        mDrawerItems.add(new DrawerItem(
                context,
                R.string.drawer_section_header_about, R.string.drawer_section_header_about_description,
                -1, -1, DrawerItem.TYPE_SECTION_HEADER));
        mDrawerItems.add(new DrawerItem(
                context,
                R.string.drawer_normal_item_copyright, R.string.drawer_normal_item_copyright_description,
                -1, -1, DrawerItem.TYPE_NORMAL_ITEM));

        setWrappedAdapter(adapter);
    }

    ListAdapter mWrappedAdapter;
    protected List<DataSetObserver> mObservers = new ArrayList<>();

    public void setWrappedAdapter(ListAdapter adapter)
    {
        for (DataSetObserver observer : mObservers)
        {
            mWrappedAdapter.unregisterDataSetObserver(observer);
            adapter.registerDataSetObserver(observer);
        }

        mWrappedAdapter = adapter;

        synchronized(mObservers)
        {
            // since onInvalidated() is implemented by the app, it could do anything, including
            // removing itself from {@link mObservers} - and that could cause problems if
            // an iterator is used on the ArrayList {@link mObservers}.
            // to avoid such problems, just march thru the list in the reverse order.
            for (int i = mObservers.size() - 1; i >= 0; i--)
                mObservers.get(i).onChanged();
        }
    }

    @Override
    public ListAdapter getWrappedAdapter()
    {
        return mWrappedAdapter;
    }

    @Override
    public boolean areAllItemsEnabled()
    {
        return false;
    }

    @Override
    public boolean isEnabled(int position)
    {
        Object object = getItem(position);

        if (!DrawerItem.class.isAssignableFrom(object.getClass()))
            return mWrappedAdapter.isEnabled(position - 1);

        DrawerItem item = (DrawerItem) object;
        return item.id != -1;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer)
    {
        mWrappedAdapter.registerDataSetObserver(observer);
        mObservers.add(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer)
    {
        mWrappedAdapter.unregisterDataSetObserver(observer);
        mObservers.remove(observer);
    }

    @Override
    public int getCount()
    {
        return mWrappedAdapter.getCount() + mDrawerItems.size();
    }

    @Override
    public Object getItem(int position)
    {
        if (position == 0)
            return mDrawerItems.get(0);

        if (position <= mWrappedAdapter.getCount())
            return mWrappedAdapter.getItem(position - 1);

        return mDrawerItems.get(position - mWrappedAdapter.getCount());
    }

    @Override
    public long getItemId(int position)
    {
        if (position == 0)
            return mDrawerItems.get(0).id;

        if (position <= mWrappedAdapter.getCount())
            return mWrappedAdapter.getItemId(position - 1);

        return mDrawerItems.get(position - mWrappedAdapter.getCount()).id;
    }

    @Override
    public boolean hasStableIds()
    {
        return false;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup)
    {
        if (position == 0)
            return getView(mDrawerItems.get(0), view, viewGroup);

        if (position <= mWrappedAdapter.getCount())
            return mWrappedAdapter.getView(position - 1, view, viewGroup);

        return getView(mDrawerItems.get(position - mWrappedAdapter.getCount()), view, viewGroup);
    }

    private static class ViewHolder
    {
        ImageView icon;
        TextView title;
        TextView summary;
        int type;

        public ViewHolder(View view, int type)
        {
            this.icon = (ImageView) view.findViewById(android.R.id.icon);
            this.title = (TextView) view.findViewById(android.R.id.title);
            this.summary = (TextView) view.findViewById(android.R.id.summary);
            this.type = type;
        }
    }

    private View getView(DrawerItem item, View view, ViewGroup viewGroup)
    {
        if (view == null)
        {
            switch (item.type)
            {
                case DrawerItem.TYPE_NORMAL_ITEM:
                    view = mInflater.inflate(R.layout.listitem_drawer_normal_item, viewGroup, false);
                    break;
                case DrawerItem.TYPE_SECTION_HEADER:
                    view = mInflater.inflate(R.layout.listitem_drawer_section_header, viewGroup, false);
                    break;
            }
            view.setTag(R.id.view_tag_view_holder, new ViewHolder(view, item.type));
        }
        ViewHolder holder = (ViewHolder)view.getTag(R.id.view_tag_view_holder);

        holder.icon.setImageDrawable(item.icon);
        holder.icon.setVisibility(item.icon == null ? View.GONE : View.VISIBLE);
        holder.title.setText(item.title);
        holder.summary.setVisibility(item.summary.isEmpty() ? View.GONE : View.VISIBLE);
        holder.summary.setText(item.summary);

        return view;
    }

    @Override
    public int getItemViewType(int position)
    {
        if (position == 0)
            return mWrappedAdapter.getViewTypeCount() + mDrawerItems.get(0).type;

        if (position <= mWrappedAdapter.getCount())
            return mWrappedAdapter.getItemViewType(position - 1);

        return mWrappedAdapter.getViewTypeCount() + mDrawerItems.get(position - mWrappedAdapter.getCount()).type;
    }

    @Override
    public int getViewTypeCount()
    {
        return mWrappedAdapter.getViewTypeCount() + 2;
    }

    @Override
    public boolean isEmpty()
    {
        return mDrawerItems.isEmpty() && mWrappedAdapter.isEmpty();
    }

    public static class DrawerItem
    {
        public static final int TYPE_NORMAL_ITEM = 0x00;
        public static final int TYPE_SECTION_HEADER = 0x01;

        public int id;
        public String title;
        public String summary;
        public int type;
        public Drawable icon;

        public DrawerItem(Context context, int title_id, int summary_id, int icon_id, int id, int type)
        {
            Resources resources = context.getResources();
            if (title_id != -1)
                this.title = resources.getString(title_id);
            if (summary_id != -1)
                this.summary = resources.getString(summary_id);
            if (icon_id != -1)
                this.icon = resources.getDrawable(icon_id);
            this.id = id;
            this.type = type;
        }
    }
}
