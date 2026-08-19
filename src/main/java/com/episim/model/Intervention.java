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

    /**
     * @param id           database identity, or 0 for a not-yet-persisted intervention
     * @param runId        id of the simulation run this intervention belongs to
     * @param name         display name
     * @param startDay     first simulated day this intervention is active
     * @param endDay       last simulated day this intervention is active
     * @param intensity    strength in [0.0, 1.0]
     * @param costPerDayRM daily cost in Malaysian Ringgit
     * @param active       runtime-only enabled flag (not persisted — see the field comment)
     */
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

    /** @return the multiplier applied to the pathogen's per-contact transmission probability while active */
    public abstract double transmissionModifier();

    /** @return the multiplier applied to hospitalisation/mortality risk while active */
    public abstract double severityModifier();

    /** @return a human-readable description of this intervention's current configuration */
    public abstract String getDescription();

    /**
     * @param day the simulated day to check
     * @return whether this intervention is enabled and within its start/end day range on the given day
     */
    public boolean isActiveOn(int day) {
        return active && day >= startDay && day <= endDay;
    }

    /** @return the total cost over this intervention's whole active date range */
    public double totalCost() {
        return costPerDayRM * (endDay - startDay + 1);
    }

    /** @return the database identity, or 0 if not yet persisted */
    public int getId() {
        return id;
    }

    /** @param id the database identity to assign, typically after a generated-key insert */
    public void setId(int id) {
        this.id = id;
    }

    /** @return the id of the simulation run this intervention belongs to */
    public int getRunId() {
        return runId;
    }

    /** @param runId the new owning run id */
    public void setRunId(int runId) {
        this.runId = runId;
    }

    /** @return the display name */
    public String getName() {
        return name;
    }

    /** @param name the new display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the first simulated day this intervention is active */
    public int getStartDay() {
        return startDay;
    }

    /** @param startDay the new start day */
    public void setStartDay(int startDay) {
        this.startDay = startDay;
    }

    /** @return the last simulated day this intervention is active */
    public int getEndDay() {
        return endDay;
    }

    /** @param endDay the new end day */
    public void setEndDay(int endDay) {
        this.endDay = endDay;
    }

    /** @return the strength in [0.0, 1.0] */
    public double getIntensity() {
        return intensity;
    }

    /** @param intensity the new strength, expected in [0.0, 1.0] */
    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    /** @return the daily cost in Malaysian Ringgit */
    public double getCostPerDayRM() {
        return costPerDayRM;
    }

    /** @param costPerDayRM the new daily cost */
    public void setCostPerDayRM(double costPerDayRM) {
        this.costPerDayRM = costPerDayRM;
    }

    /** @return the runtime-only enabled flag */
    public boolean isActive() {
        return active;
    }

    /** @param active the new enabled flag */
    public void setActive(boolean active) {
        this.active = active;
    }
}
