package com.episim.model;

/** Movement restrictions — the strongest transmission suppressor, no direct severity effect. */
public class Lockdown extends Intervention {

    /** @see Intervention#Intervention(int, int, String, int, int, double, double, boolean) */
    public Lockdown(int id, int runId, String name, int startDay, int endDay, double intensity,
                     double costPerDayRM, boolean active) {
        super(id, runId, name, startDay, endDay, intensity, costPerDayRM, active);
    }

    /** @return {@code 1.0 - (0.75 * intensity)} — the strongest transmission suppressor of the four */
    @Override
    public double transmissionModifier() {
        return 1.0 - (0.75 * getIntensity());
    }

    /** @return 1.0 — no direct effect on severity */
    @Override
    public double severityModifier() {
        return 1.0;
    }

    /** @return a description naming the current intensity */
    @Override
    public String getDescription() {
        return "Lockdown restricting movement and gatherings at intensity " + getIntensity();
    }
}
