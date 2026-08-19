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

    /**
     * @param id            database identity, or 0 for a not-yet-persisted record
     * @param runId         id of the simulation run this day belongs to
     * @param dayNumber     1-indexed simulated day
     * @param susceptible   headcount currently {@code SUSCEPTIBLE}
     * @param exposed       headcount currently {@code EXPOSED}
     * @param infected      headcount currently {@code INFECTED}
     * @param hospitalised  headcount currently {@code HOSPITALISED}
     * @param recovered     headcount currently {@code RECOVERED}
     * @param deceased      headcount currently {@code DECEASED}
     * @param newInfections new {@code SUSCEPTIBLE -> EXPOSED} transitions this day
     * @param effectiveR    newInfections / newInfectionsPreviousDay (0 if the previous day had none)
     * @param bedsOccupied  total hospital beds occupied across all districts this day
     * @param overCapacity  whether any district was overwhelmed this day
     */
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

    /** @return the total living headcount (everyone except {@code DECEASED}) */
    public int totalAlive() {
        return susceptible + exposed + infected + hospitalised + recovered;
    }

    /** @return the report section title for this day */
    @Override
    public String getReportTitle() {
        return "Daily Report — Day " + dayNumber;
    }

    /** @return a single formatted line summarising this day's SEIR counts, effective R, and bed status */
    @Override
    public String toReportLine() {
        // This line is written to plain-text report exports, so it must use SimConstants.DATA_LOCALE
        // (Locale.ROOT) rather than the machine's default locale — see SimConstants for the full policy.
        return String.format(SimConstants.DATA_LOCALE,
                "Day %d | S=%d E=%d I=%d H=%d R=%d D=%d | newInfections=%d | Reff=%.2f | beds=%d | overCapacity=%s",
                dayNumber, susceptible, exposed, infected, hospitalised, recovered, deceased,
                newInfections, effectiveR, bedsOccupied, overCapacity);
    }

    /** @return the database identity, or 0 if not yet persisted */
    public int getId() {
        return id;
    }

    /** @param id the database identity to assign, typically after a generated-key insert */
    public void setId(int id) {
        this.id = id;
    }

    /** @return the id of the simulation run this day belongs to */
    public int getRunId() {
        return runId;
    }

    /** @param runId the new owning run id */
    public void setRunId(int runId) {
        this.runId = runId;
    }

    /** @return the 1-indexed simulated day number */
    public int getDayNumber() {
        return dayNumber;
    }

    /** @param dayNumber the new day number */
    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    /** @return the headcount currently {@code SUSCEPTIBLE} */
    public int getSusceptible() {
        return susceptible;
    }

    /** @param susceptible the new susceptible headcount */
    public void setSusceptible(int susceptible) {
        this.susceptible = susceptible;
    }

    /** @return the headcount currently {@code EXPOSED} */
    public int getExposed() {
        return exposed;
    }

    /** @param exposed the new exposed headcount */
    public void setExposed(int exposed) {
        this.exposed = exposed;
    }

    /** @return the headcount currently {@code INFECTED} */
    public int getInfected() {
        return infected;
    }

    /** @param infected the new infected headcount */
    public void setInfected(int infected) {
        this.infected = infected;
    }

    /** @return the headcount currently {@code HOSPITALISED} */
    public int getHospitalised() {
        return hospitalised;
    }

    /** @param hospitalised the new hospitalised headcount */
    public void setHospitalised(int hospitalised) {
        this.hospitalised = hospitalised;
    }

    /** @return the headcount currently {@code RECOVERED} */
    public int getRecovered() {
        return recovered;
    }

    /** @param recovered the new recovered headcount */
    public void setRecovered(int recovered) {
        this.recovered = recovered;
    }

    /** @return the headcount currently {@code DECEASED} */
    public int getDeceased() {
        return deceased;
    }

    /** @param deceased the new deceased headcount */
    public void setDeceased(int deceased) {
        this.deceased = deceased;
    }

    /** @return new {@code SUSCEPTIBLE -> EXPOSED} transitions this day */
    public int getNewInfections() {
        return newInfections;
    }

    /** @param newInfections the new count of new infections */
    public void setNewInfections(int newInfections) {
        this.newInfections = newInfections;
    }

    /** @return newInfections / newInfectionsPreviousDay for this day */
    public double getEffectiveR() {
        return effectiveR;
    }

    /** @param effectiveR the new effective R value */
    public void setEffectiveR(double effectiveR) {
        this.effectiveR = effectiveR;
    }

    /** @return total hospital beds occupied across all districts this day */
    public int getBedsOccupied() {
        return bedsOccupied;
    }

    /** @param bedsOccupied the new beds-occupied count */
    public void setBedsOccupied(int bedsOccupied) {
        this.bedsOccupied = bedsOccupied;
    }

    /** @return whether any district was overwhelmed this day */
    public boolean isOverCapacity() {
        return overCapacity;
    }

    /** @param overCapacity the new over-capacity flag */
    public void setOverCapacity(boolean overCapacity) {
        this.overCapacity = overCapacity;
    }
}
