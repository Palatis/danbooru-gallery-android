package tw.idv.palatis.danboorugallery.siteapi;

import java.util.List;

import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

public interface ISiteAPI
{
	public static final int	API_JSON	= 0x01;
	public static final int	API_XML		= 0x02;

	/**
	 * get the supported (implemented) API for a site.
	 *
	 * @return supported API, either API_JSON, API_XML, or API_JSON | API_XML.
	 */
	abstract int supportedApi();

	/**
	 * choose the API to use for this site.
	 *
	 * @param api
	 *            the api to use, can be API_JSON or API_XML.
	 * @return true on success, false otherwise.
	 */
	abstract boolean selectApi(int api);

	/**
	 * get the posts, just like when you're browsing the post list page.
	 *
	 * @param page
	 *            the page to retrieve, should be an integer >= 1.
	 * @param tags
	 *            tags to limit the search. can't be null, but can ba an empty string.
	 * @param limit
	 *            posts per page to retrieve at once.
	 * @return the posts fetched
	 */
	abstract List < Post > fetchPostsIndex(int page, String tags, int limit);

	/**
	 * get the tags, just like when you're browsing the tag list page
	 *
	 * @param page
	 *            the page to retrieve, should be an integer >= 1
	 * @param name
	 *            name of the tags, can have wildcard characters to do a search
	 * @param limit
	 *            tags shown per page
	 * @return
	 */
	abstract List < Tag > fetchTagsIndex(int page, String name, int limit);

	/**
	 * cancels the operation in action, it should cancel the operation as soon
	 * as possible.
	 */
	abstract void cancel();

	/**
	 * this exception is thrown when the desired API is not supported.
	 *
	 * @author palatis
	 */
	public class UnsupportedAPIException
		extends Exception
	{
		private static final long	serialVersionUID	= 1L;

		private static String _apiToString(int api)
		{
			if (api == API_JSON)
				return "API_JSON";
			if (api == API_XML)
				return "API_XML";
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