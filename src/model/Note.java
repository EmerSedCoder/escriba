package model;

public final class Note implements NamedDescription {
    private String title;
    private String content = "";
    public Note(String title) { this.title = title; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }
    @Override public String toString() { return title; }
}
