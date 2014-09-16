package com.srch2.android.demo.sqlite;

import android.content.Context;
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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;

public class SearchResultsAdapter extends BaseAdapter implements SearchResultsListener {

    private static class ViewHolder {
        public TextView mTitleTextView;
        public TextView mAuthorTextView;
        public TextView mYearTextView;

        public ViewHolder(TextView titleTextView, TextView authorTextView,
                            TextView yearTextView) {
            mTitleTextView = titleTextView;
            mAuthorTextView = authorTextView;
            mYearTextView = yearTextView;
        }
    }

    private ArrayList<BookSearchResult> mSearchResults;
    private LayoutInflater mLayoutInflater;

    public SearchResultsAdapter(Context context) {
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSearchResults = new ArrayList<BookSearchResult>();
    }

    public void clearDisplayedSearchResults() {
        mSearchResults.clear();
        notifyDataSetChanged();
    }

    public void updateDisplayedSearchResults(
            ArrayList<BookSearchResult> newSearchResults) {
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
    public BookSearchResult getItem(int position) {
        return mSearchResults == null ? null : mSearchResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BookSearchResult searchResult = mSearchResults.get(position);
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
                TextView authorTextView = (TextView) convertView
                        .findViewById(R.id.tv_author_search_result_row);
                TextView yearTextView = (TextView) convertView
                        .findViewById(R.id.tv_year_search_result_row);
                viewHolder = new ViewHolder(titleTextView, authorTextView,
                                                 yearTextView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            // Using Html.fromHtml since the highlighted pre and post script was
            // set using html code.
            viewHolder.mTitleTextView.setText(Html.fromHtml(searchResult.getTitle()));
            viewHolder.mAuthorTextView.setText(Html.fromHtml(searchResult.getAuthor()));
            viewHolder.mYearTextView.setText(String.valueOf(searchResult.getYear()));
            return convertView;
        }
    }

    @Override
    public void onNewSearchResults(int HTTPResponseCode, String JSONresponse,
                                   HashMap<String, ArrayList<JSONObject>> resultMap) {
        if (HTTPResponseCode == HttpURLConnection.HTTP_OK) {
            ArrayList<BookSearchResult> newResults = new ArrayList<BookSearchResult>();

            // First retrieve the set of search results corresponding to the movie index.
            ArrayList<JSONObject> movieResults =
                    resultMap.get(DatabaseHelper.TABLE_NAME);

            if (movieResults != null && movieResults.size() > 0) {
                // If there are records in the set, iterate through them...
                for (JSONObject jsonObject : movieResults) {
                    BookSearchResult searchResult = null;
                    try {
                        // Each jsonObject will contain at least the original record as a JSONObject
                        // with its keys as the field names of the schema defined for this index.
                        JSONObject originalRecord =
                                jsonObject.getJSONObject(Indexable.SEARCH_RESULT_JSON_KEY_RECORD);
                        // Each jsonObject can also contain the set of highlighted fields, since the
                        // "field" title was set to be highlighted, pull highlightedFields out of each
                        // jsonObject as well.
                        JSONObject highlightedFields =
                                jsonObject.getJSONObject(Indexable.SEARCH_RESULT_JSON_KEY_HIGHLIGHTED);
                        // Instantiate a data element for the adapter, passing in the title as it
                        // was formatted by the highlighter and the genre and year from the original
                        // record.
                        searchResult = new BookSearchResult(
                                highlightedFields
                                        .getString(DatabaseHelper.COLUMN_TITLE),
                                highlightedFields
                                        .getString(DatabaseHelper.COLUMN_AUTHOR),
                                originalRecord
                                        .getInt(DatabaseHelper.COLUMN_YEAR));
                    } catch (JSONException oops) {
                        continue;
                    }

                    if (searchResult != null) {
                        newResults.add(searchResult);
                    }
                }
            }

            if (newResults.size() > 0) {
                updateDisplayedSearchResults(newResults);
            } else {
                clearDisplayedSearchResults();
            }
        }
    }

}