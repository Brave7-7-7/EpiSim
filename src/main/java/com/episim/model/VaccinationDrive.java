package com.episim.model;

/** Mass vaccination campaign — reduces both transmission and severity. */
public class VaccinationDrive extends Intervention {

    private int dosesPerDay;

    /**
     * @param dosesPerDay doses administered per day
     * @see Intervention#Intervention(int, int, String, int, int, double, double, boolean)
     */
    public VaccinationDrive(int id, int runId, String name, int startDay, int endDay, double intensity,
                             double costPerDayRM, boolean active, int dosesPerDay) {
        super(id, runId, name, startDay, endDay, intensity, costPerDayRM, active);
        this.dosesPerDay = dosesPerDay;
    }

    /** @return {@code 1.0 - (0.55 * intensity)} */
    @Override
    public double transmissionModifier() {
        return 1.0 - (0.55 * getIntensity());
    }

    /** @return {@code 1.0 - (0.60 * intensity)} — the only intervention that also reduces severity */
    @Override
    public double severityModifier() {
        return 1.0 - (0.60 * getIntensity());
    }

    /** @return a description naming the doses/day and current intensity */
    @Override
    public String getDescription() {
        return "Vaccination drive administering " + dosesPerDay + " doses/day at intensity " + getIntensity();
    }

    /** @return doses administered per day */
    public int getDosesPerDay() {
        return dosesPerDay;
    }

    /** @param dosesPerDay the new doses-per-day rate */
    public void setDosesPerDay(int dosesPerDay) {
        this.dosesPerDay = dosesPerDay;
    }
}
