package com.episim.model;

import com.episim.util.SimConstants;

import java.util.ArrayList;
import java.util.Collection;
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
    // The district table's capacity is sized against a fixed design population (see schema.sql's seed
    // data), not against whatever population a simulation run actually configures. hospitalCapacity may
    // be rescaled in-memory for a run (see SimulationEngine); this field preserves the original DB
    // value so that scaling is never lossy and the reference data itself is never mutated.
    private final int designHospitalCapacity;
    private final List<Person> residents = new ArrayList<>();

    public District(String id, String name, int population, double densityFactor, int hospitalCapacity) {
        this.id = id;
        this.name = name;
        this.population = population;
        this.densityFactor = densityFactor;
        this.hospitalCapacity = hospitalCapacity;
        this.designHospitalCapacity = hospitalCapacity;
    }

    public void addResident(Person person) {
        residents.add(person);
    }

    /**
     * Scales every district's in-memory hospitalCapacity to a simulated population, based on the ratio
     * of simulatedPopulation to the districts' combined design population column. designHospitalCapacity
     * (and the persisted DB row) are never touched. Shared by SimulationEngine (for a live run) and the
     * GUI's "load historical run" flow, so both compute the scaled capacity identically rather than
     * risking two formulas drifting apart.
     */
    public static void scaleHospitalCapacities(Collection<District> districts, int simulatedPopulation) {
        int totalDesignPopulation = districts.stream().mapToInt(District::getPopulation).sum();
        if (totalDesignPopulation <= 0) {
            return;
        }
        double scaleFactor = simulatedPopulation / (double) totalDesignPopulation;
        for (District district : districts) {
            int scaledCapacity = Math.max(1, (int) Math.ceil(district.getDesignHospitalCapacity() * scaleFactor));
            district.setHospitalCapacity(scaledCapacity);
        }
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

    /**
     * True once every bed is taken, not only once beds run negative. SimulationEngine only ever admits
     * a patient while occupiedBeds() &lt; hospitalCapacity (see applyHospitalisation()/admitFromQueue()),
     * so occupiedBeds() can never structurally exceed hospitalCapacity — a strict ">" here could never
     * fire, silently reporting the system as never overwhelmed no matter how full it actually was. ">="
     * matches the exact condition under which a patient gets queued instead of admitted.
     */
    public boolean isHospitalOverwhelmed() {
        return occupiedBeds() >= hospitalCapacity;
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

    public int getDesignHospitalCapacity() {
        return designHospitalCapacity;
    }

    public List<Person> getResidents() {
        return residents;
    }
}
