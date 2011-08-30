package tw.idv.palatis.danboorugallery.siteapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;
import android.util.Log;

public class DanbooruAPI
	implements ISiteAPI
{
	public static final String	URL_POSTS_JSON	= "/post/index.json?page=%1$s&tags=%2$s&limit=%3$s";
	public static final String	URL_TAGS_JSON	= "/tag/index.json?order=count&page=%1$s&name=*%2$s*&limit=%3$s";

	private static final int	_BUFFER_SIZE	= 8192;

	String						mSiteUrl;
	int							mApi;
	boolean						mIsCanceled;

	public DanbooruAPI()
	{
		this( "" );
	}

	public DanbooruAPI(String siteUrl)
	{
		mSiteUrl = siteUrl;
		mApi = API_JSON;
	}

	public DanbooruAPI(String siteUrl, int api) throws UnsupportedAPIException
	{
		mSiteUrl = siteUrl;
		setApi( api );
	}

	@Override
	public int getApi()
	{
		return mApi;
	}

	@Override
	public void setApi(int api) throws UnsupportedAPIException
	{
		if (api != API_JSON)
			throw new UnsupportedAPIException( api );

		mApi = api;
	}

	@Override
	public int getSupportedApi()
	{
		return API_JSON;
	}

	public String getSiteUrl()
	{
		return mSiteUrl;
	}

	public void setSiteUrl(String siteUrl)
	{
		mSiteUrl = siteUrl;
	}

	@Override
	public void cancel()
	{
		mIsCanceled = true;
	}

	@Override
	public List < Post > fetchPostsIndex(int page, String tags, int limit)
	{
		if (mApi == API_JSON)
			return fetchPostsIndexJSON( page, tags, limit );
		if (mApi == API_XML)
			return fetchPostsIndexXML( page, tags, limit );
		return null;
	}

	@Override
	public List < Tag > fetchTagsIndex(int page, String name, int limit)
	{
		if (mApi == API_JSON)
			return fetchTagsIndexJSON( page, name, limit );
		if (mApi == API_XML)
			return fetchTagsIndexXML( page, name, limit );
		return null;
	}

	private List < Post > fetchPostsIndexJSON(int page, String tags, int limit)
	{
		mIsCanceled = false;

		URL fetchUrl = null;
		try
		{
			fetchUrl = new URL( String.format( mSiteUrl + URL_POSTS_JSON, page, tags, limit ) );
		}
		catch (MalformedURLException e)
		{
			return null;
		}

		Log.v( D.LOGTAG, "DanbooruAPI::fetchPostsIndex(): fetching " + fetchUrl.toExternalForm() );

		if (mIsCanceled)
			return null;

		Reader input;
		try
		{
			input = new BufferedReader( new InputStreamReader( fetchUrl.openStream(), "UTF-8" ) );

			Writer output = new StringWriter();

			int count = 0;
			char buffer[] = new char[_BUFFER_SIZE];
			while ((count = input.read( buffer )) > 0)
			{
				output.write( buffer, 0, count );
				if (mIsCanceled)
					return null;
			}

			JSONArray json_posts = new JSONArray( output.toString() );
			int len = json_posts.length();
			ArrayList < Post > posts = new ArrayList < Post >( len );
			for (int j = 0; j < len; ++j)
				try
				{
					JSONObject json_post = json_posts.getJSONObject( j );
					posts.add( new Post( json_post ) );
					if (mIsCanceled)
						return null;
				}
				catch (JSONException ex)
				{
					return null;
				}
			return posts;
		}
		catch (UnsupportedEncodingException e)
		{
		}
		catch (IOException e)
		{
		}
		catch (JSONException e)
		{
		}
		return null;
	}

	private List < Post > fetchPostsIndexXML(int page, String tags, int limit)
	{
		return null;
	}

	private List < Tag > fetchTagsIndexJSON(int page, String name, int limit)
	{
		TreeSet < Tag > ret = new TreeSet < Tag >( new Tag.CompareById() );
		String keywords[] = new HashSet < String >( Arrays.asList( name.split( "\\+" ) ) ).toArray( new String[] { } );

		char buffer[] = new char[8192];
		for (String keyword : keywords)
		{
			// trim leading and trailing whitespace, then replace spaces
			// with underscores.
			// because typing underscore with soft-keyboard is a pain in the
			// ass...
			keyword = keyword.trim().replace( ' ', '_' );

			// get data from network...
			try
			{
				URL fetchUrl = new URL( String.format( mSiteUrl + URL_TAGS_JSON, page, keyword, limit ) );
				Log.d( D.LOGTAG, "query for tags: " + fetchUrl.toExternalForm() );

				Reader input = new BufferedReader( new InputStreamReader( fetchUrl.openStream(), "UTF-8" ) );
				Writer output = new StringWriter();

				int count = 0;
				while ((count = input.read( buffer )) > 0)
					output.write( buffer, 0, count );

				JSONArray array = new JSONArray( output.toString() );
				int length = array.length();
				for (int i = 0; i < length; ++i)
					try
					{
						ret.add( new Tag( array.getJSONObject( i ) ) );
					}
					catch (JSONException e)
					{
					}
			}
			catch (UnsupportedEncodingException e)
			{
			}
			catch (IOException e)
			{
			}
			catch (JSONException e)
			{
			}
		}

		return Arrays.asList( ret.toArray( new Tag[] { } ) );
	}

	private List < Tag > fetchTagsIndexXML(int page, String name, int limit)
	{
		return null;
	}
}