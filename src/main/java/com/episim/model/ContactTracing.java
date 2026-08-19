package com.episim.model;

/** Digital/manual contact tracing — isolates contacts before they can spread further. */
public class ContactTracing extends Intervention {

    private int tracingCapacityPerDay;

    /**
     * @param tracingCapacityPerDay contacts that can be traced per day
     * @see Intervention#Intervention(int, int, String, int, int, double, double, boolean)
     */
    public ContactTracing(int id, int runId, String name, int startDay, int endDay, double intensity,
                           double costPerDayRM, boolean active, int tracingCapacityPerDay) {
        super(id, runId, name, startDay, endDay, intensity, costPerDayRM, active);
        this.tracingCapacityPerDay = tracingCapacityPerDay;
    }

    /** @return {@code 1.0 - (0.40 * intensity)} */
    @Override
    public double transmissionModifier() {
        return 1.0 - (0.40 * getIntensity());
    }

    /** @return 1.0 — no direct effect on severity */
    @Override
    public double severityModifier() {
        return 1.0;
    }

    /** @return a description naming the tracing capacity and current intensity */
    @Override
    public String getDescription() {
        return "Contact tracing covering " + tracingCapacityPerDay + " cases/day at intensity " + getIntensity();
    }

    /** @return contacts that can be traced per day */
    public int getTracingCapacityPerDay() {
        return tracingCapacityPerDay;
    }

    /** @param tracingCapacityPerDay the new tracing capacity */
    public void setTracingCapacityPerDay(int tracingCapacityPerDay) {
        this.tracingCapacityPerDay = tracingCapacityPerDay;
    }
}
