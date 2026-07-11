package model;

import java.util.ArrayList;
import java.util.List;

public final class Location implements NamedDescription {
    private String title;
    private String content = "";
    private final List<String> visitorNames = new ArrayList<>();
    private String parentLocationName = "";
    public Location(String title) { this.title = title; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }
    public List<String> getVisitorNames() { return visitorNames; }
    public String getParentLocationName() { return parentLocationName; }
    public void setParentLocationName(String parentLocationName) { this.parentLocationName = parentLocationName == null ? "" : parentLocationName; }
    @Override public String toString() { return title; }
}
