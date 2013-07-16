package net.filiph.georgeous;

import net.filiph.georgeous.background.ReaderFeedService;
import net.filiph.georgeous.data.FeedProvider;
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
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements ArticleListFragment.Callbacks {
	private static final String TAG = "GeorgeousMainActivity";

	private static final String DATA_ALREADY_LOADED_FOR_FIRST_TIME = "DATA_LOADED_FOR_FIRST_TIME";
	public static final String LAST_CHECK_FINISHED_TIME = "LAST_CHECK_TIME";
	public static final String CHECK_IN_PROGRESS = "CHECK_IN_PROGRESS";
	public static final long MIN_TIME_BETWEEN_FEED_DOWNLOADS = 10*1000;
	public static final long MIN_TIME_TO_SHOW_GEORGE = 5000;
	
	private SharedPreferences mPrefs;
	
	private GeorgeFragment mGeorgeFragment;

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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		Log.v(TAG, "onCreate!");
		mPrefs = getPreferences(MODE_PRIVATE);
		
		if (findViewById(R.id.article_pane) != null) {
			mTwoPane = true;
		}
		
		if (!mPrefs.getBoolean(DATA_ALREADY_LOADED_FOR_FIRST_TIME, false)) {
			// The user has opened Georgeous for the first time.
			hideArticlesShowGeorge();
			mGeorgeAppearedTime = System.currentTimeMillis();
		} else if (mGeorgeFragment != null) {
			removeGeorge();
		}
		
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
	
	private void removeGeorge() {
		assert(mGeorgeFragment != null);
		Log.v(TAG, "Removing George. Bye, gorgeous.");
		FrameLayout container = (FrameLayout) findViewById(R.id.george_fragment_view);
		if (mGeorgeFragment.isVisible()) {
			collapse(container, mGeorgeFragment);
		}
	}

	private void hideArticlesShowGeorge() {
		if (mGeorgeFragment == null) {
			mGeorgeFragment = new GeorgeFragment();
		}
		Log.v(TAG, "Hiding articles, showing George! Hi!");
		FrameLayout container = (FrameLayout) findViewById(R.id.george_fragment_view);
		container.setVisibility(FrameLayout.VISIBLE);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.add(R.id.george_fragment_view, mGeorgeFragment);
		ft.commit();
		Log.v(TAG, "Articles hid, George shown!");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_articles, menu);
		return true;
	}
	
	public boolean onMenuClearClick(MenuItem item) {
		ContentResolver cr = getContentResolver();
		cr.delete(FeedProvider.ARTICLES_URI, null, null);
		
		mPrefs.edit()
		.putBoolean(DATA_ALREADY_LOADED_FOR_FIRST_TIME, false)
		.putLong(LAST_CHECK_FINISHED_TIME, 0)
		.putBoolean(CHECK_IN_PROGRESS, false)  // TODO: actually make sure it's not in progress
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

		// TODO: check if CHECK_IN_PROGRESS hasn't been set for way too long -> if so, release
		
		if (!mPrefs.getBoolean(CHECK_IN_PROGRESS, false) &&
				System.currentTimeMillis() - mPrefs.getLong(LAST_CHECK_FINISHED_TIME, 0) > MIN_TIME_BETWEEN_FEED_DOWNLOADS) {
			// Start the background service via an intent. 
			Intent refresh = new Intent(this, ReaderFeedService.class);
			refresh.setAction(Constants.GET_ARTICLES_INTENT);
			startService(refresh);
			
			mPrefs.edit().putBoolean(CHECK_IN_PROGRESS, true).commit();
			Log.v(TAG, "Starting background process to check for new articles.");
		} else {
			Log.v(TAG, "Checked recently or checking already in progress.");
		}
		
		if (mTwoPane) {
			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			((ArticleListFragment) getFragmentManager().findFragmentById(
					R.id.article_list_fragment)).setActivateOnItemClick(true);
		}
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
			
			if (!mPrefs.getBoolean(DATA_ALREADY_LOADED_FOR_FIRST_TIME, false)) {
				Log.v(TAG, "First time visitor has new articles.");
				long current = System.currentTimeMillis();
				if (current - mGeorgeAppearedTime > MIN_TIME_TO_SHOW_GEORGE) {
					removeGeorge();
				} else {
					// Delay execution.
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							removeGeorge();
						}
					}, MIN_TIME_TO_SHOW_GEORGE - (current - mGeorgeAppearedTime));
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
			.putBoolean(CHECK_IN_PROGRESS, false)
			.commit();
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
	
	/**
	 * Visually collapses a view via an animation and then sets its visibility to GONE.
	 * TODO: http://developer.android.com/guide/topics/resources/animation-resource.html
	 * @param v		The view to collapse. 
	 */
	private void collapse(final View v, final Fragment fragmentToRemove) {
	    final int initialHeight = v.getMeasuredHeight();
	    final int startHeight = initialHeight / 2;  // Start animation already 50% in.

	    if (fragmentToRemove != null) {
    		FragmentManager fm = getFragmentManager();
    		FragmentTransaction ft = fm.beginTransaction();
    		ft.remove(fragmentToRemove);
    		ft.commit();
	    }
	    
	    Animation a = new Animation()
	    {
	        @Override
	        protected void applyTransformation(float interpolatedTime, Transformation t) {
	            if (interpolatedTime == 1){
	                v.setVisibility(View.GONE);
	            } else {
	                v.getLayoutParams().height = (int)(startHeight) - (int)(startHeight * interpolatedTime);
	                v.requestLayout();
	            }
	        }

	        @Override
	        public boolean willChangeBounds() {
	            return true;
	        }
	    };

	    // 1dp/ms
	    a.setDuration((int)(startHeight / v.getContext().getResources().getDisplayMetrics().density));
	    v.startAnimation(a);
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
}
