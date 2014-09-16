package com.srch2.android.demo.helloworld;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import com.srch2.android.sdk.SRCH2Engine;


public class SearchActivity extends Activity implements InstantSearchEditText.SearchInputEnteredObserver {

    private MovieIndex mMovieIndex;

    private ListView mSearchResultsListView;
    private SearchResultsAdapter mSearchResultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);
        mSearchResultsListView = (ListView) findViewById(R.id.lv_search_results);
        mSearchResultsAdapter = new SearchResultsAdapter(this);
        mSearchResultsListView.setAdapter(mSearchResultsAdapter);

        setupSRCH2Engine();
    }

    private void setupSRCH2Engine() {
        // The following calls only need to be made when an instance of the
        // activity is created, so this method is called from onCreate().
        mMovieIndex = new MovieIndex();
        SRCH2Engine.initialize(mMovieIndex);

        // This callback can be reset and re-registered at any point.
        SRCH2Engine.setSearchResultsListener(mSearchResultsAdapter
                .getSearchResultsListener());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // This method is called in onResume because the SRCH2 search server should always be
        // available when this activity is in the foreground and is visible to the user, since
        // they may want to do searches. It is not called from onCreate() because the SRCH2
        // search server should only be active while this activity has the current focus.
        SRCH2Engine.onStart(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Since this activity is no longer going to be visible to and interacted with by the
        // user, the SRCH2 search server should stop being active. It is not immediately brought
        // to a halt but will persist for a short interval in case the user is simply switching to
        // another activity to reply to text or some other short-lived task and will return to this
        // activity. In this event, the stopping of the SRCH2 search server will be cancelled when
        // this activity's onResume() method is called, in turn calling SRCH2Engine.onStart(...).
        SRCH2Engine.onStop(this);
    }

    @Override
    public void onNewSearchInput(String newSearchText) {
        // Pass the text of the InstantSearchEditText input field to the SRCH2Engine for doing
        // searches. A search could also be performed here specifically on mMovieIndex by calling
        // either mMovieIndex.search(newSearchText) or SRCH2Engine.searchIndex(MovieIndex.INDEX_NAME).
        SRCH2Engine.searchAllIndexes(newSearchText);
    }

    @Override
    public void onNewSearchInputIsBlank() {
        // Since the input field of the InstantSearchEditText is now empty, do not perform a search
        // and simply clear the results of the list view by clearing the data set of its backing
        // adapter.
        mSearchResultsAdapter.clearDisplayedSearchResults();
    }
}
