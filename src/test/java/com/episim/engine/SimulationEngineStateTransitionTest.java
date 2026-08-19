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
    void secondInfectedPersonIsQueuedWhenTheOnlyScaledBedIsAlreadyOccupied() throws Exception {
        Pathogen pathogen = insertTestPathogen(1.0, 0.0); // always attempts hospitalisation, never dies

        // Collapse to a single district so a tiny population deterministically lands there in full,
        // rather than being split unpredictably across all four seeded districts. Hospital capacity is
        // scaled to the simulated population (see SimulationEngine.scaleHospitalCapacitiesToPopulation),
        // and every district now gets a floor of 1 bed, so a real bed-contention scenario needs at least
        // two patients wanting a bed in the same district on the same day, not merely a "capacity=0" hack.
        DistrictDao districtDao = new DistrictDao();
        List<District> allDistricts = districtDao.findAll();
        District targetDistrict = allDistricts.get(0);
        for (District district : allDistricts) {
            if (!district.getId().equals(targetDistrict.getId())) {
                districtDao.delete(district.getId());
            }
        }

        SimulationConfig config = new SimulationConfig()
                .setRunName("Overwhelmed Hospital Test")
                .setPathogen(pathogen)
                .setTotalDays(30)
                .setPopulationSize(2)
                .setSeedInfections(0)
                .setHealthcareWorkerRatio(0.0)
                .setElderlyRatio(1.0) // fixes severityMultiplier at 3.5, guaranteeing hospitalisationProbability >= 1
                .setRandomSeed(7L);
        SimulationEngine engine = new SimulationEngine(config);
        engine.start();

        List<Person> people = engine.getPopulation();
        assertEquals(2, people.size());
        for (Person person : people) {
            person.setHealthState(HealthState.INFECTED);
            person.setDaysInCurrentState(0);
        }

        engine.stepOneDay();

        long hospitalised = people.stream().filter(p -> p.getHealthState() == HealthState.HOSPITALISED).count();
        long stillInfected = people.stream().filter(p -> p.getHealthState() == HealthState.INFECTED).count();

        assertEquals(1, hospitalised, "Only one scaled bed is available, so only one patient should be admitted");
        assertEquals(1, stillInfected, "The second patient has nowhere to go and should remain INFECTED, queued for a bed");
        assertEquals(1, engine.getAdmissionQueueSize());

        // A district that is completely full (occupied == capacity, not merely occupied > capacity)
        // must be reflected as over-capacity in the persisted daily record — this is what
        // OutbreakAnalyser.daysHospitalOverCapacity() and v_run_summary.days_over_capacity both count.
        DailyRecord today = engine.getHistory().get(engine.getHistory().size() - 1);
        assertTrue(today.isOverCapacity(),
                "A fully occupied district (1/1 beds) must be recorded as over capacity");
        assertEquals(1, OutbreakAnalyser.daysHospitalOverCapacity(engine.getHistory()));
    }

    @Test
    void covid19LikeOutbreakProducesAMajorAttackRateWithNoInterventions() throws Exception {
        // Uses the seeded "COVID-19 (Delta-like)" pathogen from schema.sql directly (pathogen.name is
        // UNIQUE, so it can't be re-inserted) — R0=5.1 is well above the epidemic threshold of 1, so a
        // run with no interventions must produce a major outbreak. This regression test would have
        // caught the force-of-infection bug where lambda was missing the AVERAGE_DAILY_CONTACTS factor:
        // R-effective collapsed to ~R0/contacts (~0.4), and the outbreak died out almost immediately
        // instead of taking off.
        Pathogen covid = new PathogenDao().findAll().stream()
                .filter(p -> p.getName().equals("COVID-19 (Delta-like)"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seeded COVID-19 pathogen not found"));

        SimulationConfig config = new SimulationConfig()
                .setRunName("COVID-19 Attack Rate Regression")
                .setPathogen(covid)
                .setTotalDays(120)
                .setPopulationSize(2000)
                .setSeedInfections(3)
                .setHealthcareWorkerRatio(0.05)
                .setElderlyRatio(0.12)
                .setRandomSeed(2024L);
        SimulationEngine engine = new SimulationEngine(config);

        engine.runAll();

        double attackRate = OutbreakAnalyser.attackRate(engine.getHistory(), config.getPopulationSize());
        assertTrue(attackRate > 0.5,
                "Expected a major outbreak (attack rate > 50%) for an R0=5.1 pathogen with no interventions, "
                        + "but got " + (attackRate * 100) + "%");
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
