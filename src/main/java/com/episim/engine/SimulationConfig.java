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

    public SimulationConfig setRunName(String runName) {
        this.runName = runName;
        return this;
    }

    public SimulationConfig setPathogen(Pathogen pathogen) {
        this.pathogen = pathogen;
        return this;
    }

    public SimulationConfig setTotalDays(int totalDays) {
        this.totalDays = totalDays;
        return this;
    }

    public SimulationConfig setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
        return this;
    }

    public SimulationConfig setSeedInfections(int seedInfections) {
        this.seedInfections = seedInfections;
        return this;
    }

    public SimulationConfig setHealthcareWorkerRatio(double healthcareWorkerRatio) {
        this.healthcareWorkerRatio = healthcareWorkerRatio;
        return this;
    }

    public SimulationConfig setElderlyRatio(double elderlyRatio) {
        this.elderlyRatio = elderlyRatio;
        return this;
    }

    public SimulationConfig setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
        return this;
    }

    public String getRunName() {
        return runName;
    }

    public Pathogen getPathogen() {
        return pathogen;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public int getSeedInfections() {
        return seedInfections;
    }

    public double getHealthcareWorkerRatio() {
        return healthcareWorkerRatio;
    }

    public double getElderlyRatio() {
        return elderlyRatio;
    }

    public long getRandomSeed() {
        return randomSeed;
    }
}
