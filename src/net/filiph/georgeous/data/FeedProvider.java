package net.filiph.georgeous.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * ContentProvider for the feed. Provides URIs such as
 * "content://net.filiph.georgeous.provider/articles/1".
 */
public class FeedProvider extends ContentProvider {
    private static final String TAG = "FeedProvider";

    private DbHelper mOpenHelper;

    private static final int ARTICLES = 1;
    private static final int ARTICLES_ID = 2;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI("net.filiph.georgeous.provider", "articles", ARTICLES);
        sURIMatcher.addURI("net.filiph.georgeous.provider", "articles/#", ARTICLES_ID);
    }

    public static Uri getArticleByIdUri(long id) {
        return Uri.parse("content://" + FeedContract.AUTHORITY + "/"
                + FeedContract.ARTICLE_TABLE_NAME + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO: catch DatabaseLockedException..
        Log.w(TAG, "Delete is not fully implemented on the FeedProvider. "
                + "It just deletes the whole thing.");
        mOpenHelper.getWritableDatabase().execSQL("DELETE FROM " + FeedContract.ARTICLE_TABLE_NAME);
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case ARTICLES:
                return FeedContract.CONTENT_TYPE;
            case ARTICLES_ID:
                return FeedContract.CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.v(TAG, "FeedProvider got an insert request uri " + uri.toString());
        if (sURIMatcher.match(uri) != ARTICLES) {
            throw new IllegalArgumentException("Unsupported URI: " + uri.toString());
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowId =
                db.insertWithOnConflict(FeedContract.ARTICLE_TABLE_NAME, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE); // We assume articles stay the
                                                         // same. TODO: don't

        Uri result = null;
        if (rowId > 0) {
            result = ContentUris.withAppendedId(FeedContract.ARTICLES_URI, rowId);
            getContext().getContentResolver().notifyChange(result, null);
        }
        return result;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.v(TAG, "FeedProvider got a query for " + uri.toString());

        int match = sURIMatcher.match(uri);
        Cursor c;
        switch (match) {
            case ARTICLES:
                c = getArticles(projection, selection, selectionArgs, sortOrder);
                break;
            case ARTICLES_ID:
                c = getArticleById(ContentUris.parseId(uri));
                break;
            default:
                return null;
        }
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.i(TAG, "Update is not implemented on the FeedProvider.");
        return 0;
    }

    private Cursor getArticleById(long id) {
        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        qBuilder.setTables(FeedContract.ARTICLE_TABLE_NAME);
        qBuilder.appendWhere("_ID=" + id);
        return qBuilder
                .query(mOpenHelper.getReadableDatabase(), null, null, null, null, null, null);
    }

    private Cursor getArticles(String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        qBuilder.setTables(FeedContract.ARTICLE_TABLE_NAME);
        return qBuilder.query(mOpenHelper.getReadableDatabase(), projection, selection,
                selectionArgs, null, null, sortOrder);
    }

}
