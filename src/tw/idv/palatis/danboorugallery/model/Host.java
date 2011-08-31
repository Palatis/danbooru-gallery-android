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

	public Host()
	{
		name = "";
		url = "";
	}

	public Host(String n, String u)
	{
		name = n;
		url = u;
	}

	public Host(JSONObject json)
	{
		name = json.optString( "name" );
		url = json.optString( "url" );
	}

	public JSONObject toJSONObject()
	{
		try
		{
			JSONObject json = new JSONObject();
			json.put( "name", name );
			json.put( "url", url);
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
	}
}