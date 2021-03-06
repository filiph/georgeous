package net.filiph.georgeous.background;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import net.filiph.georgeous.Constants;
import net.filiph.georgeous.data.Article;
import net.filiph.georgeous.data.FeedContract;
import net.filiph.georgeous.data.ImageGetter;
import net.filiph.georgeous.data.ImageGetterWithManageSpace;

import org.xmlpull.v1.XmlPullParserException;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * This is the background IntentService in charge of fetching the Atom feed, parsing it, and saving
 * the data in the SQLite database. It notifies the MainActivity on completion (success, failure, no
 * new articles).
 */
public class ReaderFeedService extends IntentService {
    private static final String TAG = "GeorgeousReaderFeedService";

    /**
     * The (hardcoded) url of the feed. TODO: make this a setting.
     */
    private static final String FEED_URL = "http://android-developers.blogspot.com/atom.xml";

    private static final int MAX_ARTICLES_TO_PRELOAD = 5;

    public ReaderFeedService() {
        super("ReaderFeedIntentService");
    }

    /**
     * Connects to the Internet, fetches the Atom feed, parses it, and returns the articles.
     */
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

    /**
     * Parses the given HTML and caches the contained images.
     */
    private void getArticleImages(String contentHtml) {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File externalCacheDir = getExternalCacheDir();
            if (externalCacheDir == null) {
                Log.i(TAG, "Could not find or create external cache directory.");
                return;
            }

            // We don't care about metrics (the images won't be shown and the
            // bounds are not saved)
            DisplayMetrics metrics = new DisplayMetrics();

            ImageGetterWithManageSpace imgGetter =
                    new ImageGetter(externalCacheDir, getResources(), metrics, true);
            // Now let's make imgGetter do the work. It will fetch the images
            // and cache them.
            Html.fromHtml(contentHtml, imgGetter, null);
            imgGetter.manageSpace();
        } else {
            Log.i(TAG, "External storage cannot be mounted " + "- skipping image caching.");
        }

    }

    /**
     * Fetches the Atom XML and puts the metadata and HTML contents into the SQLite database.
     */
    private void getArticles(String urlString) {
        List<Article> articles = fetchArticles(urlString);

        if (articles != null) {
            Log.v(TAG, "Found " + articles.size() + " articles.");
            int newArticles = insertArticlesIntoDatabase(articles);

            if (newArticles == -1) {
                return; // Something went wrong in insertArticlesIntoDatabase.
            } else if (newArticles > 0) {
                sendFeedResult(Constants.FEED_RESULT_NEW_ARTICLES);
            } else {
                sendFeedResult(Constants.FEED_RESULT_NO_NEW_ARTICLES);
            }
        }
    }

    /**
     * Inserts articles into the SQLite database.
     * 
     * @return The number of articles that previously weren't in the database.
     */
    private int insertArticlesIntoDatabase(List<Article> articles) {
        int newArticles = 0;
        int preloadedArticles = 0;

        try {
            for (Article article : articles) {
                ContentValues articleValues = FeedContract.articleToContentValues(article);
                Uri newUri = getContentResolver().insert(FeedContract.ARTICLES_URI, articleValues);

                if (newUri != null) {
                    newArticles += 1;
                    Log.v(TAG, "- Added article " + article.title + " to database on row URI "
                            + newUri.toString() + ".");

                    // Add an intent to cache images (will be executed in
                    // sequence after this task is finished)
                    if (preloadedArticles < MAX_ARTICLES_TO_PRELOAD) {
                        Intent getImages = new Intent(this, ReaderFeedService.class);
                        getImages.setAction(Constants.GET_ARTICLE_IMAGES_INTENT);
                        getImages.putExtra(Constants.ARTICLE_CONTENT_EXTRA, article.content);
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
        }
        return newArticles;
    }

    /**
     * Sends the result code to interested broadcast receivers (e.g. MainActivity).
     */
    private void sendFeedResult(int resultCode) {
        Intent localIntent =
                new Intent(Constants.FEED_RESULT_INTENT).putExtra(Constants.FEED_RESULT_CODE,
                        resultCode);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(Constants.GET_ARTICLES_INTENT)) {
            String urlString = FEED_URL; // TODO: get url from intent
            getArticles(urlString);
        } else if (intent.getAction().equals(Constants.GET_ARTICLE_IMAGES_INTENT)) {
            String articleContent = intent.getStringExtra(Constants.ARTICLE_CONTENT_EXTRA);
            Log.v(TAG, "Get article images for content.");
            getArticleImages(articleContent);
        } else {
            Log.e(TAG, "Wrong intent received.");
        }
    }
}