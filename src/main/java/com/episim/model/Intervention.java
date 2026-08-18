package com.episim.model;

/**
 * Abstract base of the public-health intervention hierarchy — the second
 * abstraction point in the domain model. Each concrete measure defines how
 * strongly it suppresses transmission and severity while it is active.
 */
public abstract class Intervention {

    private int id;
    // Not persisted: the intervention table has no active column, since
    // "active on a given day" is derived dynamically from start/end day via
    // isActiveOn(). This flag is a runtime-only toggle for the engine/GUI.
    private int runId;
    private String name;
    private int startDay;
    private int endDay;
    private double intensity;
    private double costPerDayRM;
    private boolean active;

    protected Intervention(int id, int runId, String name, int startDay, int endDay, double intensity,
                            double costPerDayRM, boolean active) {
        this.id = id;
        this.runId = runId;
        this.name = name;
        this.startDay = startDay;
        this.endDay = endDay;
        this.intensity = intensity;
        this.costPerDayRM = costPerDayRM;
        this.active = active;
    }

    /** Multiplier applied to the pathogen's per-contact transmission probability while active. */
    public abstract double transmissionModifier();

    /** Multiplier applied to hospitalisation/mortality risk while active. */
    public abstract double severityModifier();

    public abstract String getDescription();

    public boolean isActiveOn(int day) {
        return active && day >= startDay && day <= endDay;
    }

    public double totalCost() {
        return costPerDayRM * (endDay - startDay + 1);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRunId() {
        return runId;
    }

    public void setRunId(int runId) {
        this.runId = runId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStartDay() {
        return startDay;
    }

    public void setStartDay(int startDay) {
        this.startDay = startDay;
    }

    public int getEndDay() {
        return endDay;
    }

    public void setEndDay(int endDay) {
        this.endDay = endDay;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    public double getCostPerDayRM() {
        return costPerDayRM;
    }

    public void setCostPerDayRM(double costPerDayRM) {
        this.costPerDayRM = costPerDayRM;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
