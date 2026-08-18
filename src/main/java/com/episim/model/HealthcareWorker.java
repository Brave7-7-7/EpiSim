package com.episim.model;

/**
 * Front-line medical staff. High contact rate but protected by PPE when
 * equipped.
 */
public class HealthcareWorker extends Person {

    private boolean hasPPE;
    private String hospitalAssigned;

    public HealthcareWorker(int id, String fullName, int age, String districtId, HealthState healthState,
                             int daysInCurrentState, boolean vaccinated, double immunityLevel,
                             boolean hasPPE, String hospitalAssigned) {
        super(id, fullName, age, districtId, healthState, daysInCurrentState, vaccinated, immunityLevel);
        this.hasPPE = hasPPE;
        this.hospitalAssigned = hospitalAssigned;
    }

    @Override
    public double getExposureMultiplier() {
        return hasPPE ? 0.7 : 2.2;
    }

    @Override
    public double getSeverityMultiplier() {
        return 0.9;
    }

    @Override
    public String getRoleLabel() {
        return "Healthcare Worker";
    }

    public boolean isHasPPE() {
        return hasPPE;
    }

    public void setHasPPE(boolean hasPPE) {
        this.hasPPE = hasPPE;
    }

    public String getHospitalAssigned() {
        return hospitalAssigned;
    }

    public void setHospitalAssigned(String hospitalAssigned) {
        this.hospitalAssigned = hospitalAssigned;
    }
}
