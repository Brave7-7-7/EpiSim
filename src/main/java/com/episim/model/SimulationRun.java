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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public int getPathogenId() {
        return pathogenId;
    }

    public void setPathogenId(int pathogenId) {
        this.pathogenId = pathogenId;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public int getSeedInfections() {
        return seedInfections;
    }

    public void setSeedInfections(int seedInfections) {
        this.seedInfections = seedInfections;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
