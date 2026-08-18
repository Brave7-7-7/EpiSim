package com.episim.model;

/** Mass vaccination campaign — reduces both transmission and severity. */
public class VaccinationDrive extends Intervention {

    private int dosesPerDay;

    public VaccinationDrive(int id, int runId, String name, int startDay, int endDay, double intensity,
                             double costPerDayRM, boolean active, int dosesPerDay) {
        super(id, runId, name, startDay, endDay, intensity, costPerDayRM, active);
        this.dosesPerDay = dosesPerDay;
    }

    @Override
    public double transmissionModifier() {
        return 1.0 - (0.55 * getIntensity());
    }

    @Override
    public double severityModifier() {
        return 1.0 - (0.60 * getIntensity());
    }

    @Override
    public String getDescription() {
        return "Vaccination drive administering " + dosesPerDay + " doses/day at intensity " + getIntensity();
    }

    public int getDosesPerDay() {
        return dosesPerDay;
    }

    public void setDosesPerDay(int dosesPerDay) {
        this.dosesPerDay = dosesPerDay;
    }
}
