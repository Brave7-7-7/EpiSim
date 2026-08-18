package com.episim.model;

/** Digital/manual contact tracing — isolates contacts before they can spread further. */
public class ContactTracing extends Intervention {

    private int tracingCapacityPerDay;

    public ContactTracing(int id, int runId, String name, int startDay, int endDay, double intensity,
                           double costPerDayRM, boolean active, int tracingCapacityPerDay) {
        super(id, runId, name, startDay, endDay, intensity, costPerDayRM, active);
        this.tracingCapacityPerDay = tracingCapacityPerDay;
    }

    @Override
    public double transmissionModifier() {
        return 1.0 - (0.40 * getIntensity());
    }

    @Override
    public double severityModifier() {
        return 1.0;
    }

    @Override
    public String getDescription() {
        return "Contact tracing covering " + tracingCapacityPerDay + " cases/day at intensity " + getIntensity();
    }

    public int getTracingCapacityPerDay() {
        return tracingCapacityPerDay;
    }

    public void setTracingCapacityPerDay(int tracingCapacityPerDay) {
        this.tracingCapacityPerDay = tracingCapacityPerDay;
    }
}
