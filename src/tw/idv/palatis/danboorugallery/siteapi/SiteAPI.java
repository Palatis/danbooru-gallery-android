package tw.idv.palatis.danboorugallery.siteapi;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;
import android.content.SharedPreferences;

public abstract class SiteAPI
{
	public static final int	API_JSON	= 0x01;
	public static final int	API_XML		= 0x02;
	public static final int	API_RSS		= 0x04;
	public static final int	API_HTML	= 0x08;	// maybe i'll just never implement one of this...

	private String			mSiteUrl;
	private int				mApi;
	private boolean			mIsCanceled	= false;

	/**
	 * get the supported (implemented) API for a site.
	 * use
	 * <code>if (site.getSupportedApi() & ISiteAPI.API_JSON) { ... }</code>
	 * to check if a specific API is supported.
	 *
	 * @return supported API, either API_JSON, API_XML, API_RSS, ATI_HTML, or any combinations of them.
	 */
	public abstract int getSupportedApi();

	/**
	 * get the posts, just like when you're browsing the post list page.
	 *
	 * @param page
	 *            the page to retrieve, should be an integer >= 1.
	 * @param tags
	 *            tags to limit the search. can't be null, but can ba an empty string.
	 * @param limit
	 *            posts per page to retrieve at once.
	 * @return the posts fetched, null if failed.
	 */
	public abstract List < Post > fetchPostsIndex(int page, String tags, int limit);

	/**
	 * get the tags, just like when you're browsing the tag list page
	 *
	 * @param page
	 *            the page to retrieve, should be an integer >= 1
	 * @param name
	 *            name of the tags, can have wildcard characters to do a search
	 * @param limit
	 *            tags shown per page
	 * @return the tags fetched, null if failed.
	 */
	public abstract List < Tag > fetchTagsIndex(int page, String name, int limit);

	/**
	 * get the default API for this implementation
	 *
	 * @return the default API
	 */
	protected abstract int getDefaultApi();

	public SiteAPI()
	{
		this( "" );
	}

	public SiteAPI(String siteUrl)
	{
		mSiteUrl = siteUrl;
		mApi = getDefaultApi();
	}

	public SiteAPI(String siteUrl, int api) throws UnsupportedAPIException
	{
		mSiteUrl = siteUrl;
		setApi( api );
	}

	/**
	 * choose the API to use for this site.
	 *
	 * @param api
	 *            the API to use, can be API_JSON or API_XML.
	 * @throws UnsupportedAPIException
	 */
	public final void setApi(int api) throws UnsupportedAPIException
	{
		if ((api & getSupportedApi()) == 0)
			throw new UnsupportedAPIException( api );
		mApi = api;
	}

	/**
	 * get the selected API
	 *
	 * @return the API which is using, can be API_JSON or API_XML.
	 */
	public final int getApi()
	{
		return mApi;
	}

	/**
	 * get the site's URL
	 *
	 * @return the site's URL
	 */
	public final String getSiteUrl()
	{
		return mSiteUrl;
	}

	/**
	 * set the site's URL
	 *
	 * @param siteUrl
	 *            the URL to set
	 */
	public final void setSiteUrl(String siteUrl)
	{
		mSiteUrl = siteUrl;
	}

	/**
	 * cancels the operation in action, it should cancel the operation as soon
	 * as possible.
	 */
	public void cancel()
	{
		mIsCanceled = true;
	}

	/**
	 * uncancel the cancel status
	 */
	protected void uncancel()
	{
		mIsCanceled = false;
	}

	/**
	 * check if the fetch operation is canceled.
	 *
	 * @return true if canceled, false otherwise.
	 */
	public boolean isCanceled()
	{
		return mIsCanceled;
	}

	private static SiteAPI	instance	= null;

	public static SiteAPI getInstance()
	{
		return instance;
	}

	/**
	 * read API from SharedPreferences.
	 *
	 * @param preferences
	 * @return {@literal true} if the current instance of API is changed, {@literal false} otherwise.
	 */
	public static boolean readPreference(SharedPreferences preferences)
	{
		try
		{
			List < Host > hosts = D.HostsFromJSONArray( new JSONArray( preferences.getString( "json_hosts", "" ) ) );
			SiteAPI api = _createFromHost( hosts.get( preferences.getInt( "selected_host", 0 ) ) );
			if (instance == null)
			{
				instance = api;
				return true;
			}

			boolean same = true;
			same &= instance.getClass() == api.getClass();
			same &= instance.getSiteUrl().equals( api.getSiteUrl() );
			same &= instance.getApi() == api.getApi();

			if ( !same)
				instance = api;

			return same;
		}
		catch (JSONException ex)
		{
			D.Log.wtf( ex );
		}
		return false;
	}

	/**
	 * construct the correct API object from Host
	 *
	 * @param host
	 *            the host
	 * @return the *API object
	 */
	private static SiteAPI _createFromHost(Host host)
	{
		if (host == null)
			return null;

		try
		{
			if (host.api.equals( "Danbooru - JSON" ))
				return new DanbooruAPI( host.url, API_JSON );
			if (host.api.equals( "Danbooru - XML" ))
				return new DanbooruAPI( host.url, API_XML );
			if (host.api.equals( "Gelbooru - XML" ))
				return new GelbooruAPI( host.url, API_XML );
			if (host.api.equals( "Shimmie2 - XML" ))
				return new ShimmieAPI( host.url, API_XML );
			if (host.api.equals( "Shimmie2 - RSS" ))
				return new ShimmieAPI( host.url, API_RSS );
		}
		catch (UnsupportedAPIException ex)
		{
			D.Log.wtf( ex );
		}
		return null;
	}

	/**
	 * this exception is thrown when the desired API is not supported.
	 *
	 * @author palatis
	 */
	public static class UnsupportedAPIException
		extends Exception
	{
		private static final long	serialVersionUID	= 1L;

		private static String _apiToString(int api)
		{
			if (api == API_JSON)
				return "API_JSON";
			if (api == API_XML)
				return "API_XML";
			if (api == API_RSS)
				return "API_RSS";
			if (api == API_HTML)
				return "API_HTML";
			return "Unknown API (" + api + ")";
		}

		private int	mUnsupportedApi;

		UnsupportedAPIException(int api)
		{
			super( "Unsupported API: " + _apiToString( api ) );
			mUnsupportedApi = api;
		}

		public int getUnsupportedAPI()
		{
			return mUnsupportedApi;
		}
	}
}