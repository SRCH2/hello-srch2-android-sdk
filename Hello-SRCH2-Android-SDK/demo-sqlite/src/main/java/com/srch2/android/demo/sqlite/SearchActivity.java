package com.srch2.android.demo.sqlite;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import com.srch2.android.sdk.SRCH2Engine;
import com.srch2.android.sdk.SearchResultsListener;


public class SearchActivity extends Activity implements InstantSearchEditText.SearchInputEnteredObserver {

    DatabaseHelper mDatabaseHelper;
    SQLiteBookIndex mDatabaseIndexable;

    private ListView mSearchResultsListView;
    private SearchResultsAdapter mSearchResultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mSearchResultsListView = (ListView) findViewById(R.id.lv_search_results);
        mSearchResultsAdapter = new SearchResultsAdapter(this);
        mSearchResultsListView.setAdapter(mSearchResultsAdapter);
        mDatabaseHelper = new DatabaseHelper(this);
        mDatabaseIndexable = new SQLiteBookIndex(mDatabaseHelper);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SRCH2Engine.setSQLiteIndexables(mDatabaseIndexable);
        SRCH2Engine.onResume(this, (SearchResultsListener) mSearchResultsAdapter, true);
    }

    @Override
    protected  void onPause() {
        super.onPause();
        SRCH2Engine.onPause(this);
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
        // Since the input field of the InstantSearchEditText is now empty, notify the
        // SRCH2Engine the search input is empty and clear the results of the list view by clearing
        // the data set of its backing adapter.
        SRCH2Engine.searchAllIndexes("");
        mSearchResultsAdapter.clearDisplayedSearchResults();
    }
}
