package com.srch2.android.demo.helloworld;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.srch2.android.sdk.Indexable;
import com.srch2.android.sdk.SearchResultsListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;

public class SearchResultsAdapter extends BaseAdapter {

    private static class ViewHolder {
        public TextView mTitleTextView;
        public TextView mGenreTextView;
        public TextView mYearTextView;

        public ViewHolder(TextView titleTextView, TextView genreTextView,
                          TextView yearTextView) {
            mTitleTextView = titleTextView;
            mGenreTextView = genreTextView;
            mYearTextView = yearTextView;
        }
    }

    private ArrayList<MovieSearchResult> mSearchResults;
    private LayoutInflater mLayoutInflater;
    private SearchResultsUiHandler mSearchResultsUiHandler;

    public SearchResultsListener getSearchResultsListener() {
        return mSearchResultsUiHandler;
    }

    public SearchResultsAdapter(Context context) {
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSearchResults = new ArrayList<MovieSearchResult>();
        mSearchResultsUiHandler = new SearchResultsUiHandler(this);
    }

    public void clearDisplayedSearchResults() {
        mSearchResults.clear();
        notifyDataSetChanged();
    }

    public void updateDisplayedSearchResults(
            ArrayList<MovieSearchResult> newSearchResults) {
        // Swap out the data set of this adapter with the new set of search results
        // and invalidate the list view this adapter is backing with these new
        // search results.
        mSearchResults.clear();
        mSearchResults.addAll(newSearchResults);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mSearchResults == null ? 0 : mSearchResults.size();
    }

    @Override
    public MovieSearchResult getItem(int position) {
        return mSearchResults == null ? null : mSearchResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MovieSearchResult searchResult = mSearchResults.get(position);
        if (searchResult == null) {
            View view = new View(parent.getContext());
            view.setVisibility(View.GONE);
            return view;
        } else {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(
                        R.layout.listview_search_result_row, parent, false);
                TextView titleTextView = (TextView) convertView
                        .findViewById(R.id.tv_title_search_result_row);
                TextView genreTextView = (TextView) convertView
                        .findViewById(R.id.tv_genre_search_result_row);
                TextView yearTextView = (TextView) convertView
                        .findViewById(R.id.tv_year_search_result_row);
                viewHolder = new ViewHolder(titleTextView, genreTextView,
                        yearTextView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            // Using Html.fromHtml since the highlighted pre and post script was
            // set using html code.
            viewHolder.mTitleTextView.setText(Html.fromHtml(searchResult.getTitle()));
            viewHolder.mGenreTextView.setText(Html.fromHtml(searchResult.getGenre()));
            viewHolder.mYearTextView.setText(String.valueOf(searchResult.getYear()));
            return convertView;
        }
    }

    private static class SearchResultsUiHandler extends Handler implements
            SearchResultsListener {

        // Since the SearchResultsListener callback method onNewSearchResults(...)
        // is executed off the Ui thread, it is implemented in a handler that will
        // pass the search results of this callback to the Ui thread: specifically,
        // to the SearchResultsAdapter so they can be displayed to the user.

        private static final int MESSAGE_WHAT_PUBLISH_NEW_RESULTS = 001;
        private static final int MESSAGE_WHAT_PUBLISH_NO_NEW_RESULTS = 002;

        private WeakReference<SearchResultsAdapter> mSearchResultsAdapterWeakReference;

        public SearchResultsUiHandler(SearchResultsAdapter searchResultsAdapter) {
            mSearchResultsAdapterWeakReference = new WeakReference<SearchResultsAdapter>(
                    searchResultsAdapter);
        }

        @Override
        public void handleMessage(Message msg) {

            SearchResultsAdapter searchResultAdapter = mSearchResultsAdapterWeakReference
                    .get();

            if (searchResultAdapter != null) {
                switch (msg.what) {
                    case MESSAGE_WHAT_PUBLISH_NEW_RESULTS:
                        ArrayList<MovieSearchResult> newResults = null;
                        try {
                            newResults = (ArrayList<MovieSearchResult>) msg.obj;
                        } catch (ClassCastException oops) {
                        }

                        if (newResults != null) {
                            searchResultAdapter
                                    .updateDisplayedSearchResults(newResults);
                        }
                        return;
                    case MESSAGE_WHAT_PUBLISH_NO_NEW_RESULTS:
                        searchResultAdapter.clearDisplayedSearchResults();
                        return;
                }
            }
        }

        @Override
        public void onNewSearchResults(int httpResponseCode,
                                       String jsonResponse,
                                       HashMap<String, ArrayList<JSONObject>> resultRecordMap) {
            if (httpResponseCode == HttpURLConnection.HTTP_OK) {
                ArrayList<MovieSearchResult> newResults = new ArrayList<MovieSearchResult>();
                // First retrieve the set of search results corresponding to the movie index.
                ArrayList<JSONObject> movieResults = resultRecordMap
                        .get(MovieIndex.INDEX_NAME);
                if (movieResults != null && movieResults.size() > 0) {
                    // If there are records in the set, iterate through them...
                    for (JSONObject jsonObject : movieResults) {
                        MovieSearchResult searchResult = null;
                        try {
                            // Each jsonObject will contain at least the original record as a JSONObject
                            // with its keys as the field names of the schema defined for this index.
                            JSONObject originalRecord = jsonObject.getJSONObject(Indexable.SEARCH_RESULT_JSON_KEY_RECORD);
                            // Each jsonObject can also contain the set of highlighted fields, since the
                            // "field" title was set to be highlighted, pull highlightedFields out of each
                            // jsonObject as well.
                            JSONObject highlightedFields = jsonObject.getJSONObject(Indexable.SEARCH_RESULT_JSON_KEY_HIGHLIGHTED);
                            // Instantiate a data element for the adapter, passing in the title as it
                            // was formatted by the highlighter and the genre and year from the original
                            // record.
                            searchResult = new MovieSearchResult(
                                    highlightedFields
                                            .getString(MovieIndex.INDEX_FIELD_TITLE),
                                    highlightedFields
                                            .getString(MovieIndex.INDEX_FIELD_GENRE),
                                    originalRecord
                                            .getInt(MovieIndex.INDEX_FIELD_YEAR));
                        } catch (JSONException oops) {
                            continue;
                        }

                        if (searchResult != null) {
                            newResults.add(searchResult);
                        }
                    }
                }
                sendMessage(Message
                        .obtain(this,
                                newResults.size() > 0 ? MESSAGE_WHAT_PUBLISH_NEW_RESULTS
                                        : MESSAGE_WHAT_PUBLISH_NO_NEW_RESULTS,
                                newResults));
            }
        }
    }
}