package com.episim.model;

/**
 * Metadata for one execution of the simulation engine — mirrors the
 * simulation_run table and backs SimulationRunDao's Dao&lt;SimulationRun&gt;
 * implementation.
 */
public class SimulationRun {

    private int id;
    private String runName;
    private int pathogenId;
    private int populationSize;
    private int totalDays;
    private int seedInfections;
    private long randomSeed;
    private String startedAt;
    private String completedAt;
    private String status;
    private String notes;

    /**
     * @param id             database identity, or 0 for a not-yet-persisted run
     * @param runName        display/auto-generated label
     * @param pathogenId     id of the pathogen this run used
     * @param populationSize configured population size
     * @param totalDays      configured simulation length in days
     * @param seedInfections configured number of seed infections
     * @param randomSeed     seed used for reproducible generation and simulation
     * @param startedAt      timestamp the run started, or {@code null} if not yet known
     * @param completedAt    timestamp the run finished or was aborted, or {@code null} while running
     * @param status         {@code RUNNING}, {@code COMPLETED}, or {@code ABORTED}
     * @param notes          free-text notes
     */
    public SimulationRun(int id, String runName, int pathogenId, int populationSize, int totalDays,
                          int seedInfections, long randomSeed, String startedAt, String completedAt,
                          String status, String notes) {
        this.id = id;
        this.runName = runName;
        this.pathogenId = pathogenId;
        this.populationSize = populationSize;
        this.totalDays = totalDays;
        this.seedInfections = seedInfections;
        this.randomSeed = randomSeed;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.status = status;
        this.notes = notes;
    }

    /** @return the database identity, or 0 if not yet persisted */
    public int getId() {
        return id;
    }

    /** @param id the database identity to assign, typically after a generated-key insert */
    public void setId(int id) {
        this.id = id;
    }

    /** @return the display/auto-generated label */
    public String getRunName() {
        return runName;
    }

    /** @param runName the new display label */
    public void setRunName(String runName) {
        this.runName = runName;
    }

    /** @return the id of the pathogen this run used */
    public int getPathogenId() {
        return pathogenId;
    }

    /** @param pathogenId the new pathogen id */
    public void setPathogenId(int pathogenId) {
        this.pathogenId = pathogenId;
    }

    /** @return the configured population size */
    public int getPopulationSize() {
        return populationSize;
    }

    /** @param populationSize the new configured population size */
    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    /** @return the configured simulation length in days */
    public int getTotalDays() {
        return totalDays;
    }

    /** @param totalDays the new configured simulation length */
    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    /** @return the configured number of seed infections */
    public int getSeedInfections() {
        return seedInfections;
    }

    /** @param seedInfections the new configured seed infection count */
    public void setSeedInfections(int seedInfections) {
        this.seedInfections = seedInfections;
    }

    /** @return the seed used for reproducible generation and simulation */
    public long getRandomSeed() {
        return randomSeed;
    }

    /** @param randomSeed the new random seed */
    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    /** @return the timestamp the run started, or {@code null} if not yet known */
    public String getStartedAt() {
        return startedAt;
    }

    /** @param startedAt the new start timestamp */
    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    /** @return the timestamp the run finished or was aborted, or {@code null} while running */
    public String getCompletedAt() {
        return completedAt;
    }

    /** @param completedAt the new completion timestamp */
    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    /** @return {@code RUNNING}, {@code COMPLETED}, or {@code ABORTED} */
    public String getStatus() {
        return status;
    }

    /** @param status the new status */
    public void setStatus(String status) {
        this.status = status;
    }

    /** @return free-text notes */
    public String getNotes() {
        return notes;
    }

    /** @param notes the new free-text notes */
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
