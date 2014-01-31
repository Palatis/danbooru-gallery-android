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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersSimpleAdapter;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import tw.idv.palatis.danboorugallery.DanbooruGallerySettings;
import tw.idv.palatis.danboorugallery.R;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.picasso.Picasso;
import tw.idv.palatis.danboorugallery.android.view.SquaredImageView;

public class PostListAdapter
    extends CursorAdapter
    implements StickyGridHeadersSimpleAdapter
{
    private static final String TAG = "PostListAdapter";

    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance();
    private int mTimeZoneOffset = TimeZone.getDefault().getRawOffset();

    private LayoutInflater mInflater;
    private Resources mResources;
    private boolean mShowTitle1;
    private boolean mShowTitle2;
    private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener =
        new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
                if (key.equals(DanbooruGallerySettings.KEY_PREF_SHOW_POST_ID))
                    mShowTitle1 = DanbooruGallerySettings.getShowPostId();
                if (key.equals(DanbooruGallerySettings.KEY_PREF_SHOW_IMAGE_RESOLUTION))
                    mShowTitle2 = DanbooruGallerySettings.getShowImageResolution();
                notifyDataSetChanged();
            }
        };

    public PostListAdapter(Context context)
    {
        super(context, null, true);
        DanbooruGallerySettings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResources = context.getResources();
        mShowTitle1 = DanbooruGallerySettings.getShowPostId();
        mShowTitle2 = DanbooruGallerySettings.getShowImageResolution();
    }

    @Override
    public long getHeaderId(int position)
    {
        Cursor cursor = (Cursor) getItem(position);
        long created_at = cursor.getLong(INDEX_POST_CREATED_AT);
        return (created_at + mTimeZoneOffset) / MSEC_PER_DAY;
    }

    private static class HeaderViewHolder
    {
        public TextView title;
        public TextView summary;

        public HeaderViewHolder(View view)
        {
            title = (TextView) view.findViewById(android.R.id.title);
            summary = (TextView) view.findViewById(android.R.id.summary);
        }
    }

    private static final long MSEC_PER_DAY = 86400 * 1000;
    @Override
    public View getHeaderView(int position, View view, ViewGroup parent)
    {
        if (view == null)
        {
            view = mInflater.inflate(R.layout.listitem_post_list_header, parent, false);
            view.setTag(R.id.view_tag_view_holder, new HeaderViewHolder(view));
        }

        HeaderViewHolder holder = (HeaderViewHolder) view.getTag(R.id.view_tag_view_holder);

        Cursor cursor = (Cursor) getItem(position);
        long created_at = cursor.getLong(INDEX_POST_CREATED_AT);

        Date date = new Date(created_at);
        holder.title.setText(DATE_FORMAT.format(date));

        return view;
    }

    private static class ViewHolder
    {
        public SquaredImageView thumbnail;
        public TextView title1;
        public TextView title2;
        public ProgressBar progress;
        public Callback callback;

        public ViewHolder(View view)
        {
            thumbnail = (SquaredImageView)view.findViewById(R.id.item_thumbnail);
            title1 = (TextView)view.findViewById(R.id.item_title1);
            title2 = (TextView)view.findViewById(R.id.item_title2);
            progress = (ProgressBar)view.findViewById(R.id.item_progress);
            callback = new Callback()
            {
                @Override
                public void onSuccess()
                {
                    progress.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onError()
                {
                    progress.setVisibility(View.INVISIBLE);
                    thumbnail.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }
            };
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        View view = mInflater.inflate(R.layout.listitem_post_list, parent, false);
        view.setTag(R.id.view_tag_view_holder, new ViewHolder(view));
        return view;
    }

    public static int INDEX_POST_DATABASE_ID = 0;
    public static int INDEX_POST_ID = 1;
    public static int INDEX_POST_PREVIEW_FILE_URL = 2;
    public static int INDEX_POST_IMAGE_WIDTH = 3;
    public static int INDEX_POST_IMAGE_HEIGHT = 4;
    public static int INDEX_POST_CREATED_AT = 5;
    public static final String[] POST_COLUMNS = new String[] {
        Post.KEY_POST_DATABASE_ID,
        Post.KEY_POST_ID,
        Post.KEY_POST_PREVIEW_FILE_URL,
        Post.KEY_POST_IMAGE_WIDTH,
        Post.KEY_POST_IMAGE_HEIGHT,
        Post.KEY_POST_CREATED_AT,
    };

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        ViewHolder holder = (ViewHolder) view.getTag(R.id.view_tag_view_holder);
        holder.progress.setVisibility(View.VISIBLE);
        Picasso.withPreview()
            .load(cursor.getString(INDEX_POST_PREVIEW_FILE_URL))
            .error(android.R.drawable.ic_delete)
            .into(holder.thumbnail, holder.callback);
        holder.thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (mShowTitle1)
        {
            holder.title1.setText(mResources.getString(
                R.string.gallery_grid_item_title1,
                cursor.getInt(INDEX_POST_ID)));
            holder.title1.setVisibility(View.VISIBLE);
        }
        else
        {
            holder.title1.setVisibility(View.GONE);
            holder.title1.setText(null);
        }

        if (mShowTitle2)
        {
            holder.title2.setText(mResources.getString(
                R.string.gallery_grid_item_title2,
                cursor.getInt(INDEX_POST_IMAGE_WIDTH), cursor.getInt(INDEX_POST_IMAGE_HEIGHT)));
            holder.title2.setVisibility(View.VISIBLE);
        }
        else
        {
            holder.title2.setVisibility(View.GONE);
            holder.title2.setText(null);
        }
    }
}
