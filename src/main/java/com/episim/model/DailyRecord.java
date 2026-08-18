package com.episim.model;

import com.episim.util.SimConstants;

/**
 * A single day's aggregate snapshot of a simulation run.
 */
public class DailyRecord implements Reportable {

    // Backs the daily_record.record_id primary key so DailyRecordDao can
    // satisfy the generic Dao<T> contract (findById/update/delete).
    private int id;
    private int runId;
    private int dayNumber;
    private int susceptible;
    private int exposed;
    private int infected;
    private int hospitalised;
    private int recovered;
    private int deceased;
    private int newInfections;
    private double effectiveR;
    private int bedsOccupied;
    private boolean overCapacity;

    public DailyRecord(int id, int runId, int dayNumber, int susceptible, int exposed, int infected,
                        int hospitalised, int recovered, int deceased, int newInfections,
                        double effectiveR, int bedsOccupied, boolean overCapacity) {
        this.id = id;
        this.runId = runId;
        this.dayNumber = dayNumber;
        this.susceptible = susceptible;
        this.exposed = exposed;
        this.infected = infected;
        this.hospitalised = hospitalised;
        this.recovered = recovered;
        this.deceased = deceased;
        this.newInfections = newInfections;
        this.effectiveR = effectiveR;
        this.bedsOccupied = bedsOccupied;
        this.overCapacity = overCapacity;
    }

    public int totalAlive() {
        return susceptible + exposed + infected + hospitalised + recovered;
    }

    @Override
    public String getReportTitle() {
        return "Daily Report — Day " + dayNumber;
    }

    @Override
    public String toReportLine() {
        // This line is written to plain-text report exports, so it must use SimConstants.DATA_LOCALE
        // (Locale.ROOT) rather than the machine's default locale — see SimConstants for the full policy.
        return String.format(SimConstants.DATA_LOCALE,
                "Day %d | S=%d E=%d I=%d H=%d R=%d D=%d | newInfections=%d | Reff=%.2f | beds=%d | overCapacity=%s",
                dayNumber, susceptible, exposed, infected, hospitalised, recovered, deceased,
                newInfections, effectiveR, bedsOccupied, overCapacity);
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

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public int getSusceptible() {
        return susceptible;
    }

    public void setSusceptible(int susceptible) {
        this.susceptible = susceptible;
    }

    public int getExposed() {
        return exposed;
    }

    public void setExposed(int exposed) {
        this.exposed = exposed;
    }

    public int getInfected() {
        return infected;
    }

    public void setInfected(int infected) {
        this.infected = infected;
    }

    public int getHospitalised() {
        return hospitalised;
    }

    public void setHospitalised(int hospitalised) {
        this.hospitalised = hospitalised;
    }

    public int getRecovered() {
        return recovered;
    }

    public void setRecovered(int recovered) {
        this.recovered = recovered;
    }

    public int getDeceased() {
        return deceased;
    }

    public void setDeceased(int deceased) {
        this.deceased = deceased;
    }

    public int getNewInfections() {
        return newInfections;
    }

    public void setNewInfections(int newInfections) {
        this.newInfections = newInfections;
    }

    public double getEffectiveR() {
        return effectiveR;
    }

    public void setEffectiveR(double effectiveR) {
        this.effectiveR = effectiveR;
    }

    public int getBedsOccupied() {
        return bedsOccupied;
    }

    public void setBedsOccupied(int bedsOccupied) {
        this.bedsOccupied = bedsOccupied;
    }

    public boolean isOverCapacity() {
        return overCapacity;
    }

    public void setOverCapacity(boolean overCapacity) {
        this.overCapacity = overCapacity;
    }
}
