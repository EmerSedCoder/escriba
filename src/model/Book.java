package model;

import java.util.ArrayList;
import java.util.List;

public final class Book {
    private String title;
    private final List<Chapter> chapters = new ArrayList<>();
    private final List<Character> characters = new ArrayList<>();
    private final List<Scene> scenes = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<Location> locations = new ArrayList<>();
    private final List<Goal> goals = new ArrayList<>();
    private final List<Note> notes = new ArrayList<>();

    public Book(String title) { this.title = title; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<Chapter> getChapters() { return chapters; }
    public List<Character> getCharacters() { return characters; }
    public List<Scene> getScenes() { return scenes; }
    public List<Item> getItems() { return items; }
    public List<Location> getLocations() { return locations; }
    public List<Goal> getGoals() { return goals; }
    public List<Note> getNotes() { return notes; }
}
