package model;

import java.util.ArrayList;
import java.util.List;

/** A user-defined reference tab. */
public final class CustomTab {
    private final String title;
    private final List<CustomFolder> folders = new ArrayList<>();
    private final List<CustomText> texts = new ArrayList<>();

    public CustomTab(String title) { this.title = title; }
    public String getTitle() { return title; }
    public List<CustomFolder> getFolders() { return folders; }
    public List<CustomText> getTexts() { return texts; }
}
