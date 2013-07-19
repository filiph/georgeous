package net.filiph.georgeous;

import net.filiph.georgeous.data.DbHelper;
import net.filiph.georgeous.data.FeedProvider;
import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * The fragment loads and shows the list of articles in the SQLite database. It notifies the parent
 * activity on item selection through Callbacks interface that the Activity needs to implement.
 */
public class ArticleListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleListFragment";

    private static final int ARTICLE_LIST_LOADER_ID = 0;

    private static final String SAVED_ACTIVATED_POSITION = "SAVED_ACTIVATED_POSITION";
    private static final String SAVED_SCROLL_INDEX = "SAVED_SCROLL_INDEX";
    private static final String SAVED_SCROLL_TOP = "SAVED_SCROLL_TOP";

    private int mScrollIndex = 0;
    private int mScrollTop = 0;

    private SimpleCursorAdapter mAdapter;

    // The current activity to call with callbacks.
    private Callbacks mCallbacks = sDummyCallbacks;

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id) {
            Log.w(TAG, "Item selected but no proper callback was setup.");
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
     * screen orientation changes).
     */
    public ArticleListFragment() {
    }

    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * Called when the dataset in the database has changed and needs to be reloaded.
     */
    public void onDatasetChanged() {
        getLoaderManager().restartLoader(ARTICLE_LIST_LOADER_ID, null, this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        String[] fromColumns = { DbHelper.KEY_TITLE, DbHelper.KEY_HUMAN_INFO };
        int[] toViews = { R.id.article_in_list_title, R.id.more_info };

        mAdapter =
                new SimpleCursorAdapter(getActivity(), R.layout.each_article_in_list, null,
                        fromColumns, toViews, 0);
        setListAdapter(mAdapter);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(TAG, "onCreateLoader called");
        return new CursorLoader(getActivity(), FeedProvider.ARTICLES_URI, null, null, null,
                DbHelper.KEY_PUBLISHED_TIMESTAMP + " DESC");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(id);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        // TODO: if data is 0 rows long, then notify main activity
    }

    @Override
    public void onPause() {
        super.onPause();
        mScrollIndex = getListView().getFirstVisiblePosition();
        View firstItem = getListView().getChildAt(0);
        mScrollTop = (firstItem == null) ? 0 : firstItem.getTop();
    }

    @Override
    public void onResume() {
        super.onResume();
        getListView().setSelectionFromTop(mScrollIndex, mScrollTop);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != AdapterView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(SAVED_ACTIVATED_POSITION, mActivatedPosition);
        }
        outState.putInt(SAVED_SCROLL_INDEX, mScrollIndex);
        outState.putInt(SAVED_SCROLL_TOP, mScrollTop);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            // Restore the previously serialized activated item position.
            if (savedInstanceState.containsKey(SAVED_ACTIVATED_POSITION)) {
                setActivatedPosition(savedInstanceState.getInt(SAVED_ACTIVATED_POSITION));
            }
            // Restore scroll position.
            if (savedInstanceState.containsKey(SAVED_SCROLL_INDEX)
                    && savedInstanceState.containsKey(SAVED_SCROLL_TOP)) {
                mScrollIndex = savedInstanceState.getInt(SAVED_SCROLL_INDEX);
                mScrollTop = savedInstanceState.getInt(SAVED_SCROLL_TOP);
            }
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be given the
     * 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView()
                .setChoiceMode(
                        activateOnItemClick ? AbsListView.CHOICE_MODE_SINGLE
                                : AbsListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == AdapterView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(long id);
    }
}
