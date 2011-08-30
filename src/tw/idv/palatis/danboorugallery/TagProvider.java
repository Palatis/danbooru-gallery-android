package tw.idv.palatis.danboorugallery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Hosts;
import tw.idv.palatis.danboorugallery.model.Tag;
import tw.idv.palatis.danboorugallery.siteapi.DanbooruAPI;
import tw.idv.palatis.danboorugallery.siteapi.ISiteAPI;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class TagProvider
	extends ContentProvider
{
	public static String			AUTHORITY			= "tw.idv.palatis.danboorugallery.TagProvider";
	public static final Uri			CONTENT_URI			= Uri.parse( "content://" + AUTHORITY + "/tag" );

	// UriMatcher stuff
	private static final int		SEARCH_TAG			= 0;
	private static final int		GET_TAG				= 1;
	private static final int		SEARCH_SUGGEST		= 2;
	private static final int		REFRESH_SHORTCUT	= 3;
	private static final UriMatcher	sURIMatcher			= buildUriMatcher();

	/**
	 * Builds up a UriMatcher for search suggestion and shortcut refresh
	 * queries.
	 */
	private static UriMatcher buildUriMatcher()
	{
		UriMatcher matcher = new UriMatcher( UriMatcher.NO_MATCH );
		// to get definitions...
		matcher.addURI( AUTHORITY, "tag", SEARCH_TAG );
		matcher.addURI( AUTHORITY, "tag/#", GET_TAG );
		// to get suggestions...
		matcher.addURI( AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST );
		matcher.addURI( AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST );

		/*
		 * The following are unused in this implementation, but if we include
		 * {@link SearchManager#SUGGEST_COLUMN_SHORTCUT_ID} as a column in our
		 * suggestions table, we
		 * could expect to receive refresh queries when a shortcutted suggestion
		 * is displayed in
		 * Quick Search Box, in which case, the following Uris would be provided
		 * and we
		 * would return a cursor with a single item representing the refreshed
		 * suggestion data.
		 */
		matcher.addURI( AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, REFRESH_SHORTCUT );
		matcher.addURI( AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", REFRESH_SHORTCUT );
		return matcher;
	}

	SharedPreferences	preferences	= null;
	ISiteAPI			site_api	= null;

	@Override
	public boolean onCreate()
	{
		preferences = getContext().getSharedPreferences( D.SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE );
		site_api = new DanbooruAPI();
		return true;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		// does nothing
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		// does nothing
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		// still does nothing
		return 0;
	}

	@Override
	public String getType(Uri uri)
	{
		switch (sURIMatcher.match( uri ))
		{
		case SEARCH_SUGGEST:
			return SearchManager.SUGGEST_MIME_TYPE;
		case REFRESH_SHORTCUT:
			return SearchManager.SHORTCUT_MIME_TYPE;
		default:
			throw new IllegalArgumentException( "Unknown URL " + uri );
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		switch (sURIMatcher.match( uri ))
		{
		case SEARCH_SUGGEST:
			if (selectionArgs == null)
				throw new IllegalArgumentException( "selectionArgs must be provided for the Uri: " + uri );
			return getSuggestions( selectionArgs[0] );
		case SEARCH_TAG:
			if (selectionArgs == null)
				throw new IllegalArgumentException( "selectionArgs must be provided for the Uri: " + uri );
		}
		return null;
	}

	static private class ResultSet
	{
		Tag		tag;
		String	keywords;

		ResultSet(Tag t, String k)
		{
			tag = t;
			keywords = k;
		}

		public static class CompareKeywords
			implements Comparator < ResultSet >
		{
			@Override
			public int compare(ResultSet object1, ResultSet object2)
			{
				return object2.keywords.split( "," ).length - object1.keywords.split( "," ).length;
			}
		}

		public static class CompareCount
			implements Comparator < ResultSet >
		{
			@Override
			public int compare(ResultSet object1, ResultSet object2)
			{
				return object2.tag.count - object1.tag.count;
			}
		}

		public static class CompareName
			implements Comparator < ResultSet >
		{
			@Override
			public int compare(ResultSet object1, ResultSet object2)
			{
				return object1.tag.name.compareTo( object2.tag.name );
			}
		}
	}

	Cursor getSuggestions(String query)
	{
		try
		{
			// get prefered host
			int selected_host = preferences.getInt( "selected_host", 0 );
			Hosts hosts = Hosts.fromCSV( preferences.getString( "serialized_hosts", "" ) );
			String host = null;
			if (hosts == null)
				host = "http://konachan.com/";
			else
				host = hosts.get( selected_host )[Hosts.HOST_URL];

			site_api.setSiteUrl( host );

			Log.d( D.LOGTAG, "getSuggestions: query = " + query );
			List < Tag > tags = site_api.fetchTagsIndex( 1, query, 300 );

			List < ResultSet > results = new ArrayList < ResultSet >( tags.size() );
			if (tags.size() > 0)
			{
				String keywords[] = query.split( "\\+" );

				for (Tag tag : tags)
					results.add( new ResultSet( tag, "" ) );

				for (String keyword : keywords)
				{
					keyword = keyword.trim().replace( ' ', '_' );

					for (ResultSet result : results)
						if (result.tag.name.contains( keyword ))
							result.keywords += ", " + keyword;
				}

				// get rid of leading ", "
				for (ResultSet result : results)
					result.keywords = result.keywords.substring( 2 );

				// the following 3 sort are just to garentee we have the correct
				// order:
				// - one with more keywords in front
				// - if keyword counts are same, one with more posts in front
				// - if post counts are same, sort by lexicographically

				// sort by name
				Collections.sort( results, new ResultSet.CompareName() );
				// sort by post count
				Collections.sort( results, new ResultSet.CompareCount() );
				// sort by keyword count
				Collections.sort( results, new ResultSet.CompareKeywords() );
			}

			// get the result ArrayList into a MatrixCursor
			int length = results.size();
			MatrixCursor cursor = new MatrixCursor( new String[] {
				BaseColumns._ID,
				SearchManager.SUGGEST_COLUMN_TEXT_1,
				SearchManager.SUGGEST_COLUMN_TEXT_2,
				SearchManager.SUGGEST_COLUMN_INTENT_DATA,
			}, length );

			for (ResultSet result : results)
				cursor.addRow( new Object[] {
					result.tag.id,
					result.tag.name,
					String.format( getContext().getString( R.string.search_suggestion_description ), result.tag.count, result.keywords ),
					result.tag.name,
				} );

			return cursor;
		}
		catch (IOException ex)
		{
			Log.d( D.LOGTAG, Log.getStackTraceString( ex ) );
		}
		return null;
	}
}
