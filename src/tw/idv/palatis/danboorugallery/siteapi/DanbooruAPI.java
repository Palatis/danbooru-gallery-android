package tw.idv.palatis.danboorugallery.siteapi;

import java.util.List;

import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

public class DanbooruAPI
	extends DanbooruStyleAPI
{
	public static final String	URL_POSTS_JSON	= "/post/index.json?page=%1$s&tags=%2$s&limit=%3$s";
	public static final String	URL_TAGS_JSON	= "/tag/index.json?order=count&page=%1$s&name=*%2$s*&limit=%3$s";
	public static final String	URL_POSTS_XML	= "/post/index.xml?page=%1$s&tags=%2$s&limit=%3$s";
	public static final String	URL_TAGS_XML	= "/tag/index.xml?order=count&page=%1$s&name=*%2$s*&limit=%3$s";

	public DanbooruAPI()
	{
		super();
	}

	public DanbooruAPI(String siteUrl)
	{
		super(siteUrl);
	}

	public DanbooruAPI(String siteUrl, int api) throws UnsupportedAPIException
	{
		super(siteUrl, api);
	}

	@Override
	public int getSupportedApi()
	{
		return API_JSON | API_XML;
	}

	@Override
	public int getDefaultApi()
	{
		return API_JSON;
	}

	@Override
	public List < Post > fetchPostsIndex(int page, String tags, int limit)
	{
		if (getApi() == API_JSON)
		{
			uncancel();
			return fetchPostsIndexJSON( getSiteUrl() + URL_POSTS_JSON, page, tags, limit );
		}

		if (getApi() == API_XML)
		{
			uncancel();
			return fetchPostsIndexXML( getSiteUrl() + URL_POSTS_XML, page, tags, limit );
		}

		return null;
	}

	@Override
	public List < Tag > fetchTagsIndex(int page, String name, int limit)
	{
		if (getApi() == API_JSON)
		{
			uncancel();
			return fetchTagsIndexJSON( getSiteUrl() + URL_TAGS_JSON, page, name, limit );
		}

		if (getApi() == API_XML)
		{
			uncancel();
			return fetchTagsIndexXML( getSiteUrl() + URL_TAGS_XML, page, name, limit );
		}

		return null;
	}

}