package com.episim.model;

/**
 * A resident of a care home. Low mobility keeps exposure risk down, but age
 * and comorbidities make severity risk high.
 */
public class ElderlyResident extends Person {

    private String careHomeName;

    /**
     * @param careHomeName name of the care home this resident lives in
     * @see Person#Person(int, String, int, String, HealthState, int, boolean, double)
     */
    public ElderlyResident(int id, String fullName, int age, String districtId, HealthState healthState,
                            int daysInCurrentState, boolean vaccinated, double immunityLevel,
                            String careHomeName) {
        super(id, fullName, age, districtId, healthState, daysInCurrentState, vaccinated, immunityLevel);
        this.careHomeName = careHomeName;
    }

    /** @return 0.6 — low mobility keeps exposure risk below baseline */
    @Override
    public double getExposureMultiplier() {
        return 0.6;
    }

    /** @return 3.5 — age and comorbidities make severity risk the highest of any role */
    @Override
    public double getSeverityMultiplier() {
        return 3.5;
    }

    /** @return "Elderly Resident" */
    @Override
    public String getRoleLabel() {
        return "Elderly Resident";
    }

    /** @return the name of the care home this resident lives in */
    public String getCareHomeName() {
        return careHomeName;
    }

    /** @param careHomeName the new care home name */
    public void setCareHomeName(String careHomeName) {
        this.careHomeName = careHomeName;
    }
}
