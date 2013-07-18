package net.filiph.georgeous;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.support.v4.app.NavUtils;

public class ArticleDisplayActivity extends Activity implements
		ArticleDisplayFragment.ArticleShownListener {

	private static final String TAG = "ArticleDisplayActivity";

	private ShareActionProvider mShareActionProvider;
	private MenuItem mShareItem;

	@Override
	public void onArticleHide() {
		if (mShareItem != null) {
			mShareItem.setEnabled(false);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.article_display, menu);

		// Locate MenuItem with ShareActionProvider, store it
		MenuItem item = menu.findItem(R.id.menu_share);
		mShareActionProvider = (ShareActionProvider) item.getActionProvider();

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mShareItem = menu.findItem(R.id.menu_share);
		mShareItem.setVisible(true);

		super.onPrepareOptionsMenu(menu);
		return true;
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.display_article_activity);
		// Show the Up button in the action bar.
		setupActionBar();
	}

}
