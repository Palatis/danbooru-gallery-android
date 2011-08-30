package tw.idv.palatis.danboorugallery.siteapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;

public class DanbooruAPI
	implements ISiteAPI
{
	public static final String	URL_POSTS_JSON	= "/post/index.json?page=%1$s&tags=%2$s&limit=%3$s";
	public static final String	URL_TAGS_JSON	= "/tag/index.json?order=count&page=%1$s&name=*%2$s*&limit=%3$s";
	public static final String	URL_POSTS_XML	= "/post/index.xml?page=%1$s&tags=%2$s&limit=%3$s";
	public static final String	URL_TAGS_XML	= "/tag/index.xml?order=count&page=%1$s&name=*%2$s*&limit=%3$s";

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
		if (api != API_JSON || api != API_XML)
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
			return fetchPostsIndexJSON( page, tags.toLowerCase(), limit );
		if (mApi == API_XML)
			return fetchPostsIndexXML( page, tags.toLowerCase(), limit );
		return null;
	}

	@Override
	public List < Tag > fetchTagsIndex(int page, String name, int limit)
	{
		if (mApi == API_JSON)
			return fetchTagsIndexJSON( page, name.toLowerCase(), limit );
		if (mApi == API_XML)
			return fetchTagsIndexXML( page, name.toLowerCase(), limit );
		return null;
	}

	private List < Post > fetchPostsIndexJSON(int page, String tags, int limit)
	{
		mIsCanceled = false;

		URL fetchUrl = null;
		try
		{
			fetchUrl = new URL( String.format( mSiteUrl + URL_POSTS_JSON, page, tags, limit ) );

			Reader input = new BufferedReader( new InputStreamReader( fetchUrl.openStream(), "UTF-8" ) );
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
		mIsCanceled = false;

		try
		{
			URL fetchUrl = new URL( String.format( mSiteUrl + URL_POSTS_XML, page, tags, limit ) );

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SAXException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ParserConfigurationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
		TreeSet < Tag > ret = new TreeSet < Tag >( new Tag.CompareById() );
		String keywords[] = new HashSet < String >( Arrays.asList( name.split( "\\+" ) ) ).toArray( new String[] { } );

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
				URL fetchUrl = new URL( String.format( mSiteUrl + URL_TAGS_XML, page, keyword, limit ) );

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse( new InputSource( fetchUrl.openStream() ) );
				doc.getDocumentElement().normalize();

				NodeList nodes = doc.getElementsByTagName( "tag" );

				int length = nodes.getLength();
				for (int i = 0; i < length; ++i)
					ret.add( new Tag( (Element) nodes.item( i ) ) );
			}
			catch (UnsupportedEncodingException e)
			{
			}
			catch (IOException e)
			{
			}
			catch (SAXException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (ParserConfigurationException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return Arrays.asList( ret.toArray( new Tag[] { } ) );
	}
}