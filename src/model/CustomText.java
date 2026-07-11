package model;

/** An editable description stored inside a custom-tab folder. */
public final class CustomText {
    private final String title;
    private String content = "";

    public CustomText(String title) { this.title = title; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }
}
