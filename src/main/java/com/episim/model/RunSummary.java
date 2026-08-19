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

    /**
     * @param runId            id of the summarised run
     * @param runName          display label
     * @param pathogenName     name of the pathogen used
     * @param populationSize   configured population size
     * @param totalDays        configured simulation length in days
     * @param peakInfections   peak prevalence of active infection (infected + hospitalised)
     * @param peakBeds         peak hospital bed occupancy reached
     * @param totalDeaths      final deceased count
     * @param totalInfections  cumulative new infections over the whole run
     * @param daysOverCapacity count of days any district was overwhelmed
     * @param status           {@code RUNNING}, {@code COMPLETED}, or {@code ABORTED}
     */
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

    /** @return the id of the summarised run */
    public int getRunId() {
        return runId;
    }

    /** @return the display label */
    public String getRunName() {
        return runName;
    }

    /** @return the name of the pathogen used */
    public String getPathogenName() {
        return pathogenName;
    }

    /** @return the configured population size */
    public int getPopulationSize() {
        return populationSize;
    }

    /** @return the configured simulation length in days */
    public int getTotalDays() {
        return totalDays;
    }

    /** @return the peak prevalence of active infection (infected + hospitalised) */
    public int getPeakInfections() {
        return peakInfections;
    }

    /** @return the peak hospital bed occupancy reached */
    public int getPeakBeds() {
        return peakBeds;
    }

    /** @return the final deceased count */
    public int getTotalDeaths() {
        return totalDeaths;
    }

    /** @return the cumulative new infections over the whole run */
    public int getTotalInfections() {
        return totalInfections;
    }

    /** @return the count of days any district was overwhelmed */
    public int getDaysOverCapacity() {
        return daysOverCapacity;
    }

    /** @return {@code RUNNING}, {@code COMPLETED}, or {@code ABORTED} */
    public String getStatus() {
        return status;
    }
}
