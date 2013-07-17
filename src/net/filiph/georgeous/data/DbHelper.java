package net.filiph.georgeous.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 3;
	private static final String DATABASE_NAME = "feeddatabase";
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
	public static final String KEY_ID = "_id";
	private static final String ARTICLE_TABLE_CREATE = "CREATE TABLE " + 
			ARTICLE_TABLE_NAME + " (" +
			KEY_TITLE + " TEXT, " +
			KEY_CONTENT + " BLOB, " +
			KEY_CANONICAL_URL + " TEXT UNIQUE NOT NULL, " +
			KEY_AUTHOR_GUESS + " TEXT, " +
			KEY_THUMBNAIL_URL + " TEXT, " +
			KEY_PUBLISHED_TIMESTAMP + " TEXT, " +
			KEY_UPDATED_TIMESTAMP + " TEXT, " +
			KEY_CATEGORIES + " TEXT, " +
			KEY_READCOUNT + " INTEGER, " +
			KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT);";
	

	public static ContentValues articleToContentValues(Article article) {
		ContentValues values = new ContentValues();
		values.put(DbHelper.KEY_TITLE, article.title);
		values.put(DbHelper.KEY_CONTENT, article.content);
		values.put(DbHelper.KEY_AUTHOR_GUESS, article.author_guess);
		values.put(DbHelper.KEY_CANONICAL_URL, article.canonical_url);
		values.put(DbHelper.KEY_THUMBNAIL_URL, article.thumbnail_url);
		values.put(DbHelper.KEY_PUBLISHED_TIMESTAMP, article.published_timestamp);
		values.put(DbHelper.KEY_UPDATED_TIMESTAMP, article.updated_timestamp);
		return values;
	}
	
	public DbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(ARTICLE_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("DROP TABLE IF EXISTS " + ARTICLE_TABLE_NAME + "; ");
		db.execSQL(ARTICLE_TABLE_CREATE);
	}

}
