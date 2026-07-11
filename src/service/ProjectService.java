package service;

import model.Book;
import model.Chapter;

public final class ProjectService {
    private Book book;

    public void newProject(String title) {
        book = new Book(title);
        book.getChapters().add(new Chapter("Chapter 1"));
    }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public Chapter addChapter(String title) {
        Chapter chapter = new Chapter(title);
        book.getChapters().add(chapter);
        return chapter;
    }
}
