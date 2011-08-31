package tw.idv.palatis.danboorugallery.siteapi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import android.net.Uri;

public class ShimmieAPI
	implements ISiteAPI
{
	public static final String	URL_POSTS_XML		= "/api/danbooru/post/index.xml&offset=%1$s&limit=%2$s";
	public static final String	URL_POSTS_XML_TAGS	= "/api/danbooru/post/index.xml&offset=%1$s&limit=%2$s&tags=%3$s";

	// this isn't working...
	// public static final String URL_TAGS_XML = "/api/danbooru/find_tags/??????";

	// this is used to format the created_at attribute in XML
	// it is here because Android frees Locale.ENGLISH when formatter destroyed,
	// resulting reloading of locale data every time which is SLOW.
	static private DateFormat	formatter			= new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

	private static final String	URL_FILE			= "/_images/%1$s/%2$d - %3$s.%4$s";
	private static final String	URL_PREVIEW			= "/_thumbs/%1$s/thumb.jpg";

	/**
	 * Generate Post from XML node ({@link Element}), it takes an XML node and try parse it into a {@link Post} structure.
	 * Shimmie2 API doesn't give as much informations as Danbooru API, we just fake some of them here.
	 *
	 * @param node
	 *            the XML {@link Element}
	 * @return the {@link Post}
	 */
	private static Post generatePostFromXML(String siteUrl, Element node)
	{
		Post post = new Post();

		try
		{
			post.created_at = formatter.parse( node.getAttribute( "date" ) );
		}
		catch (ParseException e)
		{
			D.Log.wtf( e );
		}
		// node.getAttribute( "is_warehoused" ); // what's this?

		post.source = node.getAttribute( "source" );
		post.score = Integer.valueOf( node.getAttribute( "score" ) );
		post.author = node.getAttribute( "author" );

		// these 3 are needed for *_url
		post.id = Integer.valueOf( node.getAttribute( "id" ) );
		post.tags = node.getAttribute( "tags" );
		post.md5 = node.getAttribute( "md5" );

		String file_name = "??";
		try
		{
			file_name = node.getAttribute( "file_name" );
			String file_extension = file_name.substring( file_name.lastIndexOf( '.' ) + 1 );
			post.file_url = Uri.encode( String.format( siteUrl + URL_FILE, post.md5, post.id, post.tags, file_extension ), ":/?&" );
			post.sample_url = post.file_url;
			post.preview_url = String.format( siteUrl + URL_PREVIEW, post.md5 );
			D.Log.d( post.file_url );
			D.Log.d( post.preview_url );
		}
		catch (StringIndexOutOfBoundsException e)
		{
			D.Log.d( file_name );
			D.Log.wtf( e );
		}

		String rating = node.getAttribute( "rating" );
		if (rating.equalsIgnoreCase( "Questionable" ))
			post.rating = "q";
		else if (rating.equalsIgnoreCase( "Explicit" ))
			post.rating = "e";
		else
			post.rating = "s";
		rating = null;

		return post;
	}

	String	mSiteUrl;
	int		mApi;
	boolean	mIsCanceled;

	public ShimmieAPI()
	{
		this( "" );
	}

	public ShimmieAPI(String siteUrl)
	{
		mSiteUrl = siteUrl;
		mApi = API_XML;
	}

	public ShimmieAPI(String siteUrl, int api) throws UnsupportedAPIException
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
	public List < Post > fetchPostsIndex(int page, String tags, int limit)
	{
		try
		{
			URL fetchUrl = null;
			if (tags.isEmpty())
				fetchUrl = new URL( String.format( mSiteUrl + URL_POSTS_XML, page * limit, limit ) );
			else
				fetchUrl = new URL( String.format( mSiteUrl + URL_POSTS_XML, page * limit, limit, tags ) );
			D.Log.v( "DanbooruStyleAPI::fetchPostsIndexXML(): fetching %s", fetchUrl );

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
				posts.add( generatePostFromXML( mSiteUrl, (Element) node ) );
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
		tag.name = "Sorry, tag search doesn't work for Shimmie";
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
