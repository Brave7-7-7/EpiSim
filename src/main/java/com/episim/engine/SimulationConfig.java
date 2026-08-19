package com.episim.engine;

import com.episim.model.Pathogen;

/**
 * Configuration for one simulation run, built via chained fluent setters
 * before being handed to {@link SimulationEngine}.
 */
public class SimulationConfig {

    private String runName;
    private Pathogen pathogen;
    private int totalDays;
    private int populationSize;
    private int seedInfections;
    private double healthcareWorkerRatio;
    private double elderlyRatio;
    private long randomSeed;

    /**
     * @param runName the new display/auto-generated label
     * @return this config, for chaining
     */
    public SimulationConfig setRunName(String runName) {
        this.runName = runName;
        return this;
    }

    /**
     * @param pathogen the disease to simulate
     * @return this config, for chaining
     */
    public SimulationConfig setPathogen(Pathogen pathogen) {
        this.pathogen = pathogen;
        return this;
    }

    /**
     * @param totalDays the simulation length in days
     * @return this config, for chaining
     */
    public SimulationConfig setTotalDays(int totalDays) {
        this.totalDays = totalDays;
        return this;
    }

    /**
     * @param populationSize the number of people to generate
     * @return this config, for chaining
     */
    public SimulationConfig setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
        return this;
    }

    /**
     * @param seedInfections how many people start {@code INFECTED} rather than {@code SUSCEPTIBLE}
     * @return this config, for chaining
     */
    public SimulationConfig setSeedInfections(int seedInfections) {
        this.seedInfections = seedInfections;
        return this;
    }

    /**
     * @param healthcareWorkerRatio proportion of the population generated as {@code HealthcareWorker}, in [0.0, 1.0]
     * @return this config, for chaining
     */
    public SimulationConfig setHealthcareWorkerRatio(double healthcareWorkerRatio) {
        this.healthcareWorkerRatio = healthcareWorkerRatio;
        return this;
    }

    /**
     * @param elderlyRatio proportion of the population generated as {@code ElderlyResident}, in [0.0, 1.0]
     * @return this config, for chaining
     */
    public SimulationConfig setElderlyRatio(double elderlyRatio) {
        this.elderlyRatio = elderlyRatio;
        return this;
    }

    /**
     * @param randomSeed seed for reproducible population generation and day-stepping
     * @return this config, for chaining
     */
    public SimulationConfig setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
        return this;
    }

    /** @return the display/auto-generated label */
    public String getRunName() {
        return runName;
    }

    /** @return the disease to simulate */
    public Pathogen getPathogen() {
        return pathogen;
    }

    /** @return the simulation length in days */
    public int getTotalDays() {
        return totalDays;
    }

    /** @return the number of people to generate */
    public int getPopulationSize() {
        return populationSize;
    }

    /** @return how many people start {@code INFECTED} rather than {@code SUSCEPTIBLE} */
    public int getSeedInfections() {
        return seedInfections;
    }

    /** @return the proportion of the population generated as {@code HealthcareWorker} */
    public double getHealthcareWorkerRatio() {
        return healthcareWorkerRatio;
    }

    /** @return the proportion of the population generated as {@code ElderlyResident} */
    public double getElderlyRatio() {
        return elderlyRatio;
    }

    /** @return the seed for reproducible population generation and day-stepping */
    public long getRandomSeed() {
        return randomSeed;
    }
}
