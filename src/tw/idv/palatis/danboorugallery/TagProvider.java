package tw.idv.palatis.danboorugallery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Hosts;
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

public class TagProvider extends ContentProvider
{
	public static String AUTHORITY = "tw.idv.palatis.danboorugallery.TagProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/tag");

    // UriMatcher stuff
    private static final int SEARCH_TAG = 0;
    private static final int GET_TAG = 1;
    private static final int SEARCH_SUGGEST = 2;
    private static final int REFRESH_SHORTCUT = 3;
    private static final UriMatcher sURIMatcher = buildUriMatcher();

    /**
     * Builds up a UriMatcher for search suggestion and shortcut refresh queries.
     */
    private static UriMatcher buildUriMatcher()
    {
        UriMatcher matcher =  new UriMatcher(UriMatcher.NO_MATCH);
        // to get definitions...
        matcher.addURI(AUTHORITY, "tag", SEARCH_TAG);
        matcher.addURI(AUTHORITY, "tag/#", GET_TAG);
        // to get suggestions...
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);

        /* The following are unused in this implementation, but if we include
         * {@link SearchManager#SUGGEST_COLUMN_SHORTCUT_ID} as a column in our suggestions table, we
         * could expect to receive refresh queries when a shortcutted suggestion is displayed in
         * Quick Search Box, in which case, the following Uris would be provided and we
         * would return a cursor with a single item representing the refreshed suggestion data.
         */
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, REFRESH_SHORTCUT);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", REFRESH_SHORTCUT);
        return matcher;
    }

    SharedPreferences preferences = null;

	@Override
	public boolean onCreate()
	{
		preferences = getContext().getSharedPreferences(D.SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);
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
        switch (sURIMatcher.match(uri))
        {
        case SEARCH_SUGGEST:
            return SearchManager.SUGGEST_MIME_TYPE;
        case REFRESH_SHORTCUT:
            return SearchManager.SHORTCUT_MIME_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
        switch (sURIMatcher.match(uri))
        {
        case SEARCH_SUGGEST:
            if (selectionArgs == null)
				throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
            return getSuggestions(selectionArgs[0]);
        case SEARCH_TAG:
            if (selectionArgs == null)
				throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
    	}
		return null;
	}

	static private class ResultSet
	{
		public int count;
		public String name;
		public String keywords;

		ResultSet(JSONObject obj)
		{
			count = obj.optInt("count");
			name = obj.optString("name");
			keywords = "";
		}

		public static class CompareKeywords implements Comparator<ResultSet>
		{
			@Override
			public int compare(ResultSet object1, ResultSet object2)
			{
				// we want ones with more keywords in front
				return object2.keywords.split(",").length - object1.keywords.split(",").length;
			}
		}

		public static class CompareCount implements Comparator<ResultSet>
		{
			@Override
			public int compare(ResultSet object1, ResultSet object2)
			{
				// we want descending order
				return object2.count - object1.count;
			}
		}

		public static class CompareName implements Comparator<ResultSet>
		{
			@Override
			public int compare(ResultSet object1, ResultSet object2)
			{
				return object1.name.compareTo(object2.name);
			}
		}

		@Override
		public String toString()
		{
			return name + " - (" + count + ", " + keywords + ")";
		}
	}

	Cursor getSuggestions(String query)
	{
		try
		{
			// get prefered host
			int selected_host = preferences.getInt("selected_host", 0);
			Hosts hosts = Hosts.fromCSV( preferences.getString("serialized_hosts", "") );
			String host = null;
			if ( hosts == null )
				host = "http://konachan.com/";
			else
				host = hosts.get(selected_host)[ Hosts.HOST_URL ];

			char buffer[] = new char[8192];
			String[] keywords = query.split("\\+");
			SortedSet<String> keyword_set = new TreeSet<String>(Arrays.asList(keywords));
			List<ResultSet> allresults = new ArrayList<ResultSet>();

			keywords = keyword_set.toArray(new String[] {});
			keyword_set = null;

			for (String keyword: keywords)
			{
				// trim leading and trailing whitespace, then replace spaces with underscores.
				// because typing underscore with soft-keyboard is a pain in the ass...
				keyword = keyword.trim().replace(' ', '_');

				// get data from network...
				URL url = new URL(String.format(host + D.URL_TAGS, keyword));
				Reader input = new BufferedReader( new InputStreamReader( url.openStream(), "UTF-8" ) );
				Writer output = new StringWriter();

				int count = 0;
				while ((count = input.read(buffer)) > 0)
					output.write(buffer, 0, count);

				// push the result into allresults
				JSONArray array = new JSONArray( output.toString() );
				int length = array.length();
				for ( int i = 0;i < length; ++i )
					allresults.add(new ResultSet(array.getJSONObject(i)));
			}

			if ( allresults.size() > 0 )
			{
				// merge results
				Map<String, ResultSet> map = new HashMap<String, ResultSet>();
				for (ResultSet result: allresults)
					map.put(result.name, result);

				// dump it back to an array
				allresults.clear();
				Set<Entry<String, ResultSet>> set = map.entrySet();
				for (Entry<String, ResultSet> entry : set)
					allresults.add(entry.getValue());

				// adjust keywords
				for (String keyword: keywords)
					for (ResultSet result: allresults)
						if ( result.name.contains(keyword) )
							result.keywords += ", " + keyword;

				// get rid of leading ", "
				for (ResultSet result: allresults)
					result.keywords = result.keywords.substring(2);

				// the following 3 sort are just to garentee we have the correct
				// order:
				//   - one with more keywords in front
				//   - if keyword counts are same, one with more posts in front
				//   - if post counts are same, sort by lexicographically

				// sort by name
				Collections.sort(allresults, new ResultSet.CompareName());
				// sort by post count
				Collections.sort(allresults, new ResultSet.CompareCount());
				// sort by keyword count
				Collections.sort(allresults, new ResultSet.CompareKeywords());
			}

			// get the result ArrayList into a MatrixCursor
			int length = allresults.size();
			MatrixCursor cursor = new MatrixCursor(
				new String[]
				{
					BaseColumns._ID,
					SearchManager.SUGGEST_COLUMN_TEXT_1,
					SearchManager.SUGGEST_COLUMN_TEXT_2,
					SearchManager.SUGGEST_COLUMN_INTENT_DATA,
				},
				length
			);

			for (ResultSet entry: allresults)
				cursor.addRow(
					new Object[]
					{
						entry.name.hashCode(),
						entry.name,
						String.format(
							getContext().getString( R.string.search_suggestion_description ),
							entry.count,
							entry.keywords
						),
						entry.name,
					}
				);

			return cursor;
		}
		catch (IOException ex)
		{
			Log.d(D.LOGTAG, Log.getStackTraceString(ex));
		}
		catch (JSONException ex)
		{
			Log.d(D.LOGTAG, Log.getStackTraceString(ex));
		}
		return null;
	}
}
