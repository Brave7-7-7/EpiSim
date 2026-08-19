package com.episim.model;

/** A single event_log row, read back for the Run History detail pane. */
public class EventLogEntry {

    private final int dayNumber;
    private final String eventType;
    private final String description;
    private final String loggedAt;

    public EventLogEntry(int dayNumber, String eventType, String description, String loggedAt) {
        this.dayNumber = dayNumber;
        this.eventType = eventType;
        this.description = description;
        this.loggedAt = loggedAt;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public String getLoggedAt() {
        return loggedAt;
    }
}
