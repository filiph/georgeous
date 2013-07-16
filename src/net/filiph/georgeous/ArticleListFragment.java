package net.filiph.georgeous;

import net.filiph.georgeous.data.DbHelper;
import net.filiph.georgeous.data.FeedProvider;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;

public class ArticleListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = "ArticleListFragment";
	private static final int ARTICLE_LIST_ID = 0;
	
	private static final String SAVED_LISTVIEW_INDEX = "SAVED_LISTVIEW_INDEX";
	private static final String SAVED_LISTVIEW_TOP = "SAVED_LISTVIEW_TOP";
	private static final String SAVED_ACTIVATED_POSITION = "SAVED_ACTIVATED_POSITION";
	
	private SimpleCursorAdapter mAdapter;
	
	private int mIndex;
	private int mTop;
	
	// The current activity to call with callbacks.
	private Callbacks mCallbacks = sDummyCallbacks;
	
	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(long id);
	}
	
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(long id) {
			Log.w(TAG, "Item selected but no proper callback was setup.");
		}
	};
	
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ArticleListFragment() {
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mIndex = savedInstanceState.getInt(SAVED_LISTVIEW_INDEX, 0);
			mTop = savedInstanceState.getInt(SAVED_LISTVIEW_TOP, 0);
			mActivatedPosition = savedInstanceState.getInt(SAVED_ACTIVATED_POSITION, ListView.INVALID_POSITION);
			Log.v(TAG, "Loaded instance state: " + mIndex + ":" + mTop);
		}
		
		View inflatedView = inflater.inflate(R.layout.list_articles_fragment, container, false);
		mListView = (ListView) inflatedView.findViewById(R.id.article_list);
		mListView.setOnItemClickListener(onItemClickListener);
		
		return inflatedView;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Log.v(TAG, "onStart called");
		
		String[] fromColumns = {DbHelper.KEY_TITLE, DbHelper.KEY_CANONICAL_URL};
		int[] toViews = {R.id.article_in_list_title, R.id.more_info};
		
		mAdapter = new SimpleCursorAdapter(getActivity(), 
				R.layout.each_article_in_list, null, 
				fromColumns, toViews, 0);
		mListView.setAdapter(mAdapter);
		
		getLoaderManager().initLoader(ARTICLE_LIST_ID, null, this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mListView.setSelectionFromTop(mIndex, mTop);
		setActivatedPosition(mActivatedPosition);
	}
	
	OnItemClickListener onItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.v(TAG, "Clicked on id " + id + " on position " + position + ".");
//			setActivatedPosition(position);
			mActivatedPosition = position;
			mCallbacks.onItemSelected(id);
		}
	};
	private ListView mListView = null;
	private int mActivatedPosition = ListView.INVALID_POSITION;
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mListView != null) {
			// save index and top position of ArticleList
			mIndex = mListView.getFirstVisiblePosition();
			View v = mListView.getChildAt(0);
			mTop = (v == null) ? 0 : v.getTop();
			outState.putInt(SAVED_LISTVIEW_INDEX, mIndex);
			outState.putInt(SAVED_LISTVIEW_TOP, mTop);
			outState.putInt(SAVED_ACTIVATED_POSITION, mActivatedPosition);
			Log.v(TAG, "Saving instance state to: " + mIndex + ":" + mTop);
		} else {
			Log.w(TAG, "Couldn't find listview in MainActivity.");
		}
		
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.v(TAG, "onCreateLoader called");
		return new CursorLoader(getActivity(), FeedProvider.ARTICLES_URI, null, null,
	            null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.v(TAG, "onLoadFinished called");
		if (data == null) {
			Log.e(TAG, "- cursor is null!");
		} else {
			Log.v(TAG, "- the cursor has length " + data.getCount());
		}
		mAdapter.swapCursor(data);
		// TODO: if data is 0 rows long, then notify main activity
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
	
	public void notifyDatasetChanged() {
		// mAdapter.notifyDataSetChanged(); // this doesn't do anything
		getLoaderManager().restartLoader(ARTICLE_LIST_ID, null, this);
	}
	
	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		Log.v(TAG, "Setting activateOnItemClick to " + activateOnItemClick);
		mListView.setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}
	
	private void setActivatedPosition(int position) {
		Log.v(TAG, "Setting activated position to " + position);
		if (position == ListView.INVALID_POSITION) {
			mListView.setItemChecked(mActivatedPosition, false);
		} else {
			mListView.setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}
}
