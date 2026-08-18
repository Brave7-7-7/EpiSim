package com.episim.model;

/**
 * An ordinary member of the public. Average exposure risk; severity risk
 * scales with age bracket.
 */
public class Citizen extends Person {

    public Citizen(int id, String fullName, int age, String districtId, HealthState healthState,
                    int daysInCurrentState, boolean vaccinated, double immunityLevel) {
        super(id, fullName, age, districtId, healthState, daysInCurrentState, vaccinated, immunityLevel);
    }

    @Override
    public double getExposureMultiplier() {
        return 1.0;
    }

    @Override
    public double getSeverityMultiplier() {
        int age = getAge();
        if (age < 18) {
            return 0.5;
        } else if (age <= 59) {
            return 1.0;
        } else {
            return 2.5;
        }
    }

    @Override
    public String getRoleLabel() {
        return "Citizen";
    }
}
