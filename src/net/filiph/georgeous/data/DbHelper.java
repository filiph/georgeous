package net.filiph.georgeous.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "feeddatabase";

    public static ContentValues articleToContentValues(Article article) {
        ContentValues values = new ContentValues();
        values.put(FeedContract.KEY_TITLE, article.title);
        values.put(FeedContract.KEY_CONTENT, article.content);
        values.put(FeedContract.KEY_AUTHOR_GUESS, article.author_guess);
        values.put(FeedContract.KEY_CANONICAL_URL, article.canonical_url);
        values.put(FeedContract.KEY_THUMBNAIL_URL, article.thumbnail_url);
        values.put(FeedContract.KEY_PUBLISHED_TIMESTAMP, article.published_timestamp);
        values.put(FeedContract.KEY_UPDATED_TIMESTAMP, article.updated_timestamp);
        values.put(FeedContract.KEY_HUMAN_INFO, article.human_info);
        return values;
    }

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(FeedContract.ARTICLE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FeedContract.ARTICLE_TABLE_NAME + "; ");
        db.execSQL(FeedContract.ARTICLE_TABLE_CREATE);
    }

}
