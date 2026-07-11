package service;

import model.Book;
import storage.JsonStorage;

import java.io.IOException;
import java.nio.file.Path;

public final class SaveService {
    private final JsonStorage storage;
    public SaveService(JsonStorage storage) { this.storage = storage; }
    public void save(Path file, Book book) throws IOException { storage.save(file, book); }
    public Book open(Path file) throws IOException { return storage.load(file); }
}
