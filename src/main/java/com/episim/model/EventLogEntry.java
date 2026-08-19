package com.episim.model;

/** A single event_log row, read back for the Run History detail pane. */
public class EventLogEntry {

    private final int dayNumber;
    private final String eventType;
    private final String description;
    private final String loggedAt;

    /**
     * @param dayNumber   simulated day the event occurred on
     * @param eventType   short category, e.g. {@code "HOSPITAL_OVERWHELMED"}
     * @param description human-readable detail
     * @param loggedAt    wall-clock timestamp the event was recorded
     */
    public EventLogEntry(int dayNumber, String eventType, String description, String loggedAt) {
        this.dayNumber = dayNumber;
        this.eventType = eventType;
        this.description = description;
        this.loggedAt = loggedAt;
    }

    /** @return the simulated day the event occurred on */
    public int getDayNumber() {
        return dayNumber;
    }

    /** @return the short category, e.g. {@code "HOSPITAL_OVERWHELMED"} */
    public String getEventType() {
        return eventType;
    }

    /** @return the human-readable detail */
    public String getDescription() {
        return description;
    }

    /** @return the wall-clock timestamp the event was recorded */
    public String getLoggedAt() {
        return loggedAt;
    }
}
