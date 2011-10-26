package tw.idv.palatis.danboorugallery.siteapi;

import java.util.ArrayList;
import java.util.List;

import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

public class GelbooruAPI
	extends DanbooruStyleAPI
{
	public static final String	URL_POSTS_XML	= "/index.php?page=dapi&s=post&q=index&pid=%1$s&tags=%2$s&limit=%3$s";
	// public static final String URL_TAGS_XML = "/index.php?page=dapi&s=tag&q=index&order=count&pid=%1$s&name_pattern=%2$s&limit=%3$s";

	String						mSiteUrl;
	int							mApi;
	boolean						mIsCanceled;

	public GelbooruAPI()
	{
		this( "" );
	}

	public GelbooruAPI(String siteUrl)
	{
		mSiteUrl = siteUrl;
		mApi = API_XML;
	}

	public GelbooruAPI(String siteUrl, int api) throws UnsupportedAPIException
	{
		mSiteUrl = siteUrl;
		setApi( api );
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
	public int getSupportedApi()
	{
		return API_XML;
	}

	@Override
	public int getApi()
	{
		return mApi;
	}

	@Override
	public void setApi(int api) throws UnsupportedAPIException
	{
		if (api != API_XML)
			throw new UnsupportedAPIException( api );

		mApi = api;
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
		List < Tag > tags = new ArrayList < Tag >( 1 );
		Tag tag = null;
		tag = new Tag();
		tag.id = 987654321;
		tag.count = 987654321;
		tag.type = 987654321;
		tag.ambiguous = false;
		tag.name = "Sorry, tag search doesn't work for Gelbooru";
		tags.add( tag );
		tag = new Tag();
		tag.id = 987654321;
		tag.count = 987654320;
		tag.type = 987654321;
		tag.ambiguous = false;
		tag.name = "You can still manually input tags for filter";
		tags.add( tag );
		return tags;
	}
}