package model;

/** A named objective with the desired outcome and opposing conflict. */
public final class Goal {
    private String title;
    private String outcome = "";
    private String conflict = "";
    public Goal(String title) { this.title = title; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome == null ? "" : outcome; }
    public String getConflict() { return conflict; }
    public void setConflict(String conflict) { this.conflict = conflict == null ? "" : conflict; }
    @Override public String toString() { return title; }
}
