package tw.idv.palatis.danboorugallery.model;

import org.json.JSONException;
import org.json.JSONObject;

import tw.idv.palatis.danboorugallery.defines.D;
import android.os.Parcel;
import android.os.Parcelable;

public class Host
	implements Parcelable
{
	public String	name;
	public String	url;
	public String	api;

	public Host()
	{
		this( "", "", "Danbooru - XML" );
	}

	public Host(String n, String u, String a)
	{
		name = n;
		url = u;
		api = a;
	}

	public Host(JSONObject json)
	{
		name = json.optString( "name" );
		url = json.optString( "url" );
		api = json.optString( "api" );
	}

	public JSONObject toJSONObject()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put( "name", name );
			json.put( "url", url );
			json.put( "api", api );
			return json;
		}
		catch (JSONException ex)
		{
			D.Log.wtf( ex );
		}
		return null;
	}

	// parcelable related
	public static final Parcelable.Creator < Host >	CREATOR	= new HostParcelableCreater();

	private static class HostParcelableCreater
		implements Parcelable.Creator < Host >
	{
		@Override
		public Host createFromParcel(Parcel source)
		{
			return new Host( source );
		}

		@Override
		public Host[] newArray(int size)
		{
			return new Host[size];
		}
	}

	private Host(Parcel parcel)
	{
		name = parcel.readString();
		url = parcel.readString();
		api = parcel.readString();
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString( name );
		dest.writeString( url );
		dest.writeString( api );
	}
}