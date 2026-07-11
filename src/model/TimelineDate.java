package model;

/** A date/event within a timeline. */
public final class TimelineDate {
    private String name;
    private int position;

    public TimelineDate(String name, int position) {
        this.name = name;
        this.position = position;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    @Override
    public String toString() { return name; }
}
