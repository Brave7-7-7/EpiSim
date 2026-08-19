package com.episim.model;

/** Mandatory face coverings — moderate transmission suppressor, no direct severity effect. */
public class MaskMandate extends Intervention {

    /** @see Intervention#Intervention(int, int, String, int, int, double, double, boolean) */
    public MaskMandate(int id, int runId, String name, int startDay, int endDay, double intensity,
                        double costPerDayRM, boolean active) {
        super(id, runId, name, startDay, endDay, intensity, costPerDayRM, active);
    }

    /** @return {@code 1.0 - (0.35 * intensity)} — a moderate transmission suppressor */
    @Override
    public double transmissionModifier() {
        return 1.0 - (0.35 * getIntensity());
    }

    /** @return 1.0 — no direct effect on severity */
    @Override
    public double severityModifier() {
        return 1.0;
    }

    /** @return a description naming the current intensity */
    @Override
    public String getDescription() {
        return "Mask mandate in effect at intensity " + getIntensity();
    }
}
