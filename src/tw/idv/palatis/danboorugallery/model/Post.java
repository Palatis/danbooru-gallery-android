package tw.idv.palatis.danboorugallery.model;

/*
 * This file is part of Danbooru Gallery
 *
 * Copyright 2011
 *   - Victor Tseng <palatis@gmail.com>
 *
 * Danbooru Gallery is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Danbooru Gallery is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Danbooru Gallery.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This is the data structure used to represent a post.
 * for example (json):
 * {
 * "score":0,"status":"pending","has_children":false,"sample_width":1126,"has_notes":false,
 * "height":1600,"parent_id":null,"rating":"s","preview_width":105,"sample_height":1600,
 * "sample_url":"http://hijiribe.donmai.us/data/c012c65a3da3b141e453e5c47f1be28d.jpg",
 * "preview_height":150,"creator_id":109291,"created_at":{"n":3685000,"json_class":"Time","s":1314634121},
 * "preview_url":"/ssd/data/preview/c012c65a3da3b141e453e5c47f1be28d.jpg",
 * "md5":"c012c65a3da3b141e453e5c47f1be28d","source":"","change":4579734,"author":"DocAstaroth",
 * "tags":"chihiro_(kemonomichi) comic monochrome touhou translation_request",
 * "width":1126,"file_size":463211,
 * "file_url":"http://hijiribe.donmai.us/data/c012c65a3da3b141e453e5c47f1be28d.jpg",
 * "id":984275,"has_comments":false
 * }
 * or (xml):
 * <post score="0" sample_width="532" has_children="false" status="active"
 * preview_width="107" rating="s" parent_id="" height="745" has_notes="false"
 * sample_url="http://hijiribe.donmai.us/data/7241c10a977ebf3ab033212a8aa22285.jpg"
 * sample_height="745" created_at="Mon Aug 29 12:01:53 -0400 2011"
 * creator_id="136247" preview_height="150" change="4579722"
 * source="http://img09.pixiv.net/img/kumadano/21272789_big_p3.jpg"
 * md5="7241c10a977ebf3ab033212a8aa22285"
 * preview_url="/ssd/data/preview/7241c10a977ebf3ab033212a8aa22285.jpg"
 * tags="bracelet brown_hair dress grey_hair headphones jewelry kumadano
 * ritual_baton solo sword touhou toyosatomimi_no_miko weapon"
 * author="saizo0070" file_size="197349" width="532" id="984268"
 * file_url="http://hijiribe.donmai.us/data/7241c10a977ebf3ab033212a8aa22285.jpg"
 * has_comments="false"
 * />
 *
 * @author palatis
 */
public class Post
	implements Parcelable
{
	public int		id;
	public int		parent_id;
	public int		creator_id;
	public int		change;
	public int		score;
	public String	status;
	public String	rating;
	public String	tags;
	public String	source;
	public String	author;
	public Date		created_at;
	public boolean	has_notes;
	public boolean	has_children;
	public boolean	has_comments;

	public String	md5;
	public String	file_url;
	public int		file_size;
	public int		width;
	public int		height;
	public String	sample_url;
	public int		sample_width;
	public int		sample_height;
	public String	preview_url;
	public int		preview_width;
	public int		preview_height;

	public Post()
	{
	}

	public Post(JSONObject json)
	{
		id = json.optInt( "id" );
		parent_id = json.optInt( "parent_id", -1 );
		creator_id = json.optInt( "creater_id" );
		change = json.optInt( "creater_id" );
		score = json.optInt( "score" );
		status = json.optString( "status" );
		rating = json.optString( "rating" );
		tags = json.optString( "tags" );
		source = json.optString( "source" );
		author = json.optString( "author" );

		// date is a little tricky, we might get 2 formats...
		// we might get a JSONObject with json_class="Time",
		// or just a long to represent the timestamp.
		try
		{
			// it might just be an long value
			created_at = new Date( json.getLong( "created_at" ) * 1000 );
		}
		catch (JSONException ex)
		{
			try
			{
				// or it might be a JSONObject with json_class == "Time"
				JSONObject json_created_at = json.getJSONObject( "created_at" );
				if (json_created_at.getString( "json_class" ).equals( "Time" ))
					created_at = new Date( json_created_at.getLong( "s" ) * 1000 + json_created_at.getLong( "n" ) / 1000000 );
			}
			catch (JSONException ex2)
			{
				// it's okay to not having a date...
			}
		}

		has_notes = json.optBoolean( "has_notes" );
		has_children = json.optBoolean( "has_children" );
		has_comments = json.optBoolean( "has_comments" );

		md5 = json.optString( "md5" );
		file_url = json.optString( "file_url" );
		file_size = json.optInt( "file_size" );
		width = json.optInt( "width" );
		height = json.optInt( "height" );
		sample_url = json.optString( "sample_url" );
		sample_width = json.optInt( "sample_width" );
		sample_height = json.optInt( "sample_height" );
		preview_url = json.optString( "preview_url" );
		preview_width = json.optInt( "preview_width" );
		preview_height = json.optInt( "preview_height" );

		// if the preview_url is a relative url, add the host part from file_url to it.
		Uri puri = Uri.parse( preview_url );
		if (puri.getHost() == null)
		{
			Uri furi = Uri.parse( file_url );
			preview_url = furi.getScheme() + "://" + furi.getHost() + "/" + preview_url;
		}
	}

	// parcelable related
	public static final Parcelable.Creator < Post >	CREATOR	= new PostParcelableCreater();

	private static class PostParcelableCreater
		implements Parcelable.Creator < Post >
	{
		@Override
		public Post createFromParcel(Parcel source)
		{
			return new Post( source );
		}

		@Override
		public Post[] newArray(int size)
		{
			return new Post[size];
		}
	}

	private Post(Parcel parcel)
	{
		id = parcel.readInt();
		parent_id = parcel.readInt();
		creator_id = parcel.readInt();
		change = parcel.readInt();
		score = parcel.readInt();
		status = parcel.readString();
		rating = parcel.readString();
		tags = parcel.readString();
		source = parcel.readString();
		author = parcel.readString();
		created_at = new Date( parcel.readLong() );
		has_notes = parcel.readInt() != 0;
		has_children = parcel.readInt() != 0;
		has_comments = parcel.readInt() != 0;

		md5 = parcel.readString();
		file_url = parcel.readString();
		file_size = parcel.readInt();
		width = parcel.readInt();
		height = parcel.readInt();
		sample_url = parcel.readString();
		sample_width = parcel.readInt();
		sample_height = parcel.readInt();
		preview_url = parcel.readString();
		preview_width = parcel.readInt();
		preview_height = parcel.readInt();
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt( id );
		dest.writeInt( parent_id );
		dest.writeInt( creator_id );
		dest.writeInt( change );
		dest.writeInt( score );
		dest.writeString( status );
		dest.writeString( rating );
		dest.writeString( tags );
		dest.writeString( source );
		dest.writeString( author );
		dest.writeLong( created_at.getTime() );
		dest.writeInt( has_notes ? 0 : 1 );
		dest.writeInt( has_children ? 0 : 1 );
		dest.writeInt( has_comments ? 0 : 1 );

		dest.writeString( md5 );
		dest.writeString( file_url );
		dest.writeInt( file_size );
		dest.writeInt( width );
		dest.writeInt( height );
		dest.writeString( sample_url );
		dest.writeInt( sample_width );
		dest.writeInt( sample_height );
		dest.writeString( preview_url );
		dest.writeInt( preview_width );
		dest.writeInt( preview_height );
	}
}
