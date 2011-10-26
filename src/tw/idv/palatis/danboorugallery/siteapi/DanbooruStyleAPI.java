package tw.idv.palatis.danboorugallery.siteapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.model.Tag;
import android.net.Uri;

public abstract class DanbooruStyleAPI
	extends SiteAPI
{
	private static final int	_BUFFER_SIZE	= 8192;

	// this is used to format the created_at attribute in XML
	// it is here because Android frees Locale.ENGLISH when formatter destroyed,
	// resulting reloading of locale data every time which is SLOW.
	static private DateFormat	formatter		= new SimpleDateFormat( "EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH );

	/**
	 * Generate Post from XML node ({@link Element}), it takes an XML node and try parse it into a {@link Post} structure.
	 *
	 * @param node
	 *            the XML {@link Element}
	 * @return the {@link Post}
	 */
	protected static Post generatePostFromXML(Element node)
	{
		Post post = new Post();

		post.id = Integer.valueOf( node.getAttribute( "id" ) );
		try
		{
			post.parent_id = Integer.valueOf( node.getAttribute( "parent_id" ) );
		}
		catch (NumberFormatException ex)
		{
			post.parent_id = -1;
		}
		try
		{
			post.creator_id = Integer.valueOf( node.getAttribute( "creator_id" ) );
		}
		catch (NumberFormatException ex)
		{
			post.creator_id = -1;
		}
		post.change = Integer.valueOf( node.getAttribute( "change" ) );
		post.score = Integer.valueOf( node.getAttribute( "score" ) );
		post.status = node.getAttribute( "status" );
		post.rating = node.getAttribute( "rating" );
		post.tags = node.getAttribute( "tags" );
		post.source = node.getAttribute( "source" );
		post.author = node.getAttribute( "author" );
		try
		{
			post.created_at = formatter.parse( "Mon Aug 29 12:01:53 -0400 2011" );
		}
		catch (ParseException e)
		{
		}

		post.has_notes = node.getAttribute( "has_notes" ).equals( "true" );
		post.has_children = node.getAttribute( "has_children" ).equals( "true" );
		post.has_comments = node.getAttribute( "has_comments" ).equals( "true" );

		post.md5 = node.getAttribute( "md5" );
		post.file_url = node.getAttribute( "file_url" );
		post.file_size = Integer.valueOf( "0" + node.getAttribute( "file_size" ) );
		post.width = Integer.valueOf( node.getAttribute( "width" ) );
		post.height = Integer.valueOf( node.getAttribute( "height" ) );
		post.sample_url = node.getAttribute( "sample_url" );
		post.sample_width = Integer.valueOf( node.getAttribute( "sample_width" ) );
		post.sample_height = Integer.valueOf( node.getAttribute( "sample_height" ) );
		post.preview_url = node.getAttribute( "preview_url" );
		post.preview_width = Integer.valueOf( node.getAttribute( "preview_width" ) );
		post.preview_height = Integer.valueOf( node.getAttribute( "preview_height" ) );

		// if the preview_url is a relative url, add the host part from file_url to it.
		Uri puri = Uri.parse( post.preview_url );
		if (puri.getHost() == null)
		{
			Uri furi = Uri.parse( post.file_url );
			post.preview_url = furi.getScheme() + "://" + furi.getHost() + "/" + post.preview_url;
		}

		return post;
	}

	/**
	 * Generate Post from XML node ({@link Element}), it takes an XML node and try parse it into a {@link Post} structure.
	 *
	 * @param node
	 *            the XML {@link Element}
	 * @return the {@link Post}
	 */
	protected static Post generatePostFromJSON(JSONObject json)
	{
		Post post = new Post();

		post.id = json.optInt( "id" );
		post.parent_id = json.optInt( "parent_id", -1 );
		post.creator_id = json.optInt( "creater_id" );
		post.change = json.optInt( "creater_id" );
		post.score = json.optInt( "score" );
		post.status = json.optString( "status" );
		post.rating = json.optString( "rating" );
		post.tags = json.optString( "tags" );
		post.source = json.optString( "source" );
		post.author = json.optString( "author" );

		// date is a little tricky, we might get 2 formats...
		// we might get a JSONObject with json_class="Time",
		// or just a long to represent the timestamp.
		try
		{
			// it might just be an long value
			post.created_at = new Date( json.getLong( "created_at" ) * 1000 );
		}
		catch (JSONException ex)
		{
			try
			{
				// or it might be a JSONObject with json_class == "Time"
				JSONObject json_created_at = json.getJSONObject( "created_at" );
				if (json_created_at.getString( "json_class" ).equals( "Time" ))
					post.created_at = new Date( json_created_at.getLong( "s" ) * 1000 + json_created_at.getLong( "n" ) / 1000000 );
			}
			catch (JSONException ex2)
			{
				// it's okay to not having a date...
			}
		}

		post.has_notes = json.optBoolean( "has_notes" );
		post.has_children = json.optBoolean( "has_children" );
		post.has_comments = json.optBoolean( "has_comments" );

		post.md5 = json.optString( "md5" );
		post.file_url = json.optString( "file_url" );
		post.file_size = json.optInt( "file_size" );
		post.width = json.optInt( "width" );
		post.height = json.optInt( "height" );
		post.sample_url = json.optString( "sample_url" );
		post.sample_width = json.optInt( "sample_width" );
		post.sample_height = json.optInt( "sample_height" );
		post.preview_url = json.optString( "preview_url" );
		post.preview_width = json.optInt( "preview_width" );
		post.preview_height = json.optInt( "preview_height" );

		// if the preview_url is a relative url, add the host part from file_url to it.
		Uri puri = Uri.parse( post.preview_url );
		if (puri.getHost() == null)
		{
			Uri furi = Uri.parse( post.file_url );
			post.preview_url = furi.getScheme() + "://" + furi.getHost() + "/" + post.preview_url;
		}

		return post;
	}

	/**
	 * Inherited class should overwrite this function to indicate that if a fetch
	 * operation is canceled or not.
	 * Default behavior will always return ${@literal false}.
	 *
	 * @return {@literal true} if canceled, {@literal false} otherwise.
	 */
	protected boolean isCanceled()
	{
		return false;
	}

	/**
	 * This is a helper function to fetch {@link List< {@link Post} >} from Danbooru's JSON API.
	 *
	 * @param url_format
	 *            the format of URL in {@link String}
	 * @param page
	 *            page index
	 * @param tags
	 *            tags to filter
	 * @param limit
	 *            posts to retrieve per page
	 * @return the posts fetched
	 */
	protected List < Post > fetchPostsIndexJSON(String url_format, int page, String tags, int limit)
	{
		try
		{
			URL fetchUrl = new URL( String.format( url_format, page, tags, limit ) );
			D.Log.v( "DanbooruStyleAPI::fetchPostsIndexJSON(): fetching %s", fetchUrl );

			Reader input = new BufferedReader( new InputStreamReader( fetchUrl.openStream(), "UTF-8" ) );
			Writer output = new StringWriter();

			int count = 0;
			char buffer[] = new char[_BUFFER_SIZE];
			while ((count = input.read( buffer )) > 0)
			{
				output.write( buffer, 0, count );
				if (isCanceled())
					return null;
			}

			JSONArray json_posts = new JSONArray( output.toString() );
			int len = json_posts.length();
			ArrayList < Post > posts = new ArrayList < Post >( len );
			for (int i = 0; i < len; ++i)
				try
				{
					posts.add( generatePostFromJSON( json_posts.getJSONObject( i ) ) );
					if (isCanceled())
						return null;
				}
				catch (JSONException ex)
				{
					D.Log.wtf( ex );
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
		catch (JSONException e)
		{
			D.Log.wtf( e );
		}
		return null;
	}

	/**
	 * This is a helper function to fetch {@link List< {@link Post} >} from Danbooru's API.
	 *
	 * @param url_format
	 *            the format of URL in {@link String}
	 * @param page
	 *            page index
	 * @param tags
	 *            tags to filter
	 * @param limit
	 *            posts to retrieve per page
	 * @return the posts fetched
	 */
	protected List < Post > fetchPostsIndexXML(String url_format, int page, String tags, int limit)
	{
		try
		{
			URL fetchUrl = new URL( String.format( url_format, page, tags, limit ) );
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
				posts.add( generatePostFromXML( (Element) node ) );
				if (isCanceled())
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

	protected List < Tag > fetchTagsIndexJSON(String url_format, int page, String name, int limit)
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
				URL fetchUrl = new URL( String.format( url_format, page, keyword, limit ) );
				D.Log.v( "DanbooruStyleAPI::fetchTagsIndexJSON(): fetching %s", fetchUrl );

				Reader input = new BufferedReader( new InputStreamReader( fetchUrl.openStream(), "UTF-8" ) );
				Writer output = new StringWriter();

				int count = 0;
				while ((count = input.read( buffer )) > 0)
				{
					output.write( buffer, 0, count );
					if (isCanceled())
						return null;
				}

				JSONArray array = new JSONArray( output.toString() );
				int length = array.length();
				for (int i = 0; i < length; ++i)
					try
					{
						ret.add( new Tag( array.getJSONObject( i ) ) );
						if (isCanceled())
							return null;
					}
					catch (JSONException e)
					{
					}
			}
			catch (UnsupportedEncodingException e)
			{
				D.Log.wtf( e );
			}
			catch (IOException e)
			{
				D.Log.wtf( e );
			}
			catch (JSONException e)
			{
				D.Log.wtf( e );
			}
		}

		return Arrays.asList( ret.toArray( new Tag[] { } ) );
	}

	protected List < Tag > fetchTagsIndexXML(String url_format, int page, String name, int limit)
	{
		TreeSet < Tag > ret = new TreeSet < Tag >( new Tag.CompareById() );
		String keywords[] = new HashSet < String >( Arrays.asList( name.split( "\\+" ) ) ).toArray( new String[] { } );

		for (String keyword : keywords)
		{
			// trim leading and trailing whitespace, then replace spaces with underscores.
			// because typing underscore with soft-keyboard is a pain in the ass...
			keyword = keyword.trim().replace( ' ', '_' );

			// get data from network...
			try
			{
				URL fetchUrl = new URL( String.format( url_format, page, keyword, limit ) );
				D.Log.v( "DanbooruStyleAPI::fetchTagsIndexXML(): fetching %s", fetchUrl );

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse( new InputSource( fetchUrl.openStream() ) );
				doc.getDocumentElement().normalize();

				NodeList nodes = doc.getElementsByTagName( "tag" );

				int length = nodes.getLength();
				for (int i = 0; i < length; ++i)
				{
					ret.add( new Tag( (Element) nodes.item( i ) ) );
					if (isCanceled())
						return null;
				}
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
		}

		return Arrays.asList( ret.toArray( new Tag[] { } ) );
	}
}
