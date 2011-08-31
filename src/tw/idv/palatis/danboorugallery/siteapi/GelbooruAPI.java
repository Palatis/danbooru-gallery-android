package tw.idv.palatis.danboorugallery.siteapi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

public class GelbooruAPI
	implements ISiteAPI
{
	public static final String	URL_POSTS_XML	= "/index.php?page=dapi&s=post&q=index&pid=%1$s&tags=%2$s&limit=%3$s";
	// public static final String URL_TAGS_XML = "/tag/index.xml?order=count&page=%1$s&name=*%2$s*&limit=%3$s";

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
	public List < Post > fetchPostsIndex(int page, String tags, int limit)
	{
		mIsCanceled = false;

		try
		{
			URL fetchUrl = new URL( String.format( mSiteUrl + URL_POSTS_XML, page, tags, limit ) );
			D.Log.v( "GelbooruAPI::fetchPostsIndex(): fetching %s", fetchUrl );

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse( new InputSource( fetchUrl.openStream() ) );
			doc.getDocumentElement().normalize();

			NodeList nodes = doc.getElementsByTagName( "post" );

			int length = nodes.getLength();
			ArrayList < Post > posts = new ArrayList < Post >( length );
			for (int i = 0; i < length; ++i)
			{
				Node node = nodes.item( i );
				posts.add( new Post( (Element) node ) );
				if (mIsCanceled)
					return null;
			}
			return posts;
		}
		catch (UnsupportedEncodingException e)
		{
			D.Log.wtf( e );
		}
		catch (IOException e)
		{
			D.Log.wtf( e );
		}
		catch (SAXException e)
		{
			D.Log.wtf( e );
		}
		catch (ParserConfigurationException e)
		{
			D.Log.wtf( e );
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
