package com.episim.model;

/**
 * Read-only projection of the v_run_summary SQL view, used by the Analysis
 * tab to list past runs without re-aggregating daily_record on the client.
 */
public class RunSummary {

    private final int runId;
    private final String runName;
    private final String pathogenName;
    private final int populationSize;
    private final int totalDays;
    private final int peakInfections;
    private final int peakBeds;
    private final int totalDeaths;
    private final int totalInfections;
    private final int daysOverCapacity;
    private final String status;

    public RunSummary(int runId, String runName, String pathogenName, int populationSize, int totalDays,
                       int peakInfections, int peakBeds, int totalDeaths, int totalInfections,
                       int daysOverCapacity, String status) {
        this.runId = runId;
        this.runName = runName;
        this.pathogenName = pathogenName;
        this.populationSize = populationSize;
        this.totalDays = totalDays;
        this.peakInfections = peakInfections;
        this.peakBeds = peakBeds;
        this.totalDeaths = totalDeaths;
        this.totalInfections = totalInfections;
        this.daysOverCapacity = daysOverCapacity;
        this.status = status;
    }

    public int getRunId() {
        return runId;
    }

    public String getRunName() {
        return runName;
    }

    public String getPathogenName() {
        return pathogenName;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public int getPeakInfections() {
        return peakInfections;
    }

    public int getPeakBeds() {
        return peakBeds;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public int getTotalInfections() {
        return totalInfections;
    }

    public int getDaysOverCapacity() {
        return daysOverCapacity;
    }

    public String getStatus() {
        return status;
    }
}
