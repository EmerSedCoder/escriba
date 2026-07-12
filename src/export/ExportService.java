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
                    .append(stripHtml(chapter.getContent())).append("\n\n");
        }
        Files.writeString(destination, manuscript.toString(), StandardCharsets.UTF_8);
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        // Replace block tags with newlines to keep formatting spacing readable
        String clean = html.replaceAll("(?i)<br\\s*/?>", "\n")
                           .replaceAll("(?i)</p>", "\n")
                           .replaceAll("(?i)</h1>|</h2>|</h3>|</h4>|</td>|</tr>", "\n");
        // Strip remaining tags
        clean = clean.replaceAll("<[^>]*>", "");
        // Decode entities
        clean = clean.replace("&nbsp;", " ")
                     .replace("&amp;", "&")
                     .replace("&lt;", "<")
                     .replace("&gt;", ">")
                     .replace("&quot;", "\"")
                     .replace("&apos;", "'");
        return clean.trim();
    }
}

