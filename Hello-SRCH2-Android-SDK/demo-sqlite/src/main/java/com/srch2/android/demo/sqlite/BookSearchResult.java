package com.srch2.android.demo.sqlite;

public class BookSearchResult {
    public String mTitle;
    public String mAuthor;
    public int mYear;

    public String getAuthor() {
        return mAuthor;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getYear() {
        return mYear;
    }

    public BookSearchResult(String title, String author, int year) {
        mAuthor = author;
        mTitle = title;
        mYear = year;
    }
}
