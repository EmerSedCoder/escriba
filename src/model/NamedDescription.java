package model;

/** A named book reference with editable explanatory text. */
public interface NamedDescription {
    String getTitle();
    void setTitle(String title);
    String getContent();
    void setContent(String content);
}
