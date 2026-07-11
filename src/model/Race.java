package model;

/** A sentient or non-sentient species in the story world. */
public final class Race implements NamedDescription {
    private String title;
    private String content = "";

    public Race(String title) { this.title = title; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }
    @Override public String toString() { return title; }
}
