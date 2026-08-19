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

    /**
     * @param id                 database identity, or 0 for a not-yet-persisted person
     * @param fullName           display name
     * @param age                age in years
     * @param districtId         id of the district this person resides in
     * @param healthState        current epidemiological state
     * @param daysInCurrentState days since the last state transition
     * @param vaccinated         whether this person has received a vaccine
     * @param immunityLevel      immunity strength in [0.0, 1.0]
     */
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

    /** @return how much more/less likely this person is to be exposed per contact */
    public abstract double getExposureMultiplier();

    /** @return how much more/less likely this person is to need hospitalisation or die */
    public abstract double getSeverityMultiplier();

    /** @return the human-readable role label for this concrete subclass, e.g. "Healthcare Worker" */
    public abstract String getRoleLabel();

    /**
     * Moves this person to a new health state, resetting the day counter — the only sanctioned way to
     * change {@link #getHealthState()}, since setting it directly would leave
     * {@link #getDaysInCurrentState()} stale.
     *
     * @param state the state to transition into
     */
    public void transitionTo(HealthState state) {
        this.healthState = state;
        this.daysInCurrentState = 0;
    }

    /** @return the database identity, or 0 if not yet persisted */
    public int getId() {
        return id;
    }

    /** @param id the database identity to assign, typically after a generated-key insert */
    public void setId(int id) {
        this.id = id;
    }

    /** @return the display name */
    public String getFullName() {
        return fullName;
    }

    /** @param fullName the new display name */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /** @return the age in years */
    public int getAge() {
        return age;
    }

    /** @param age the new age in years */
    public void setAge(int age) {
        this.age = age;
    }

    /** @return the id of the district this person resides in */
    public String getDistrictId() {
        return districtId;
    }

    /** @param districtId the new district id */
    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    /** @return the current epidemiological state */
    public HealthState getHealthState() {
        return healthState;
    }

    /**
     * Sets the health state directly, without resetting the day counter. Prefer {@link #transitionTo}
     * unless you specifically need to set state and day count independently (e.g. reconstructing a
     * person from a database row).
     *
     * @param healthState the new state
     */
    public void setHealthState(HealthState healthState) {
        this.healthState = healthState;
    }

    /** @return days elapsed since the last call to {@link #transitionTo} */
    public int getDaysInCurrentState() {
        return daysInCurrentState;
    }

    /** @param daysInCurrentState the new day count */
    public void setDaysInCurrentState(int daysInCurrentState) {
        this.daysInCurrentState = daysInCurrentState;
    }

    /** @return whether this person has received a vaccine */
    public boolean isVaccinated() {
        return vaccinated;
    }

    /** @param vaccinated the new vaccination status */
    public void setVaccinated(boolean vaccinated) {
        this.vaccinated = vaccinated;
    }

    /** @return immunity strength in [0.0, 1.0] */
    public double getImmunityLevel() {
        return immunityLevel;
    }

    /** @param immunityLevel the new immunity strength, expected in [0.0, 1.0] */
    public void setImmunityLevel(double immunityLevel) {
        this.immunityLevel = immunityLevel;
    }

    /** @return a debug-oriented summary including the runtime subclass name and polymorphic role label */
    @Override
    public String toString() {
        return String.format("%s{id=%d, name='%s', age=%d, role=%s, state=%s}",
                getClass().getSimpleName(), id, fullName, age, getRoleLabel(), healthState);
    }
}
