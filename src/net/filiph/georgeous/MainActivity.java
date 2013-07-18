package net.filiph.georgeous;

import net.filiph.georgeous.background.ReaderFeedService;
import net.filiph.georgeous.data.FeedProvider;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements ArticleListFragment.Callbacks, ArticleDisplayFragment.ArticleShownListener {
	private static final String TAG = "GeorgeousMainActivity";

	private static final String DATA_ALREADY_LOADED_FOR_FIRST_TIME = "DATA_LOADED_FOR_FIRST_TIME";
	public static final String LAST_CHECK_FINISHED_TIME = "LAST_CHECK_TIME";
	public static final String CHECK_IN_PROGRESS = "CHECK_IN_PROGRESS";
	public static final long MIN_TIME_BETWEEN_FEED_DOWNLOADS = 10*1000;
	public static final long MIN_TIME_TO_SHOW_GEORGE = 5000;
	
	private SharedPreferences mPrefs;
	private boolean mCheckInProgress = false;
	
	private long mLatestArticleId = -1;
	
	private View mGeorgeGreeter;
	
	private static final IntentFilter receiverIntentFilter;
	static {
		receiverIntentFilter = new IntentFilter();
		receiverIntentFilter.addAction(Constants.FEED_RESULT_INTENT);
		receiverIntentFilter.addAction(Constants.IMAGES_CACHED_INTENT);
	}
	
	private final MainActivityReceiver mReceiver = new MainActivityReceiver();

	/**
	 * The time when we showed George, or null if we didn't. The value is used to give user enough time to read what George has to say.
	 */
	private long mGeorgeAppearedTime;
	
	private long mLastCheckTime;

	private boolean mTwoPane = false;

	private long mShortAnimationDuration;

	private ShareActionProvider mShareActionProvider;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.main_activity);
		
		Log.v(TAG, "onCreate!");
		mPrefs = getPreferences(MODE_PRIVATE);
		
		mGeorgeGreeter = findViewById(R.id.george_greeter);
		mShortAnimationDuration = getResources().getInteger(
				android.R.integer.config_shortAnimTime);
		
		if (findViewById(R.id.article_pane) != null) {
			mTwoPane = true;
			invalidateOptionsMenu();
			
			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			((ArticleListFragment) getFragmentManager().findFragmentById(
					R.id.article_list_fragment)).setActivateOnItemClick(true);
			
		}
		
		if (!mPrefs.getBoolean(DATA_ALREADY_LOADED_FOR_FIRST_TIME, false)) {
			// The user has opened Georgeous for the first time.
			showGeorgeGreeter();
			mGeorgeAppearedTime = System.currentTimeMillis();
			sendArticleListRefreshIntent();
		}
		
		notifyArticleListDatasetChanged();
		
//		loadFromInstanceState(savedInstanceState);
		
//		Button button = (Button) findViewById(R.id.welcome_button);
//		button.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//				collapse(findViewById(R.id.welcome_layout));
//			}
//		});
	}
	
	private MenuItem mShareItem;
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
	    MenuItem shareItem = menu.findItem(R.id.menu_share);
	    shareItem.setVisible(mTwoPane);
	    // TODO enabled according to article shown
	    
	    MenuItem refreshItem = menu.findItem(R.id.menu_refresh);
	    refreshItem.setVisible(!mCheckInProgress);
	    
	    // Locate MenuItem with ShareActionProvider, store it
	    mShareItem = menu.findItem(R.id.menu_share);
	    mShareActionProvider = (ShareActionProvider) mShareItem.getActionProvider();
	    
	    super.onPrepareOptionsMenu(menu);
	    return true;
	}
	
	private void fadeOutGeorgeGreeter() {
		mGeorgeGreeter.animate()
			.alpha(0f)
			.setDuration(mShortAnimationDuration)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mGeorgeGreeter.setVisibility(View.GONE);
				}
			});
	}
	
	private void showGeorgeGreeter() {
		mGeorgeGreeter.setVisibility(View.VISIBLE);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.list_articles, menu);
		
	    // Locate MenuItem with ShareActionProvider, store it
	    MenuItem item = menu.findItem(R.id.menu_share);
	    mShareActionProvider = (ShareActionProvider) item.getActionProvider();

		return true;
	}
	
	public boolean onMenuRefreshClick(MenuItem item) {
		sendArticleListRefreshIntent();
		return true;
	}
	
	public boolean onMenuClearClick(MenuItem item) {
		ContentResolver cr = getContentResolver();
		cr.delete(FeedProvider.ARTICLES_URI, null, null);
		
		mPrefs.edit()
		.putBoolean(DATA_ALREADY_LOADED_FOR_FIRST_TIME, false)
		.putLong(LAST_CHECK_FINISHED_TIME, 0)
		.commit();
		
		notifyArticleListDatasetChanged();
		return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Register the intents coming from our ReaderFeedService with LocalBroadcastManager.
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mReceiver, receiverIntentFilter);

		mCheckInProgress = false;  // The return intent may have arrived in the meantime. Let's not prevent user from hitting refresh again.
		setProgressBarIndeterminateVisibility(false);
		invalidateOptionsMenu();
		
	}

	/**
	 * Start the background service via an intent. 
	 */
	public void sendArticleListRefreshIntent() {
		Intent refresh = new Intent(this, ReaderFeedService.class);
		refresh.setAction(Constants.GET_ARTICLES_INTENT);
		startService(refresh);
		
		setProgressBarIndeterminateVisibility(true);
		mCheckInProgress = true;
		invalidateOptionsMenu();
	}
	
	
//	public void setProgressPercent(int percent) {
//		ProgressBar progressBar = (ProgressBar) findViewById(R.id.welcome_progress_bar);
//		progressBar.setProgress(percent);
//	}

//	public void populateWith(ArticleList result) {
//		ProgressBar progressBar = (ProgressBar) findViewById(R.id.welcome_progress_bar);
//		progressBar.setProgress(100);
//		
//		findViewById(R.id.list_progress_spinner).setVisibility(View.GONE);
//		findViewById(R.id.articles_list_view).setVisibility(View.VISIBLE);
//	}
	
	/**
	 * Broadcast receiver for receiving status updates from the IntentService.
	 */
	private class MainActivityReceiver extends BroadcastReceiver {
		// Prevents instantiation
	    private MainActivityReceiver() {}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.FEED_RESULT_INTENT)) {
				handleFeedResultIntent(intent);
			} else if (intent.getAction().equals(Constants.IMAGES_CACHED_INTENT)) {
				// TODO (maybe we don't want to catch that intent here? but in article fragment?
			} else {
				Log.e(TAG, "Wrong intent received.");
			}
		}

		/**
		 * Receives the message that the RSS was received (or not) and takes care of notifying the user and the fragments.
		 * 
		 * @param intent
		 */
		private void handleFeedResultIntent(Intent intent) {
			Log.v(TAG, "Received intent from a service!");
			int feedResult = intent.getIntExtra(Constants.FEED_RESULT_CODE, 0);
			Log.v(TAG, "The service says: '" + feedResult + "'. Yay!");
			
			boolean willInvalidateOptionsMenuLater = false;
			if (!mPrefs.getBoolean(DATA_ALREADY_LOADED_FOR_FIRST_TIME, false)) {   // TODO: read mPrefs only on load, not on UI thread
				Log.v(TAG, "First time visitor has new articles.");
				long current = System.currentTimeMillis();
				if (current - mGeorgeAppearedTime > MIN_TIME_TO_SHOW_GEORGE) {
					fadeOutGeorgeGreeter();
				} else {
					// Delay execution.
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							fadeOutGeorgeGreeter();
							invalidateOptionsMenu();
							setProgressBarIndeterminateVisibility(false);
						}
					}, MIN_TIME_TO_SHOW_GEORGE - (current - mGeorgeAppearedTime));
					willInvalidateOptionsMenuLater = true;
				}
				notifyArticleListDatasetChanged();
				mPrefs.edit().putBoolean(DATA_ALREADY_LOADED_FOR_FIRST_TIME, true).commit();
			} else if (feedResult == Constants.FEED_RESULT_NEW_ARTICLES) {
				Log.v(TAG, "n-th time visitor has new articles.");
				notifyArticleListDatasetChanged();
			} else {
				Log.v(TAG, "No new articles.");
				makeGeorgeToast(getString(R.string.no_new_articles_toast));
			}

			mLastCheckTime = System.currentTimeMillis();
			mPrefs.edit()
			.putLong(LAST_CHECK_FINISHED_TIME, mLastCheckTime)
			.commit();
			
			mCheckInProgress = false;
			
			if (!willInvalidateOptionsMenuLater) {
				invalidateOptionsMenu();
				setProgressBarIndeterminateVisibility(false);
			}
		}
	}
	
	private void makeGeorgeToast(CharSequence msg) {
//		LinearLayout v = new LinearLayout(getApplicationContext());
//		v.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
//		TextView t = new TextView(getApplicationContext());
//		t.setText(msg);
//		ImageView img = new ImageView(getApplicationContext());
//		img.setimage;;
		
		LayoutInflater inflater = getLayoutInflater();
	    View view = inflater.inflate(R.layout.george_toast, (ViewGroup) findViewById(R.id.george_toast));
	    ((TextView) view.findViewById(R.id.george_toast_text)).setText(msg);

	    Toast toast = new Toast(getApplicationContext());
	    toast.setDuration(Toast.LENGTH_LONG);  // TODO: according to length of msg
	    toast.setView(view);
	    toast.show();
	}
	
	public void notifyArticleListDatasetChanged() {
		Log.v(TAG, "Notifying on dataset changed.");
		ArticleListFragment articleListFragment = 
				(ArticleListFragment) getFragmentManager().findFragmentById(R.id.article_list_fragment);
		if (articleListFragment == null) {
			// TODO: set pref to reload on create?
			Log.v(TAG, "- Fragment not found.");
		} else {
			Log.v(TAG, "- Fragment found.");
			articleListFragment.notifyDatasetChanged();
		}
	}

	@Override
	public void onItemSelected(long id) {
		mLatestArticleId = id;
		if (!mTwoPane) {
			Intent intent = new Intent(this, ArticleDisplayActivity.class);
			intent.putExtra(Constants.ARTICLE_ID_EXTRA, id);
			startActivity(intent);
		} else {
			((ArticleDisplayFragment) getFragmentManager()
					.findFragmentById(R.id.article_display_fragment))
					.loadArticle(id);
		}
	}

	@Override
	public void onArticleShow(String url) {
		if (url != null) {
			Log.w(TAG, "Showing article: " + url);
			if (mShareItem != null && mShareActionProvider != null) {
				mShareItem.setEnabled(true);
				
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.putExtra(Intent.EXTRA_TEXT, url);
				shareIntent.setType("text/plain");
				mShareActionProvider.setShareIntent(shareIntent);
			}
		}
	}

	@Override
	public void onArticleHide() {
		if (mShareItem != null) {
			mShareItem.setEnabled(false);
		}
	}
}
