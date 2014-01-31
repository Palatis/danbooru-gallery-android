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
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Tag;

/**
 * Created by 其威 on 2014/1/22.
 */
public class TagSearchCursorAdapter
    extends CursorAdapter
{
    private static String sHostSeparator = null;
    private static String sItemTitle = null;
    private static String sItemSummary = null;
    private LayoutInflater mInflater = null;

    public TagSearchCursorAdapter(Context context, Cursor cursor, boolean autoRequery)
    {
        super(context, cursor, autoRequery);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (sHostSeparator == null)
            sHostSeparator = context.getResources().getString(R.string.tags_search_item_host_separator);
        if (sItemTitle == null)
            sItemTitle = context.getResources().getString(R.string.tags_search_item_title);
        if (sItemSummary == null)
            sItemSummary = context.getResources().getString(R.string.tags_search_item_summary);
    }

    private static class ViewHolder
    {
        public TextView title;
        public TextView summary;

        public ViewHolder(View view)
        {
            title = (TextView) view.findViewById(android.R.id.title);
            summary = (TextView) view.findViewById(android.R.id.summary);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        View view = mInflater.inflate(R.layout.search_item, parent, false);
        view.setTag(R.id.view_tag_view_holder, new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        Tag tag = ((TagCursor)cursor).getTag();
        ViewHolder holder = (ViewHolder) view.getTag(R.id.view_tag_view_holder);

        StringBuilder sb = new StringBuilder();
        for (Host host : tag.hosts)
            sb.append(host.name).append(sHostSeparator);
        sb.delete(sb.length() - sHostSeparator.length(), sb.length());
        String hosts = sb.toString();

        holder.title.setText(String.format(sItemTitle, tag.id, tag.name, tag.post_count, hosts));
        holder.summary.setText(String.format(sItemSummary, tag.id, tag.name, tag.post_count, hosts));
    }
}
