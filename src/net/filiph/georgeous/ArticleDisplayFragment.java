package net.filiph.georgeous;

import net.filiph.georgeous.data.BlankImageGetter;
import net.filiph.georgeous.data.DbHelper;
import net.filiph.georgeous.data.FeedProvider;
import net.filiph.georgeous.data.ImageGetter;
import net.filiph.georgeous.data.ImageGetterWithManageSpace;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class ArticleDisplayFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = "ArticleDisplayFragment";
	private static final int ARTICLE_DISPLAY_ID = 1;
	private static final String SAVED_ARTICLE_ID = "SAVED_ARTICLE_ID";
	private static final String SAVED_SCROLL_POSITION = "SAVED_SCROLL_POSITION";
	private long mArticleId = -1;
	
	private ScrollView mScrollView;
	private int mScrollX = 0;
	private int mScrollY = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(SAVED_ARTICLE_ID)) {
				mArticleId = savedInstanceState.getLong(SAVED_ARTICLE_ID);
			}
			
			if (savedInstanceState.containsKey(SAVED_SCROLL_POSITION)) {
				final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
				if (position != null) {
					mScrollX = position[0];
					mScrollY = position[1];
				}
			}
		}
		
		View inflatedView = inflater.inflate(R.layout.display_article_fragment, container, false); 
		mScrollView = (ScrollView) inflatedView.findViewById(R.id.article_scroll_view);
		
		return inflatedView;
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.v(TAG, "onStart called, mArticleId = " + mArticleId);
		
		if (mArticleId != -1) {
			// mArticle was initialized from savedInstanceState.
			removeGeorgePlaceholder();
			loadArticle(mArticleId);
		} else {
			Intent intent = getActivity().getIntent();
			mArticleId = intent.getLongExtra(Constants.ARTICLE_ID_EXTRA, -1);

			if (mArticleId != -1) {
				removeGeorgePlaceholder();
				getLoaderManager().initLoader(ARTICLE_DISPLAY_ID, null, this);
			} else {
				ProgressBar progressCircle = (ProgressBar) getActivity().findViewById(R.id.progress_circle);
				progressCircle.setVisibility(ProgressBar.GONE);
			}
		}
	}

	/**
	 * 
	 */
	public void removeGeorgePlaceholder() {
		getActivity().findViewById(R.id.george_placeholder).setVisibility(View.GONE);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.v(TAG, "onCreateLoader called");

		if (mArticleId == -1) {
			throw new IllegalStateException("onCreateLoader called, but mArticleId is invalid");
		} else {
			return new CursorLoader(getActivity(), FeedProvider.getArticleByIdUri(mArticleId), null, null,
					null, null);
		}
	}

	public void loadArticle(long articleId) {
		Log.v(TAG, "loadArticleId called (" + articleId + ")");
		mArticleId = articleId;

		TextView titleView = (TextView) getActivity().findViewById(R.id.article_display_title);
		TextView contentView = (TextView) getActivity().findViewById(R.id.article_content);
		titleView.setText(R.string.empty);
		contentView.setText(R.string.empty);
		removeGeorgePlaceholder();
		ProgressBar progressCircle = (ProgressBar) getActivity().findViewById(R.id.progress_circle);
		progressCircle.setVisibility(ProgressBar.VISIBLE);

		// Ask loader to fetch the article from SQLite.
		getLoaderManager().restartLoader(ARTICLE_DISPLAY_ID, null, this);
	}
	
	private int getWidth() {
		TextView contentView = (TextView) getActivity().findViewById(R.id.article_content);
		if (contentView == null) { 
			return 0;
		} else {
			return contentView.getWidth() - contentView.getTotalPaddingLeft() - contentView.getTotalPaddingRight();
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Use AsyncTask to parse the HTML content and add images.
		new ArticleDisplayTask(mArticleId, false).execute(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(SAVED_ARTICLE_ID, mArticleId);
		outState.putIntArray(SAVED_SCROLL_POSITION, new int[] { mScrollView.getScrollX(), mScrollView.getScrollY() });
	}
	
	private class ArticleDisplayTask extends AsyncTask<Cursor, Void, CharSequence[]> {
		/**
		 * Whether to get images with the content. Default behaviour: first time around,
		 * don't get images (so we have a fast display of text), then immediately after
		 * showing text go back and try to bring all the images, too. 
		 */
		private final boolean mGetImages;
		private String title;
		private String contentHtml;
		private final long mLoadingArticleId;
		
		public ArticleDisplayTask(long articleId, boolean getImages) {
			mGetImages = getImages;
			title = null;
			contentHtml = null;
			mLoadingArticleId = articleId;
		}
		
		public ArticleDisplayTask(long articleId, boolean getImages, String title, String contentHtml) {
			mGetImages = getImages;
			this.title = title;
			this.contentHtml = contentHtml;
			mLoadingArticleId = articleId;
		}

		@Override
		protected CharSequence[] doInBackground(Cursor... params) {
			Activity activity = getActivity();
			if (activity == null) {
				this.cancel(true);
				return null;
			}
			
			if (title == null && contentHtml == null) {
				assert(params != null);
				assert(params.length == 1);
				Cursor data = params[0];
				
				if (data.isClosed()) {
					this.cancel(true);
					return null;
				}
				
				if (data.moveToFirst()) {
					title = data.getString(data.getColumnIndexOrThrow(DbHelper.KEY_TITLE));
					contentHtml = data.getString(data.getColumnIndexOrThrow(DbHelper.KEY_CONTENT));
				}
				
				if (title == null || contentHtml == null) {
					throw new IllegalStateException("The cursor received an article with null contents.");
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
				imgGetter = new ImageGetter(
						getActivity().getExternalCacheDir(), 
						getResources(),
						metrics,
						false);
			} else {
				imgGetter = new BlankImageGetter(getResources(), metrics);
			}
			CharSequence content = Html.fromHtml(contentHtml, imgGetter, null);  // This is expensive.
			imgGetter.manageSpace();

			return new CharSequence[] {title, content};
		}


		@Override
		protected void onPostExecute(CharSequence[] strings) {
			Activity activity = getActivity();
			if (activity == null || mLoadingArticleId != mArticleId) {
				// We fetched the article but the user is already elsewhere. :( Some manners!
				return;
			}
			if (strings != null && strings.length >= 2) {
				CharSequence title = strings[0];
				CharSequence content = strings[1];
				TextView titleView = (TextView) getActivity().findViewById(R.id.article_display_title);
				TextView contentView = (TextView) getActivity().findViewById(R.id.article_content);
				ProgressBar progressCircle = (ProgressBar) getActivity().findViewById(R.id.progress_circle);
				if (titleView != null && contentView != null && progressCircle != null) {
					titleView.setText(title);
					contentView.setMovementMethod(LinkMovementMethod.getInstance());
					contentView.setText(content);
					progressCircle.setVisibility(ProgressBar.GONE);
					mScrollView.setScrollX(mScrollX); // TODO: make this work
					mScrollView.setScrollY(mScrollY);
					
					if (!mGetImages) {
						// Go again, this time with images.
						new ArticleDisplayTask(mLoadingArticleId, true, this.title, contentHtml).execute();
					}
				} else {
					// We fetched the article but the user is already elsewhere. :( Some manners!
				}
			} else {
				// TODO: handle
			}
		}

	}
}
