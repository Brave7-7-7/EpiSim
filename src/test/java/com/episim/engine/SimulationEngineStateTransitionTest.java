package com.episim.engine;

import com.episim.dao.DatabaseManager;
import com.episim.dao.DistrictDao;
import com.episim.dao.PathogenDao;
import com.episim.model.DailyRecord;
import com.episim.model.District;
import com.episim.model.HealthState;
import com.episim.model.Pathogen;
import com.episim.model.Person;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises SimulationEngine's day-stepping state-transition rules. Uses an in-memory SQLite database
 * (never the real data/episim.db) since start()/stepOneDay() persist as they go.
 */
class SimulationEngineStateTransitionTest {

    @BeforeEach
    void setUpInMemoryDatabase() {
        DatabaseManager.useInMemoryDatabaseForTests();
    }

    @AfterEach
    void tearDownInMemoryDatabase() {
        DatabaseManager.close();
    }

    private Pathogen insertTestPathogen(double hospitalisationRate, double mortalityRate) throws Exception {
        // incubationDays=3, infectiousDays=5
        Pathogen pathogen = new Pathogen(0, "Test Pathogen", 3.0, 3, 5, hospitalisationRate, mortalityRate, 0.0,
                "Synthetic pathogen used for engine unit tests.");
        new PathogenDao().insert(pathogen);
        return pathogen;
    }

    private SimulationEngine singlePersonEngine(Pathogen pathogen, double elderlyRatio, long randomSeed) {
        SimulationConfig config = new SimulationConfig()
                .setRunName("State Transition Test")
                .setPathogen(pathogen)
                .setTotalDays(30)
                .setPopulationSize(1)
                .setSeedInfections(0)
                .setHealthcareWorkerRatio(0.0)
                .setElderlyRatio(elderlyRatio)
                .setRandomSeed(randomSeed);
        SimulationEngine engine = new SimulationEngine(config);
        engine.start();
        return engine;
    }

    @Test
    void exposedPersonBecomesInfectedOnlyAfterTheIncubationPeriodElapses() throws Exception {
        Pathogen pathogen = insertTestPathogen(0.0, 0.0); // never hospitalised, never dies
        SimulationEngine engine = singlePersonEngine(pathogen, 0.0, 42L);

        Person person = engine.getPopulation().get(0);
        person.setHealthState(HealthState.EXPOSED);
        person.setDaysInCurrentState(2); // one day short of the 3-day incubation period

        engine.stepOneDay();
        assertEquals(HealthState.EXPOSED, person.getHealthState(),
                "Should not incubate yet: daysInCurrentState was still below incubationDays at the start of the day");
        assertEquals(3, person.getDaysInCurrentState());

        engine.stepOneDay();
        assertEquals(HealthState.INFECTED, person.getHealthState(),
                "Should become infected once daysInCurrentState reaches incubationDays");
        assertEquals(1, person.getDaysInCurrentState(),
                "transitionTo() resets the day counter to 0, which then advances by one within the same step");
    }

    @Test
    void infectedPersonRecoversAfterInfectiousDaysWhenMortalityRateIsZero() throws Exception {
        Pathogen pathogen = insertTestPathogen(0.0, 0.0); // never hospitalised, never dies
        SimulationEngine engine = singlePersonEngine(pathogen, 0.0, 42L);

        Person person = engine.getPopulation().get(0);
        person.setHealthState(HealthState.INFECTED);
        person.setDaysInCurrentState(0);

        for (int day = 1; day <= 6; day++) {
            engine.stepOneDay();
        }

        assertEquals(HealthState.RECOVERED, person.getHealthState());
        assertEquals(0.9, person.getImmunityLevel(), 1e-9);
    }

    @Test
    void infectedPersonDiesAfterInfectiousDaysWhenMortalityRateIsCertain() throws Exception {
        Pathogen pathogen = insertTestPathogen(0.0, 1.0); // never hospitalised, always dies on resolution
        // elderlyRatio=1.0 fixes severityMultiplier at 3.5 (Citizen's severity varies by random age,
        // which would make a "certain death" assertion non-deterministic).
        SimulationEngine engine = singlePersonEngine(pathogen, 1.0, 42L);

        Person person = engine.getPopulation().get(0);
        person.setHealthState(HealthState.INFECTED);
        person.setDaysInCurrentState(0);

        for (int day = 1; day <= 6; day++) {
            engine.stepOneDay();
        }

        assertEquals(HealthState.DECEASED, person.getHealthState());
    }

    @Test
    void infectedPersonIsQueuedInsteadOfAdmittedWhenNoHospitalBedsAreAvailable() throws Exception {
        Pathogen pathogen = insertTestPathogen(1.0, 0.0); // always attempts hospitalisation

        DistrictDao districtDao = new DistrictDao();
        for (District district : districtDao.findAll()) {
            district.setHospitalCapacity(0);
            districtDao.update(district);
        }

        // elderlyRatio=1.0 fixes severityMultiplier at 3.5, guaranteeing hospitalisationProbability >= 1.
        SimulationEngine engine = singlePersonEngine(pathogen, 1.0, 7L);

        Person person = engine.getPopulation().get(0);
        person.setHealthState(HealthState.INFECTED);
        person.setDaysInCurrentState(0);

        engine.stepOneDay();

        assertEquals(HealthState.INFECTED, person.getHealthState(),
                "No bed is available, so the person stays INFECTED rather than becoming HOSPITALISED");
        assertEquals(1, engine.getAdmissionQueueSize());
    }

    @Test
    void runAllCompletesTheConfiguredNumberOfDaysAndMarksTheEngineFinished() throws Exception {
        Pathogen pathogen = insertTestPathogen(0.05, 0.01);
        SimulationConfig config = new SimulationConfig()
                .setRunName("Full Run Test")
                .setPathogen(pathogen)
                .setTotalDays(15)
                .setPopulationSize(50)
                .setSeedInfections(5)
                .setHealthcareWorkerRatio(0.1)
                .setElderlyRatio(0.1)
                .setRandomSeed(99L);
        SimulationEngine engine = new SimulationEngine(config);

        engine.runAll();

        assertTrue(engine.isFinished());
        List<DailyRecord> history = engine.getHistory();
        assertEquals(15, history.size());
    }
}
