package model;

public final class Character implements NamedDescription {
    private String title;
    private String content = "";
    private String appearance = "";
    private String goalTitle = "";
    public Character(String title) { this.title = title; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }
    public String getAppearance() { return appearance; }
    public void setAppearance(String appearance) { this.appearance = appearance == null ? "" : appearance; }
    public String getGoalTitle() { return goalTitle; }
    public void setGoalTitle(String goalTitle) { this.goalTitle = goalTitle == null ? "" : goalTitle; }
    @Override public String toString() { return title; }
}
