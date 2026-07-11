package model;

import java.util.ArrayList;
import java.util.List;

public final class Chapter {
    private final String title;
    private String content = "";
    private final List<Scene> scenes = new ArrayList<>();

    public Chapter(String title) { this.title = title; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }
    public List<Scene> getScenes() { return scenes; }
    @Override public String toString() { return title; }
}
