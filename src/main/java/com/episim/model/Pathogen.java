package com.episim.model;

import com.episim.util.SimConstants;

/**
 * The disease being simulated: its transmissibility, timeline and outcome
 * rates.
 */
public class Pathogen {

    private int id;
    private String name;
    private double r0;
    private int incubationDays;
    private int infectiousDays;
    private double hospitalisationRate;
    private double mortalityRate;
    private double vaccineEffectiveness;
    private String description;

    public Pathogen(int id, String name, double r0, int incubationDays, int infectiousDays,
                     double hospitalisationRate, double mortalityRate, double vaccineEffectiveness,
                     String description) {
        this.id = id;
        this.name = name;
        this.r0 = r0;
        this.incubationDays = incubationDays;
        this.infectiousDays = infectiousDays;
        this.hospitalisationRate = hospitalisationRate;
        this.mortalityRate = mortalityRate;
        this.vaccineEffectiveness = vaccineEffectiveness;
        this.description = description;
    }

    /** Probability that a single contact with an infectious person transmits the pathogen. */
    public double perContactTransmissionProbability() {
        return r0 / (infectiousDays * SimConstants.AVERAGE_DAILY_CONTACTS);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getR0() {
        return r0;
    }

    public void setR0(double r0) {
        this.r0 = r0;
    }

    public int getIncubationDays() {
        return incubationDays;
    }

    public void setIncubationDays(int incubationDays) {
        this.incubationDays = incubationDays;
    }

    public int getInfectiousDays() {
        return infectiousDays;
    }

    public void setInfectiousDays(int infectiousDays) {
        this.infectiousDays = infectiousDays;
    }

    public double getHospitalisationRate() {
        return hospitalisationRate;
    }

    public void setHospitalisationRate(double hospitalisationRate) {
        this.hospitalisationRate = hospitalisationRate;
    }

    public double getMortalityRate() {
        return mortalityRate;
    }

    public void setMortalityRate(double mortalityRate) {
        this.mortalityRate = mortalityRate;
    }

    public double getVaccineEffectiveness() {
        return vaccineEffectiveness;
    }

    public void setVaccineEffectiveness(double vaccineEffectiveness) {
        this.vaccineEffectiveness = vaccineEffectiveness;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
