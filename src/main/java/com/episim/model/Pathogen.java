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

    /**
     * @param id                    database identity, or 0 for a not-yet-persisted pathogen
     * @param name                  display name, unique across the reference data
     * @param r0                    basic reproduction number
     * @param incubationDays        days spent {@code EXPOSED} before becoming infectious
     * @param infectiousDays        days spent infectious before resolving to recovered/deceased
     * @param hospitalisationRate   daily probability of hospitalisation while infected, in [0.0, 1.0]
     * @param mortalityRate         probability of death on resolution, in [0.0, 1.0]
     * @param vaccineEffectiveness  immunity conferred to a vaccinated person, in [0.0, 1.0]
     * @param description           free-text notes shown in the pathogen picker
     */
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

    /** @return the probability that a single contact with an infectious person transmits the pathogen */
    public double perContactTransmissionProbability() {
        return r0 / (infectiousDays * SimConstants.AVERAGE_DAILY_CONTACTS);
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
    public String getName() {
        return name;
    }

    /** @param name the new display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the basic reproduction number */
    public double getR0() {
        return r0;
    }

    /** @param r0 the new basic reproduction number */
    public void setR0(double r0) {
        this.r0 = r0;
    }

    /** @return days spent {@code EXPOSED} before becoming infectious */
    public int getIncubationDays() {
        return incubationDays;
    }

    /** @param incubationDays the new incubation period in days */
    public void setIncubationDays(int incubationDays) {
        this.incubationDays = incubationDays;
    }

    /** @return days spent infectious before resolving to recovered/deceased */
    public int getInfectiousDays() {
        return infectiousDays;
    }

    /** @param infectiousDays the new infectious period in days */
    public void setInfectiousDays(int infectiousDays) {
        this.infectiousDays = infectiousDays;
    }

    /** @return the daily probability of hospitalisation while infected */
    public double getHospitalisationRate() {
        return hospitalisationRate;
    }

    /** @param hospitalisationRate the new hospitalisation probability, expected in [0.0, 1.0] */
    public void setHospitalisationRate(double hospitalisationRate) {
        this.hospitalisationRate = hospitalisationRate;
    }

    /** @return the probability of death on resolution */
    public double getMortalityRate() {
        return mortalityRate;
    }

    /** @param mortalityRate the new mortality probability, expected in [0.0, 1.0] */
    public void setMortalityRate(double mortalityRate) {
        this.mortalityRate = mortalityRate;
    }

    /** @return the immunity conferred to a vaccinated person */
    public double getVaccineEffectiveness() {
        return vaccineEffectiveness;
    }

    /** @param vaccineEffectiveness the new effectiveness, expected in [0.0, 1.0] */
    public void setVaccineEffectiveness(double vaccineEffectiveness) {
        this.vaccineEffectiveness = vaccineEffectiveness;
    }

    /** @return free-text notes about this pathogen */
    public String getDescription() {
        return description;
    }

    /** @param description the new free-text notes */
    public void setDescription(String description) {
        this.description = description;
    }
}
