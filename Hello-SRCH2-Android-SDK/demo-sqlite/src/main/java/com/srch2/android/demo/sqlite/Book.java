package com.srch2.android.demo.sqlite;

import java.util.ArrayList;

public class Book {
    public String mAuthor;
    public String mTitle;
    public String mGenre;
    public int mYear;
    public static final String mDescription = "Has not yet been set!";
    public float mUserRating;
    public static final String mThumbnail =
            "0100000101101100011011000010000001111001011011110111010101110010" +
            "0010000001110011011001010110000101110010011000110110100000100000" +
            "0110000101110010011001010010000001100010011001010110110001101111" +
            "011011100110011100100000011101000110111100100000011101010111001100101110";

    public Book(String author, String title, String genre, int year, float userRating) {
        mAuthor = author;
        mTitle = title;
        mGenre = genre;
        mYear = year;
        mUserRating = userRating;
    }

    public static ArrayList<Book> getBookList() {
        ArrayList<Book> books = new ArrayList<Book>();

        books.add(
            new Book("Charles Dickens", "A Tale Of Two Cities",
                            "Historical Fiction", 1859, 25));
        books.add(
            new Book("J.R.R. Tolkien", "The Lord Of The Rings",
                            "Fantasy Fiction", 1954, 35));
        books.add(
            new Book("Antoine de Saint-Exupéry", "Le Petit Prince (The Little Prince)",
                            "Children's Fiction", 1943, 1));
        books.add(
            new Book("J. K. Rowling", "Harry Potter and the Philosopher's Stone",
                            "Fantasy Fiction", 1997, 1));
        books.add(
                new Book("Agatha Christie", "And Then There Were None",
                        "Mystery Fiction", 1939, 1));
        books.add(
                new Book("Cao Xueqin", "紅樓夢/红楼梦 (Dream of the Red Chamber)",
                        "Semi-Autobiographical Historical Fiction", 1754, 25));
        books.add(
                new Book("J.R.R. Tolkien", "The Hobbit",
                        "Fantasy Fiction", 1937, 35));
        books.add(
                new Book("H. Rider Haggard", "She: A History of Adventure",
                        "Historical Fiction", 1887, 1));
        books.add(
                new Book("C. S. Lewis", "The Lion, the Witch and the Wardrobe",
                        "Fantasy Fiction", 1950, 1));
        books.add(
                new Book("Dan Brown", "The Da Vinci Code",
                        "Mystery Suspense Fiction", 2003, 1));
        books.add(
                new Book("Napoleon Hill", "Think and Grow Rich",
                        "Personal Development Self-Help", 1937, 1));
        books.add(
                new Book("J. D. Salinger", "The Catcher in the Rye",
                        "Fiction", 1951, 25));
        books.add(
                new Book("Gabriel García Márquez",
                            "Cien años de soledad (One Hundred Years of Solitude)",
                                "Magical Realism", 1967, 45));
        books.add(
                new Book("Vladimir Nabokov", "Lolita", "Fiction", 1955, 10));
        books.add(
                new Book("Johanna Spyri",
                            "Heidis Lehr-und Wanderjahre (Heidi's Years of Wandering and Learning)",
                                "Children's Fiction", 1880, 10));
        books.add(
                new Book("Umberto Eco", "Il Nome della Rosa (The Name of the Rose)",
                        "Mystery Historical Fiction", 1980, 10));
        books.add(
                new Book("E.B. White", "Charlotte's Web",
                            "Children's Fiction", 1952, 10));
        books.add(
                new Book("Leo Tolstoy", "Война и мир (Voyna i mir; War and Peace)",
                        "Historical Fiction", 1869, 35));
        books.add(
                new Book("George Orwell", "Nineteen Eighty-Four",
                        "Speculative Fiction", 1949, 35));
        books.add(
                new Book("Maurice Sendak", "Where the Wild Things Are",
                        "Children's Fiction", 1963, 10));
        books.add(
                new Book("Margaret Wise Brown", "Goodnight Moon",
                        "Children's Fiction", 1947, 20));
        books.add(
                new Book("John Steinbeck", "The Grapes of Wrath",
                        "Historical Fiction", 1947, 25));
        books.add(
                new Book("Douglas Adams", "The Hitchhiker's Guide to the Galaxy",
                        "Science Fiction Comedy", 1979, 40));
        books.add(
                new Book("Haruki Murakami", "ノルウェイの森, Noruwei no Mori (Norwegian Wood)",
                        "Magic Realism", 1987, 40));
        books.add(
                new Book("Frank Herbert", "Dune", "Science Fiction", 1965, 30));
        return books;
    }
}
