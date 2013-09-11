package net.filiph.georgeous;

import net.filiph.georgeous.data.BlankImageGetter;
import net.filiph.georgeous.data.FeedContract;
import net.filiph.georgeous.data.ImageGetter;
import net.filiph.georgeous.data.ImageGetterWithManageSpace;

import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Fragment that contains the contents of an article. It uses TextView for showing the contents (as
 * opposed to WebView) - that would not be necessary, but it allows for more control.
 */
public class ArticleDisplayFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDisplayFragment";
    private static final int ARTICLE_DISPLAY_LOADER_ID = 1;
    private static final String SAVED_ARTICLE_ID = "SAVED_ARTICLE_ID";
    private static final String SAVED_Y_POSITION = "SAVED_Y_POSITION";

    private long mArticleId = -1;

    private ScrollView mScrollView;
    /**
     * Used to persist the relative scroll position of the article for when device orientation
     * changes. 0f is top, 1f is bottom.
     */
    private float mYRelativePosition = 0f;

    /**
     * The fragment is showing the current article in the first pass (usually without images). It is
     * OK to scroll to a previously saved position automatically (user hasn't had the chance to
     * scroll themselves yet).
     */
    private boolean mFirstPass = true;

    private static ArticleShownListener sDummyCallbacks = new ArticleShownListener() {

        @Override
        public void onArticleHide() {
            Log.w(TAG, "Article hidden but no proper callback was setup.");
        }

        @Override
        public void onArticleShow(String url) {
            Log.w(TAG, "Article shown but no proper callback was setup.");
        }

    };

    private ArticleShownListener mCallbacks = sDummyCallbacks;

    /**
     * Shows an article with the given id. The fragment uses a LoaderManager to fetch the article
     * asynchronously, and then uses an AsyncTask to build the Spanned for the TextView.
     */
    public void loadArticle(long articleId) {
        mArticleId = articleId;

        TextView titleView = (TextView) getActivity().findViewById(R.id.article_display_title);
        TextView contentView = (TextView) getActivity().findViewById(R.id.article_content);
        titleView.setText(R.string.empty);
        contentView.setText(R.string.empty);
        removeGeorgePlaceholder();
        ProgressBar progressCircle = (ProgressBar) getActivity().findViewById(R.id.progress_circle);
        progressCircle.setVisibility(View.VISIBLE);

        // Ask loader to fetch the article from SQLite.
        getLoaderManager().restartLoader(ARTICLE_DISPLAY_LOADER_ID, null, this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof ArticleShownListener)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (ArticleShownListener) activity;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mArticleId == -1) {
            throw new IllegalStateException("onCreateLoader called, but mArticleId is invalid");
        } else {
            return FeedContract.getArticleByIdLoader(getActivity(), mArticleId);
        }
    }

    @Override
    public View
            onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SAVED_ARTICLE_ID)) {
                mArticleId = savedInstanceState.getLong(SAVED_ARTICLE_ID);
            }

            if (savedInstanceState.containsKey(SAVED_Y_POSITION)) {
                mYRelativePosition = savedInstanceState.getFloat(SAVED_Y_POSITION, 0f);
            }
        }

        View inflatedView = inflater.inflate(R.layout.display_article_fragment, container, false);
        mScrollView = (ScrollView) inflatedView.findViewById(R.id.article_scroll_view);

        return inflatedView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks.onArticleHide();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // This method is called by the LoaderManager, so this must be the first
        // pass.
        mFirstPass = true;

        // If the user has scrolled already, it means there was time for getting
        // images (maybe before an orientation change?). So let's not do the
        // two-pass thing now and instead load both text and the images on the
        // first pass.
        boolean loadImagesOnFirstPass = mYRelativePosition != 0f;

        // Use AsyncTask to parse the HTML content and add images.
        new ArticleDisplayTask(mArticleId, loadImagesOnFirstPass).execute(data);
    }

    @Override
    public void onPause() {
        super.onPause();
        mYRelativePosition = mScrollView.getScrollY() / mScrollView.getMaxScrollAmount();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(SAVED_ARTICLE_ID, mArticleId);
        outState.putFloat(SAVED_Y_POSITION, mYRelativePosition);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mArticleId != -1) {
            // mArticle was initialized from savedInstanceState.
            removeGeorgePlaceholder();
            loadArticle(mArticleId);
        } else {
            Intent intent = getActivity().getIntent();
            mArticleId = intent.getLongExtra(Constants.ARTICLE_ID_EXTRA, -1);

            if (mArticleId != -1) {
                removeGeorgePlaceholder();
                getLoaderManager().initLoader(ARTICLE_DISPLAY_LOADER_ID, null, this);
            } else {
                ProgressBar progressCircle =
                        (ProgressBar) getActivity().findViewById(R.id.progress_circle);
                progressCircle.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Removes George. This might be a more elaborate method in the future.
     */
    public void removeGeorgePlaceholder() {
        getActivity().findViewById(R.id.george_placeholder).setVisibility(View.GONE);
    }

    private int getWidth() {
        TextView contentView = (TextView) getActivity().findViewById(R.id.article_content);
        if (contentView == null) {
            return 0;
        } else {
            return contentView.getWidth() - contentView.getTotalPaddingLeft()
                    - contentView.getTotalPaddingRight();
        }
    }

    /**
     * Interface that must be implemented by the parent Activity. This allows the fragment to inform
     * the activity on the currently displayed article.
     */
    public interface ArticleShownListener {
        /**
         * Callback for when the fragment doesn't show any article anymore.
         */
        public void onArticleHide();

        /**
         * Callback for when article has been shown to the user.
         */
        public void onArticleShow(String url);
    }

    /**
     * This AsyncTask is called after we load the article's contents from SQLite. Converting the
     * HTML to a Spanned (for use in TextView) is quite an expensive task that would otherwise block
     * the UI thread.
     * 
     * The task is ordinarily called in two passes. First, we show the text of the article (which is
     * guaranteed to be accessible offline, i.e. fast) so that user has almost immediately something
     * to read.
     * 
     * Next, we call the ArticleDisplay Task again (from within its own onPostExecute method) so
     * that it can load the images.
     */
    private class ArticleDisplayTask extends AsyncTask<Cursor, Void, CharSequence[]> {
        public ArticleDisplayTask(long articleId, boolean getImages) {
            mGetImages = getImages;
            title = null;
            contentHtml = null;
            mLoadingArticleId = articleId;
        }

        public ArticleDisplayTask(long articleId, boolean getImages, String title,
                String contentHtml, String url) {
            mGetImages = getImages;
            this.title = title;
            this.contentHtml = contentHtml;
            this.url = url;
            mLoadingArticleId = articleId;
        }

        /**
         * Whether to get images with the content. Default behaviour: first time around, don't get
         * images (so we have a fast display of text), then immediately after showing text go back
         * and try to bring all the images, too.
         */
        private final boolean mGetImages;
        private String title;
        private String contentHtml;
        private String url;

        private final long mLoadingArticleId;

        @Override
        protected CharSequence[] doInBackground(Cursor... params) {
            Activity activity = getActivity();
            if (activity == null) {
                this.cancel(true);
                return null;
            }

            if (title == null || contentHtml == null || url == null) {
                assert (params != null);
                assert (params.length == 1);
                Cursor data = params[0];

                if (data.isClosed()) {
                    this.cancel(true);
                    return null;
                }

                if (data.moveToFirst()) {
                    title = data.getString(data.getColumnIndexOrThrow(FeedContract.KEY_TITLE));
                    contentHtml =
                            data.getString(data.getColumnIndexOrThrow(FeedContract.KEY_CONTENT));
                    url =
                            data.getString(data
                                    .getColumnIndexOrThrow(FeedContract.KEY_CANONICAL_URL));
                }

                if (title == null || contentHtml == null) {
                    throw new IllegalStateException(
                            "The cursor received article with null contents.");
                }
                data.close();
            }

            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int contentWidth = getWidth();
            if (contentWidth != 0) {
                metrics.widthPixels = getWidth();
            }
            ImageGetterWithManageSpace imgGetter = null;
            if (mGetImages) {
                imgGetter =
                        new ImageGetter(getActivity().getExternalCacheDir(), getResources(),
                                metrics, false);
            } else {
                imgGetter = new BlankImageGetter(getResources(), metrics);
            }
            // Next is the expensive part
            CharSequence content =
                    Html.fromHtml(contentHtml, imgGetter, new IgnoreStyleTagHandler());
            imgGetter.manageSpace();

            return new CharSequence[] { title, content };
        }

        private class IgnoreStyleTagHandler implements Html.TagHandler {
            int startStyle = -1;

            @Override
            public void
                    handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
                if (tag.equalsIgnoreCase("style")) {
                    if (opening) {
                        Log.d(TAG, "Encountered opening <style> tag.");
                        int len = output.length();
                        // Mark start of <style>.
                        startStyle = len;
                        // output.setSpan(new StrikethroughSpan(), len, len,
                        // Spannable.SPAN_MARK_MARK);
                    } else {
                        Log.d(TAG, "Encountered closing </style> tag.");
                        if (startStyle != -1) {
                            output.delete(startStyle, output.length());
                            startStyle = -1;
                        }
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(CharSequence[] strings) {
            Activity activity = getActivity();
            if (activity == null || mLoadingArticleId != mArticleId) {
                // We fetched the article but the user is already elsewhere. :(
                // Some manners!
                return;
            }
            if (strings != null && strings.length >= 2) {
                CharSequence title = strings[0];
                CharSequence content = strings[1];
                TextView titleView =
                        (TextView) getActivity().findViewById(R.id.article_display_title);
                JellyBeanSpanFixTextView contentView =
                        (JellyBeanSpanFixTextView) getActivity().findViewById(R.id.article_content);
                ProgressBar progressCircle =
                        (ProgressBar) getActivity().findViewById(R.id.progress_circle);
                if (titleView != null && contentView != null && progressCircle != null) {
                    titleView.setText(title);
                    contentView.setMovementMethod(LinkMovementMethod.getInstance());
                    try {
                        contentView.setText(content);
                    } catch (IndexOutOfBoundsException e) {
                        // The JellyBeanSpanFixTextView fix doesn't work in all
                        // cases. Fall back to plain text.
                        contentView.setText(content.toString());
                        // TODO: Apologize to the user?
                        e.printStackTrace();
                    }
                    progressCircle.setVisibility(View.GONE);
                    if (mFirstPass) {
                        mScrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                // Good enough for now. TODO: better.
                                int pos =
                                        (int) (mScrollView.getMaxScrollAmount() * mYRelativePosition);
                                mScrollView.setScrollY(pos);
                            }
                        });
                    }

                    if (!mGetImages) {
                        // Go again, this time with images.
                        mFirstPass = false;
                        new ArticleDisplayTask(mLoadingArticleId, true, this.title,
                                this.contentHtml, this.url).execute();
                    } else {
                        // This was the final pass.
                        mCallbacks.onArticleShow(url);
                    }
                } else {
                    // We fetched the article but the user is already elsewhere.
                    // :( Some manners!
                }
            } else {
                Log.w(TAG, "Bad call to onPostExecute.");
            }
        }

    }
}
