package tw.idv.palatis.danboorugallery.siteapi;

import java.util.List;

import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

public class DanbooruAPI
	extends DanbooruStyleAPI
	implements ISiteAPI
{
	public static final String	URL_POSTS_JSON	= "/post/index.json?page=%1$s&tags=%2$s&limit=%3$s";
	public static final String	URL_TAGS_JSON	= "/tag/index.json?order=count&page=%1$s&name=*%2$s*&limit=%3$s";
	public static final String	URL_POSTS_XML	= "/post/index.xml?page=%1$s&tags=%2$s&limit=%3$s";
	public static final String	URL_TAGS_XML	= "/tag/index.xml?order=count&page=%1$s&name=*%2$s*&limit=%3$s";

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
		mApi = API_XML;
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
		if (api != API_JSON && api != API_XML)
			throw new UnsupportedAPIException( api );

		mApi = api;
	}

	@Override
	public int getSupportedApi()
	{
		return API_JSON;
	}

	@Override
	public String getSiteUrl()
	{
		return mSiteUrl;
	}

	@Override
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
	protected boolean isCanceled()
	{
		return mIsCanceled;
	}

	@Override
	public List < Post > fetchPostsIndex(int page, String tags, int limit)
	{
		if (mApi == API_JSON)
		{
			mIsCanceled = false;
			return fetchPostsIndexJSON( mSiteUrl + URL_POSTS_JSON, page, tags, limit );
		}

		if (mApi == API_XML)
		{
			mIsCanceled = false;
			return fetchPostsIndexXML( mSiteUrl + URL_POSTS_XML, page, tags, limit );
		}

		return null;
	}

	@Override
	public List < Tag > fetchTagsIndex(int page, String name, int limit)
	{
		if (mApi == API_JSON)
		{
			mIsCanceled = false;
			return fetchTagsIndexJSON( mSiteUrl + URL_TAGS_JSON, page, name, limit );
		}

		if (mApi == API_XML)
		{
			mIsCanceled = false;
			return fetchTagsIndexXML( mSiteUrl + URL_TAGS_XML, page, name, limit);
		}

		return null;
	}

}