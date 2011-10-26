package tw.idv.palatis.danboorugallery.siteapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	extends ISiteAPI
{
	public static final String	URL_POSTS_XML		= "/api/danbooru/post/index.xml&offset=%1$s&limit=%2$s";
	public static final String	URL_POSTS_XML_TAGS	= "/api/danbooru/post/index.xml&offset=%1$s&limit=%2$s&tags=%3$s";
	public static final String	URL_POSTS_RSS		= "/rss/images/%1$s";
	public static final String	URL_POSTS_RSS_TAGS	= "/rss/images/%2$s/%1$s";

	// this isn't working...
	// public static final String URL_TAGS_XML = "/api/danbooru/find_tags/??????";

	static private DateFormat	formatter_xml		= new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
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
			post.created_at = formatter_xml.parse( node.getAttribute( "date" ) );
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
			// D.Log.d( post.file_url );
			// D.Log.d( post.preview_url );
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

	private static DateFormat	formatter_rss		= new SimpleDateFormat( "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH );
	private static Pattern		id_tags				= Pattern.compile( "(\\d+) - (.*)" );
	private static Pattern		width_height_author	= Pattern.compile( "([0-9]+)x([0-9]+).*&lt;p&gt;Uploaded by (.*)&lt;/p&gt;", Pattern.MULTILINE | Pattern.DOTALL );

	private static Post generatePostFromRSS(String siteUrl, Element node)
	{
		Post post = new Post();
		post.rating = "s";

		NodeList childnodes = node.getChildNodes();
		for (int i = 0; i < childnodes.getLength(); ++i)
			try
			{
				Element element = (Element) childnodes.item( i );
				if (element.getTagName().equals( "title" ))
				{
					Matcher matcher = id_tags.matcher( element.getTextContent() );
					if (matcher.find())
					{
						post.id = Integer.valueOf( matcher.group( 1 ) );
						post.tags = matcher.group( 2 );
					}
				}
				else if (element.getTagName().equals( "link" ))
					post.source = element.getTextContent();
				else if (element.getTagName().equals( "media:thumbnail" ))
					post.preview_url = element.getAttribute( "url" );
				else if (element.getTagName().equals( "media:content" ))
				{
					post.sample_url = element.getAttribute( "url" );
					post.file_url = element.getAttribute( "url" );
				}
				else if (element.getTagName().equals( "pubDate" ))
					try
					{
						post.created_at = formatter_rss.parse( element.getTextContent() );
					}
					catch (ParseException e)
					{
					}
				else if (element.getTagName().equals( "description" ))
				{
					Matcher matcher = width_height_author.matcher( element.getTextContent() );
					if (matcher.find())
					{
						post.width = Integer.valueOf( matcher.group( 1 ) );
						post.height = Integer.valueOf( matcher.group( 2 ) );
						post.author = matcher.group( 3 );
					}
				}
			}
			catch (ClassCastException e)
			{
			}

		if (post.preview_url.startsWith( "/" ))
			post.preview_url = siteUrl + post.preview_url;
		if (post.sample_url.startsWith( "/" ))
			post.sample_url = siteUrl + post.sample_url;
		if (post.file_url.startsWith( "/" ))
			post.file_url = siteUrl + post.file_url;

		// replace // with / for buggy Shimmie2 handlers
		post.preview_url = post.preview_url.replace(  "//", "/" );
		post.preview_url = post.preview_url.replace( ":/", "://");
		post.sample_url = post.sample_url.replace(  "//", "/" );
		post.sample_url = post.sample_url.replace( ":/", "://");
		post.file_url = post.file_url.replace(  "//", "/" );
		post.file_url = post.file_url.replace( ":/", "://");

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
		if (api != API_XML && api != API_RSS)
			throw new UnsupportedAPIException( api );

		mApi = api;
	}

	@Override
	public int getSupportedApi()
	{
		return API_XML | API_RSS;
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

	protected List < Post > fetchPostsIndexXML(String url)
	{
		try
		{
			URL fetchUrl = new URL( url );
			D.Log.v( "DanbooruStyleAPI::fetchPostsIndexXML(): fetching %s", fetchUrl );

			InputStream input = fetchUrl.openStream();
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			D.CopyStream( input, output );
			output.flush();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse( new InputSource( new StringReader( output.toString().replace( "&", "&amp;" ) ) ) );
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

	public List < Post > fetchPostsIndexRSS(String url)
	{
		try
		{
			URL fetchUrl = new URL( url );
			D.Log.v( "DanbooruStyleAPI::fetchPostsIndexXML(): fetching %s", fetchUrl );

			InputStream input = fetchUrl.openStream();
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			D.CopyStream( input, output );
			output.flush();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse( new InputSource( new StringReader( output.toString().replace( "&", "&amp;" ) ) ) );
			doc.getDocumentElement().normalize();

			NodeList nodes = doc.getElementsByTagName( "item" );

			int length = nodes.getLength();
			D.Log.d( "got %d items", length );
			ArrayList < Post > posts = new ArrayList < Post >( length );
			String siteUrlHost = fetchUrl.getProtocol() + "://" + fetchUrl.getHost() + "/";
			for (int i = 0; i < length; ++i)
			{
				Node node = nodes.item( i );
				posts.add( generatePostFromRSS( siteUrlHost, (Element) node ) );
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
	public List < Post > fetchPostsIndex(int page, String tags, int limit)
	{
		if (mApi == API_XML)
		{
			String url;
			if (tags.isEmpty())
				url = String.format( mSiteUrl + URL_POSTS_XML, (page - 1) * limit, limit );
			else
				url = String.format( mSiteUrl + URL_POSTS_XML_TAGS, (page - 1) * limit, limit, tags );

			return fetchPostsIndexXML( url );
		}

		if (mApi == API_RSS)
		{
			String url;
			if (tags.isEmpty())
				url = String.format( mSiteUrl + URL_POSTS_RSS, page );
			else
				url = String.format( mSiteUrl + URL_POSTS_RSS_TAGS, page, tags );

			return fetchPostsIndexRSS( url );
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
