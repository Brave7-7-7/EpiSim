package com.episim.model;

/**
 * Front-line medical staff. High contact rate but protected by PPE when
 * equipped.
 */
public class HealthcareWorker extends Person {

    private boolean hasPPE;
    private String hospitalAssigned;

    /**
     * @param hasPPE            whether this worker is currently equipped with personal protective equipment
     * @param hospitalAssigned  name of the hospital this worker is assigned to
     * @see Person#Person(int, String, int, String, HealthState, int, boolean, double)
     */
    public HealthcareWorker(int id, String fullName, int age, String districtId, HealthState healthState,
                             int daysInCurrentState, boolean vaccinated, double immunityLevel,
                             boolean hasPPE, String hospitalAssigned) {
        super(id, fullName, age, districtId, healthState, daysInCurrentState, vaccinated, immunityLevel);
        this.hasPPE = hasPPE;
        this.hospitalAssigned = hospitalAssigned;
    }

    /** @return 0.7 with PPE, 2.2 without — high contact rate, but protected when equipped */
    @Override
    public double getExposureMultiplier() {
        return hasPPE ? 0.7 : 2.2;
    }

    /** @return 0.9 — slightly lower than baseline, reflecting working-age health and rapid care access */
    @Override
    public double getSeverityMultiplier() {
        return 0.9;
    }

    /** @return "Healthcare Worker" */
    @Override
    public String getRoleLabel() {
        return "Healthcare Worker";
    }

    /** @return whether this worker is currently equipped with PPE */
    public boolean isHasPPE() {
        return hasPPE;
    }

    /** @param hasPPE the new PPE status */
    public void setHasPPE(boolean hasPPE) {
        this.hasPPE = hasPPE;
    }

    /** @return the name of the hospital this worker is assigned to */
    public String getHospitalAssigned() {
        return hospitalAssigned;
    }

    /** @param hospitalAssigned the new hospital assignment */
    public void setHospitalAssigned(String hospitalAssigned) {
        this.hospitalAssigned = hospitalAssigned;
    }
}
