package net.filiph.georgeous.background;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import net.filiph.georgeous.Constants;
import net.filiph.georgeous.data.Article;
import net.filiph.georgeous.data.DbHelper;
import net.filiph.georgeous.data.ImageGetter;
import net.filiph.georgeous.data.ImageGetterWithManageSpace;
import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;

public class ReaderFeedService extends IntentService {
	private static final String TAG = "GeorgeousReaderFeedService";
	
	private static final int MAX_ARTICLES_TO_PRELOAD = 5;
	
	public ReaderFeedService() {
		super("ReaderFeedIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v(TAG, "HandleIntent called. Intent action: " + intent.getAction());
		
		if (intent.getAction().equals(Constants.GET_ARTICLES_INTENT)) {
			String urlString = "http://android-developers.blogspot.com/atom.xml"; // TODO: get from intent
			getArticles(urlString);
		} else if (intent.getAction().equals(Constants.GET_ARTICLE_IMAGES_INTENT)) {
			String articleContent = intent.getStringExtra(Constants.ARTICLE_CONTENT_EXTRA);
			Log.v(TAG, "Get article images for content.");
			getArticleImages(articleContent);
		} else {
			Log.e(TAG, "Wrong intent received.");
		}
	}
	
	private void getArticleImages(String contentHtml) {
		String state = Environment.getExternalStorageState();
		
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File externalCacheDir = getExternalCacheDir();
			if (externalCacheDir == null) {
				Log.i(TAG, "Could not find or create external cache directory.");
				return;
			}
			
		    // We can read and write the media
			DisplayMetrics metrics = new DisplayMetrics();  // we don't care about metrics (the images won't be shown and the bounds are not saved) 
			
			ImageGetterWithManageSpace imgGetter = new ImageGetter(
					externalCacheDir, 
					getResources(),
					metrics,
					true);
			// Now let's make imgGetter do the work. It will fetch the images and cache them.
			Html.fromHtml(contentHtml, imgGetter, null);
			imgGetter.manageSpace();
		} else {
			Log.i(TAG, "External storage cannot be mounted - skipping image caching.");
		}
		
	}


	/**
	 * Fetches the Atom XML and puts the metadata and HTML contents into the SQLite database.
	 * 
	 * @param urlString
	 */
	private void getArticles(String urlString) {
		List<Article> articles = fetchArticles(urlString);
		
		if (articles != null) {
			Log.v(TAG, "Found " + articles.size() + " articles.");
			int newArticles = insertArticlesIntoDatabase(articles);
			
			if (newArticles == -1) {
				return;  // Something went wrong in insertArticlesIntoDatabase.
			} else if (newArticles > 0) {
				sendFeedResult(Constants.FEED_RESULT_NEW_ARTICLES);
			} else {
				sendFeedResult(Constants.FEED_RESULT_NO_NEW_ARTICLES);
			}
		}
	}

	/**
	 * 
	 * @param articles
	 * @return The number of articles that previously weren't in the database.
	 */
	private int insertArticlesIntoDatabase(List<Article> articles) {
		int newArticles = 0;
		int preloadedArticles = 0;
		
		SQLiteDatabase db = null;
		try {
			DbHelper helper = new DbHelper(this);
			db = helper.getWritableDatabase();
			
			for (Article article : articles) {
				long rowId = db.insertWithOnConflict(
						DbHelper.ARTICLE_TABLE_NAME, 
						null, 
						DbHelper.articleToContentValues(article),
						SQLiteDatabase.CONFLICT_IGNORE);  // we assume articles stay the same - TODO: don't
				if (rowId != -1) {
					newArticles += 1;
					Log.v(TAG, "- Added article " + article.title + " to database on row " + rowId + ".");
					
					// Add an intent to cache images (will be executed in sequence after this task is finished)
					if (preloadedArticles < MAX_ARTICLES_TO_PRELOAD) {
						Intent getImages = new Intent(this, ReaderFeedService.class);
						getImages.setAction(Constants.GET_ARTICLE_IMAGES_INTENT);
						getImages.putExtra(Constants.ARTICLE_CONTENT_EXTRA, article.content);  // TODO: is sending content through intent the best way?
						startService(getImages);
						preloadedArticles += 1;
					}
				} else {
					Log.v(TAG, "- Article " + article.title + " already in database.");
				}
			}
		} catch (SQLiteException e) {
			Log.e(TAG, "Cannot open database.");
			sendFeedResult(Constants.FEED_RESULT_DATABASE_ERROR);
			e.printStackTrace();
			return -1;
		} finally {
			if (db != null) {
				db.close();
			}
		}
		return newArticles;
	}

	private List<Article> fetchArticles(String urlString) {
		List<Article> articles = null;
		try {
			URL url = new URL(urlString);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setReadTimeout(10000 /* milliseconds */);
			urlConnection.setConnectTimeout(15000 /* milliseconds */);
			urlConnection.setRequestMethod("GET");
			try {
				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				articles = AtomParser.parse(in);
			} finally {
				urlConnection.disconnect();
			}
		} catch (MalformedURLException e) {
			sendFeedResult(Constants.FEED_RESULT_OTHER_ERROR);
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			sendFeedResult(Constants.FEED_RESULT_NET_ERROR);
			e.printStackTrace();
			return null;
		} catch (XmlPullParserException e) {
			Log.e(TAG, "The received XML was malformed or there was an error with parsing it.");
			sendFeedResult(Constants.FEED_RESULT_OTHER_ERROR);
			e.printStackTrace();
			return null;
		} catch (SecurityException e) {
			Log.e(TAG, "App doesn't have permission to access internet.");
			sendFeedResult(Constants.FEED_RESULT_OTHER_ERROR);
			e.printStackTrace();
			return null;
		}
		return articles;
	}

	private void sendFeedResult(int resultCode) {
		Intent localIntent = new Intent(Constants.FEED_RESULT_INTENT)
		.putExtra(Constants.FEED_RESULT_CODE, resultCode);
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		Log.v(TAG, "Sending broadcast done.");
	}
	
//	private static String readStream(InputStream in) {
//		java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
//		return s.hasNext() ? s.next() : null;
//	}
}
