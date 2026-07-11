package export;

import model.Book;
import model.Chapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Creates a plain-text manuscript suitable for sharing or further formatting. */
public final class ExportService {
    public void exportPlainText(Book book, Path destination) throws IOException {
        StringBuilder manuscript = new StringBuilder(book.getTitle()).append("\n\n");
        for (Chapter chapter : book.getChapters()) {
            manuscript.append(chapter.getTitle()).append("\n\n")
                    .append(chapter.getContent()).append("\n\n");
        }
        Files.writeString(destination, manuscript.toString(), StandardCharsets.UTF_8);
    }
}
