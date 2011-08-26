package tw.idv.palatis.danboorugallery;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class TagProvider extends ContentProvider
{
	public static String AUTHORITY = "tw.idv.palatis.danboorugallery.TagProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/tag");

    // MIME types used for searching tags or looking up a single tag
    public static final String WORDS_MIME_TYPE =
    	ContentResolver.CURSOR_DIR_BASE_TYPE + "/tw.idv.palatis.danboorugallery";
    public static final String DEFINITION_MIME_TYPE =
    	ContentResolver.CURSOR_ITEM_BASE_TYPE + "/tw.idv.palatis.danboorugallery";

    // UriMatcher stuff
    private static final int SEARCH_TAG = 0;
    private static final int GET_TAG = 1;
    private static final int SEARCH_SUGGEST = 2;
    private static final int REFRESH_SHORTCUT = 3;
    private static final UriMatcher sURIMatcher = buildUriMatcher();

    /**
     * Builds up a UriMatcher for search suggestion and shortcut refresh queries.
     */
    private static UriMatcher buildUriMatcher() {
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


	@Override
	public boolean onCreate()
	{
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
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
        switch (sURIMatcher.match(uri))
        {
        case SEARCH_SUGGEST:
            if (selectionArgs == null)
				throw new IllegalArgumentException(
				      "selectionArgs must be provided for the Uri: " + uri);
            //return getSuggestions(selectionArgs[0]);
        case SEARCH_TAG:
            if (selectionArgs == null)
				throw new IllegalArgumentException(
				      "selectionArgs must be provided for the Uri: " + uri);
    	}
		return null;
	}

}
