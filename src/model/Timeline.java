package model;

import java.util.ArrayList;
import java.util.List;

/** A timeline with a series of dates/events. */
public final class Timeline {
    private String title;
    private final List<TimelineDate> dates = new ArrayList<>();

    public Timeline(String title) {
        this.title = title;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<TimelineDate> getDates() { return dates; }

    @Override
    public String toString() { return title; }
}
