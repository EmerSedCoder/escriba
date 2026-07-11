package model;

import java.util.ArrayList;
import java.util.List;

/** A named scene in the book's planning material. */
public final class Scene implements NamedDescription {
    private String title;
    private String content = "";
    private String locationName = "";
    private final List<String> participantNames = new ArrayList<>();
    public Scene(String title) { this.title = title; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName == null ? "" : locationName; }
    public List<String> getParticipantNames() { return participantNames; }
    @Override public String toString() { return title; }
}
