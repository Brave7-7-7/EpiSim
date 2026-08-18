package com.episim.model;

/** Movement restrictions — the strongest transmission suppressor, no direct severity effect. */
public class Lockdown extends Intervention {

    public Lockdown(int id, int runId, String name, int startDay, int endDay, double intensity,
                     double costPerDayRM, boolean active) {
        super(id, runId, name, startDay, endDay, intensity, costPerDayRM, active);
    }

    @Override
    public double transmissionModifier() {
        return 1.0 - (0.75 * getIntensity());
    }

    @Override
    public double severityModifier() {
        return 1.0;
    }

    @Override
    public String getDescription() {
        return "Lockdown restricting movement and gatherings at intensity " + getIntensity();
    }
}
