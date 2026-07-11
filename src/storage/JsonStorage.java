package storage;

import model.Book;
import model.Chapter;
import model.Character;
import model.Item;
import model.Location;
import model.Note;
import model.Scene;
import model.Goal;
import model.NamedDescription;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Persists a project as a small portable JSON document, without external libraries. */
public final class JsonStorage {
    private static final String STRING = "\\\"((?:\\\\.|[^\\\"])*)\\\"";

    public void save(Path file, Book book) throws IOException {
        StringBuilder json = new StringBuilder("{\n  \"title\": \"").append(escape(book.getTitle())).append("\",\n  \"chapters\": [");
        for (int i = 0; i < book.getChapters().size(); i++) {
            Chapter chapter = book.getChapters().get(i);
            if (i > 0) json.append(',');
            json.append("\n    {\"title\": \"").append(escape(chapter.getTitle()))
                    .append("\", \"content\": \"").append(escape(chapter.getContent())).append("\"}");
        }
        json.append("\n  ],\n  \"references\": [");
        appendReferences(json, "character", book.getCharacters());
        appendReferences(json, "scene", book.getScenes());
        appendReferences(json, "item", book.getItems());
        appendReferences(json, "location", book.getLocations());
        appendReferences(json, "note", book.getNotes());
        for (Goal goal : book.getGoals()) {
            if (hasReferences(json)) json.append(',');
            json.append("\n    {\"type\": \"goal\", \"title\": \"").append(escape(goal.getTitle()))
                    .append("\", \"outcome\": \"").append(escape(goal.getOutcome()))
                    .append("\", \"conflict\": \"").append(escape(goal.getConflict())).append("\"}");
        }
        for (Character character : book.getCharacters()) {
            if (hasReferences(json)) json.append(',');
            json.append("\n    {\"type\": \"characterMetadata\", \"title\": \"").append(escape(character.getTitle()))
                    .append("\", \"appearance\": \"").append(escape(character.getAppearance())).append("\"}");
        }
        for (Scene scene : book.getScenes()) {
            if (hasReferences(json)) json.append(',');
            json.append("\n    {\"type\": \"sceneMetadata\", \"title\": \"").append(escape(scene.getTitle()))
                    .append("\", \"location\": \"").append(escape(scene.getLocationName())).append("\", \"participants\": \"")
                    .append(escape(String.join("\u001F", scene.getParticipantNames()))).append("\"}");
        }
        for (Location location : book.getLocations()) {
            if (hasReferences(json)) json.append(',');
            json.append("\n    {\"type\": \"locationMetadata\", \"title\": \"").append(escape(location.getTitle()))
                    .append("\", \"visitors\": \"").append(escape(String.join("\u001F", location.getVisitorNames())))
                    .append("\", \"parent\": \"").append(escape(location.getParentLocationName())).append("\"}");
        }
        json.append("\n  ]\n}\n");
        Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
    }

    public Book load(Path file) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        Matcher title = Pattern.compile("\\\"title\\\"\\s*:\\s*" + STRING).matcher(json);
        if (!title.find()) throw new IOException("Invalid Escriba project file.");
        Book book = new Book(unescape(title.group(1)));
        Pattern chapter = Pattern.compile("\\{\\s*\\\"title\\\"\\s*:\\s*" + STRING + "\\s*,\\s*\\\"content\\\"\\s*:\\s*" + STRING + "\\s*}");
        Matcher chapters = chapter.matcher(json);
        while (chapters.find()) {
            Chapter item = new Chapter(unescape(chapters.group(1)));
            item.setContent(unescape(chapters.group(2)));
            book.getChapters().add(item);
        }
        if (book.getChapters().isEmpty()) book.getChapters().add(new Chapter("Chapter 1"));
        Pattern reference = Pattern.compile("\\{\\s*\\\"type\\\"\\s*:\\s*" + STRING + "\\s*,\\s*\\\"title\\\"\\s*:\\s*" + STRING + "\\s*,\\s*\\\"content\\\"\\s*:\\s*" + STRING + "\\s*}");
        Matcher references = reference.matcher(json);
        while (references.find()) addReference(book, unescape(references.group(1)), unescape(references.group(2)), unescape(references.group(3)));
        Pattern goal = Pattern.compile("\\{\\s*\\\"type\\\"\\s*:\\s*\\\"goal\\\"\\s*,\\s*\\\"title\\\"\\s*:\\s*" + STRING + "\\s*,\\s*\\\"outcome\\\"\\s*:\\s*" + STRING + "\\s*,\\s*\\\"conflict\\\"\\s*:\\s*" + STRING + "\\s*}");
        Matcher goals = goal.matcher(json);
        while (goals.find()) {
            Goal item = new Goal(unescape(goals.group(1)));
            item.setOutcome(unescape(goals.group(2))); item.setConflict(unescape(goals.group(3)));
            book.getGoals().add(item);
        }
        Pattern characterMetadata = Pattern.compile("\\{\\s*\\\"type\\\"\\s*:\\s*\\\"characterMetadata\\\"\\s*,\\s*\\\"title\\\"\\s*:\\s*" + STRING + "\\s*,\\s*\\\"appearance\\\"\\s*:\\s*" + STRING + "\\s*}");
        Matcher characterDetails = characterMetadata.matcher(json);
        while (characterDetails.find()) for (Character character : book.getCharacters()) if (character.getTitle().equals(unescape(characterDetails.group(1)))) character.setAppearance(unescape(characterDetails.group(2)));
        Pattern sceneMetadata = Pattern.compile("\\{\\s*\\\"type\\\"\\s*:\\s*\\\"sceneMetadata\\\"\\s*,\\s*\\\"title\\\"\\s*:\\s*" + STRING + "\\s*,\\s*\\\"location\\\"\\s*:\\s*" + STRING + "\\s*,\\s*\\\"participants\\\"\\s*:\\s*" + STRING + "\\s*}");
        Matcher sceneDetails = sceneMetadata.matcher(json);
        while (sceneDetails.find()) for (Scene scene : book.getScenes()) if (scene.getTitle().equals(unescape(sceneDetails.group(1)))) {
            scene.setLocationName(unescape(sceneDetails.group(2)));
            String values = unescape(sceneDetails.group(3));
            if (!values.isEmpty()) java.util.Collections.addAll(scene.getParticipantNames(), values.split("\u001F", -1));
        }
        Pattern locationMetadata = Pattern.compile("\\{\\s*\\\"type\\\"\\s*:\\s*\\\"locationMetadata\\\"\\s*,\\s*\\\"title\\\"\\s*:\\s*" + STRING + "\\s*,\\s*\\\"visitors\\\"\\s*:\\s*" + STRING + "(?:\\s*,\\s*\\\"parent\\\"\\s*:\\s*" + STRING + ")?\\s*}");
        Matcher locationDetails = locationMetadata.matcher(json);
        while (locationDetails.find()) for (Location location : book.getLocations()) if (location.getTitle().equals(unescape(locationDetails.group(1)))) {
            String values = unescape(locationDetails.group(2));
            if (!values.isEmpty()) java.util.Collections.addAll(location.getVisitorNames(), values.split("\u001F", -1));
            if (locationDetails.group(3) != null) location.setParentLocationName(unescape(locationDetails.group(3)));
        }
        return book;
    }

    private void appendReferences(StringBuilder json, String type, java.util.List<? extends NamedDescription> entries) {
        for (NamedDescription entry : entries) {
            if (hasReferences(json)) json.append(',');
            json.append("\n    {\"type\": \"").append(type).append("\", \"title\": \"").append(escape(entry.getTitle()))
                    .append("\", \"content\": \"").append(escape(entry.getContent())).append("\"}");
        }
    }
    private boolean hasReferences(StringBuilder json) { return json.charAt(json.length() - 1) != '['; }
    private void addReference(Book book, String type, String title, String content) {
        NamedDescription item = switch (type) {
            case "character" -> new Character(title); case "scene" -> new Scene(title);
            case "item" -> new Item(title); case "location" -> new Location(title); case "note" -> new Note(title);
            default -> null;
        };
        if (item == null) return;
        item.setContent(content);
        switch (type) {
            case "character" -> book.getCharacters().add((Character) item);
            case "scene" -> book.getScenes().add((Scene) item);
            case "item" -> book.getItems().add((Item) item);
            case "location" -> book.getLocations().add((Location) item);
            case "note" -> book.getNotes().add((Note) item);
        }
    }

    private String escape(String text) { return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
    private String unescape(String text) { return text.replace("\\r", "\r").replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\"); }
}
