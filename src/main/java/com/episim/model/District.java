package com.episim.model;

import com.episim.util.SimConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A geographic area with its own population, hospital capacity, and roster
 * of residents.
 */
public class District implements Reportable {

    private String id;
    private String name;
    private int population;
    private double densityFactor;
    private int hospitalCapacity;
    private final List<Person> residents = new ArrayList<>();

    public District(String id, String name, int population, double densityFactor, int hospitalCapacity) {
        this.id = id;
        this.name = name;
        this.population = population;
        this.densityFactor = densityFactor;
        this.hospitalCapacity = hospitalCapacity;
    }

    public void addResident(Person person) {
        residents.add(person);
    }

    /** Tally of residents currently in each health state. */
    public Map<HealthState, Integer> stateBreakdown() {
        Map<HealthState, Integer> breakdown = new HashMap<>();
        for (Person person : residents) {
            breakdown.merge(person.getHealthState(), 1, Integer::sum);
        }
        return breakdown;
    }

    public int occupiedBeds() {
        int occupied = 0;
        for (Person person : residents) {
            if (person.getHealthState() == HealthState.HOSPITALISED) {
                occupied++;
            }
        }
        return occupied;
    }

    public boolean isHospitalOverwhelmed() {
        return occupiedBeds() > hospitalCapacity;
    }

    @Override
    public String getReportTitle() {
        return "District Report: " + name;
    }

    @Override
    public String toReportLine() {
        // This line is written to plain-text report exports, so it must use SimConstants.DATA_LOCALE
        // (Locale.ROOT) rather than the machine's default locale — see SimConstants for the full policy.
        return String.format(SimConstants.DATA_LOCALE, "%s (%s) | population=%d | beds=%d/%d | overwhelmed=%s",
                name, id, population, occupiedBeds(), hospitalCapacity, isHospitalOverwhelmed());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public double getDensityFactor() {
        return densityFactor;
    }

    public void setDensityFactor(double densityFactor) {
        this.densityFactor = densityFactor;
    }

    public int getHospitalCapacity() {
        return hospitalCapacity;
    }

    public void setHospitalCapacity(int hospitalCapacity) {
        this.hospitalCapacity = hospitalCapacity;
    }

    public List<Person> getResidents() {
        return residents;
    }
}
