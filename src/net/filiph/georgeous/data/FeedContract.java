package net.filiph.georgeous.data;

import android.content.ContentValues;
import android.net.Uri;

public class FeedContract {
    private FeedContract() {
    }

    public static final String AUTHORITY = "net.filiph.georgeous.provider";

    public static final String CONTENT_ITEM_TYPE =
            "vdn.android.cursor.item/vdn.net.filiph.georgeous.provider.articles";
    public static final String CONTENT_TYPE =
            "vdn.android.cursor.dir/vdn.net.filiph.georgeous.provider.articles";

    public static final String ARTICLE_TABLE_NAME = "articles";
    public static final String KEY_TITLE = "title";
    public static final String KEY_CONTENT = "content";
    public static final String KEY_CANONICAL_URL = "canonical_url";
    public static final String KEY_AUTHOR_GUESS = "author_guess";
    public static final String KEY_THUMBNAIL_URL = "thumbnail_url";
    public static final String KEY_PUBLISHED_TIMESTAMP = "created_timestamp";
    public static final String KEY_UPDATED_TIMESTAMP = "updated_timestamp";
    public static final String KEY_READCOUNT = "readcount";
    public static final String KEY_CATEGORIES = "categories";
    public static final String KEY_HUMAN_INFO = "human_info";
    public static final String KEY_ID = "_id";

    public static final String ARTICLE_TABLE_CREATE = "CREATE TABLE " + ARTICLE_TABLE_NAME + " ("
            + KEY_TITLE + " TEXT, " + KEY_CONTENT + " BLOB, " + KEY_CANONICAL_URL
            + " TEXT UNIQUE NOT NULL, " + KEY_AUTHOR_GUESS + " TEXT, " + KEY_THUMBNAIL_URL
            + " TEXT, " + KEY_PUBLISHED_TIMESTAMP + " TEXT, " + KEY_UPDATED_TIMESTAMP + " TEXT, "
            + KEY_CATEGORIES + " TEXT, " + KEY_HUMAN_INFO + " TEXT, " + KEY_READCOUNT
            + " INTEGER, " + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT);";

    public static ContentValues articleToContentValues(Article article) {
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, article.title);
        values.put(KEY_CONTENT, article.content);
        values.put(KEY_AUTHOR_GUESS, article.author_guess);
        values.put(KEY_CANONICAL_URL, article.canonical_url);
        values.put(KEY_THUMBNAIL_URL, article.thumbnail_url);
        values.put(KEY_PUBLISHED_TIMESTAMP, article.published_timestamp);
        values.put(KEY_UPDATED_TIMESTAMP, article.updated_timestamp);
        values.put(KEY_HUMAN_INFO, article.human_info);
        return values;
    }

    public static final Uri ARTICLES_URI = Uri.parse("content://" + AUTHORITY + "/"
            + ARTICLE_TABLE_NAME);
}
