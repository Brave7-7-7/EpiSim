package com.episim.model;

/** Mandatory face coverings — moderate transmission suppressor, no direct severity effect. */
public class MaskMandate extends Intervention {

    public MaskMandate(int id, int runId, String name, int startDay, int endDay, double intensity,
                        double costPerDayRM, boolean active) {
        super(id, runId, name, startDay, endDay, intensity, costPerDayRM, active);
    }

    @Override
    public double transmissionModifier() {
        return 1.0 - (0.35 * getIntensity());
    }

    @Override
    public double severityModifier() {
        return 1.0;
    }

    @Override
    public String getDescription() {
        return "Mask mandate in effect at intensity " + getIntensity();
    }
}
