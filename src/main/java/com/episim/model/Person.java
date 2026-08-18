package com.episim.model;

/**
 * Abstract base of the person hierarchy. Holds every field common to a
 * simulated individual behind private state with full accessors
 * (encapsulation) and exposes three abstract hooks that each concrete role
 * must implement (polymorphism evidence for the simulation engine, which
 * only ever programs against {@code Person}).
 */
public abstract class Person {

    private int id;
    private String fullName;
    private int age;
    private String districtId;
    private HealthState healthState;
    private int daysInCurrentState;
    private boolean vaccinated;
    private double immunityLevel;

    protected Person(int id, String fullName, int age, String districtId, HealthState healthState,
                      int daysInCurrentState, boolean vaccinated, double immunityLevel) {
        this.id = id;
        this.fullName = fullName;
        this.age = age;
        this.districtId = districtId;
        this.healthState = healthState;
        this.daysInCurrentState = daysInCurrentState;
        this.vaccinated = vaccinated;
        this.immunityLevel = immunityLevel;
    }

    /** How much more/less likely this person is to be exposed per contact. */
    public abstract double getExposureMultiplier();

    /** How much more/less likely this person is to need hospitalisation or die. */
    public abstract double getSeverityMultiplier();

    public abstract String getRoleLabel();

    public void transitionTo(HealthState state) {
        this.healthState = state;
        this.daysInCurrentState = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public HealthState getHealthState() {
        return healthState;
    }

    public void setHealthState(HealthState healthState) {
        this.healthState = healthState;
    }

    public int getDaysInCurrentState() {
        return daysInCurrentState;
    }

    public void setDaysInCurrentState(int daysInCurrentState) {
        this.daysInCurrentState = daysInCurrentState;
    }

    public boolean isVaccinated() {
        return vaccinated;
    }

    public void setVaccinated(boolean vaccinated) {
        this.vaccinated = vaccinated;
    }

    public double getImmunityLevel() {
        return immunityLevel;
    }

    public void setImmunityLevel(double immunityLevel) {
        this.immunityLevel = immunityLevel;
    }

    @Override
    public String toString() {
        return String.format("%s{id=%d, name='%s', age=%d, role=%s, state=%s}",
                getClass().getSimpleName(), id, fullName, age, getRoleLabel(), healthState);
    }
}
