package com.episim.model;

/**
 * A resident of a care home. Low mobility keeps exposure risk down, but age
 * and comorbidities make severity risk high.
 */
public class ElderlyResident extends Person {

    private String careHomeName;

    public ElderlyResident(int id, String fullName, int age, String districtId, HealthState healthState,
                            int daysInCurrentState, boolean vaccinated, double immunityLevel,
                            String careHomeName) {
        super(id, fullName, age, districtId, healthState, daysInCurrentState, vaccinated, immunityLevel);
        this.careHomeName = careHomeName;
    }

    @Override
    public double getExposureMultiplier() {
        return 0.6;
    }

    @Override
    public double getSeverityMultiplier() {
        return 3.5;
    }

    @Override
    public String getRoleLabel() {
        return "Elderly Resident";
    }

    public String getCareHomeName() {
        return careHomeName;
    }

    public void setCareHomeName(String careHomeName) {
        this.careHomeName = careHomeName;
    }
}
